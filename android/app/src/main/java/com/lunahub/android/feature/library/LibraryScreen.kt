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
        onDownloadSelected = viewModel::downloadSelected,
        onSelect = viewModel::enterSelectionMode,
    )
}

@Composable
private fun LibraryScreen(
    uiState: LibraryUiState,
    onFilter: (MediaFilter) -> Unit,
    onMediaClick: (CameraMedia) -> Unit,
    onMediaLongClick: (CameraMedia) -> Unit,
    onClearSelection: () -> Unit,
    onDownloadSelected: () -> Unit,
    onSelect: () -> Unit,
) {
    LunaPage(
        title = if (uiState.selectionMode) "已选择 ${uiState.selectedIds.size} 项" else "相机相册",
        subtitle = if (uiState.selectionMode) "选择素材后可下载到 App，后续支持导出到手机相册" else "按日期浏览相机中的照片和视频",
    ) {
        when {
            uiState.isLoading -> LunaLoadingState()
            uiState.errorMessage != null -> LunaErrorState(uiState.errorMessage) {}
            uiState.media.isEmpty() -> LunaEmptyState("相机相册是空的", "先连接 Luna 开头的相机 Wi-Fi，再扫描相机读取照片和视频")
            else -> {
                LunaSectionHeader(
                    title = "相机文件",
                    action = if (uiState.selectionMode) "完成" else "选择",
                    onAction = if (uiState.selectionMode) onClearSelection else onSelect,
                )
                Spacer(Modifier.height(10.dp))
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
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LunaPrimaryButton("下载到 App", onDownloadSelected, Modifier.weight(1f), enabled = uiState.selectedIds.isNotEmpty())
                        LunaPrimaryButton("取消选择", onClearSelection, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("提示：下载会保存到 Luna Hub 本地相册；导出到系统相册将在后续版本接入。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
                if (uiState.message != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(uiState.message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(12.dp))
                val groups = uiState.filteredMedia.groupBy { it.createdAt.formatDate() }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 104.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                    Text("当前筛选没有素材，可以切换到“全部”查看。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
