package com.example.worktime

import androidx.datastore.preferences.core.*

object SettingsKeys {
  val WEEK_TARGET_HOURS = intPreferencesKey("week_target_hours")
  val WEEK_START_MONDAY = booleanPreferencesKey("week_start_monday")
}
