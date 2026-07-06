package com.lunahub.android.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunahub.android.domain.model.CameraDevice
import com.lunahub.android.domain.model.CameraMedia
import com.lunahub.android.domain.model.ConnectionStatus
import com.lunahub.android.domain.repository.LunaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val device: CameraDevice? = null,
    val recentMedia: List<CameraMedia> = emptyList(),
) {
    val connected: Boolean = device?.connectionStatus == ConnectionStatus.Connected
}

@HiltViewModel
class HomeViewModel @Inject constructor(repository: LunaRepository) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(repository.cameraDevice, repository.media) { device, media ->
        HomeUiState(device = device, recentMedia = media.sortedByDescending { it.createdAt }.take(4))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(isLoading = true))
}
