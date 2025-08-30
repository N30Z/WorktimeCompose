package com.example.worktime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.util.Calendar

/**
 * Raster-Kalender für einen Monat mit Summen pro Woche (links).
 *
 * @param computeWeekSum Funktion, die für (wsMillis, weMillis) die Arbeitszeit in ms zurückliefert.
 */
@Composable
fun WeekSumCalendar(
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    targetHours: Int,
    computeWeekSum: @Composable (Long, Long) -> Long
) {
    val month = selected.withDayOfMonth(1)
    // Starte bei Montag der ersten Rasterzeile
    val firstCell = month.minusDays(((month.dayOfWeek.value + 6) % 7).toLong())
    val days = (0 until 42).map { firstCell.plusDays(it.toLong()) }

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("KW/Σ", modifier = Modifier.width(72.dp))
            listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So").forEach {
                Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }
        for (w in 0 until 6) {
            val weekDays = days.drop(w * 7).take(7)
            val mondayMillis = Calendar.getInstance().apply {
                set(weekDays.first().year, weekDays.first().monthValue - 1, weekDays.first().dayOfMonth, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val (ws, we) = weekBounds(mondayMillis, true)
            val sumMs = computeWeekSum(ws, we) // kommt von CalendarScreenV2

            val targetMs = targetHours * 3_600_000L
            val color = when {
                sumMs >= targetMs -> Color(0xFF2E7D32) // grün
                sumMs >= targetMs * 3 / 4 -> Color(0xFFF9A825) // gelb
                else -> Color(0xFFC62828) // rot
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.width(72.dp).height(24.dp)
                        .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(msToHHMM(sumMs), color = color, style = MaterialTheme.typography.bodyMedium)
                }
                weekDays.forEach { d ->
                    val inMonth = d.month == month.month
                    val isSel = d == selected
                    val bg = if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
                    val col = if (inMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    Box(
                        Modifier.weight(1f).aspectRatio(1f)
                            .background(bg, RoundedCornerShape(8.dp))
                            .clickable { onSelect(d) },
                        contentAlignment = Alignment.Center
                    ) { Text(d.dayOfMonth.toString(), color = col) }
                }
            }
        }
    }
}
