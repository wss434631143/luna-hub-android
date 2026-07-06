package com.lunahub.android.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunahub.android.domain.model.AppSettings
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

data class SettingsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val settings: AppSettings? = null,
    val message: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: LunaRepository,
) : ViewModel() {
    private val transient = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> = combine(repository.settings, transient) { settings, state ->
        state.copy(settings = settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(isLoading = true))

    fun clearCache() {
        viewModelScope.launch {
            transient.update { it.copy(isLoading = true, message = null) }
            repository.clearCache()
            transient.update { it.copy(isLoading = false, message = "缓存已清理") }
        }
    }
}
