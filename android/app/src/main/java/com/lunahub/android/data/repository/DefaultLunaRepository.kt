package com.lunahub.android.data.repository

import com.lunahub.android.core.network.CameraNetworkHelper
import com.lunahub.android.data.mapper.CameraMediaMapper
import com.lunahub.android.data.remote.CameraRemoteDataSource
import com.lunahub.android.domain.model.AppSettings
import com.lunahub.android.domain.model.CameraDevice
import com.lunahub.android.domain.model.CameraMedia
import com.lunahub.android.domain.model.ConnectionStatus
import com.lunahub.android.domain.model.DataSourceMode
import com.lunahub.android.domain.model.MediaType
import com.lunahub.android.domain.model.ThemeMode
import com.lunahub.android.domain.repository.LunaRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultLunaRepository @Inject constructor(
    private val remoteDataSource: CameraRemoteDataSource,
    private val mediaMapper: CameraMediaMapper,
    private val cameraNetworkHelper: CameraNetworkHelper,
) : LunaRepository {
    private val deviceState = MutableStateFlow(mockDevice(ConnectionStatus.Disconnected))
    private val mediaState = MutableStateFlow(emptyList<CameraMedia>())
    private val settingsState = MutableStateFlow(
        AppSettings(
            themeMode = ThemeMode.System,
            downloadWifiOnly = true,
            defaultDownloadFolder = "Pictures/Luna Hub",
            watermarkEnabled = true,
            cacheSize = 186L * 1024 * 1024,
            dataSourceMode = DataSourceMode.Real,
        ),
    )

    override val cameraDevice: Flow<CameraDevice> = deviceState.asStateFlow()
    override val media: Flow<List<CameraMedia>> = mediaState.asStateFlow()
    override val settings: Flow<AppSettings> = settingsState.asStateFlow()

    override suspend fun scanCamera(): CameraDevice {
        return when (settingsState.value.dataSourceMode) {
            DataSourceMode.Mock -> connectMockCamera()
            DataSourceMode.Real -> connectRealCamera()
        }
    }

    override suspend fun connectCamera(): CameraDevice = scanCamera()

    override suspend fun getMedia(mediaId: String): CameraMedia? {
        return mediaState.value.firstOrNull { it.id == mediaId }
    }

    override suspend fun markMediaDownloaded(mediaId: String, localPath: String) {
        mediaState.update { current ->
            current.map {
                if (it.id == mediaId) it.copy(isDownloaded = true, localPath = localPath) else it
            }
        }
    }

    override suspend fun clearCache() {
        delay(250)
        settingsState.update { it.copy(cacheSize = 0) }
    }

    override suspend fun setDataSourceMode(mode: DataSourceMode) {
        settingsState.update { it.copy(dataSourceMode = mode) }
        if (mode == DataSourceMode.Mock) {
            deviceState.value = mockDevice(ConnectionStatus.Disconnected)
            mediaState.value = mockMedia()
        } else {
            deviceState.update { it.copy(connectionStatus = ConnectionStatus.Disconnected) }
            mediaState.value = emptyList()
        }
    }

    private suspend fun connectMockCamera(): CameraDevice {
        deviceState.update { it.copy(connectionStatus = ConnectionStatus.Connecting) }
        delay(700)
        val connected = mockDevice(ConnectionStatus.Connected)
        deviceState.value = connected
        mediaState.value = mockMedia()
        return connected
    }

    private suspend fun connectRealCamera(): CameraDevice {
        val settings = settingsState.value
        val wifiBound = cameraNetworkHelper.bindProcessToWifi()
        val wifiInfo = cameraNetworkHelper.wifiNetworkInfo()
        val hosts = cameraNetworkHelper.cameraHostCandidates(settings.cameraHost)
        val firstHost = hosts.firstOrNull() ?: settings.cameraHost
        deviceState.update {
            it.copy(
                ipAddress = firstHost,
                connectionStatus = ConnectionStatus.Connecting,
            )
        }
        return try {
            val paths = listOf(settings.cameraPath, "/DCIM/").distinct()
            var connectedHost: String? = null
            var connectedPath: String? = null
            val diagnostics = mutableListOf<String>()
            hosts.firstOrNull { host ->
                paths.firstOrNull { path ->
                    val status = remoteDataSource.checkStatus(host, path)
                    diagnostics += "$host$path: ${status.message}"
                    val ok = status.httpOk || (status.controlOk && status.tcpFiles > 0)
                    if (ok) {
                        connectedHost = host
                        connectedPath = path
                    }
                    ok
                } != null
            } ?: run {
                val failed = deviceState.value.copy(connectionStatus = ConnectionStatus.Failed)
                deviceState.value = failed
                throw IllegalStateException(
                    "未检测到相机媒体服务。请确认手机已连接 Luna 开头的相机 Wi-Fi，并关闭 WLAN+ / 自动切换网络。\n\n网络：绑定 Wi-Fi=${if (wifiBound) "成功" else "失败"}，手机IP=${wifiInfo.deviceIp ?: "未知"}，网关=${wifiInfo.gatewayIp ?: "未知"}，DHCP=${wifiInfo.serverIp ?: "未知"}。\n\n已尝试：${hosts.joinToString("、")}。\n\n诊断：${diagnostics.joinToString("；")}",
                )
            }
            val host = checkNotNull(connectedHost)
            val path = checkNotNull(connectedPath)
            val status = remoteDataSource.checkStatus(host, path)
            if (!status.httpOk && !status.controlOk) {
                val failed = deviceState.value.copy(connectionStatus = ConnectionStatus.Failed)
                deviceState.value = failed
                throw IllegalStateException(status.message)
            }
            val remoteMedia = remoteDataSource.listMedia(host, path)
            mediaState.value = remoteMedia.map(mediaMapper::fromRemote)
            val connected = deviceState.value.copy(
                name = "Luna Ultra",
                ipAddress = host,
                connectionStatus = ConnectionStatus.Connected,
                firmwareVersion = null,
                batteryLevel = null,
            )
            deviceState.value = connected
            connected
        } catch (error: Throwable) {
            val failed = deviceState.value.copy(connectionStatus = ConnectionStatus.Failed)
            deviceState.value = failed
            mediaState.value = emptyList()
            throw error
        }
    }
}

private fun mockDevice(status: ConnectionStatus): CameraDevice {
    return CameraDevice(
        id = "luna-ultra",
        name = "Luna Ultra",
        ipAddress = "192.168.42.1",
        connectionStatus = status,
        firmwareVersion = "1.3.8",
        batteryLevel = 82,
        storageTotal = 256L * 1024 * 1024 * 1024,
        storageUsed = 96L * 1024 * 1024 * 1024,
    )
}

private fun mockMedia(): List<CameraMedia> {
    val now = System.currentTimeMillis()
    return listOf(
        CameraMedia("m1", "IMG_20260707_081220.jpg", "/DCIM/Camera01/IMG_20260707_081220.jpg", null, "", MediaType.Photo, 6_400_000, null, 4000, 3000, now - 1_800_000, false, null),
        CameraMedia("m2", "VID_20260707_083100.mp4", "/DCIM/Camera01/VID_20260707_083100.mp4", null, "", MediaType.Video, 216_000_000, 42_000, 3840, 2160, now - 1_500_000, false, null),
        CameraMedia("m3", "IMG_20260707_090504.jpg", "/DCIM/Camera01/IMG_20260707_090504.jpg", null, "", MediaType.Photo, 7_200_000, null, 4000, 3000, now - 800_000, true, "/storage/emulated/0/Pictures/Luna Hub/IMG_20260707_090504.jpg"),
        CameraMedia("m4", "IMG_20260706_174232.jpg", "/DCIM/Camera01/IMG_20260706_174232.jpg", null, "", MediaType.Photo, 5_900_000, null, 4000, 3000, now - 86_400_000, false, null),
        CameraMedia("m5", "VID_20260706_181045.mp4", "/DCIM/Camera01/VID_20260706_181045.mp4", null, "", MediaType.Video, 486_000_000, 85_000, 3840, 2160, now - 84_000_000, false, null),
        CameraMedia("m6", "IMG_20260705_120016.jpg", "/DCIM/Camera01/IMG_20260705_120016.jpg", null, "", MediaType.Photo, 8_100_000, null, 4000, 3000, now - 172_800_000, false, null),
    )
}
