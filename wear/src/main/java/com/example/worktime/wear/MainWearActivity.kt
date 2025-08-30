package com.example.worktime.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.wearable.Wearable

class MainWearActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MaterialTheme {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { send("ACT_START") }) { Text("Start") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { send("ACT_TOGGLE_PAUSE") }) { Text("Pause") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { send("ACT_END") }) { Text("Stop") }
          }
        }
      }
    }
  }
  private fun send(cmd: String) {
    val msgClient = Wearable.getMessageClient(this)
    Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
      nodes.forEach { n -> msgClient.sendMessage(n.id, "/worktime/cmd", cmd.toByteArray()) }
    }
  }
}
