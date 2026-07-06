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

            val auth = runCatching {
                val response = session.sendMessage(CODE_CHECK_AUTHORIZATION)
                inferAuthorization(response.body)
            }.getOrNull()

            val paths = linkedSetOf<String>()
            for (selector in listOf(2, 3)) {
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
                authorized = auth,
                files = paths.map { pathToRemoteMedia(normalizedHost, it) },
                summary = buildString {
                    append("TCP 6666 可连接")
                    append(if (auth == true) "，授权已通过" else if (auth == false) "，可能需要在相机上确认授权" else "，授权状态未确认")
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

        fun sendFile(code: Int, body: ByteArray, timeoutMs: Int = 5000): TcpResponse {
            val request = requestId.getAndIncrement()
            write(fileCommand(code, request, body))
            return readUntil(request, TYPE_FILE, timeoutMs)
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

        private fun fileCommand(code: Int, requestId: Int, body: ByteArray): ByteArray {
            val raw = ByteBuffer.allocate(9 + body.size).order(ByteOrder.LITTLE_ENDIAN)
            raw.putShort(code.toShort())
            raw.put(0x02)
            raw.putShort(requestId.toShort())
            raw.putInt(0x8000)
            raw.put(body)
            val length = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(raw.array().size).array()
            val frame = ucd2(TYPE_FILE, length + raw.array())
            return frame + checksumTrailer(frame)
        }

        private fun ucd2(type: Int, payload: ByteArray): ByteArray {
            return byteArrayOf(0x55, 0x43, 0x44, 0x32, 0x01, 0x0c, type.toByte(), nextSeq().toByte()) + payload
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
        const val PAGE_SIZE = 100
        const val TYPE_MSG = 0x03
        const val TYPE_FILE = 0x04
        const val TYPE_STREAM = 0x05
        const val CODE_CHECK_AUTHORIZATION = 39
        const val CODE_GET_FILE_LIST = 13
        const val PACKET_CHECKSUM_POLY = 0x04c11db7L
    }
}
