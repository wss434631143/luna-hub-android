package com.lunahub.android.feature.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunahub.android.domain.model.DownloadTask
import com.lunahub.android.domain.usecase.CancelDownloadUseCase
import com.lunahub.android.domain.usecase.ObserveDownloadsUseCase
import com.lunahub.android.domain.usecase.RetryDownloadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DownloadUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val tasks: List<DownloadTask> = emptyList(),
)

@HiltViewModel
class DownloadViewModel @Inject constructor(
    observeDownloads: ObserveDownloadsUseCase,
    private val retryDownload: RetryDownloadUseCase,
    private val cancelDownload: CancelDownloadUseCase,
) : ViewModel() {
    val uiState: StateFlow<DownloadUiState> = observeDownloads()
        .map { DownloadUiState(tasks = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadUiState(isLoading = true))

    fun retry(taskId: String) {
        viewModelScope.launch { retryDownload(taskId) }
    }

    fun cancel(taskId: String) {
        viewModelScope.launch { cancelDownload(taskId) }
    }
}
