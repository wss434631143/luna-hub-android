package com.lunahub.android.feature.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunahub.android.domain.model.CameraDevice
import com.lunahub.android.domain.model.ConnectionStatus
import com.lunahub.android.domain.repository.LunaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CameraConnectUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val device: CameraDevice? = null,
    val wifiHint: String = "当前 Wi-Fi 可能不是相机热点，第一版使用 mock 连接。",
)

@HiltViewModel
class CameraConnectViewModel @Inject constructor(
    private val repository: LunaRepository,
) : ViewModel() {
    private val transient = MutableStateFlow(CameraConnectUiState())

    val uiState: StateFlow<CameraConnectUiState> = combine(repository.cameraDevice, transient) { device, state ->
        state.copy(device = device)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CameraConnectUiState(isLoading = true))

    fun scanCamera() {
        viewModelScope.launch {
            transient.update { it.copy(isLoading = true, errorMessage = null, wifiHint = "正在扫描 Luna / Insta360 相机热点...") }
            runCatching { repository.scanCamera() }
                .onSuccess { device ->
                    transient.update {
                        it.copy(
                            isLoading = false,
                            wifiHint = if (device.connectionStatus == ConnectionStatus.Connected) "已发现 ${device.name}，mock 连接成功。" else "未发现相机",
                        )
                    }
                }
                .onFailure { error ->
                    transient.update { it.copy(isLoading = false, errorMessage = error.message ?: "扫描失败") }
                }
        }
    }
}
