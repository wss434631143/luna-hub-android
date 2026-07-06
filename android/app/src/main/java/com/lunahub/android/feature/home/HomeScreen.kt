package com.lunahub.android.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunahub.android.core.design.LunaCard
import com.lunahub.android.core.design.LunaEmptyState
import com.lunahub.android.core.design.LunaErrorState
import com.lunahub.android.core.design.LunaLoadingState
import com.lunahub.android.core.design.LunaPage
import com.lunahub.android.core.design.LunaPrimaryButton
import com.lunahub.android.core.design.LunaSectionHeader
import com.lunahub.android.core.design.LunaSecondaryButton
import com.lunahub.android.core.design.LunaSpacing
import com.lunahub.android.core.util.formatBytes
import com.lunahub.android.domain.model.ConnectionStatus

@Composable
fun HomeRoute(
    onConnectClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(uiState, onConnectClick, onLibraryClick, onDownloadsClick, onSettingsClick)
}

@Composable
private fun HomeScreen(
    uiState: HomeUiState,
    onConnectClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    LunaPage(
        title = "Luna Hub",
        subtitle = "相机素材管理、预览与轻量导出",
    ) {
        when {
            uiState.isLoading -> LunaLoadingState()
            uiState.errorMessage != null -> LunaErrorState(uiState.errorMessage) {}
            uiState.device == null -> LunaEmptyState("还没有设备", "请先连接 Luna 或 Insta360 相机", "连接相机", onConnectClick)
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LunaSpacing.SectionGap),
            ) {
                item {
                    LunaCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Wifi, null, tint = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                Text(uiState.device.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = if (uiState.connected) "已连接 ${uiState.device.ipAddress}" else "未连接，点击扫描相机",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            Text(
                                text = when (uiState.device.connectionStatus) {
                                    ConnectionStatus.Connected -> "在线"
                                    ConnectionStatus.Connecting -> "连接中"
                                    ConnectionStatus.Failed -> "失败"
                                    ConnectionStatus.Disconnected -> "离线"
                                },
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        LunaPrimaryButton("连接相机", onConnectClick, Modifier.fillMaxWidth())
                    }
                }
                item {
                    LunaSectionHeader("快捷入口")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LunaSecondaryButton("素材库", onLibraryClick, Modifier.weight(1f))
                        LunaSecondaryButton("下载", onDownloadsClick, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LunaSecondaryButton("设置", onSettingsClick, Modifier.weight(1f))
                        LunaSecondaryButton("扫描", onConnectClick, Modifier.weight(1f))
                    }
                }
                item {
                    LunaSectionHeader("最近素材", action = "查看全部", onAction = onLibraryClick)
                    Spacer(Modifier.height(10.dp))
                    if (uiState.recentMedia.isEmpty()) {
                        LunaEmptyState("暂无素材", "连接相机后会显示最近拍摄的照片和视频")
                    } else {
                        LunaCard {
                            uiState.recentMedia.forEach { media ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        if (media.fileName.endsWith(".mp4")) Icons.Outlined.CloudDownload else Icons.Outlined.PhotoLibrary,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(media.fileName, maxLines = 1, style = MaterialTheme.typography.bodyLarge)
                                        Text(media.fileSize.formatBytes(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
