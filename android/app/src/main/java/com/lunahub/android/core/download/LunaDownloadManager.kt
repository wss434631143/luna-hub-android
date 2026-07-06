package com.lunahub.android.core.download

import android.content.Context
import android.os.Environment
import com.lunahub.android.domain.model.CameraMedia
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LunaDownloadManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    fun download(media: CameraMedia): Flow<DownloadEvent> = flow {
        emit(DownloadEvent.Queued)
        val destination = destinationFile(media.fileName)
        if (media.mediaUrl.isBlank()) {
            repeat(5) { index ->
                delay(140)
                emit(DownloadEvent.Progress((index + 1) / 5f, 4L * 1024 * 1024))
            }
            destination.parentFile?.mkdirs()
            destination.writeText("Mock Luna Hub download for ${media.fileName}\n")
            emit(DownloadEvent.Success(destination.absolutePath))
            return@flow
        }

        val request = Request.Builder().url(media.mediaUrl).get().build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("相机返回 HTTP ${response.code}，请确认相机仍在线")
            }
            val body = response.body ?: throw IOException("相机返回空文件流")
            val totalBytes = body.contentLength().takeIf { it > 0 } ?: media.fileSize
            destination.parentFile?.mkdirs()
            body.byteStream().use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = 0L
                    var bytesSinceLastTick = 0L
                    var lastTick = System.currentTimeMillis()
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        bytesSinceLastTick += read
                        val now = System.currentTimeMillis()
                        if (now - lastTick >= 350 || downloaded == totalBytes) {
                            val elapsed = (now - lastTick).coerceAtLeast(1)
                            val speed = bytesSinceLastTick * 1000 / elapsed
                            val progress = if (totalBytes > 0) downloaded.toFloat() / totalBytes else 0f
                            emit(DownloadEvent.Progress(progress.coerceIn(0f, 0.999f), speed))
                            bytesSinceLastTick = 0L
                            lastTick = now
                        }
                    }
                    output.flush()
                }
            }
        }
        emit(DownloadEvent.Success(destination.absolutePath))
    }.flowOn(Dispatchers.IO)

    private fun destinationFile(fileName: String): File {
        val picturesRoot = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: File(context.filesDir, Environment.DIRECTORY_PICTURES)
        return uniqueFile(File(picturesRoot, "Luna Hub"), fileName)
    }

    private fun uniqueFile(directory: File, fileName: String): File {
        val cleanName = fileName.replace(Regex("""[\\/:*?"<>|]"""), "_")
        var candidate = File(directory, cleanName)
        if (!candidate.exists()) return candidate
        val baseName = cleanName.substringBeforeLast('.', cleanName)
        val extension = cleanName.substringAfterLast('.', "")
        var index = 1
        while (candidate.exists()) {
            val nextName = if (extension.isBlank()) "$baseName-$index" else "$baseName-$index.$extension"
            candidate = File(directory, nextName)
            index += 1
        }
        return candidate
    }
}
