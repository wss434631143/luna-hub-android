package com.lunahub.android.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import com.lunahub.android.core.design.LunaIconTile
import com.lunahub.android.core.design.LunaLoadingState
import com.lunahub.android.core.design.LunaPage
import com.lunahub.android.core.design.LunaPrimaryButton
import com.lunahub.android.core.design.LunaSectionHeader
import com.lunahub.android.core.design.LunaSecondaryButton
import com.lunahub.android.core.design.LunaSpacing
import com.lunahub.android.core.design.LunaStatusPill
import com.lunahub.android.core.util.formatBytes
import com.lunahub.android.domain.model.ConnectionStatus
import com.lunahub.android.domain.model.MediaType

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
            uiState.device == null -> LunaEmptyState("还没有连接相机", "连接相机 Wi-Fi 后，照片和视频会自动出现在素材库里", "连接相机", onConnectClick)
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LunaSpacing.SectionGap),
            ) {
                item {
                    LunaCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LunaIconTile(Icons.Outlined.Wifi)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                Text(uiState.device.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = if (uiState.connected) "已连接 ${uiState.device.ipAddress}" else "未连接相机，点击重新扫描",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            LunaStatusPill(
                                text = when (uiState.device.connectionStatus) {
                                    ConnectionStatus.Connected -> "在线"
                                    ConnectionStatus.Connecting -> "连接中"
                                    ConnectionStatus.Failed -> "失败"
                                    ConnectionStatus.Disconnected -> "离线"
                                },
                                active = uiState.connected,
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            LunaPrimaryButton("连接相机", onConnectClick, Modifier.weight(1f))
                            LunaSecondaryButton("打开素材", onLibraryClick, Modifier.weight(1f))
                        }
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
                        LunaEmptyState("还没有最近素材", "连接相机并刷新素材库后，这里会显示最新照片和视频")
                    } else {
                        LunaCard {
                            uiState.recentMedia.forEach { media ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    LunaIconTile(if (media.mediaType == MediaType.Video) Icons.Outlined.CloudDownload else Icons.Outlined.PhotoLibrary)
                                    Spacer(Modifier.width(12.dp))
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
