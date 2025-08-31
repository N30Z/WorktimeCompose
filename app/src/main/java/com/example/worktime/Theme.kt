package com.example.worktime

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

private val Light = lightColorScheme()
private val Dark = darkColorScheme()

@Composable
fun WorktimeTheme(app: App?, content: @Composable () -> Unit) {
    val forceDark = if (app == null) false else {
        val prefs by app.dataStore.data.collectAsState(initial = null)
        prefs?.get(SettingsKeys.FORCE_DARK_MODE) ?: false
    }
    MaterialTheme(colorScheme = if (forceDark) Dark else Light, content = content)
}
