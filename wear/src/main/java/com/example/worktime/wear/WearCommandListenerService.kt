package com.example.worktime.wear

import com.google.android.gms.wearable.WearableListenerService
import com.google.android.gms.wearable.MessageEvent

class WearCommandListenerService : WearableListenerService() {
  override fun onMessageReceived(event: MessageEvent) { /* Phone module listens instead */ }
}
