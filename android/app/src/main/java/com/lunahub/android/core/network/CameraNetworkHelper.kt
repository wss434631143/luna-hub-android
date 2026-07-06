package com.lunahub.android.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraNetworkHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    data class WifiNetworkInfo(
        val gatewayIp: String?,
        val serverIp: String?,
        val deviceIp: String?,
    )

    fun bindProcessToWifi(): Boolean {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val wifiNetwork = connectivityManager.allNetworks.firstOrNull { network ->
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
        return if (wifiNetwork != null) {
            connectivityManager.bindProcessToNetwork(wifiNetwork)
        } else {
            false
        }
    }

    fun releaseNetworkBinding() {
        context.getSystemService(ConnectivityManager::class.java).bindProcessToNetwork(null as Network?)
    }

    fun cameraHostCandidates(defaultHost: String): List<String> {
        val wifiInfo = wifiNetworkInfo()
        val deviceSubnetGateway = wifiInfo.deviceIp
            ?.substringBeforeLast('.', missingDelimiterValue = "")
            ?.takeIf { it.isNotBlank() }
            ?.let { "$it.1" }
        return listOfNotNull(
            wifiInfo.gatewayIp,
            wifiInfo.serverIp,
            deviceSubnetGateway,
            defaultHost,
            "192.168.42.1",
            "192.168.43.1",
            "192.168.0.1",
            "192.168.1.1",
            "192.168.100.1",
            "172.16.10.1",
            "10.0.0.1",
        ).map { it.removePrefix("http://").removePrefix("https://").trimEnd('/') }
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun wifiNetworkInfo(): WifiNetworkInfo {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val dhcpInfo = wifiManager?.dhcpInfo
        return WifiNetworkInfo(
            gatewayIp = dhcpInfo?.gateway?.toIpString(),
            serverIp = dhcpInfo?.serverAddress?.toIpString(),
            deviceIp = dhcpInfo?.ipAddress?.toIpString(),
        )
    }

    private fun Int.toIpString(): String? {
        if (this == 0) return null
        val bytes = ByteBuffer.allocate(Int.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(this)
            .array()
        return bytes.joinToString(".") { byte -> String.format(Locale.US, "%d", byte.toInt() and 0xff) }
    }
}
