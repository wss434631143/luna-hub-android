package com.lunahub.android.feature.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunahub.android.core.design.LunaCard
import com.lunahub.android.core.design.LunaErrorState
import com.lunahub.android.core.design.LunaIconButton
import com.lunahub.android.core.design.LunaLoadingState
import com.lunahub.android.core.design.LunaPage
import com.lunahub.android.core.design.LunaPrimaryButton
import com.lunahub.android.core.design.LunaSecondaryButton
import com.lunahub.android.core.design.LunaSpacing
import com.lunahub.android.core.util.formatBytes
import com.lunahub.android.core.util.formatDateTime
import com.lunahub.android.core.util.formatDuration
import com.lunahub.android.domain.model.CameraMedia
import com.lunahub.android.domain.model.MediaType

@Composable
fun PreviewRoute(
    onBack: () -> Unit,
    viewModel: PreviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PreviewScreen(uiState, onBack, viewModel::download, viewModel::exportPlaceholder)
}

@Composable
private fun PreviewScreen(
    uiState: PreviewUiState,
    onBack: () -> Unit,
    onDownload: () -> Unit,
    onExport: () -> Unit,
) {
    LunaPage(title = "素材预览", subtitle = uiState.media?.fileName) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LunaIconButton(Icons.Outlined.ArrowBack, "返回", onBack)
            Text("返回素材库", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        when {
            uiState.isLoading -> LunaLoadingState("正在打开素材")
            uiState.errorMessage != null -> LunaErrorState(uiState.errorMessage, onBack)
            uiState.media != null -> PreviewContent(uiState.media, uiState.actionMessage, onDownload, onExport)
        }
    }
}

@Composable
private fun PreviewContent(
    media: CameraMedia,
    actionMessage: String?,
    onDownload: () -> Unit,
    onExport: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LunaSpacing.SectionGap),
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (media.mediaType == MediaType.Video) Icons.Outlined.PlayCircle else Icons.Outlined.Image,
                    contentDescription = null,
                    modifier = Modifier.size(68.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = if (media.mediaType == MediaType.Video) "视频预览将在 Media3 接入后播放" else "图片预览占位",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(18.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item {
            LunaCard {
                Text("文件信息", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                InfoRow("文件名", media.fileName)
                InfoRow("大小", media.fileSize.formatBytes())
                InfoRow("分辨率", "${media.width ?: "-"} x ${media.height ?: "-"}")
                InfoRow("拍摄时间", media.createdAt.formatDateTime())
                InfoRow("类型", if (media.mediaType == MediaType.Video) "视频 ${media.duration?.formatDuration() ?: ""}" else "照片")
                InfoRow("保存状态", if (media.isDownloaded) "已保存到本地" else "未下载")
                if (actionMessage != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(actionMessage, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LunaPrimaryButton("下载到 App", onDownload, Modifier.weight(1f))
                LunaSecondaryButton("导出到手机", onExport, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
    Spacer(Modifier.height(8.dp))
}
