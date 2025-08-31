package com.example.worktime

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Application.dataStoreDelegate: DataStore<Preferences> by preferencesDataStore(name = "settings")

class App : Application() {
  lateinit var db: AppDb
    private set

  val dataStore: DataStore<Preferences> by lazy { this.dataStoreDelegate }

  lateinit var repo: TimeRepository
    private set

  override fun onCreate() {
    super.onCreate()
    db = AppDb.build(this)
    repo = TimeRepository(db, dataStore)
    // Trigger nur wenn aktiviert
    TriggerScheduler.scheduleOrCancel(this)
  }
}
