package com.example.worktime

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class TimeRepository(val db: AppDb, private val dataStore: DataStore<Preferences>) {

  suspend fun startSession(projectId: Long, startTs: Long = System.currentTimeMillis(), manual: Boolean = false): Long =
    withContext(Dispatchers.IO) {
      db.sessionDao().getRunning()?.id
        ?: db.sessionDao().insert(WorkSession(projectId = projectId, startTs = startTs, startedManuallyAt = manual))
    }

  suspend fun startSessionAt(projectId: Long, startTs: Long, manual: Boolean = true): Long =
    startSession(projectId, startTs, manual)

  suspend fun endSession(now: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
    db.sessionDao().getRunning()?.let { s ->
      db.pauseDao().getOpenPause(s.id)?.let { db.pauseDao().update(it.copy(endTs = now)) }
      db.sessionDao().update(s.copy(endTs = now))
    }
  }

  suspend fun togglePause(now: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
    db.sessionDao().getRunning()?.let { s ->
      val p = db.pauseDao().getOpenPause(s.id)
      if (p == null) db.pauseDao().insert(PauseSegment(sessionId = s.id, startTs = now))
      else db.pauseDao().update(p.copy(endTs = now))
    }
  }

  /** Projektwechsel: beendet aktuelle Session und startet neue für newProjectId */
  suspend fun switchProject(newProjectId: Long, at: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
    db.sessionDao().getRunning()?.let { running ->
      // beende alte
      db.pauseDao().getOpenPause(running.id)?.let { db.pauseDao().update(it.copy(endTs = at)) }
      db.sessionDao().update(running.copy(endTs = at))
    }
    // starte neue
    startSession(newProjectId, at, manual = false)
  }

  suspend fun effectiveToday(now: Long = System.currentTimeMillis()): Long = withContext(Dispatchers.IO) {
    val start = dayStart(now); val end = start + 86_400_000L
    db.sessionDao().effectiveSumBetween(start, end, now)
  }

  suspend fun effectiveWeek(now: Long = System.currentTimeMillis(), mondayStart: Boolean = true): Long =
    withContext(Dispatchers.IO) {
      val (ws, we) = weekBounds(now, mondayStart)
      db.sessionDao().effectiveSumBetween(ws, we, now)
    }
}

/** Utils (kannst du auch in TimeUtils.kt lassen, wenn vorhanden) */
fun dayStart(now: Long): Long {
  val c = Calendar.getInstance()
  c.timeInMillis = now
  c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
  return c.timeInMillis
}

fun weekBounds(now: Long, mondayStart: Boolean): Pair<Long, Long> {
  val c = Calendar.getInstance(); c.timeInMillis = now
  c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
  val dow = c.get(Calendar.DAY_OF_WEEK) // So=1..Sa=7
  val shift = if (mondayStart) ((dow + 5) % 7) else ((dow % 7)) // Mo=0..So=6 bzw. So=0..Sa=6
  c.add(Calendar.DAY_OF_MONTH, -shift)
  val start = c.timeInMillis
  val end = start + 7 * 86_400_000L
  return start to end
}

fun msToHHMM(ms: Long): String {
  val totalMin = (ms / 60000).toInt()
  val h = totalMin / 60
  val m = totalMin % 60
  return "%02d:%02d".format(h, m)
}

fun timeStr(ts: Long?): String {
  if (ts == null) return "—"
  val c = Calendar.getInstance().apply { timeInMillis = ts }
  val h = c.get(Calendar.HOUR_OF_DAY)
  val m = c.get(Calendar.MINUTE)
  return String.format(Locale.getDefault(), "%02d:%02d", h, m)
}

