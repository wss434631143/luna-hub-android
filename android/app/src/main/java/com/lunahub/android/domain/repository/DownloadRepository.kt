package com.lunahub.android.domain.repository

import com.lunahub.android.domain.model.DownloadTask
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    val downloads: Flow<List<DownloadTask>>

    suspend fun startDownload(mediaId: String)
    suspend fun startDownloads(mediaIds: Collection<String>)
    suspend fun retryDownload(taskId: String)
    suspend fun cancelDownload(taskId: String)
}
