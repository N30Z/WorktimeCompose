package com.example.worktime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val app = application as App
    setContent {
      WorktimeTheme(app) {
        val nav = rememberNavController()
        var title by remember { mutableStateOf("Worktime") }
        val prefs by app.dataStore.data.collectAsState(initial = null)
        val projLabel = prefs?.get(SettingsKeys.PROJECT_LABEL) ?: "Projekte"

        Scaffold(topBar = { TopAppBar(title = { Text(title) }) }) { padding ->
          NavHost(navController = nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") {
              title = "Worktime"
              MainHomeScreen(
                app = app,
                onOpenProjects = { nav.navigate("projects") },
                onOpenExport   = { nav.navigate("export") },
                onOpenCalendar = { nav.navigate("calendar") },
                onOpenSettings = { nav.navigate("settings") }
              )
            }
            composable("calendar") { title = "Kalender"; CalendarScreenV2(app) { sid -> nav.navigate("editor/$sid") } }
            composable("projects") { title = projLabel; ProjectsScreen(app) { pid -> nav.navigate("project/$pid") } }
            composable("project/{pid}", arguments = listOf(navArgument("pid"){ type = NavType.LongType })) { back ->
              val pid = back.arguments?.getLong("pid") ?: return@composable
              title = projLabel
              ProjectDetailScreen(app, projectId = pid) { nav.popBackStack() }
            }
            composable("export")   { title = "Export";   PlaceholderScreen("Export (TODO)") }
            composable("settings") { title = "Einstellungen"; SettingsScreen(app) }
            composable("editor/{sid}", arguments = listOf(navArgument("sid"){ type = NavType.LongType })) { back ->
              val sid = back.arguments?.getLong("sid") ?: return@composable
              SessionEditorScreen(app = app, sessionId = sid) { nav.popBackStack() }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PlaceholderScreen(label: String) {
  androidx.compose.foundation.layout.Box(
    Modifier.fillMaxSize(),
    contentAlignment = androidx.compose.ui.Alignment.Center
  ) { Text(label) }
}
