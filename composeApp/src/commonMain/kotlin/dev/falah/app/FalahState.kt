package dev.falah.app

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.falah.core.AsrFactor
import dev.falah.core.DayTimes
import dev.falah.core.Hijri
import dev.falah.core.Method
import dev.falah.core.Prayer
import dev.falah.core.PrayerLog
import dev.falah.core.PrayerTimes
import dev.falah.core.TasbihLog
import dev.falah.core.of
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** The next prayer, as hours from midnight today (past 24 when it is tomorrow's), and how long the stretch since the one before it is. */
data class NextPrayer(val prayer: Prayer, val at: Double, val tomorrow: Boolean, val progress: Float)

class FalahState(val prefs: Prefs, val files: AppFiles, val platform: Platform) {
    var placeName by mutableStateOf("Cairo")
    var lat by mutableStateOf(30.0444)
    var lon by mutableStateOf(31.2357)
    var method by mutableStateOf(Method.Egyptian)
    var asr by mutableStateOf(AsrFactor.Standard)
    var themeMode by mutableStateOf("system")
    var langChoice by mutableStateOf("system")
    var arabicDigits by mutableStateOf(false)
    var hijriAdjust by mutableIntStateOf(0)
    var now by mutableStateOf(Clock.System.now())
    var log by mutableStateOf(PrayerLog())
    var notifyTick by mutableIntStateOf(0)
    var tasbih by mutableStateOf(TasbihLog())

    init { reload() }

    /** Reads everything again from the saved values and files (at start, and after the data folder changed). */
    fun reload() {
        placeName = prefs.get("place") ?: "Cairo"
        lat = prefs.get("lat")?.toDoubleOrNull() ?: 30.0444
        lon = prefs.get("lon")?.toDoubleOrNull() ?: 31.2357
        method = Method.entries.firstOrNull { it.name == prefs.get("method") } ?: Method.Egyptian
        asr = AsrFactor.entries.firstOrNull { it.name == prefs.get("asr") } ?: AsrFactor.Standard
        themeMode = prefs.get("theme") ?: "system"
        langChoice = prefs.get("lang") ?: "system"
        arabicDigits = prefs.get("digits") == "ar"
        hijriAdjust = prefs.get("hijri.adjust")?.toIntOrNull() ?: 0
        log = PrayerLog.decode(files.read(LOG_FILE).orEmpty())
        tasbih = TasbihLog.decode(files.read(TASBIH_FILE).orEmpty())
    }

    private val zone get() = TimeZone.currentSystemDefault()
    val local get() = now.toLocalDateTime(zone)
    val today get() = local.date
    val hourNow: Double get() = local.hour + local.minute / 60.0 + local.second / 3600.0
    private val offset get() = zone.offsetAt(now).totalSeconds / 3600.0

    val times: DayTimes by derivedStateOf { PrayerTimes.compute(today, lat, lon, offset, method, asr) }
    val hijri get() = Hijri.of(today, hijriAdjust)

    val next: NextPrayer by derivedStateOf {
        val h = hourNow
        val order = Prayer.entries.filter { it != Prayer.Sunrise }
        val upcoming = order.firstOrNull { (times.of(it) ?: -1.0) > h }
        val nextAt: Double; val prayer: Prayer; val tomorrow: Boolean
        if (upcoming != null) { prayer = upcoming; nextAt = times.of(upcoming)!!; tomorrow = false }
        else {
            val t = PrayerTimes.compute(today.plus(1, DateTimeUnit.DAY), lat, lon, offset, method, asr)
            prayer = Prayer.Fajr; nextAt = (t.fajr ?: 5.0) + 24.0; tomorrow = true
        }
        val before = Prayer.entries.mapNotNull { times.of(it) }.filter { it <= h }.maxOrNull() ?: ((times.isha ?: 20.0) - 24.0)
        NextPrayer(prayer, nextAt, tomorrow, ((h - before) / (nextAt - before)).toFloat().coerceIn(0f, 1f))
    }

    fun minutesLeft(): Int = ((next.at - hourNow) * 60).toInt().coerceAtLeast(0)

    fun choosePlace(p: Place) {
        placeName = p.name; lat = p.latitude; lon = p.longitude
        prefs.set("place", p.name); prefs.set("lat", p.latitude.toString()); prefs.set("lon", p.longitude.toString()); platform.reschedule()
    }
    fun chooseMethod(m: Method) { method = m; prefs.set("method", m.name); platform.reschedule() }
    fun chooseAsr(a: AsrFactor) { asr = a; prefs.set("asr", a.name); platform.reschedule() }
    fun chooseTheme(mode: String) { themeMode = mode; prefs.set("theme", mode) }
    fun chooseLanguage(choice: String) { langChoice = choice; prefs.set("lang", choice) }
    fun chooseDigits(arabic: Boolean) { arabicDigits = arabic; prefs.set("digits", if (arabic) "ar" else "en") }
    fun chooseHijriAdjust(days: Int) { hijriAdjust = days; prefs.set("hijri.adjust", days.toString()) }

    fun notifyOn(p: Prayer): Boolean { notifyTick; return prefs.get("notify.${p.name}") == "1" }
    fun setNotify(p: Prayer, on: Boolean) {
        if (on) platform.ensureNotifications { ok -> if (ok) { prefs.set("notify.${p.name}", "1"); platform.reschedule(); notifyTick++ } }
        else { prefs.set("notify.${p.name}", "0"); platform.reschedule(); notifyTick++ }
    }

    fun prayed(p: Prayer): Boolean = p in log.done(today)
    fun mark(p: Prayer, on: Boolean) = markOn(today, p, on)
    fun markOn(date: kotlinx.datetime.LocalDate, p: Prayer, on: Boolean) { log = log.mark(date, p, on); files.write(LOG_FILE, log.encode()) }

    fun tap(key: String, n: Int = 1) { tasbih = tasbih.add(today, key, n); files.write(TASBIH_FILE, tasbih.encode()) }

    /** Minutes before the prayer the notification fires; 0 is at the prayer. */
    fun offsetOf(p: Prayer): Int { notifyTick; return prefs.get("offset.${p.name}")?.toIntOrNull() ?: 0 }
    fun setOffset(p: Prayer, minutes: Int) { prefs.set("offset.${p.name}", minutes.toString()); platform.reschedule(); notifyTick++ }

    companion object { const val LOG_FILE = "prayer-log.csv"
        const val TASBIH_FILE = "tasbih.csv" }
}
