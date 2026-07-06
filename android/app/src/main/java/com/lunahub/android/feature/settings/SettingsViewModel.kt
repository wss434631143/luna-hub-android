package com.lunahub.android.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunahub.android.domain.model.AppSettings
import com.lunahub.android.domain.model.DataSourceMode
import com.lunahub.android.domain.usecase.ClearCacheUseCase
import com.lunahub.android.domain.usecase.ObserveSettingsUseCase
import com.lunahub.android.domain.usecase.SetDataSourceModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val settings: AppSettings? = null,
    val message: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    private val clearCacheUseCase: ClearCacheUseCase,
    private val setDataSourceModeUseCase: SetDataSourceModeUseCase,
) : ViewModel() {
    private val transient = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> = combine(observeSettings(), transient) { settings, state ->
        state.copy(settings = settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(isLoading = true))

    fun clearCache() {
        viewModelScope.launch {
            transient.update { it.copy(isLoading = true, message = null) }
            clearCacheUseCase()
            transient.update { it.copy(isLoading = false, message = "缓存已清理") }
        }
    }

    fun setDataSourceMode(mode: DataSourceMode) {
        viewModelScope.launch {
            transient.update { it.copy(message = null, errorMessage = null) }
            runCatching { setDataSourceModeUseCase(mode) }
                .onSuccess {
                    transient.update {
                        it.copy(message = if (mode == DataSourceMode.Real) "已切换为真实相机模式" else "已切换为模拟模式")
                    }
                }
                .onFailure { error ->
                    transient.update { it.copy(errorMessage = error.message ?: "切换失败") }
                }
        }
    }
}
