package com.lunahub.android.feature.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunahub.android.domain.model.DownloadTask
import com.lunahub.android.domain.repository.LunaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
class DownloadViewModel @Inject constructor(repository: LunaRepository) : ViewModel() {
    val uiState: StateFlow<DownloadUiState> = repository.downloads
        .map { DownloadUiState(tasks = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadUiState(isLoading = true))
}
