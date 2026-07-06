package com.lunahub.android.data.mapper

import com.lunahub.android.data.remote.LunaRemoteMedia
import com.lunahub.android.domain.model.CameraMedia
import javax.inject.Inject

class CameraMediaMapper @Inject constructor() {
    fun fromRemote(remote: LunaRemoteMedia): CameraMedia {
        return CameraMedia(
            id = remote.id,
            fileName = remote.fileName,
            filePath = remote.filePath,
            thumbnailUrl = remote.thumbnailUrl,
            mediaUrl = remote.mediaUrl,
            mediaType = remote.mediaType,
            fileSize = remote.fileSize,
            duration = null,
            width = null,
            height = null,
            createdAt = remote.createdAt,
            isDownloaded = false,
            localPath = null,
        )
    }
}
