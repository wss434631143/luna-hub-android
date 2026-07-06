package com.lunahub.android.feature.preview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunahub.android.domain.model.CameraMedia
import com.lunahub.android.domain.usecase.GetMediaUseCase
import com.lunahub.android.domain.usecase.StartDownloadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PreviewUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val media: CameraMedia? = null,
    val actionMessage: String? = null,
)

@HiltViewModel
class PreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMedia: GetMediaUseCase,
    private val startDownload: StartDownloadUseCase,
) : ViewModel() {
    private val mediaId: String = checkNotNull(savedStateHandle["mediaId"])
    private val mutableUiState = MutableStateFlow(PreviewUiState())
    val uiState: StateFlow<PreviewUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            val media = getMedia(mediaId)
            mutableUiState.value = if (media == null) {
                PreviewUiState(isLoading = false, errorMessage = "素材不存在")
            } else {
                PreviewUiState(isLoading = false, media = media)
            }
        }
    }

    fun download() {
        val media = mutableUiState.value.media ?: return
        viewModelScope.launch {
            mutableUiState.update { it.copy(actionMessage = "已加入下载队列") }
            startDownload(media.id)
        }
    }

    fun exportPlaceholder() {
        mutableUiState.update { it.copy(actionMessage = "导出到手机相册将在后续版本接入") }
    }
}
