package com.example.worktime

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(app: App) {
    val scope = rememberCoroutineScope()
    val prefs by app.dataStore.data.collectAsState(initial = null)

    // Woche / Kalender
    var weekHours by remember { mutableIntStateOf(prefs?.get(SettingsKeys.WEEK_TARGET_HOURS) ?: 40) }
    var weekStartMonday by remember { mutableStateOf(prefs?.get(SettingsKeys.WEEK_START_MONDAY) ?: true) }
    var stateIso by remember { mutableStateOf(prefs?.get(SettingsKeys.HOLIDAY_STATE) ?: "BY") }

    // Rundung
    var roundingMin by remember { mutableIntStateOf(prefs?.get(SettingsKeys.ROUNDING_MINUTES) ?: 0) }
    var roundingMode by remember { mutableStateOf(prefs?.get(SettingsKeys.ROUNDING_MODE) ?: "NONE") }

    // WLAN
    var wifiEnabled by remember { mutableStateOf(prefs?.get(SettingsKeys.WIFI_ENABLED) ?: false) }
    var wifiSsid by remember { mutableStateOf(TextFieldValue(prefs?.get(SettingsKeys.WIFI_SSID) ?: "")) }

    // Trigger
    var checkMin by remember { mutableStateOf(prefs?.get(SettingsKeys.TRIGGER_CHECK_MIN) ?: 15) }
    var lateAfter by remember { mutableIntStateOf(prefs?.get(SettingsKeys.LATE_AFTER_MIN) ?: 10) }

    // Standard-Startzeiten
    val stdKeys = listOf(
        SettingsKeys.STD_START_MON, SettingsKeys.STD_START_TUE, SettingsKeys.STD_START_WED,
        SettingsKeys.STD_START_THU, SettingsKeys.STD_START_FRI, SettingsKeys.STD_START_SAT, SettingsKeys.STD_START_SUN
    )
    var stdStarts by remember {
        mutableStateOf(stdKeys.map { prefs?.get(it) ?: "09:00" }.toMutableList())
    }

    // UI
    var forceDark by remember { mutableStateOf(prefs?.get(SettingsKeys.FORCE_DARK_MODE) ?: false) }
    var projectLabel by remember { mutableStateOf(TextFieldValue(prefs?.get(SettingsKeys.PROJECT_LABEL) ?: "Projekte")) }

    // ======= SSID Runtime-Permissions =======
    var ssidRequestPending by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantMap ->
        val allGranted = grantMap.values.all { it }
        if (allGranted) {
            getCurrentSsid(app)?.let { wifiSsid = TextFieldValue(it) }
        }
        ssidRequestPending = false
    }
    fun requestSsidAndFill() {
        val perms = ssidRequiredPermissions()
        ssidRequestPending = true
        permissionLauncher.launch(perms)
    }

    fun save() = scope.launch(Dispatchers.IO) {
        app.dataStore.edit {
            it[SettingsKeys.WEEK_TARGET_HOURS] = weekHours
            it[SettingsKeys.WEEK_START_MONDAY] = weekStartMonday
            it[SettingsKeys.HOLIDAY_STATE] = stateIso

            it[SettingsKeys.ROUNDING_MINUTES] = roundingMin
            it[SettingsKeys.ROUNDING_MODE] = roundingMode

            it[SettingsKeys.WIFI_ENABLED] = wifiEnabled
            it[SettingsKeys.WIFI_SSID] = wifiSsid.text

            it[SettingsKeys.TRIGGER_CHECK_MIN] = checkMin.coerceIn(15, 60)
            it[SettingsKeys.LATE_AFTER_MIN] = lateAfter.coerceIn(0, 120)

            stdKeys.forEachIndexed { idx, key -> it[key] = stdStarts[idx] }

            it[SettingsKeys.FORCE_DARK_MODE] = forceDark
            it[SettingsKeys.PROJECT_LABEL] = projectLabel.text.ifBlank { "Projekte" }
        }
        TriggerScheduler.scheduleOrCancel(app)
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Einstellungen", style = MaterialTheme.typography.titleLarge)

        // Woche & Kalender
        ElevatedCard {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Woche & Kalender")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = weekHours.toString(),
                        onValueChange = { v -> weekHours = v.toIntOrNull()?.coerceIn(0, 80) ?: weekHours },
                        label = { Text("Stunden/Woche") }, singleLine = true, modifier = Modifier.width(160.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = weekStartMonday, onCheckedChange = { weekStartMonday = it })
                        Text("Woche beginnt Montag")
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bundesland:")
                    val states = listOf("BW","BY","BE","BB","HB","HH","HE","MV","NI","NW","RP","SL","SN","ST","SH","TH")
                    states.forEach { s ->
                        AssistChip(
                            onClick = { stateIso = s },
                            label = { Text(s) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (stateIso == s) MaterialTheme.colorScheme.primary.copy(0.15f) else MaterialTheme.colorScheme.surface
                            )
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                }
            }
        }

        // Rundung
        ElevatedCard {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Rundung")
                val options = listOf(0,5,10,15)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { opt ->
                        FilterChip(selected = roundingMin == opt, onClick = { roundingMin = opt },
                            label = { Text(if (opt==0) "Keine" else "${opt}m") })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("NONE" to "Aus", "NEAREST" to "Nächste", "DOWN" to "Ab", "UP" to "Auf").forEach { (v, label) ->
                        FilterChip(selected = roundingMode == v, onClick = { roundingMode = v }, label = { Text(label) })
                    }
                }
                Text("Rundung wirkt auf Anzeige/Export, DB speichert Rohwerte.")
            }
        }

        // WLAN + Trigger
        ElevatedCard {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("WLAN & Trigger")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = wifiEnabled, onCheckedChange = { wifiEnabled = it })
                        Text("WLAN-Erkennung aktiv")
                    }
                }

                if (wifiEnabled) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = wifiSsid,
                            onValueChange = { wifiSsid = it },
                            label = { Text("SSID") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            enabled = !ssidRequestPending,
                            onClick = { requestSsidAndFill() }
                        ) { Text("Aktuelles WLAN übernehmen") }
                    }
                    Text("Hinweis: Auf Android 10–12 muss der Gerätestandort aktiv sein, sonst ist die SSID nicht lesbar.",
                        style = MaterialTheme.typography.bodySmall)
                }

                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                Text("Trigger")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = checkMin.toString(), onValueChange = { checkMin = it.toIntOrNull() ?: checkMin },
                        label = { Text("Prüfintervall (Min)") }, singleLine = true, modifier = Modifier.width(180.dp))
                    OutlinedTextField(value = lateAfter.toString(), onValueChange = { lateAfter = it.toIntOrNull() ?: lateAfter },
                        label = { Text("Zu spät nach (Min)") }, singleLine = true, modifier = Modifier.width(180.dp))
                }
                Text("Benachrichtigungen: • Am Ort (SSID), aber nach Plan nicht eingeloggt • Ort verlassen bei laufender Session",
                    style = MaterialTheme.typography.bodySmall)
            }
        }

        // Standard-Arbeitsbeginn
        ElevatedCard {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Standard-Arbeitsbeginn (HH:mm)")
                val labels = listOf("Mo","Di","Mi","Do","Fr","Sa","So")
                for (i in 0..6) {
                    OutlinedTextField(
                        value = stdStarts[i],
                        onValueChange = { v -> if (v.matches(Regex("^\\d{0,2}:?\\d{0,2}\$"))) stdStarts[i] = normalizeTimeInput(v) },
                        label = { Text(labels[i]) },
                        singleLine = true,
                        modifier = Modifier.width(120.dp)
                    )
                }
            }
        }

        // UI
        ElevatedCard {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Darstellung")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = forceDark, onCheckedChange = { forceDark = it })
                    Text("Dark Mode erzwingen")
                }
                OutlinedTextField(projectLabel, { projectLabel = it }, label = { Text("Bezeichnung für Projekte (z. B. Kunden)") }, singleLine = true)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(onClick = { save() }) { Text("Speichern") }
        }
    }
}

private fun normalizeTimeInput(v: String): String {
    val raw = v.replace(":", "")
    if (raw.isEmpty()) return ""
    val hh = raw.take(2).padStart(2,'0').take(2)
    val mm = raw.drop(2).padEnd(2,'0').take(2)
    val h = hh.toInt().coerceIn(0, 23)
    val m = mm.toInt().coerceIn(0, 59)
    return "%02d:%02d".format(h, m)
}
