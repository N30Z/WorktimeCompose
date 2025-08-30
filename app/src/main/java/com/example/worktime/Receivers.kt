package com.example.worktime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class GeofenceReceiver : BroadcastReceiver() { override fun onReceive(ctx: Context, intent: Intent) { /* TODO: geofence logic */ } }
class BootReceiver : BroadcastReceiver() { override fun onReceive(ctx: Context, intent: Intent) { /* TODO: re-register geofence, show notif if running */ } }
