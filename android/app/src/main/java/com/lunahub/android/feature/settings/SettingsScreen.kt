package com.lunahub.android.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SettingsInputAntenna
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import com.lunahub.android.core.design.LunaErrorState
import com.lunahub.android.core.design.LunaIconTile
import com.lunahub.android.core.design.LunaLoadingState
import com.lunahub.android.core.design.LunaPage
import com.lunahub.android.core.design.LunaSectionHeader
import com.lunahub.android.core.design.LunaSpacing
import com.lunahub.android.core.util.formatBytes
import com.lunahub.android.domain.model.DataSourceMode

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(uiState, viewModel::clearCache, viewModel::setDataSourceMode)
}

@Composable
private fun SettingsScreen(
    uiState: SettingsUiState,
    onClearCache: () -> Unit,
    onDataSourceModeChange: (DataSourceMode) -> Unit,
) {
    LunaPage(title = "设置", subtitle = "连接、下载与应用偏好") {
        when {
            uiState.isLoading && uiState.settings == null -> LunaLoadingState("正在读取设置")
            uiState.errorMessage != null -> LunaErrorState(uiState.errorMessage) {}
            uiState.settings != null -> LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LunaSpacing.SectionGap),
            ) {
                item {
                    LunaSectionHeader("外观")
                    Spacer(Modifier.height(10.dp))
                    LunaCard {
                        SettingRow(Icons.Outlined.DarkMode, "主题", "跟随系统，深色模式下自动降低背景亮度") {
                            Text(uiState.settings.themeMode.name, color = MaterialTheme.colorScheme.primary)
                        }
                        SettingRow(Icons.Outlined.Download, "下载位置", "App 专属目录 / ${uiState.settings.defaultDownloadFolder}")
                    }
                }
                item {
                    LunaSectionHeader("相机")
                    Spacer(Modifier.height(10.dp))
                    LunaCard {
                        SettingRow(Icons.Outlined.SettingsInputAntenna, "数据来源", "模拟模式用于体验；真实模式读取相机目录") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = uiState.settings.dataSourceMode == DataSourceMode.Mock,
                                    onClick = { onDataSourceModeChange(DataSourceMode.Mock) },
                                    label = { Text("Mock") },
                                )
                                FilterChip(
                                    selected = uiState.settings.dataSourceMode == DataSourceMode.Real,
                                    onClick = { onDataSourceModeChange(DataSourceMode.Real) },
                                    label = { Text("真实") },
                                )
                            }
                        }
                        SettingRow(
                            Icons.Outlined.Info,
                            "默认地址",
                            "http://${uiState.settings.cameraHost}${uiState.settings.cameraPath}",
                        )
                    }
                }
                item {
                    LunaSectionHeader("导出")
                    Spacer(Modifier.height(10.dp))
                    LunaCard {
                        SettingRow(Icons.Outlined.WaterDrop, "水印", "后续支持位置、大小和透明度") {
                            Switch(checked = uiState.settings.watermarkEnabled, onCheckedChange = null)
                        }
                    }
                }
                item {
                    LunaSectionHeader("存储")
                    Spacer(Modifier.height(10.dp))
                    LunaCard {
                        SettingRow(Icons.Outlined.CleaningServices, "清理缓存", "当前缓存 ${uiState.settings.cacheSize.formatBytes()}", onClick = onClearCache)
                        if (uiState.message != null) {
                            Text(uiState.message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                item {
                    LunaCard {
                        SettingRow(Icons.Outlined.Info, "关于 Luna Hub", "面向 Insta360 相机的移动素材管理工具")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LunaIconTile(icon)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        trailing?.invoke()
    }
    Spacer(Modifier.height(14.dp))
}
