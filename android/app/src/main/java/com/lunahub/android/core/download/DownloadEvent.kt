package com.lunahub.android.core.download

sealed interface DownloadEvent {
    data object Queued : DownloadEvent
    data class Progress(val progress: Float, val speed: Long) : DownloadEvent
    data class Success(val localPath: String) : DownloadEvent
    data class Failed(val message: String) : DownloadEvent
}
