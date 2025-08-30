package com.example.worktime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
      MaterialTheme {
        val nav = rememberNavController()
        var title by remember { mutableStateOf("Worktime") }

        Scaffold(topBar = { TopAppBar(title = { Text(title) }) }) { padding ->
          NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(padding)
          ) {
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
            composable("calendar") {
              title = "Kalender"
              CalendarScreenV2(app) { sid -> nav.navigate("editor/$sid") }
            }
            composable("projects") {
              title = "Projekte"
              // TODO: Projekte-Screen einhängen
              PlaceholderScreen("Projekte (TODO)")
            }
            composable("export") {
              title = "Export"
              // TODO: Export-Screen
              PlaceholderScreen("Export (TODO)")
            }
            composable("settings") {
              title = "Einstellungen"
              // TODO: Settings-Screen (Woche 40h etc.)
              PlaceholderScreen("Settings (TODO)")
            }
            composable(
              route = "editor/{sid}",
              arguments = listOf(navArgument("sid") { type = NavType.LongType })
            ) { backStack ->
              val sid = backStack.arguments?.getLong("sid") ?: return@composable
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
    modifier = Modifier
      .fillMaxSize(),
    contentAlignment = androidx.compose.ui.Alignment.Center
  ) { Text(label) }
}
