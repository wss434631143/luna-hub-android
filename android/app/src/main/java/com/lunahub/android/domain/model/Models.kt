package com.lunahub.android.domain.model

enum class ConnectionStatus {
    Disconnected,
    Connecting,
    Connected,
    Failed,
}

enum class MediaType {
    Photo,
    Video,
}

enum class DownloadStatus {
    Queued,
    Downloading,
    Success,
    Failed,
}

enum class ThemeMode {
    System,
    Light,
    Dark,
}

enum class DataSourceMode {
    Mock,
    Real,
}

enum class WatermarkPosition {
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight,
    BottomCenter,
}

data class CameraDevice(
    val id: String,
    val name: String,
    val ipAddress: String,
    val connectionStatus: ConnectionStatus,
    val firmwareVersion: String?,
    val batteryLevel: Int?,
    val storageTotal: Long?,
    val storageUsed: Long?,
)

data class CameraMedia(
    val id: String,
    val fileName: String,
    val filePath: String,
    val thumbnailUrl: String?,
    val mediaUrl: String,
    val mediaType: MediaType,
    val fileSize: Long,
    val duration: Long?,
    val width: Int?,
    val height: Int?,
    val createdAt: Long,
    val isDownloaded: Boolean,
    val localPath: String?,
)

data class DownloadTask(
    val id: String,
    val mediaId: String,
    val fileName: String,
    val progress: Float,
    val status: DownloadStatus,
    val speed: Long,
    val errorMessage: String?,
    val localPath: String?,
)

data class WatermarkConfig(
    val enabled: Boolean,
    val style: String,
    val position: WatermarkPosition,
    val size: Float,
    val opacity: Float,
    val text: String?,
)

data class AppSettings(
    val themeMode: ThemeMode,
    val downloadWifiOnly: Boolean,
    val defaultDownloadFolder: String,
    val watermarkEnabled: Boolean,
    val cacheSize: Long,
    val dataSourceMode: DataSourceMode = DataSourceMode.Mock,
    val cameraHost: String = "192.168.42.1",
    val cameraPath: String = "/storage_internal/DCIM/",
)

enum class MediaFilter {
    All,
    Photo,
    Video,
}
