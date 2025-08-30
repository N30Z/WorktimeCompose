package com.example.worktime

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import androidx.core.app.NotificationCompat

class TimeTrackingService : Service() {
  companion object { const val CH_ID = "time_track"; const val NOTIF_ID = 1001 }
  override fun onCreate() { super.onCreate(); createChannel(); startForeground(NOTIF_ID, buildNotif()) }
  override fun onStartCommand(i: Intent?, flags: Int, startId: Int): Int { return START_STICKY }
  override fun onBind(i: Intent?) = null
  private fun buildNotif(): Notification {
    val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
    return NotificationCompat.Builder(this, CH_ID).setSmallIcon(R.drawable.ic_timer).setContentTitle("Arbeitszeit").setContentText("Läuft").setOngoing(true).setContentIntent(pi).build()
  }
  private fun createChannel(){ (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(NotificationChannel(CH_ID, "Zeiterfassung", NotificationManager.IMPORTANCE_LOW)) }
}
