package com.lunahub.android.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunahub.android.core.design.LunaEmptyState
import com.lunahub.android.core.design.LunaErrorState
import com.lunahub.android.core.design.LunaLoadingState
import com.lunahub.android.core.design.LunaMediaGridItem
import com.lunahub.android.core.design.LunaPage
import com.lunahub.android.core.design.LunaPrimaryButton
import com.lunahub.android.core.design.LunaSectionHeader
import com.lunahub.android.core.design.LunaSpacing
import com.lunahub.android.core.util.formatDate
import com.lunahub.android.domain.model.CameraMedia
import com.lunahub.android.domain.model.MediaFilter

@Composable
fun LibraryRoute(
    onMediaClick: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LibraryScreen(
        uiState = uiState,
        onFilter = viewModel::setFilter,
        onMediaClick = { media ->
            if (uiState.selectionMode) viewModel.toggleSelection(media.id) else onMediaClick(media.id)
        },
        onMediaLongClick = { viewModel.toggleSelection(it.id) },
        onClearSelection = viewModel::clearSelection,
    )
}

@Composable
private fun LibraryScreen(
    uiState: LibraryUiState,
    onFilter: (MediaFilter) -> Unit,
    onMediaClick: (CameraMedia) -> Unit,
    onMediaLongClick: (CameraMedia) -> Unit,
    onClearSelection: () -> Unit,
) {
    LunaPage(
        title = if (uiState.selectionMode) "已选择 ${uiState.selectedIds.size} 项" else "相机素材库",
        subtitle = "按日期浏览相机中的照片和视频",
    ) {
        when {
            uiState.isLoading -> LunaLoadingState()
            uiState.errorMessage != null -> LunaErrorState(uiState.errorMessage) {}
            uiState.media.isEmpty() -> LunaEmptyState("暂无素材", "连接相机后会在这里显示照片和视频")
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MediaFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = uiState.filter == filter,
                            onClick = { onFilter(filter) },
                            label = {
                                Text(
                                    when (filter) {
                                        MediaFilter.All -> "全部"
                                        MediaFilter.Photo -> "照片"
                                        MediaFilter.Video -> "视频"
                                    },
                                )
                            },
                        )
                    }
                }
                if (uiState.selectionMode) {
                    Spacer(Modifier.height(10.dp))
                    LunaPrimaryButton("清除选择", onClearSelection, Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(12.dp))
                val groups = uiState.filteredMedia.groupBy { it.createdAt.formatDate() }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    groups.forEach { (day, itemsForDay) ->
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            LunaSectionHeader(day)
                            Spacer(Modifier.height(2.dp))
                        }
                        items(itemsForDay, key = { it.id }) { media ->
                            LunaMediaGridItem(
                                media = media,
                                selected = uiState.selectedIds.contains(media.id),
                                onClick = { onMediaClick(media) },
                                onLongClick = { onMediaLongClick(media) },
                            )
                        }
                    }
                }
                if (uiState.filteredMedia.isEmpty()) {
                    Text("当前筛选没有素材", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
