package com.lunahub.android.data.repository

import com.lunahub.android.core.database.DownloadTaskDao
import com.lunahub.android.core.database.DownloadTaskEntity
import com.lunahub.android.core.download.DownloadEvent
import com.lunahub.android.core.download.LunaDownloadManager
import com.lunahub.android.data.mapper.DownloadTaskMapper
import com.lunahub.android.domain.model.CameraMedia
import com.lunahub.android.domain.model.DownloadStatus
import com.lunahub.android.domain.model.DownloadTask
import com.lunahub.android.domain.model.MediaType
import com.lunahub.android.domain.repository.DownloadRepository
import com.lunahub.android.domain.repository.LunaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultDownloadRepository @Inject constructor(
    private val downloadTaskDao: DownloadTaskDao,
    private val downloadManager: LunaDownloadManager,
    private val downloadTaskMapper: DownloadTaskMapper,
    private val lunaRepository: LunaRepository,
) : DownloadRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, kotlinx.coroutines.Job>()
    private val semaphore = Semaphore(permits = 2)

    override val downloads: Flow<List<DownloadTask>> = downloadTaskDao.observeAll()
        .map { entities -> entities.map(downloadTaskMapper::fromEntity) }

    override suspend fun startDownload(mediaId: String) {
        val media = lunaRepository.getMedia(mediaId) ?: return
        enqueue(media)
    }

    override suspend fun startDownloads(mediaIds: Collection<String>) {
        mediaIds.forEach { startDownload(it) }
    }

    override suspend fun retryDownload(taskId: String) {
        val existing = downloadTaskDao.getTask(taskId) ?: return
        enqueue(existing.toCameraMedia())
    }

    override suspend fun cancelDownload(taskId: String) {
        activeJobs.remove(taskId)?.cancel()
        downloadTaskDao.updateState(
            taskId = taskId,
            progress = downloadTaskDao.getTask(taskId)?.progress ?: 0f,
            status = DownloadStatus.Canceled.name,
            speed = 0,
            errorMessage = "下载已取消",
            localPath = downloadTaskDao.getTask(taskId)?.localPath,
            updatedAt = System.currentTimeMillis(),
        )
    }

    private suspend fun enqueue(media: CameraMedia) {
        val now = System.currentTimeMillis()
        val taskId = taskIdFor(media.id)
        val existing = downloadTaskDao.getTask(taskId)
        if (existing?.status == DownloadStatus.Downloading.name || activeJobs.containsKey(taskId)) return

        downloadTaskDao.upsert(
            DownloadTaskEntity(
                id = taskId,
                mediaId = media.id,
                fileName = media.fileName,
                mediaUrl = media.mediaUrl,
                fileSize = media.fileSize,
                progress = if (existing?.status == DownloadStatus.Success.name) existing.progress else 0f,
                status = DownloadStatus.Queued.name,
                speed = 0,
                errorMessage = null,
                localPath = existing?.localPath,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            ),
        )

        val job = scope.launch {
            semaphore.withPermit {
                runDownload(taskId, media)
            }
        }
        activeJobs[taskId] = job
        job.invokeOnCompletion { activeJobs.remove(taskId) }
    }

    private suspend fun runDownload(taskId: String, media: CameraMedia) {
        try {
            downloadTaskDao.updateState(taskId, 0f, DownloadStatus.Downloading.name, 0, null, null, System.currentTimeMillis())
            downloadManager.download(media).collect { event ->
                when (event) {
                    DownloadEvent.Queued -> downloadTaskDao.updateState(
                        taskId,
                        0f,
                        DownloadStatus.Queued.name,
                        0,
                        null,
                        null,
                        System.currentTimeMillis(),
                    )
                    is DownloadEvent.Progress -> downloadTaskDao.updateState(
                        taskId,
                        event.progress,
                        DownloadStatus.Downloading.name,
                        event.speed,
                        null,
                        null,
                        System.currentTimeMillis(),
                    )
                    is DownloadEvent.Success -> {
                        downloadTaskDao.updateState(
                            taskId,
                            1f,
                            DownloadStatus.Success.name,
                            0,
                            null,
                            event.localPath,
                            System.currentTimeMillis(),
                        )
                        lunaRepository.markMediaDownloaded(media.id, event.localPath)
                    }
                    is DownloadEvent.Failed -> markFailed(taskId, event.message)
                }
            }
        } catch (canceled: CancellationException) {
            downloadTaskDao.updateState(
                taskId,
                downloadTaskDao.getTask(taskId)?.progress ?: 0f,
                DownloadStatus.Canceled.name,
                0,
                "下载已取消",
                downloadTaskDao.getTask(taskId)?.localPath,
                System.currentTimeMillis(),
            )
            throw canceled
        } catch (error: Throwable) {
            markFailed(taskId, humanReadableError(error))
        }
    }

    private suspend fun markFailed(taskId: String, message: String) {
        downloadTaskDao.updateState(
            taskId = taskId,
            progress = downloadTaskDao.getTask(taskId)?.progress ?: 0f,
            status = DownloadStatus.Failed.name,
            speed = 0,
            errorMessage = message,
            localPath = downloadTaskDao.getTask(taskId)?.localPath,
            updatedAt = System.currentTimeMillis(),
        )
    }

    private fun humanReadableError(error: Throwable): String {
        val raw = error.message.orEmpty()
        return when {
            raw.contains("timeout", ignoreCase = true) -> "下载超时，请确认相机 Wi-Fi 连接稳定"
            raw.contains("failed to connect", ignoreCase = true) -> "相机连接已断开，请重新连接相机 Wi-Fi"
            raw.contains("HTTP", ignoreCase = true) -> raw
            else -> raw.ifBlank { "下载失败，请检查相机连接后重试" }
        }
    }

    private fun taskIdFor(mediaId: String): String = "download:$mediaId"

    private fun DownloadTaskEntity.toCameraMedia(): CameraMedia {
        return CameraMedia(
            id = mediaId,
            fileName = fileName,
            filePath = mediaUrl,
            thumbnailUrl = null,
            mediaUrl = mediaUrl,
            mediaType = if (fileName.endsWith(".mp4", true) || fileName.endsWith(".mov", true)) MediaType.Video else MediaType.Photo,
            fileSize = fileSize,
            duration = null,
            width = null,
            height = null,
            createdAt = createdAt,
            isDownloaded = false,
            localPath = localPath,
        )
    }
}
