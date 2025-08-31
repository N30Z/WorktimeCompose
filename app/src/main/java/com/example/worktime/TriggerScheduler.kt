package com.example.worktime

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

object TriggerScheduler {
    private const val WORK_NAME = "worktime_triggers"

    fun scheduleOrCancel(app: App) {
        val prefs = app.dataStore.data.blockingFirst()
        val wifi = prefs[SettingsKeys.WIFI_ENABLED] ?: false
        val enabled = wifi
        val mins = (prefs[SettingsKeys.TRIGGER_CHECK_MIN] ?: 15).coerceIn(15, 60)

        val wm = WorkManager.getInstance(app)
        if (!enabled) {
            wm.cancelUniqueWork(WORK_NAME)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
        val req = PeriodicWorkRequestBuilder<TriggerWorker>(mins.toLong(), TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
    }
}

// Helfer für blockierendes Lesen (nur Setup/Worker)
fun <T> kotlinx.coroutines.flow.Flow<T>.blockingFirst(): T = kotlinx.coroutines.runBlocking { first() }
fun <T> androidx.datastore.core.DataStore<T>.firstBlocking(): T = kotlinx.coroutines.runBlocking { data.first() }
