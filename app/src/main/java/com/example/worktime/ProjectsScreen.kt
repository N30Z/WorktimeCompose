package com.example.worktime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(app: App, onOpenProject: (Long) -> Unit) {
    val scope = rememberCoroutineScope()
    val projectsAll by app.db.projectDao().getAll().collectAsState(initial = emptyList())
    var showNew by remember { mutableStateOf(false) }

    val prefs by app.dataStore.data.collectAsState(initial = null)
    val projLabel = prefs?.get(SettingsKeys.PROJECT_LABEL) ?: "Projekte"

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(projLabel, style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = { showNew = true }) { Text("+ Neu") }
        }

        var tab by remember { mutableStateOf(0) }
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab==0, onClick = { tab = 0 }, text = { Text("Aktiv") })
            Tab(selected = tab==1, onClick = { tab = 1 }, text = { Text("Archiv") })
        }

        val filtered = projectsAll.filter { if (tab==0) !it.isArchived else it.isArchived }

        LazyColumn {
            items(filtered, key = { it.id }) { p ->
                ProjectRow(
                    app = app,
                    project = p,
                    onClick = { onOpenProject(p.id) },
                    onArchiveToggle = {
                        scope.launch(Dispatchers.IO) {
                            if (p.isArchived) app.db.projectDao().unarchive(p.id) else app.db.projectDao().archive(p.id)
                        }
                    }
                )
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            }
        }
    }

    if (showNew) {
        NewProjectDialog(
            onDismiss = { showNew = false },
            onCreate = { name, number ->
                scope.launch(Dispatchers.IO) { app.db.projectDao().insert(Project(name = name, number = number)) }
                showNew = false
            }
        )
    }
}

@Composable
private fun ProjectRow(
    app: App,
    project: Project,
    onClick: () -> Unit,
    onArchiveToggle: () -> Unit
) {
    val totalMs by produceState(0L, project.id) {
        value = withContext(Dispatchers.IO) { app.db.sessionDao().effectiveSumForProject(project.id, System.currentTimeMillis()) }
    }

    ListItem(
        headlineContent = { Text(project.name, fontWeight = FontWeight.SemiBold) },
        supportingContent = {
            Text(buildString {
                append("Zeit gesamt: "); append(msToHHMM(totalMs))
                project.number?.takeIf { it.isNotBlank() }?.let { append(" • Nr: "); append(it) }
                if (project.isArchived) append(" • (archiviert)")
            })
        },
        trailingContent = {
            IconButton(onClick = onArchiveToggle) {
                if (project.isArchived) Icon(Icons.Filled.Unarchive, contentDescription = "Reaktivieren")
                else Icon(Icons.Filled.Archive, contentDescription = "Archivieren")
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .background(Color.Transparent)
            .clickable { onClick() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(app: App, projectId: Long, onBack: () -> Unit) {
    val p by produceState<Project?>(initialValue = null, projectId) {
        value = withContext(Dispatchers.IO) { app.db.projectDao().getByIdSync(projectId) }
    }
    val sessions by produceState(initialValue = emptyList<WorkSession>(), projectId) {
        value = withContext(Dispatchers.IO) { app.db.sessionDao().sessionsForProject(projectId) }
    }

    val grouped = remember(sessions) {
        sessions.groupBy { dayStart(it.startTs) }.toSortedMap()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(p?.name ?: "Projekt") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.Unarchive, contentDescription = "Zurück") } // Platzhalter-Icon
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            p?.number?.let { Text("Projekt-Nr: $it", modifier = Modifier.padding(12.dp)) }
            LazyColumn {
                grouped.forEach { (dayStartMillis, list) ->
                    item {
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = dayStartMillis }
                        val title = "%02d.%02d.%04d".format(
                            cal.get(java.util.Calendar.DAY_OF_MONTH),
                            cal.get(java.util.Calendar.MONTH)+1,
                            cal.get(java.util.Calendar.YEAR)
                        )
                        val eff = list.sumOf { s -> (app.db.sessionDao().effectiveForSession(s.id, System.currentTimeMillis()) ?: 0L) }
                        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text("$title  •  ${msToHHMM(eff)}", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    items(list) { s ->
                        val eff by produceState(0L, s.id) {
                            value = withContext(Dispatchers.IO) { app.db.sessionDao().effectiveForSession(s.id, System.currentTimeMillis()) ?: 0L }
                        }
                        ListItem(
                            headlineContent = { Text("${timeStr(s.startTs)} – ${s.endTs?.let { timeStr(it) } ?: "läuft"}") },
                            supportingContent = { Text("Effektiv: ${msToHHMM(eff)}") }
                        )
                        HorizontalDivider(
                            Modifier,
                            DividerDefaults.Thickness,
                            DividerDefaults.color
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NewProjectDialog(onDismiss: () -> Unit, onCreate: (name: String, number: String?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onCreate(name.trim(), number.trim().ifBlank { null }) }) { Text("Erstellen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
        title = { Text("Neues Projekt") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("Projektnummer (optional)") })
            }
        }
    )
}
