package com.example.worktime

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

val Application.appDataStore: DataStore<Preferences> by preferencesDataStore("settings")

class App : Application() {
  lateinit var db: AppDb
  lateinit var repo: TimeRepository
  lateinit var appScope: CoroutineScope
  lateinit var dataStore: DataStore<Preferences>

  override fun onCreate() {
    super.onCreate()
    dataStore = appDataStore
    db = AppDb.build(this)
    repo = TimeRepository(db, dataStore)
    appScope = CoroutineScope(SupervisorJob())
  }
}
