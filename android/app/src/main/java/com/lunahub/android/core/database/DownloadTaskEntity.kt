package com.lunahub.android.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey val id: String,
    val mediaId: String,
    val fileName: String,
    val mediaUrl: String,
    val fileSize: Long,
    val progress: Float,
    val status: String,
    val speed: Long,
    val errorMessage: String?,
    val localPath: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
