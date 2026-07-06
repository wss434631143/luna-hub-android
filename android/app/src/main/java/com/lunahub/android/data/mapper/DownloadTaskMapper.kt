package com.lunahub.android.data.mapper

import com.lunahub.android.core.database.DownloadTaskEntity
import com.lunahub.android.domain.model.DownloadStatus
import com.lunahub.android.domain.model.DownloadTask
import javax.inject.Inject

class DownloadTaskMapper @Inject constructor() {
    fun fromEntity(entity: DownloadTaskEntity): DownloadTask {
        return DownloadTask(
            id = entity.id,
            mediaId = entity.mediaId,
            fileName = entity.fileName,
            progress = entity.progress,
            status = runCatching { DownloadStatus.valueOf(entity.status) }.getOrDefault(DownloadStatus.Failed),
            speed = entity.speed,
            errorMessage = entity.errorMessage,
            localPath = entity.localPath,
        )
    }
}
