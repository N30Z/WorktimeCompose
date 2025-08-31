package com.example.worktime

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MainHomeScreen(
    app: App,
    onOpenProjects: () -> Unit,
    onOpenExport:   () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    val prefs by app.dataStore.data.collectAsState(initial = null)
    val projLabel = prefs?.get(SettingsKeys.PROJECT_LABEL) ?: "Projekte"
    val rMin = prefs?.get(SettingsKeys.ROUNDING_MINUTES) ?: 0
    val rMode = prefs?.get(SettingsKeys.ROUNDING_MODE) ?: "NONE"

    val projects by remember { app.db.projectDao().getActive() }.collectAsState(initial = emptyList())
    var currentProjectId by remember { mutableStateOf<Long?>(projects.firstOrNull()?.id) }
    var showNew by remember { mutableStateOf(false) }

    val runningSession by produceState<WorkSession?>(initialValue = null, projects) {
        value = withContext(Dispatchers.IO) { app.db.sessionDao().getRunning() }
    }
    val todayMsRaw by produceState(0L, runningSession) {
        value = withContext(Dispatchers.IO) { app.repo.effectiveToday() }
    }
    val weekMsRaw by produceState(0L, runningSession) {
        value = withContext(Dispatchers.IO) { app.repo.effectiveWeek() }
    }
    val currentProjectMsRaw by produceState(0L, runningSession, currentProjectId) {
        value = withContext(Dispatchers.IO) {
            currentProjectId?.let { app.db.sessionDao().effectiveSumForProject(it, System.currentTimeMillis()) } ?: 0L
        }
    }

    val todayMs = roundDuration(todayMsRaw, rMin, rMode)
    val weekMs  = roundDuration(weekMsRaw, rMin, rMode)
    val currentProjectMs = roundDuration(currentProjectMsRaw, rMin, rMode)

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(3f)) {
            item { BigCard(projLabel, onOpenProjects) }
            item { BigCard("Export",   onOpenExport) }
            item { BigCard("Kalender", onOpenCalendar) }
            item { BigCard("Settings", onOpenSettings) }
        }

        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Heute", msToHHMM(todayMs), Modifier.weight(1f))
                StatCard("Woche", msToHHMM(weekMs),  Modifier.weight(1f))
                val projName = projects.firstOrNull { it.id == currentProjectId }?.name ?: "—"
                StatCard(projLabel.dropLastWhile { false }, "${msToHHMM(currentProjectMs)} ($projName)", Modifier.weight(1f))
            }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                var expanded by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.weight(1f)) {
                    Text(projects.firstOrNull { it.id == currentProjectId }?.name ?: "$projLabel wählen")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    projects.forEach { p ->
                        DropdownMenuItem(text = { Text(p.name) }, onClick = {
                            currentProjectId = p.id; expanded = false
                        })
                    }
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { showNew = true }) { Text("+") }
            }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (runningSession == null) {
                    Button(onClick = {
                        val pid = currentProjectId ?: return@Button
                        scope.launch(Dispatchers.IO) { app.repo.startSession(pid) }
                    }, modifier = Modifier.weight(1f)) { Text("Einloggen") }

                    OutlinedButton(onClick = {
                        val pid = currentProjectId ?: return@OutlinedButton
                        val now = java.util.Calendar.getInstance()
                        TimePickerDialog(ctx, { _, hh, mm ->
                            scope.launch(Dispatchers.IO) {
                                val base = dayStart(System.currentTimeMillis()) + (hh * 60 + mm) * 60_000L
                                app.repo.startSessionAt(pid, base, manual = true)
                            }
                        }, now.get(java.util.Calendar.HOUR_OF_DAY), now.get(java.util.Calendar.MINUTE), true).show()
                    }, modifier = Modifier.weight(1f)) { Text("Fing an um") }
                } else {
                    Button(onClick = { scope.launch(Dispatchers.IO) { app.repo.endSession() } },
                        modifier = Modifier.weight(1f)) { Text("Ausloggen") }
                    OutlinedButton(onClick = { scope.launch(Dispatchers.IO) { app.repo.togglePause() } },
                        modifier = Modifier.weight(1f)) { Text("Pause") }
                    OutlinedButton(onClick = {
                        val toId = currentProjectId ?: return@OutlinedButton
                        scope.launch(Dispatchers.IO) { app.repo.switchProject(toId) }
                    }, modifier = Modifier.weight(1f)) { Text("Projekt wechseln") }
                }
            }

            Spacer(Modifier.height(8.dp))
            val sinceLogin by produceState("—", runningSession?.id) {
                if (runningSession == null) value = "—" else {
                    while (true) {
                        val dur = System.currentTimeMillis() - (runningSession?.startTs ?: System.currentTimeMillis())
                        value = msToHHMM(roundDuration(dur, rMin, rMode)); kotlinx.coroutines.delay(1_000)
                    }
                }
            }
            Text("Seit Einloggen: $sinceLogin")
        }
    }

    if (showNew) {
        NewProjectDialog(
            onDismiss = { showNew = false },
            onCreate = { name, number ->
                scope.launch(Dispatchers.IO) {
                    val id = app.db.projectDao().insert(Project(name = name, number = number))
                    withContext(Dispatchers.Main) { currentProjectId = id; showNew = false }
                }
            }
        )
    }
}

@Composable
private fun BigCard(title: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier
        .padding(8.dp)
        .fillMaxWidth()
        .height(120.dp)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(title) }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}
