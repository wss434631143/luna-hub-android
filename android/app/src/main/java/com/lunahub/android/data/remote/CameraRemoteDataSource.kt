package com.lunahub.android.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

data class RemoteCameraStatus(
    val httpOk: Boolean,
    val controlOk: Boolean,
    val message: String,
    val mediaPath: String? = null,
    val tcpFiles: Int = 0,
)

@Singleton
class CameraRemoteDataSource @Inject constructor(
    private val service: CameraHttpService,
    private val parser: LunaIndexParser,
    private val okHttpClient: OkHttpClient,
    private val tcpClient: Insta360TcpClient,
) {
    suspend fun checkStatus(host: String, cameraPath: String): RemoteCameraStatus {
        val normalizedHost = normalizeHost(host)
        return withContext(Dispatchers.IO) {
            val httpResult = runCatching { requestDirectory(directoryUrl(normalizedHost, cameraPath)) }
            val tcpProbe = runCatching { tcpClient.probe(normalizedHost) }.getOrNull()
            val httpOk = httpResult.isSuccess
            val controlOk = tcpProbe?.connected ?: runCatching { checkPort(normalizedHost, 6666) }.getOrDefault(false)
            val tcpFiles = tcpProbe?.files?.size ?: 0
            RemoteCameraStatus(
                httpOk = httpOk,
                controlOk = controlOk,
                tcpFiles = tcpFiles,
                mediaPath = if (httpOk) cameraPath else null,
                message = when {
                    httpOk && controlOk -> "已检测到 Luna 相机，HTTP 媒体服务和 TCP 控制通道可用"
                    httpOk -> "已检测到 HTTP 媒体服务，TCP 控制端口暂不可用"
                    controlOk && tcpFiles > 0 -> "已检测到相机 TCP 控制通道，并读取到 $tcpFiles 个媒体路径"
                    controlOk -> "已检测到相机 TCP 控制通道，但未读取到媒体路径。可能需要在相机屏幕上确认授权。"
                    else -> "未检测到相机媒体服务"
                },
            )
        }
    }

    suspend fun listMedia(host: String, cameraPath: String): List<LunaRemoteMedia> {
        val normalizedHost = normalizeHost(host)
        return runCatching { listMediaFromHttp(normalizedHost, cameraPath) }
            .getOrElse { httpError ->
                val tcp = tcpClient.probe(normalizedHost)
                if (tcp.files.isNotEmpty()) {
                    tcp.files
                } else {
                    throw RemoteCameraException("HTTP 目录读取失败，TCP 也没有返回媒体路径：${httpError.message}. ${tcp.summary}", httpError)
                }
            }
    }

    private suspend fun listMediaFromHttp(host: String, cameraPath: String): List<LunaRemoteMedia> {
        val rootUrl = directoryUrl(host, cameraPath)
        val rootHtml = requestDirectoryWithRetry(rootUrl)
        val cameraDirs = parser.extractCameraSubdirs(rootHtml)
        return if (cameraDirs.isEmpty()) {
            parser.parse(rootHtml, rootUrl)
        } else {
            cameraDirs.flatMap { dir ->
                val childUrl = directoryUrl(host, "${normalizePath(cameraPath)}$dir/")
                runCatching { parser.parse(requestDirectoryWithRetry(childUrl), childUrl) }.getOrDefault(emptyList())
            }.sortedByDescending { it.createdAt }
        }
    }

    private suspend fun requestDirectoryWithRetry(url: String): String {
        var lastError: Throwable? = null
        repeat(4) { attempt ->
            if (attempt > 0) delay(if (attempt == 1) 350 else 650)
            try {
                return requestDirectory(url)
            } catch (error: Throwable) {
                lastError = error
            }
        }
        throw RemoteCameraException("读取相机媒体列表失败：${lastError?.message ?: "未知错误"}", lastError)
    }

    private suspend fun requestDirectory(url: String): String {
        val response = service.getDirectory(url)
        if (!response.isSuccessful) {
            throw RemoteCameraException("HTTP ${response.code()}")
        }
        return response.body()?.string() ?: throw RemoteCameraException("相机返回空内容")
    }

    private fun checkPort(host: String, port: Int): Boolean {
        return runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host.substringBefore(':'), port), 1500)
            }
        }.isSuccess
    }

    private fun normalizeHost(host: String): String = host.removePrefix("http://").removePrefix("https://").trimEnd('/')

    private fun normalizePath(path: String): String {
        val withLeadingSlash = if (path.startsWith("/")) path else "/$path"
        return if (withLeadingSlash.endsWith("/")) withLeadingSlash else "$withLeadingSlash/"
    }

    private fun directoryUrl(host: String, path: String): String = "http://$host${normalizePath(path)}"

    @Suppress("unused")
    private fun callFactory(): OkHttpClient = okHttpClient
}
