package com.lunahub.android.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunahub.android.domain.model.CameraMedia
import com.lunahub.android.domain.model.MediaFilter
import com.lunahub.android.domain.model.MediaType
import com.lunahub.android.domain.usecase.ObserveCameraMediaUseCase
import com.lunahub.android.domain.usecase.StartBatchDownloadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val filter: MediaFilter = MediaFilter.All,
    val media: List<CameraMedia> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val selecting: Boolean = false,
    val message: String? = null,
) {
    val filteredMedia: List<CameraMedia> = media.filter {
        filter == MediaFilter.All ||
            (filter == MediaFilter.Photo && it.mediaType == MediaType.Photo) ||
            (filter == MediaFilter.Video && it.mediaType == MediaType.Video)
    }
    val selectionMode: Boolean = selecting || selectedIds.isNotEmpty()
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    observeCameraMedia: ObserveCameraMediaUseCase,
    private val startBatchDownload: StartBatchDownloadUseCase,
) : ViewModel() {
    private val controls = MutableStateFlow(LibraryUiState())

    val uiState: StateFlow<LibraryUiState> = combine(observeCameraMedia(), controls) { media, state ->
        state.copy(media = media)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState(isLoading = true))

    fun setFilter(filter: MediaFilter) {
        controls.update { it.copy(filter = filter) }
    }

    fun enterSelectionMode() {
        controls.update { it.copy(selecting = true, message = "请选择要下载或导出的素材") }
    }

    fun toggleSelection(mediaId: String) {
        controls.update { state ->
            val next = state.selectedIds.toMutableSet()
            if (!next.add(mediaId)) next.remove(mediaId)
            state.copy(selectedIds = next)
        }
    }

    fun clearSelection() {
        controls.update { it.copy(selectedIds = emptySet(), selecting = false, message = null) }
    }

    fun downloadSelected() {
        val selected = controls.value.selectedIds
        if (selected.isEmpty()) return
        viewModelScope.launch {
            startBatchDownload(selected)
            controls.update { it.copy(selectedIds = emptySet(), selecting = false, message = "已加入 ${selected.size} 个下载任务") }
        }
    }
}
