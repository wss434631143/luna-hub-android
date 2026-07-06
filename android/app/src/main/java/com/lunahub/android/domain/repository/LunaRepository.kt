package com.lunahub.android.domain.repository

import com.lunahub.android.domain.model.AppSettings
import com.lunahub.android.domain.model.CameraDevice
import com.lunahub.android.domain.model.CameraMedia
import com.lunahub.android.domain.model.DataSourceMode
import kotlinx.coroutines.flow.Flow

interface LunaRepository {
    val cameraDevice: Flow<CameraDevice>
    val media: Flow<List<CameraMedia>>
    val settings: Flow<AppSettings>

    suspend fun scanCamera(): CameraDevice
    suspend fun connectCamera(): CameraDevice
    suspend fun getMedia(mediaId: String): CameraMedia?
    suspend fun markMediaDownloaded(mediaId: String, localPath: String)
    suspend fun clearCache()
    suspend fun setDataSourceMode(mode: DataSourceMode)
}
