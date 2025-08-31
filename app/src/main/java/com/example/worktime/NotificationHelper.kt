package com.example.worktime

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationHelper {
    const val CHANNEL_ID = "worktime_triggers"

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(CHANNEL_ID, "Worktime Hinweise", NotificationManager.IMPORTANCE_DEFAULT)
        nm.createNotificationChannel(ch)
    }
}
