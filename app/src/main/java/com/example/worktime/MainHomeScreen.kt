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
import kotlinx.coroutines.flow.first
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

    // Projekte (aktiv) laden
    val projectsFlow = remember { app.db.projectDao().getActive() }
    val projects by projectsFlow.collectAsState(initial = emptyList())

    // Letztes/aktuelles Projekt
    var currentProjectId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(projects) {
        if (currentProjectId == null && projects.isNotEmpty()) {
            // versuche gespeichertes letztes Projekt zu laden (optional)
            currentProjectId = withContext(Dispatchers.IO) {
                // falls du einen Key dafür hast – sonst nimm erstes aktives
                projects.firstOrNull()?.id
            }
        }
    }

    // Laufende Session + Zeiten
    val runningSession by produceState<WorkSession?>(initialValue = null, projects) {
        value = withContext(Dispatchers.IO) { app.db.sessionDao().getRunning() }
    }
    val todayMs by produceState(0L, runningSession) {
        value = withContext(Dispatchers.IO) { app.repo.effectiveToday() }
    }
    val weekMs by produceState(0L, runningSession) {
        value = withContext(Dispatchers.IO) { app.repo.effectiveWeek() }
    }
    val currentProjectMs by produceState(0L, runningSession, currentProjectId) {
        value = withContext(Dispatchers.IO) {
            currentProjectId?.let { app.db.sessionDao().effectiveSumForProject(it, System.currentTimeMillis()) } ?: 0L
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // OBEREN 3/4: Buttons zu anderen Screens
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(3f)
        ) {
            item { BigCard("Projekte", onOpenProjects) }
            item { BigCard("Export",   onOpenExport) }
            item { BigCard("Kalender", onOpenCalendar) }
            item { BigCard("Settings", onOpenSettings) }
        }

        // UNTERES 1/4: Timer/Steuerung
        Column(Modifier.weight(1f)) {
            // obere Zeile: Heute / Woche / aktuelles Projekt
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Heute", msToHHMM(todayMs), Modifier.weight(1f))
                StatCard("Woche", msToHHMM(weekMs),  Modifier.weight(1f))
                val projLabel = projects.firstOrNull { it.id == currentProjectId }?.name ?: "—"
                StatCard("Projekt", "${msToHHMM(currentProjectMs)} ($projLabel)", Modifier.weight(1f))
            }

            Spacer(Modifier.height(8.dp))

            // Projekt-Auswahl + +Button
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var expanded by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.weight(1f)) {
                    Text(projects.firstOrNull { it.id == currentProjectId }?.name ?: "Projekt wählen")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    projects.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.name) },
                            onClick = {
                                expanded = false
                                currentProjectId = p.id
                            }
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = {
                    // TODO: Dialog „Neues Projekt“
                }) { Text("+") }
            }

            Spacer(Modifier.height(8.dp))

            // Login/Start/Stop/Pause/Wechsel
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (runningSession == null) {
                    Button(
                        onClick = {
                            val pid = currentProjectId ?: return@Button
                            scope.launch(Dispatchers.IO) {
                                app.repo.startSession(pid)
                                withContext(Dispatchers.Main) { /* Trigger Recompose: neu einlesen */ }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Einloggen") }

                    OutlinedButton(
                        onClick = {
                            val pid = currentProjectId ?: return@OutlinedButton
                            // „Fing an um“ – manueller Startzeitpunkt
                            val now = java.util.Calendar.getInstance()
                            TimePickerDialog(ctx, { _, hh, mm ->
                                scope.launch(Dispatchers.IO) {
                                    val base = dayStart(System.currentTimeMillis()) + (hh * 60 + mm) * 60_000L
                                    app.repo.startSessionAt(pid, base, manual = true)
                                }
                            }, now.get(java.util.Calendar.HOUR_OF_DAY), now.get(java.util.Calendar.MINUTE), true).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Fing an um") }
                } else {
                    Button(
                        onClick = {
                            scope.launch(Dispatchers.IO) { app.repo.endSession() }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Ausloggen") }

                    OutlinedButton(
                        onClick = { scope.launch(Dispatchers.IO) { app.repo.togglePause() } },
                        modifier = Modifier.weight(1f)
                    ) { Text("Pause") }

                    OutlinedButton(
                        onClick = {
                            // Projekt wechseln: stoppe aktuelle Session und starte neue für gewähltes Projekt
                            val toId = currentProjectId ?: return@OutlinedButton
                            scope.launch(Dispatchers.IO) { app.repo.switchProject(toId) }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Projekt wechseln") }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Timer seit Einloggen
            val sinceLogin by produceState<String>(initialValue = "—", runningSession?.id) {
                if (runningSession == null) {
                    value = "—"
                } else {
                    while (true) {
                        val now = System.currentTimeMillis()
                        val dur = (runningSession!!.startTs).let { now - it }
                        value = msToHHMM(dur)
                        kotlinx.coroutines.delay(1_000)
                    }
                }
            }
            Text("Seit Einloggen: $sinceLogin")
        }
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
