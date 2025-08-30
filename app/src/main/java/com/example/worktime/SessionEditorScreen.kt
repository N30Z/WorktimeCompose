package com.example.worktime

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

@Composable
fun SessionEditorScreen(app: App, sessionId: Long, onClose: () -> Unit) {
  val scope = rememberCoroutineScope()

  // Startzustände aus DB lesen (einmalig, hier synchron ok; alternativ als produceState auf IO laden)
  val s = remember { mutableStateOf(app.db.sessionDao().getById(sessionId)) }
  val pauses = remember {
    mutableStateListOf<PauseSegment>().also { it += app.db.pauseDao().allForSession(sessionId) }
  }

  if (s.value == null) { onClose(); return }
  val session = s.value!!

  val ctx = LocalContext.current

  fun pickTime(context: Context, ts: Long?, onSet: (Long) -> Unit) {
    val base = ts ?: System.currentTimeMillis()
    val c = Calendar.getInstance().apply { timeInMillis = base }
    TimePickerDialog(
      context,
      { _, hh, mm -> onSet(dayStart(base) + (hh * 60 + mm) * 60_000L) },
      c.get(Calendar.HOUR_OF_DAY),
      c.get(Calendar.MINUTE),
      true
    ).show()
  }

  Dialog(onDismissRequest = onClose) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text("Session bearbeiten", style = MaterialTheme.typography.titleLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(onClick = {
            pickTime(ctx, session.startTs) { new ->
              if (new != session.startTs) {
                scope.launch(Dispatchers.IO) {
                  app.db.sessionDao().update(session.copy(startTs = new))
                  app.db.editDao().insert(
                    SessionEdit(
                      sessionId = sessionId,
                      field = "startTs",
                      oldValue = session.startTs.toString(),
                      newValue = new.toString(),
                      reason = null
                    )
                  )
                  val refreshed = app.db.sessionDao().getById(sessionId)
                  withContext(Dispatchers.Main) { s.value = refreshed }
                }
              }
            }
          }) { Text("Start: ${timeStr(session.startTs)}") }

          OutlinedButton(onClick = {
            pickTime(ctx, session.endTs ?: System.currentTimeMillis()) { new ->
              if (new != session.endTs) {
                scope.launch(Dispatchers.IO) {
                  app.db.sessionDao().update(session.copy(endTs = new))
                  app.db.editDao().insert(
                    SessionEdit(
                      sessionId = sessionId,
                      field = "endTs",
                      oldValue = session.endTs?.toString(),
                      newValue = new.toString(),
                      reason = null
                    )
                  )
                  val refreshed = app.db.sessionDao().getById(sessionId)
                  withContext(Dispatchers.Main) { s.value = refreshed }
                }
              }
            }
          }) { Text("Ende: ${session.endTs?.let { timeStr(it) } ?: "—"}") }
        }

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        Text("Pausen")

        pauses.forEach { ps ->
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedButton(onClick = {
              pickTime(ctx, ps.startTs) { new ->
                if (new != ps.startTs) {
                  scope.launch(Dispatchers.IO) {
                    app.db.pauseDao().update(ps.copy(startTs = new))
                    app.db.editDao().insert(
                      SessionEdit(
                        sessionId = sessionId,
                        field = "pause_edit",
                        oldValue = "${ps.startTs}-${ps.endTs}",
                        newValue = "$new-${ps.endTs}",
                        reason = null
                      )
                    )
                  }
                }
              }
            }) { Text(timeStr(ps.startTs)) }

            Text("–")

            OutlinedButton(onClick = {
              pickTime(ctx, ps.endTs ?: System.currentTimeMillis()) { new ->
                if (new != ps.endTs) {
                  scope.launch(Dispatchers.IO) {
                    app.db.pauseDao().update(ps.copy(endTs = new))
                    app.db.editDao().insert(
                      SessionEdit(
                        sessionId = sessionId,
                        field = "pause_edit",
                        oldValue = "${ps.startTs}-${ps.endTs}",
                        newValue = "${ps.startTs}-$new",
                        reason = null
                      )
                    )
                  }
                }
              }
            }) { Text(ps.endTs?.let { timeStr(it) } ?: "—") }

            IconButton(onClick = {
              scope.launch(Dispatchers.IO) {
                app.db.pauseDao().delete(ps)
                app.db.editDao().insert(
                  SessionEdit(
                    sessionId = sessionId,
                    field = "pause_delete",
                    oldValue = "${ps.startTs}-${ps.endTs}",
                    newValue = null,
                    reason = null
                  )
                )
                withContext(Dispatchers.Main) { pauses.remove(ps) }
              }
            }) { Icon(Icons.Default.Delete, contentDescription = null) }
          }
        }

        TextButton(onClick = {
          val base = dayStart(session.startTs) + 12 * 60 * 60_000L
          val np = PauseSegment(sessionId = sessionId, startTs = base, endTs = base + 30 * 60_000L)
          scope.launch(Dispatchers.IO) {
            val id = app.db.pauseDao().insert(np)
            app.db.editDao().insert(
              SessionEdit(
                sessionId = sessionId,
                field = "pause_add",
                oldValue = null,
                newValue = "${np.startTs}-${np.endTs}",
                reason = null
              )
            )
            withContext(Dispatchers.Main) { pauses.add(np.copy(id = id)) }
          }
        }) { Text("+ Pause hinzufügen") }

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        // Effektivzeit der Session asynchron berechnen
        val eff by produceState(0L, session.id) {
          value = withContext(Dispatchers.IO) {
            app.db.sessionDao().effectiveForSession(session.id, System.currentTimeMillis()) ?: 0L
          }
        }
        Text("Effektiv: ${msToHHMM(eff)}")

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          Button(onClick = onClose) { Text("Fertig") }
        }
      }
    }
  }
}
