package com.lunahub.android.domain.usecase

import com.lunahub.android.domain.model.CameraDevice
import com.lunahub.android.domain.model.CameraMedia
import com.lunahub.android.domain.model.DataSourceMode
import com.lunahub.android.domain.repository.LunaRepository
import javax.inject.Inject

class ObserveCameraDeviceUseCase @Inject constructor(
    private val repository: LunaRepository,
) {
    operator fun invoke() = repository.cameraDevice
}

class ObserveCameraMediaUseCase @Inject constructor(
    private val repository: LunaRepository,
) {
    operator fun invoke() = repository.media
}

class ObserveDownloadsUseCase @Inject constructor(
    private val repository: LunaRepository,
) {
    operator fun invoke() = repository.downloads
}

class ObserveSettingsUseCase @Inject constructor(
    private val repository: LunaRepository,
) {
    operator fun invoke() = repository.settings
}

class ConnectCameraUseCase @Inject constructor(
    private val repository: LunaRepository,
) {
    suspend operator fun invoke(): CameraDevice = repository.connectCamera()
}

class GetMediaUseCase @Inject constructor(
    private val repository: LunaRepository,
) {
    suspend operator fun invoke(mediaId: String): CameraMedia? = repository.getMedia(mediaId)
}

class StartDownloadUseCase @Inject constructor(
    private val repository: LunaRepository,
) {
    suspend operator fun invoke(mediaId: String) = repository.startMockDownload(mediaId)
}

class ClearCacheUseCase @Inject constructor(
    private val repository: LunaRepository,
) {
    suspend operator fun invoke() = repository.clearCache()
}

class SetDataSourceModeUseCase @Inject constructor(
    private val repository: LunaRepository,
) {
    suspend operator fun invoke(mode: DataSourceMode) = repository.setDataSourceMode(mode)
}
