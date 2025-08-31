package com.example.worktime

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build

/** Liefert die aktuell verbundene WLAN-SSID oder null (Permission/Standort erforderlich je nach API). */
@Suppress("DEPRECATION")
fun getCurrentSsid(context: Context): String? {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val active = cm.activeNetwork ?: return null
    val caps = cm.getNetworkCapabilities(active) ?: return null
    if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null

    val wifiInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        caps.transportInfo as? WifiInfo
    } else {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wm.connectionInfo
    } ?: return null

    val raw = wifiInfo.ssid ?: return null
    val ssid = raw.removePrefix("\"").removeSuffix("\"")
    return if (ssid.equals("<unknown ssid>", ignoreCase = true)) null else ssid
}

/** Runtime-Permissions für SSID-Zugriff. */
fun ssidRequiredPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(android.Manifest.permission.NEARBY_WIFI_DEVICES)
    } else {
        arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }
}
