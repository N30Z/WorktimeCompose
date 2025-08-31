package com.example.worktime

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class TimeRepository(
  private val db: AppDb,
  private val dataStore: DataStore<Preferences>
) {

  // --- Laufzeiten (roh, ohne Rundung) ---
  suspend fun effectiveToday(now: Long = System.currentTimeMillis()): Long = withContext(Dispatchers.IO) {
    val start = dayStart(now)
    val end = start + 86_400_000L
    db.sessionDao().effectiveSumBetween(start, end, now)
  }

  suspend fun effectiveWeek(now: Long = System.currentTimeMillis()): Long = withContext(Dispatchers.IO) {
    // Woche startet laut Setting (Default Montag)
    val prefs = dataStore.dataFirstBlocking()
    val startMonday = prefs[SettingsKeys.WEEK_START_MONDAY] ?: true
    val (ws, we) = weekBounds(now, startMonday)
    db.sessionDao().effectiveSumBetween(ws, we, now)
  }

  // --- Session-Steuerung ---
  suspend fun startSession(projectId: Long, now: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
    // Falls bereits eine läuft, ignoriere
    if (db.sessionDao().getRunning() != null) return@withContext
    db.sessionDao().insert(WorkSession(projectId = projectId, startTs = now, startedManuallyAt = false))
  }

  suspend fun startSessionAt(projectId: Long, startTs: Long, manual: Boolean) = withContext(Dispatchers.IO) {
    if (db.sessionDao().getRunning() != null) return@withContext
    db.sessionDao().insert(WorkSession(projectId = projectId, startTs = startTs, startedManuallyAt = manual))
  }

  suspend fun endSession(now: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
    val running = db.sessionDao().getRunning() ?: return@withContext
    db.sessionDao().update(running.copy(endTs = now))
  }

  suspend fun togglePause(now: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
    val running = db.sessionDao().getRunning() ?: return@withContext
    val open = db.pauseDao().getOpenPause(running.id)
    if (open == null) {
      db.pauseDao().insert(PauseSegment(sessionId = running.id, startTs = now))
    } else {
      db.pauseDao().update(open.copy(endTs = now))
    }
  }

  /** Stoppe aktuelle Session (Projekt A), schreibe Eintrag, starte neue Session (Projekt B) */
  suspend fun switchProject(toProjectId: Long, now: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
    val running = db.sessionDao().getRunning()
    if (running != null) {
      db.sessionDao().update(running.copy(endTs = now))
    }
    db.sessionDao().insert(WorkSession(projectId = toProjectId, startTs = now))
  }
}

// --- kleine Blocking-Helper genau hier (damit keine Zyklen entstehen) ---
private fun <T> DataStore<T>.dataFirstBlocking(): T = kotlinx.coroutines.runBlocking { data.first() }
