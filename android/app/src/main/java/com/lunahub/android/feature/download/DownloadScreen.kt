package com.lunahub.android.feature.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunahub.android.core.design.LunaCard
import com.lunahub.android.core.design.LunaEmptyState
import com.lunahub.android.core.design.LunaErrorState
import com.lunahub.android.core.design.LunaIconTile
import com.lunahub.android.core.design.LunaLoadingState
import com.lunahub.android.core.design.LunaPage
import com.lunahub.android.core.design.LunaSecondaryButton
import com.lunahub.android.core.design.LunaSpacing
import com.lunahub.android.core.design.LunaStatusPill
import com.lunahub.android.core.util.formatBytes
import com.lunahub.android.domain.model.DownloadStatus
import com.lunahub.android.domain.model.DownloadTask

@Composable
fun DownloadRoute(viewModel: DownloadViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DownloadScreen(uiState, viewModel::retry, viewModel::cancel)
}

@Composable
private fun DownloadScreen(
    uiState: DownloadUiState,
    onRetry: (String) -> Unit,
    onCancel: (String) -> Unit,
) {
    LunaPage(title = "下载任务", subtitle = "查看素材保存进度与失败原因") {
        when {
            uiState.isLoading -> LunaLoadingState("正在读取下载队列")
            uiState.errorMessage != null -> LunaErrorState(uiState.errorMessage) {}
            uiState.tasks.isEmpty() -> LunaEmptyState("还没有下载任务", "在预览页或素材库多选后点击下载，进度会显示在这里")
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LunaSpacing.SectionGap),
            ) {
                items(uiState.tasks, key = { it.id }) { task ->
                    DownloadTaskCard(task, onRetry, onCancel)
                }
            }
        }
    }
}

@Composable
private fun DownloadTaskCard(
    task: DownloadTask,
    onRetry: (String) -> Unit,
    onCancel: (String) -> Unit,
) {
    val icon: ImageVector = when (task.status) {
        DownloadStatus.Success -> Icons.Outlined.CheckCircle
        DownloadStatus.Failed -> Icons.Outlined.ErrorOutline
        DownloadStatus.Queued -> Icons.Outlined.Schedule
        DownloadStatus.Canceled -> Icons.Outlined.ErrorOutline
        else -> Icons.Outlined.CloudDownload
    }
    LunaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LunaIconTile(icon, tint = if (task.status == DownloadStatus.Failed || task.status == DownloadStatus.Canceled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(task.fileName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = when (task.status) {
                        DownloadStatus.Queued -> "等待中"
                        DownloadStatus.Downloading -> "下载中 ${task.speed.formatBytes()}/s"
                        DownloadStatus.Success -> task.localPath ?: "已完成"
                        DownloadStatus.Failed -> task.errorMessage ?: "失败"
                        DownloadStatus.Canceled -> "已取消"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            LunaStatusPill(
                text = when (task.status) {
                    DownloadStatus.Queued -> "等待"
                    DownloadStatus.Downloading -> "${(task.progress * 100).toInt()}%"
                    DownloadStatus.Success -> "完成"
                    DownloadStatus.Failed -> "失败"
                    DownloadStatus.Canceled -> "取消"
                },
                active = task.status == DownloadStatus.Downloading || task.status == DownloadStatus.Success,
            )
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(progress = { task.progress }, modifier = Modifier.fillMaxWidth())
        if (task.status == DownloadStatus.Failed || task.status == DownloadStatus.Canceled || task.status == DownloadStatus.Downloading || task.status == DownloadStatus.Queued) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (task.status == DownloadStatus.Failed || task.status == DownloadStatus.Canceled) {
                    LunaSecondaryButton("重试", { onRetry(task.id) }, Modifier.weight(1f))
                }
                if (task.status == DownloadStatus.Downloading || task.status == DownloadStatus.Queued) {
                    LunaSecondaryButton("取消", { onCancel(task.id) }, Modifier.weight(1f))
                }
            }
        }
    }
}
