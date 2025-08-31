package com.example.worktime

import android.Manifest
import android.R
import android.content.Context
import android.net.wifi.WifiManager
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class TriggerWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    private val app = appContext.applicationContext as App

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // FIX: Extension auf DataStore, nicht auf Flow
        val prefs = app.dataStore.firstBlocking()

        val wifiEnabled = prefs[SettingsKeys.WIFI_ENABLED] ?: false
        val ssid = prefs[SettingsKeys.WIFI_SSID] ?: ""
        val lateAfter = prefs[SettingsKeys.LATE_AFTER_MIN] ?: 10
        val now = System.currentTimeMillis()

        val expectStart = parseStdStartToday(prefs)
        val isRunning = app.db.sessionDao().getRunning() != null
        val atWork = wifiEnabled && ssid.isNotBlank() && isOnWifi(ssid)

        if (expectStart != null && atWork && !isRunning && now > expectStart + lateAfter * 60_000L) {
            notify("Nicht eingeloggt", "Du bist am Arbeitsplatz (WLAN), aber noch nicht eingeloggt.")
        }
        if (isRunning && !atWork && wifiEnabled) {
            notify("Arbeitsort verlassen", "WLAN nicht mehr verbunden, Session läuft.")
        }

        Result.success()
    }

    private fun parseStdStartToday(prefs: Preferences): Long? {
        val c = java.util.Calendar.getInstance()
        val idx = (c.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7 // Mo=0..So=6
        val map = listOf(
            SettingsKeys.STD_START_MON, SettingsKeys.STD_START_TUE, SettingsKeys.STD_START_WED,
            SettingsKeys.STD_START_THU, SettingsKeys.STD_START_FRI, SettingsKeys.STD_START_SAT, SettingsKeys.STD_START_SUN
        )
        val hhmm = prefs[map[idx]] ?: "09:00"
        val m = Regex("^(\\d{1,2}):(\\d{2})$").matchEntire(hhmm) ?: return null
        val h = m.groupValues[1].toInt().coerceIn(0,23)
        val mi = m.groupValues[2].toInt().coerceIn(0,59)
        c.set(java.util.Calendar.HOUR_OF_DAY, h); c.set(java.util.Calendar.MINUTE, mi)
        c.set(java.util.Calendar.SECOND, 0); c.set(java.util.Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun isOnWifi(targetSsid: String): Boolean {
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wm.connectionInfo ?: return false
        val ssid = info.ssid?.replace("\"","") ?: return false
        return ssid.equals(targetSsid, ignoreCase = true)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun notify(title: String, text: String) {
        NotificationHelper.ensureChannel(applicationContext)
        val n = NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(title.hashCode(), n)
    }
}

// dieselbe Helper-Extension wie in TriggerScheduler.kt, falls du sie hier lokal brauchst
private fun DataStore<Preferences>.firstBlocking(): Preferences = runBlocking { data.first() }
