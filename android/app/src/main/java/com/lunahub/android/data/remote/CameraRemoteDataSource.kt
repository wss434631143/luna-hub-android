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
)

@Singleton
class CameraRemoteDataSource @Inject constructor(
    private val service: CameraHttpService,
    private val parser: LunaIndexParser,
    private val okHttpClient: OkHttpClient,
) {
    suspend fun checkStatus(host: String, cameraPath: String): RemoteCameraStatus {
        val normalizedHost = normalizeHost(host)
        return withContext(Dispatchers.IO) {
            val httpOk = runCatching { requestDirectory(directoryUrl(normalizedHost, cameraPath)) }.isSuccess
            val controlOk = runCatching { checkPort(normalizedHost, 6666) }.getOrDefault(false)
            RemoteCameraStatus(
                httpOk = httpOk,
                controlOk = controlOk,
                message = when {
                    httpOk && controlOk -> "已检测到 Luna 相机"
                    httpOk -> "已检测到媒体服务，控制端口暂不可用"
                    else -> "未检测到相机媒体服务"
                },
            )
        }
    }

    suspend fun listMedia(host: String, cameraPath: String): List<LunaRemoteMedia> {
        val normalizedHost = normalizeHost(host)
        val rootUrl = directoryUrl(normalizedHost, cameraPath)
        val rootHtml = requestDirectoryWithRetry(rootUrl)
        val cameraDirs = parser.extractCameraSubdirs(rootHtml)
        return if (cameraDirs.isEmpty()) {
            parser.parse(rootHtml, rootUrl)
        } else {
            cameraDirs.flatMap { dir ->
                val childUrl = directoryUrl(normalizedHost, "${normalizePath(cameraPath)}$dir/")
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
