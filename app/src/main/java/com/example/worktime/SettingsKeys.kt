package com.example.worktime

import androidx.datastore.preferences.core.*

object SettingsKeys {
  // Woche / Kalender
  val WEEK_TARGET_HOURS   = intPreferencesKey("week_target_hours")      // default 40
  val WEEK_START_MONDAY   = booleanPreferencesKey("week_start_monday")  // default true
  val HOLIDAY_STATE       = stringPreferencesKey("holiday_state")        // "BY","NW",...

  // Rundung
  val ROUNDING_MINUTES    = intPreferencesKey("rounding_minutes")        // 0,5,10,15
  val ROUNDING_MODE       = stringPreferencesKey("rounding_mode")        // "NONE","NEAREST","DOWN","UP"

  // WLAN (einziges Ortskriterium)
  val WIFI_ENABLED        = booleanPreferencesKey("wifi_enabled")        // default false
  val WIFI_SSID           = stringPreferencesKey("wifi_ssid")

  // Trigger
  val TRIGGER_CHECK_MIN   = intPreferencesKey("trigger_check_min")       // Intervall WorkManager, default 15
  val LATE_AFTER_MIN      = intPreferencesKey("late_after_min")          // X Minuten nach Beginn, default 10

  // Standard-Arbeitsbeginn (pro Wochentag HH:mm)
  val STD_START_MON       = stringPreferencesKey("std_start_mon")
  val STD_START_TUE       = stringPreferencesKey("std_start_tue")
  val STD_START_WED       = stringPreferencesKey("std_start_wed")
  val STD_START_THU       = stringPreferencesKey("std_start_thu")
  val STD_START_FRI       = stringPreferencesKey("std_start_fri")
  val STD_START_SAT       = stringPreferencesKey("std_start_sat")
  val STD_START_SUN       = stringPreferencesKey("std_start_sun")

  // UI
  val FORCE_DARK_MODE     = booleanPreferencesKey("force_dark_mode")     // default false
  val PROJECT_LABEL       = stringPreferencesKey("project_label")        // z.B. "Kunden"

  // NEU: letztes ausgewähltes Projekt
  val LAST_PROJECT_ID     = longPreferencesKey("last_project_id")
}
