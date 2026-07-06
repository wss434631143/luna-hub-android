package com.lunahub.android.data.remote

import com.lunahub.android.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

data class TcpProbeResult(
    val connected: Boolean,
    val authorized: Boolean?,
    val authorizationRequested: Boolean,
    val files: List<LunaRemoteMedia>,
    val summary: String,
)

@Singleton
class Insta360TcpClient @Inject constructor() {
    suspend fun probe(host: String, port: Int = CONTROL_PORT): TcpProbeResult = withContext(Dispatchers.IO) {
        val normalizedHost = host.substringBefore(':')
        Socket().use { socket ->
            socket.connect(InetSocketAddress(normalizedHost, port), CONNECT_TIMEOUT_MS)
            socket.soTimeout = READ_TIMEOUT_MS

            val session = TcpSession(socket)
            session.write(session.streamHello())
            session.readAvailable()
            session.bootstrapLikeDesktop()

            val auth = runCatching {
                val response = session.sendMessage(CODE_CHECK_AUTHORIZATION)
                inferAuthorization(response.body)
            }.getOrNull()
            var finalAuth = auth
            var requestedAuthorization = false

            if (finalAuth != true) {
                requestedAuthorization = true
                runCatching {
                    session.sendNotify(CODE_PHONE_INFO)
                    val response = session.sendMessage(CODE_REQUEST_AUTHORIZATION, timeoutMs = AUTH_TIMEOUT_MS)
                    finalAuth = inferAuthorization(response.body) ?: finalAuth
                }
            }

            val paths = linkedSetOf<String>()
            for (selector in listOf(2, 3)) {
                for (command in desktopFileListCommands(selector)) {
                    val response = runCatching {
                        session.sendExactFile(command.seq, command.requestId, CODE_GET_FILE_LIST, command.body, timeoutMs = 8000)
                    }.getOrNull() ?: continue
                    val pagePaths = extractMediaPaths(response.body)
                    paths.addAll(pagePaths)
                    if (pagePaths.size < PAGE_SIZE) break
                }
                if (paths.isNotEmpty()) continue

                var offset = 0
                while (offset <= 500) {
                    val response = runCatching {
                        session.sendFile(CODE_GET_FILE_LIST, fileListBody(selector, offset), timeoutMs = 7000)
                    }.getOrNull() ?: break
                    val pagePaths = extractMediaPaths(response.body)
                    paths.addAll(pagePaths)
                    if (pagePaths.size < PAGE_SIZE) break
                    offset += PAGE_SIZE
                }
            }

            TcpProbeResult(
                connected = true,
                authorized = finalAuth,
                authorizationRequested = requestedAuthorization,
                files = paths.map { pathToRemoteMedia(normalizedHost, it) },
                summary = buildString {
                    append("TCP 6666 可连接")
                    append(
                        when {
                            finalAuth == true -> "，授权已通过"
                            requestedAuthorization -> "，已请求授权，请在相机屏幕上确认后重试"
                            finalAuth == false -> "，可能需要在相机上确认授权"
                            else -> "，授权状态未确认"
                        },
                    )
                    append("，发现 ${paths.size} 个媒体路径")
                },
            )
        }
    }

    private fun inferAuthorization(body: ByteArray): Boolean? {
        if (body.isEmpty()) return null
        val hasOne = body.any { it.toInt() == 1 }
        val hasZero = body.any { it.toInt() == 0 }
        return when {
            hasOne && !hasZero -> true
            hasZero -> false
            else -> null
        }
    }

    private data class ExactFileCommand(val seq: Int, val requestId: Int, val body: ByteArray)

    private fun desktopFileListCommands(selector: Int): List<ExactFileCommand> {
        return if (selector == 2) {
            listOf(
                ExactFileCommand(seq = 0x2c, requestId = 8, body = fileListBody(2, 0)),
                ExactFileCommand(seq = 0x2d, requestId = 9, body = fileListBody(2, 100)),
                ExactFileCommand(seq = 0x2e, requestId = 10, body = fileListBody(2, 200)),
            )
        } else {
            listOf(ExactFileCommand(seq = 0x2f, requestId = 11, body = fileListBody(3, 0)))
        }
    }

    private fun fileListBody(storageSelector: Int, offset: Int): ByteArray {
        val out = ByteArrayOutputStream()
        out.writeVarintField(1, storageSelector)
        if (offset > 0) out.writeVarintField(2, offset)
        out.writeVarintField(3, PAGE_SIZE)
        out.writeVarintField(4, 2)
        return out.toByteArray()
    }

    private fun extractMediaPaths(body: ByteArray): List<String> {
        val text = body.toString(Charsets.ISO_8859_1)
        val regex = Regex(
            """/(?:storage_internal|sdcard|DCIM)[^\u0000\n\r"'<>\s]+?\.(?:mp4|mov|lrv|jpg|jpeg|dng|insp|png|webp)""",
            RegexOption.IGNORE_CASE,
        )
        return regex.findAll(text).map { it.value }.distinct().toList()
    }

    private fun pathToRemoteMedia(host: String, path: String): LunaRemoteMedia {
        val fileName = path.substringAfterLast('/').ifBlank { path }
        val type = when (fileName.substringAfterLast('.', "").lowercase(Locale.US)) {
            "jpg", "jpeg", "png", "dng", "insp", "webp" -> MediaType.Photo
            else -> MediaType.Video
        }
        val url = "http://$host$path"
        return LunaRemoteMedia(
            id = url,
            fileName = fileName,
            filePath = path,
            mediaUrl = url,
            thumbnailUrl = if (type == MediaType.Photo) url else null,
            mediaType = type,
            fileSize = 0L,
            createdAt = capturedAtFromName(fileName) ?: System.currentTimeMillis(),
            previewName = null,
            previewUrl = null,
        )
    }

    private fun capturedAtFromName(name: String): Long? {
        val match = Regex("""(?:VID|LRV|IMG|LIV|PIC|PANO)_(\d{4})(\d{2})(\d{2})_(\d{2})(\d{2})(\d{2})""", RegexOption.IGNORE_CASE)
            .find(name) ?: return null
        val calendar = java.util.Calendar.getInstance()
        calendar.set(match.groupValues[1].toInt(), match.groupValues[2].toInt() - 1, match.groupValues[3].toInt(), match.groupValues[4].toInt(), match.groupValues[5].toInt(), match.groupValues[6].toInt())
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private inner class TcpSession(private val socket: Socket) {
        private var seq = 0x24
        private val requestId = AtomicInteger(1)

        fun streamHello(): ByteArray {
            return ucd2(TYPE_STREAM, byteArrayOf(0, 0, 0, 0, 0xf6.toByte(), 0xcc.toByte(), 0x4f, 0x09))
        }

        fun sendMessage(code: Int, body: ByteArray = ByteArray(0), timeoutMs: Int = 3000): TcpResponse {
            val request = requestId.getAndIncrement()
            write(ucd2(TYPE_MSG, messageEnvelope(request, code, body)))
            return readUntil(request, TYPE_MSG, timeoutMs)
        }

        fun sendNotify(code: Int, body: ByteArray = ByteArray(0)) {
            write(ucd2(TYPE_MSG, messageEnvelope(0, code, body)))
        }

        fun sendFile(code: Int, body: ByteArray, timeoutMs: Int = 5000): TcpResponse {
            val request = requestId.getAndIncrement()
            write(fileCommand(code, request, body))
            return readUntil(request, TYPE_FILE, timeoutMs)
        }

        fun sendExactFile(seq: Int, requestId: Int, code: Int, body: ByteArray, timeoutMs: Int = 5000): TcpResponse {
            write(fileCommand(code, requestId, body, forcedSeq = seq))
            this.requestId.set(maxOf(this.requestId.get(), requestId + 1))
            return readUntil(requestId, TYPE_FILE, timeoutMs)
        }

        fun bootstrapLikeDesktop() {
            runCatching {
                sendExactFile(
                    seq = 0x25,
                    requestId = 1,
                    code = CODE_GET_OPTIONS,
                    body = getOptionsSmallBody(),
                    timeoutMs = 4000,
                )
            }
            runCatching {
                sendExactFile(
                    seq = 0x26,
                    requestId = 2,
                    code = CODE_GET_CURRENT_CAPTURE_STATUS,
                    body = ByteArray(0),
                    timeoutMs = 4000,
                )
            }
            runCatching {
                sendExactFile(
                    seq = 0x27,
                    requestId = 3,
                    code = CODE_GET_OPTIONS,
                    body = getOptionsLargeBody(),
                    timeoutMs = 4000,
                )
            }
            requestId.set(maxOf(requestId.get(), 12))
        }

        fun write(data: ByteArray) {
            socket.getOutputStream().write(data)
            socket.getOutputStream().flush()
        }

        fun readAvailable() {
            socket.soTimeout = 500
            runCatching {
                val buffer = ByteArray(512)
                socket.getInputStream().read(buffer)
            }
            socket.soTimeout = READ_TIMEOUT_MS
        }

        private fun readUntil(requestId: Int, expectedType: Int, timeoutMs: Int): TcpResponse {
            val deadline = System.currentTimeMillis() + timeoutMs
            val buffer = ByteArrayOutputStream()
            while (System.currentTimeMillis() < deadline) {
                val chunk = ByteArray(4096)
                val read = socket.getInputStream().read(chunk)
                if (read <= 0) continue
                buffer.write(chunk, 0, read)
                val response = parseFrames(buffer.toByteArray(), requestId, expectedType)
                if (response != null) return response
            }
            throw java.net.SocketTimeoutException("TCP 命令 $requestId 超时")
        }

        private fun fileCommand(code: Int, requestId: Int, body: ByteArray, forcedSeq: Int? = null): ByteArray {
            val raw = ByteBuffer.allocate(9 + body.size).order(ByteOrder.LITTLE_ENDIAN)
            raw.putShort(code.toShort())
            raw.put(0x02)
            raw.putShort(requestId.toShort())
            raw.putInt(0x8000)
            raw.put(body)
            val length = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(raw.array().size).array()
            val frame = ucd2(TYPE_FILE, length + raw.array(), forcedSeq)
            return frame + checksumTrailer(frame)
        }

        private fun ucd2(type: Int, payload: ByteArray, forcedSeq: Int? = null): ByteArray {
            val selectedSeq = forcedSeq ?: nextSeq()
            if (forcedSeq != null) seq = maxOf(seq, (forcedSeq + 1) and 0xff)
            return byteArrayOf(0x55, 0x43, 0x44, 0x32, 0x01, 0x0c, type.toByte(), selectedSeq.toByte()) + payload
        }

        private fun nextSeq(): Int {
            val value = seq and 0xff
            seq = (seq + 1) and 0xff
            return value
        }
    }

    private data class TcpResponse(val requestId: Int, val code: Int, val body: ByteArray)

    private fun parseFrames(bytes: ByteArray, requestId: Int, expectedType: Int): TcpResponse? {
        var offset = bytes.indexOfMagic(0)
        while (offset >= 0 && offset + 8 <= bytes.size) {
            val type = bytes[offset + 6].toInt() and 0xff
            if (type == TYPE_MSG) {
                val next = bytes.indexOfMagic(offset + 8).takeIf { it > offset } ?: bytes.size
                val message = parseMessageEnvelope(bytes.copyOfRange(offset + 8, next))
                if (expectedType == TYPE_MSG && message.requestId == requestId) return message
                offset = bytes.indexOfMagic(next)
            } else if (type == TYPE_FILE && offset + 12 <= bytes.size) {
                val rawLen = ByteBuffer.wrap(bytes, offset + 8, 4).order(ByteOrder.LITTLE_ENDIAN).int
                val end = offset + 12 + rawLen + 4
                if (end > bytes.size) return null
                val rawOffset = offset + 12
                if (rawLen >= 9) {
                    val code = ByteBuffer.wrap(bytes, rawOffset, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xffff
                    val req = ByteBuffer.wrap(bytes, rawOffset + 3, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xffff
                    if (expectedType == TYPE_FILE && req == requestId) {
                        return TcpResponse(req, code, bytes.copyOfRange(rawOffset + 9, rawOffset + rawLen))
                    }
                }
                offset = bytes.indexOfMagic(end)
            } else {
                offset = bytes.indexOfMagic(offset + 1)
            }
        }
        return null
    }

    private fun parseMessageEnvelope(bytes: ByteArray): TcpResponse {
        var offset = 0
        var requestId = 0
        var code = 0
        var body = ByteArray(0)
        while (offset < bytes.size) {
            val tag = readVarint(bytes, offset)
            offset = tag.nextOffset
            val field = tag.value shr 3
            val wireType = tag.value and 7
            if (wireType == 0) {
                val value = readVarint(bytes, offset)
                offset = value.nextOffset
                if (field == 1) requestId = value.value
                if (field == 2) code = value.value
            } else if (wireType == 2) {
                val length = readVarint(bytes, offset)
                offset = length.nextOffset
                body = bytes.copyOfRange(offset, (offset + length.value).coerceAtMost(bytes.size))
                offset += length.value
            } else {
                break
            }
        }
        return TcpResponse(requestId, code, body)
    }

    private data class Varint(val value: Int, val nextOffset: Int)

    private fun readVarint(bytes: ByteArray, start: Int): Varint {
        var offset = start
        var value = 0
        var shift = 0
        while (offset < bytes.size) {
            val byte = bytes[offset++].toInt() and 0xff
            value = value or ((byte and 0x7f) shl shift)
            if ((byte and 0x80) == 0) break
            shift += 7
        }
        return Varint(value, offset)
    }

    private fun messageEnvelope(requestId: Int, code: Int, body: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.writeVarintField(1, requestId)
        out.writeVarintField(2, code)
        out.writeFieldBytes(3, body)
        return out.toByteArray()
    }

    private fun getOptionsSmallBody(): ByteArray {
        val out = ByteArrayOutputStream()
        out.writeVarintField(1, 48)
        out.writeVarintField(1, 15)
        out.writeVarintField(1, 11)
        return out.toByteArray()
    }

    private fun getOptionsLargeBody(): ByteArray {
        return """
            08 01 08 03 08 02 08 4c 08 06 08 4e 08 4f 08 0b 08 55 08 0c
            08 0d 08 af 01 08 0e 08 0f 08 13 08 37 08 11 08 14 08 1e
            08 24 08 6e 08 72 08 75 08 59 08 74 08 73 08 25 08 26
            08 2a 08 28 08 29 08 30 08 31 08 32 08 42 08 84 01 08
            3a 08 3b 08 3c 08 43 08 44 08 5d 08 53 08 52 08 46 08
            58 08 67 08 10 08 61 08 85 01 08 86 01 08 77 08 7a 08
            7b 08 7c 08 80 01 08 81 01 08 87 01 08 96 01 08 95 01
            08 93 01 08 9b 01 08 9d 01 08 9e 01 08 a0 01 08 b3 01
            08 a1 01 08 16 08 50 08 51 08 a7 01 08 a9 01 08 ad 01
            08 b4 01 08 b0 01 08 b1 01 08 78 08 6f 08 79 08 ac 01
        """.replace(Regex("""\s+"""), "")
            .chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    private fun ByteArray.indexOfMagic(start: Int): Int {
        for (i in start..(size - 4)) {
            if (this[i] == 0x55.toByte() && this[i + 1] == 0x43.toByte() && this[i + 2] == 0x44.toByte() && this[i + 3] == 0x32.toByte()) return i
        }
        return -1
    }

    private fun ByteArrayOutputStream.writeVarintField(field: Int, value: Int) {
        writeVarint(field shl 3)
        writeVarint(value)
    }

    private fun ByteArrayOutputStream.writeFieldBytes(field: Int, value: ByteArray) {
        writeVarint((field shl 3) or 2)
        writeVarint(value.size)
        write(value)
    }

    private fun ByteArrayOutputStream.writeVarint(value: Int) {
        var current = value
        while (current > 0x7f) {
            write((current and 0x7f) or 0x80)
            current = current ushr 7
        }
        write(current and 0x7f)
    }

    private fun checksumTrailer(frame: ByteArray): ByteArray {
        val checksum = packetChecksum(frame)
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(checksum.toInt()).array()
    }

    private fun packetChecksum(frame: ByteArray): Long {
        var checksum = 0xffffffffL
        for (byte in frame) {
            checksum = checksum xor (byte.toLong() and 0xff)
            repeat(4) {
                checksum = if ((checksum and 0x80000000L) != 0L) {
                    ((checksum shl 8) xor PACKET_CHECKSUM_POLY) and 0xffffffffL
                } else {
                    (checksum shl 8) and 0xffffffffL
                }
            }
        }
        return checksum and 0xffffffffL
    }

    private companion object {
        const val CONTROL_PORT = 6666
        const val CONNECT_TIMEOUT_MS = 1800
        const val READ_TIMEOUT_MS = 4000
        const val AUTH_TIMEOUT_MS = 30000
        const val PAGE_SIZE = 100
        const val TYPE_MSG = 0x03
        const val TYPE_FILE = 0x04
        const val TYPE_STREAM = 0x05
        const val CODE_GET_OPTIONS = 8
        const val CODE_GET_CURRENT_CAPTURE_STATUS = 15
        const val CODE_CHECK_AUTHORIZATION = 39
        const val CODE_REQUEST_AUTHORIZATION = 86
        const val CODE_PHONE_INFO = 220
        const val CODE_GET_FILE_LIST = 13
        const val PACKET_CHECKSUM_POLY = 0x04c11db7L
    }
}
