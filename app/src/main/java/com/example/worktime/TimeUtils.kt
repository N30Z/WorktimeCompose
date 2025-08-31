package com.example.worktime

import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

fun timeStr(ts: Long?): String {
    if (ts == null) return "—"
    val c = Calendar.getInstance().apply { timeInMillis = ts }
    val h = c.get(Calendar.HOUR_OF_DAY); val m = c.get(Calendar.MINUTE)
    return String.format(Locale.getDefault(), "%02d:%02d", h, m)
}

fun msToHHMM(ms: Long): String {
    val totalMin = (ms / 60000).toInt()
    val h = totalMin / 60; val m = totalMin % 60
    return String.format(Locale.getDefault(), "%02d:%02d", h, m)
}

fun dayStart(ts: Long): Long {
    val c = Calendar.getInstance().apply { timeInMillis = ts }
    c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

fun weekBounds(dayMillis: Long, weekStartMonday: Boolean): Pair<Long, Long> {
    val c = Calendar.getInstance().apply { timeInMillis = dayMillis }
    val dow = c.get(Calendar.DAY_OF_WEEK) // So=1..Sa=7
    val shift = if (weekStartMonday) ((dow + 5) % 7) else (dow % 7)
    c.add(Calendar.DAY_OF_MONTH, -shift)
    val start = dayStart(c.timeInMillis)
    val end = start + 7 * 86_400_000L
    return start to end
}

/** Rundet Dauer (ms). mode: NONE/NEAREST/DOWN/UP, minutes: 0/5/10/15 */
fun roundDuration(ms: Long, minutes: Int, mode: String): Long {
    if (minutes <= 0 || mode == "NONE") return ms
    val unit = (minutes * 60_000L).toDouble()
    val q = ms.toDouble() / unit
    val factor = when (mode) {
        "NEAREST" -> round(q)
        "DOWN"    -> floor(q)
        "UP"      -> ceil(q)
        else      -> q
    }
    return (factor * unit).toLong()
}

/** Bundesland-Feiertage (relevante). */
fun isHolidayDE(stateIso: String, millis: Long): Boolean {
    val c = Calendar.getInstance().apply { timeInMillis = millis }
    val y = c.get(Calendar.YEAR)
    fun md(y: Int, m: Int, d: Int): Long {
        val cc = Calendar.getInstance(); cc.clear(); cc.set(y, m-1, d, 0,0,0); return cc.timeInMillis
    }
    val easter = easterSunday(y)
    fun offset(d: Int) = Calendar.getInstance().apply { timeInMillis = easter; add(Calendar.DAY_OF_YEAR, d) }.timeInMillis
    val today = dayStart(millis)

    // Bundesweit
    if (today in setOf(md(y,1,1), md(y,5,1), md(y,10,3), md(y,12,25), md(y,12,26))) return true
    if (today in setOf(offset(-2), offset(1), offset(39), offset(50))) return true // Karfr, Ostermontag, Himmelfahrt, Pfingstmontag

    // Länder
    if (today == md(y,1,6)  && stateIso in setOf("BY","BW","ST")) return true // Hl. Drei Könige
    if (today == offset(60) && stateIso in setOf("BW","BY","HE","NW","RP","SL")) return true // Fronleichnam
    if (today == md(y,11,1) && stateIso in setOf("BW","BY","NW","RP","SL")) return true // Allerheiligen
    if (today == md(y,10,31) && stateIso in setOf("BB","HB","HH","MV","NI","SN","ST","SH","TH")) return true // Reformationstag
    if (today == md(y,8,15) && stateIso in setOf("BY","SL")) return true // Mariae Himmelfahrt (teils BY)
    if (today == md(y,3,8)  && stateIso == "BE") return true // Frauentag Berlin
    if (today == md(y,9,20) && stateIso == "TH") return true // Weltkindertag TH

    // Buß- und Bettag (SN: Mittwoch vor 23.11.)
    if (stateIso == "SN" && today == busUndBettag(y)) return true

    return false
}

private fun easterSunday(year: Int): Long {
    val a = year % 19
    val b = year / 100
    val c = year % 100
    val d = b / 4
    val e = b % 4
    val g = (8 * b + 13) / 25
    val h = (19 * a + b - d - g + 15) % 30
    val j = c / 4
    val k = c % 4
    val m = (a + 11 * h) / 319
    val r = (2 * e + 2 * j - k - h + m + 32) % 7
    val n = (h - m + r + 90) / 25
    val p = (h - m + r + 90) % 25 + 1
    val cal = Calendar.getInstance(); cal.clear(); cal.set(year, n-1, p, 0,0,0)
    return cal.timeInMillis
}

private fun busUndBettag(year: Int): Long {
    val c = Calendar.getInstance(); c.clear(); c.set(year, Calendar.NOVEMBER, 23, 0,0,0)
    while (c.get(Calendar.DAY_OF_WEEK) != Calendar.WEDNESDAY) c.add(Calendar.DAY_OF_MONTH, -1)
    c.add(Calendar.DAY_OF_MONTH, -7)
    return c.timeInMillis
}
