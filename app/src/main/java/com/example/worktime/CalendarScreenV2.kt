package com.example.worktime

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Calendar

@Composable
fun CalendarScreenV2(app: App, onOpenEditor: (Long) -> Unit) {
    val prefs by app.dataStore.data.collectAsState(initial = null)
    val targetHours = prefs?.get(SettingsKeys.WEEK_TARGET_HOURS) ?: 40

    var selected by remember { mutableStateOf(LocalDate.now()) }
    val sessionsForDay = remember { mutableStateListOf<WorkSession>() }

    // Tageseinträge asynchron laden
    LaunchedEffect(selected) {
        withContext(Dispatchers.IO) {
            val c = Calendar.getInstance().apply {
                set(selected.year, selected.monthValue - 1, selected.dayOfMonth, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val list = app.db.sessionDao().sessionsForDay(c.timeInMillis)
            withContext(Dispatchers.Main) {
                sessionsForDay.clear()
                sessionsForDay.addAll(list)
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        WeekSumCalendar(
            selected = selected,
            onSelect = { selected = it },
            targetHours = targetHours
        ) { ws, we ->
            val total by produceState(0L, ws, we) {
                value = withContext(Dispatchers.IO) {
                    app.db.sessionDao().effectiveSumBetween(ws, we, System.currentTimeMillis())
                }
            }
            total
        }

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        Text(
            "Einträge für $selected",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(8.dp)
        )

        LazyColumn {
            items(sessionsForDay) { s ->
                val eff by produceState(0L, s.id) {
                    value = withContext(Dispatchers.IO) {
                        app.db.sessionDao().effectiveForSession(s.id, System.currentTimeMillis()) ?: 0L
                    }
                }
                ListItem(
                    headlineContent = { Text("${timeStr(s.startTs)} – ${s.endTs?.let{ timeStr(it) } ?: "läuft"}") }, // Changed from headlineText
                    supportingContent = { Text("Effektiv: ${msToHHMM(eff)} – Projekt ${s.projectId}") }, // Changed from supportingText
                    modifier = Modifier.clickable { onOpenEditor(s.id) }
                )
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            }
        }
    }
}
