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
        return listOfNotNull(
            wifiGatewayIp(),
            defaultHost,
            "192.168.42.1",
            "192.168.1.1",
        ).map { it.removePrefix("http://").removePrefix("https://").trimEnd('/') }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun wifiGatewayIp(): String? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val gateway = wifiManager?.dhcpInfo?.gateway ?: return null
        if (gateway == 0) return null
        val bytes = ByteBuffer.allocate(Int.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(gateway)
            .array()
        return bytes.joinToString(".") { byte -> String.format(Locale.US, "%d", byte.toInt() and 0xff) }
    }
}
