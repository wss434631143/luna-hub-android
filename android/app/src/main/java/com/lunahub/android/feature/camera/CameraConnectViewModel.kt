package com.lunahub.android.feature.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunahub.android.domain.model.CameraDevice
import com.lunahub.android.domain.model.ConnectionStatus
import com.lunahub.android.domain.model.DataSourceMode
import com.lunahub.android.domain.usecase.ConnectCameraUseCase
import com.lunahub.android.domain.usecase.ObserveCameraDeviceUseCase
import com.lunahub.android.domain.usecase.ObserveSettingsUseCase
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
    val mode: DataSourceMode = DataSourceMode.Mock,
    val cameraHost: String = "192.168.42.1",
    val cameraPath: String = "/storage_internal/DCIM/",
    val wifiHint: String = "",
)

@HiltViewModel
class CameraConnectViewModel @Inject constructor(
    private val connectCamera: ConnectCameraUseCase,
    observeCameraDevice: ObserveCameraDeviceUseCase,
    observeSettings: ObserveSettingsUseCase,
) : ViewModel() {
    private val transient = MutableStateFlow(CameraConnectUiState())

    val uiState: StateFlow<CameraConnectUiState> = combine(
        observeCameraDevice(),
        observeSettings(),
        transient,
    ) { device, settings, state ->
        state.copy(
            device = device,
            mode = settings.dataSourceMode,
            cameraHost = settings.cameraHost,
            cameraPath = settings.cameraPath,
            wifiHint = state.wifiHint.ifBlank { defaultHint(settings.dataSourceMode, settings.cameraHost, settings.cameraPath) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CameraConnectUiState(isLoading = true))

    fun scanCamera() {
        viewModelScope.launch {
            val current = uiState.value
            transient.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    wifiHint = if (current.mode == DataSourceMode.Real) {
                        "正在请求 http://${current.cameraHost}${current.cameraPath}，请确认手机已连接相机 Wi-Fi。"
                    } else {
                        "正在使用 mock 数据模拟 Luna / Insta360 相机连接..."
                    },
                )
            }
            runCatching { connectCamera() }
                .onSuccess { device ->
                    transient.update {
                        it.copy(
                            isLoading = false,
                            wifiHint = if (device.connectionStatus == ConnectionStatus.Connected) {
                                if (current.mode == DataSourceMode.Real) {
                                    "已连接 ${device.name}，媒体列表已从相机目录读取。"
                                } else {
                                    "已发现 ${device.name}，mock 连接成功。"
                                }
                            } else {
                                "未发现相机"
                            },
                        )
                    }
                }
                .onFailure { error ->
                    transient.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "扫描失败，请检查相机 Wi-Fi 或稍后重试。",
                            wifiHint = "相机可能已断连，或当前 Wi-Fi 无法访问 ${current.cameraHost}。",
                        )
                    }
                }
        }
    }

    private fun defaultHint(mode: DataSourceMode, host: String, path: String): String {
        return if (mode == DataSourceMode.Real) {
            "真实模式会访问 http://$host$path 读取相机目录索引。"
        } else {
            "当前为模拟模式，不会请求真实相机。"
        }
    }
}
