package com.lunahub.android.data.remote

import com.lunahub.android.domain.model.MediaType

data class LunaRemoteMedia(
    val id: String,
    val fileName: String,
    val filePath: String,
    val mediaUrl: String,
    val thumbnailUrl: String?,
    val mediaType: MediaType,
    val fileSize: Long,
    val createdAt: Long,
    val previewName: String?,
    val previewUrl: String?,
)
