package com.lunahub.android.feature.camera

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Battery5Bar
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunahub.android.core.design.LunaCard
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
import com.lunahub.android.domain.model.DataSourceMode

@Composable
fun CameraConnectRoute(
    onOpenLibrary: () -> Unit,
    viewModel: CameraConnectViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CameraConnectScreen(uiState, viewModel::scanCamera, onOpenLibrary)
}

@Composable
private fun CameraConnectScreen(
    uiState: CameraConnectUiState,
    onScan: () -> Unit,
    onOpenLibrary: () -> Unit,
) {
    val context = LocalContext.current
    LunaPage(title = "连接相机", subtitle = "连接相机 Wi-Fi 后读取机内照片和视频") {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LunaSpacing.SectionGap),
        ) {
            item {
                LunaCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LunaIconTile(Icons.Outlined.Wifi)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("连接引导", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (uiState.mode == DataSourceMode.Real) {
                                    "请先连接名称以 Luna 开头的相机 Wi-Fi，再返回这里扫描。"
                                } else {
                                    "当前为模拟模式，可先体验完整页面流程。"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(uiState.wifiHint, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    if (uiState.mode == DataSourceMode.Real) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "步骤：打开相机无线连接 -> 手机进入 Wi-Fi -> 选择 Luna... 热点 -> 回到 App 点击扫描相机。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "如果连接后仍失败，请关闭系统的 WLAN+ / 自动切换网络，或在 Wi-Fi 设置中忘记该 Luna 热点后重新连接。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "探测范围：http://${uiState.cameraHost}${uiState.cameraPath}，以及相机 TCP 6666 控制通道。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LunaSecondaryButton(
                            text = "打开 Wi-Fi",
                            onClick = { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) },
                            modifier = Modifier.weight(1f),
                        )
                        LunaPrimaryButton(
                            text = if (uiState.isLoading) "扫描中..." else "扫描相机",
                            onClick = onScan,
                            enabled = !uiState.isLoading,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            if (uiState.isLoading) {
                item { LunaLoadingState("正在检测当前 Wi-Fi 与相机服务") }
            }
            if (uiState.errorMessage != null) {
                item { LunaErrorState(uiState.errorMessage, onScan) }
            }
            uiState.device?.let { device ->
                item {
                    LunaSectionHeader("设备状态")
                    Spacer(Modifier.height(10.dp))
                    LunaCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LunaIconTile(Icons.Outlined.CheckCircle)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(device.name, style = MaterialTheme.typography.titleMedium)
                                Text(device.ipAddress, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            LunaStatusPill(
                                text = if (device.connectionStatus == ConnectionStatus.Connected) "已连接" else "未连接",
                                active = device.connectionStatus == ConnectionStatus.Connected,
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            InfoPill(Icons.Outlined.Battery5Bar, "${device.batteryLevel ?: 0}%")
                            InfoPill(Icons.Outlined.Storage, "${device.storageUsed?.formatBytes()} / ${device.storageTotal?.formatBytes()}")
                        }
                        Spacer(Modifier.height(16.dp))
                        LunaPrimaryButton("打开素材库", onOpenLibrary, Modifier.fillMaxWidth(), enabled = device.connectionStatus == ConnectionStatus.Connected)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
