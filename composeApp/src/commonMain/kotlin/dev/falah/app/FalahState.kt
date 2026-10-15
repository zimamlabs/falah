package dev.falah.app

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.falah.core.AppSettings
import dev.falah.core.AsrFactor
import dev.falah.core.ThemeMode
import dev.falah.core.Countdown
import dev.falah.core.DayTimes
import dev.falah.core.shifted
import dev.falah.core.Hijri
import dev.falah.core.Method
import dev.falah.core.Prayer
import dev.falah.core.PrayerLog
import dev.falah.core.PrayerTimes
import dev.falah.core.Str
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
    /** Every setting, read through [SettingsRepository] and changed only with [update]. */
    var settings by mutableStateOf(AppSettings())
        private set
    private val repository = SettingsRepository(prefs)
    val placeName get() = settings.placeName
    val lat get() = settings.latitude
    val lon get() = settings.longitude
    val method get() = settings.method
    val asr get() = settings.asr
    val themeMode get() = settings.theme
    val langChoice get() = settings.language
    val arabicDigits get() = settings.arabicDigits
    val hijriAdjust get() = settings.hijriAdjust
    var now by mutableStateOf(Clock.System.now())
    var log by mutableStateOf(PrayerLog())
    var tasbih by mutableStateOf(TasbihLog())

    /** Data files that exist but could not be read; they are never written over. */
    var unreadable by mutableStateOf(emptySet<String>())
        private set
    /** True when the last save did not reach the disk. */
    var saveFailed by mutableStateOf(false)
        private set

    init { reload() }

    private fun loadData(name: String): String = when (val r = files.load(name)) {
        is FileLoad.Loaded -> r.text
        FileLoad.Missing -> ""
        FileLoad.Unreadable -> { unreadable = unreadable + name; "" }
    }

    /** Saves a data file unless it was unreadable at the start (writing then would replace the real data with an empty file). */
    private fun save(name: String, text: String): Boolean {
        if (name in unreadable) { saveFailed = true; return false }
        saveFailed = !files.write(name, text)
        return !saveFailed
    }

    /** Reads everything again from the saved values and files (at start, and after the data folder changed). */
    fun reload() {
        settings = repository.load()
        unreadable = emptySet(); saveFailed = false
        log = PrayerLog.decode(loadData(LOG_FILE))
        tasbih = TasbihLog.decode(loadData(TASBIH_FILE))
    }

    private val zone get() = TimeZone.currentSystemDefault()
    val local get() = now.toLocalDateTime(zone)
    val today get() = local.date
    val hourNow: Double get() = local.hour + local.minute / 60.0 + local.second / 3600.0
    private val offset get() = zone.offsetAt(now).totalSeconds / 3600.0

    val times: DayTimes by derivedStateOf { PrayerTimes.compute(today, lat, lon, offset, method, asr, settings.highLatitude).shifted(settings.adjust) }
    val hijri get() = Hijri.of(today, hijriAdjust)

    /** The next prayer, or null when no prayer has a time in the coming days (midnight sun); no time is ever made up. */
    val next: NextPrayer? by derivedStateOf {
        val h = hourNow
        val order = Prayer.entries.filter { it != Prayer.Sunrise }
        // Compared on the minute that is shown, so the countdown and the list never disagree about which prayer is next.
        val shown = { t: DayTimes, p: Prayer -> t.of(p)?.let { Math.round(it * 60) / 60.0 } }
        val upcoming = order.firstOrNull { (shown(times, it) ?: -1.0) > h }
        var found: NextPrayer? = null
        if (upcoming != null) found = NextPrayer(upcoming, shown(times, upcoming)!!, false, 0f)
        else {
            val t = PrayerTimes.compute(today.plus(1, DateTimeUnit.DAY), lat, lon, offset, method, asr, settings.highLatitude).shifted(settings.adjust)
            order.firstOrNull { shown(t, it) != null }?.let { found = NextPrayer(it, shown(t, it)!! + 24.0, true, 0f) }
        }
        found?.let { n ->
            val before = Prayer.entries.mapNotNull { times.of(it) }.filter { it <= h }.maxOrNull() ?: times.isha?.minus(24.0)
            n.copy(progress = if (before == null) 0f else ((h - before) / (n.at - before)).toFloat().coerceIn(0f, 1f))
        }
    }

    /** Seconds to the next prayer, counted to the minute that is shown for it. */
    fun secondsLeft(): Long = next?.let { Math.round(Math.round(it.at * 60) * 60 - hourNow * 3600).coerceAtLeast(0) } ?: 0L
    val countdown get() = Countdown.of(secondsLeft())
    fun minutesLeft(): Int = (secondsLeft() / 60).toInt()

    /** Changes settings and saves only what differs. Alarms are set again when anything they depend on changed. */
    fun update(change: AppSettings.() -> AppSettings) {
        val old = settings; val new = old.change()
        if (new == old) return
        settings = new; repository.save(old, new)
        if (new.alarmInputs() != old.alarmInputs()) platform.reschedule()
    }

    private fun AppSettings.alarmInputs() = listOf(latitude, longitude, method, asr, highLatitude, notify, offsets, adjust)

    val placeChosen: Boolean get() = settings.placeChosen

    fun choosePlace(p: Place, source: dev.falah.core.PlaceSource = dev.falah.core.PlaceSource.Manual) = update { copy(placeName = p.name, latitude = p.latitude, longitude = p.longitude, placeChosen = true, placeSource = source) }
    fun chooseCustomPlace(name: String, latitude: Double, longitude: Double) = choosePlace(Place(name.trim().ifEmpty { Str.CustomPlaceDefaultName.en }, latitude, longitude))
    /** Why the last "Use my location" did not give a place; null while it works or after it succeeded. */
    var locationProblem by mutableStateOf<dev.falah.core.LocationFix?>(null)
        private set
    var locating by mutableStateOf(false)
        private set

    /** Asks the phone for its position and, if it comes, uses it as the place (named after a built-in city within 25 km, otherwise "My location"). Never falls back to a default place. */
    fun useLocation() {
        locating = true; locationProblem = null
        platform.requestLocation { fix ->
            locating = false
            when {
                fix is dev.falah.core.LocationFix.Found && dev.falah.core.Geo.valid(fix.latitude, fix.longitude) -> {
                    val near = dev.falah.core.Geo.nearest(fix.latitude, fix.longitude, Places.map { Triple(it.name, it.latitude, it.longitude) }, 25.0)
                    choosePlace(Place(near ?: Str.GpsPlaceName.en, fix.latitude, fix.longitude), dev.falah.core.PlaceSource.Gps)
                }
                fix is dev.falah.core.LocationFix.Found -> locationProblem = dev.falah.core.LocationFix.Unavailable
                else -> locationProblem = fix
            }
        }
    }
    fun clearLocationProblem() { locationProblem = null }

    fun chooseMethod(m: Method) = update { copy(method = m) }
    fun chooseHighLatitude(r: dev.falah.core.HighLatitude) = update { copy(highLatitude = r) }
    fun chooseAsr(a: AsrFactor) = update { copy(asr = a) }
    fun chooseTheme(mode: ThemeMode) = update { copy(theme = mode) }
    fun chooseAccent(a: dev.falah.core.Accent) = update { copy(accent = a) }
    fun chooseTextSize(t: dev.falah.core.TextSize) = update { copy(textSize = t) }
    fun chooseDensity(d: dev.falah.core.Density) = update { copy(density = d) }
    fun chooseReduceMotion(on: Boolean) = update { copy(reduceMotion = on) }
    /** Shows or hides a section; Home cannot be hidden. A newly shown section goes last. */
    fun showSection(section: dev.falah.core.Section, show: Boolean) = update { withSections(if (show) sections + section else sections - section) }
    /** Moves a shown section one place earlier (-1) or later (+1); Home stays first. */
    fun moveSection(section: dev.falah.core.Section, by: Int) = update {
        val list = sections.toMutableList(); val from = list.indexOf(section); val to = from + by
        if (section == dev.falah.core.Section.Home || from < 0 || to < 1 || to > list.lastIndex) this else { list.add(to, list.removeAt(from)); withSections(list) }
    }
    fun chooseStart(section: dev.falah.core.Section) = update { if (section in sections) copy(startSection = section) else this }
    fun chooseLanguage(choice: String) = update { copy(language = choice) }
    fun chooseDigits(arabic: Boolean) = update { copy(arabicDigits = arabic) }
    fun chooseHijriAdjust(days: Int) = update { copy(hijriAdjust = days) }

    fun notifyOn(p: Prayer): Boolean = p in settings.notify
    fun setNotify(p: Prayer, on: Boolean) {
        if (on) platform.ensureNotifications { ok -> if (ok) update { copy(notify = notify + p) } }
        else update { copy(notify = notify - p) }
    }

    fun prayed(p: Prayer): Boolean = p in log.done(today)
    fun mark(p: Prayer, on: Boolean) = markOn(today, p, on)
    fun markOn(date: kotlinx.datetime.LocalDate, p: Prayer, on: Boolean) {
        val next = log.mark(date, p, on)
        if (save(LOG_FILE, next.encode()) || LOG_FILE !in unreadable) log = next
    }

    fun tap(key: String, n: Int = 1) {
        val next = tasbih.add(today, key, n)
        if (save(TASBIH_FILE, next.encode()) || TASBIH_FILE !in unreadable) tasbih = next
    }

    /** Minutes before the prayer the notification fires; 0 is at the prayer. */
    fun offsetOf(p: Prayer): Int = settings.offsetOf(p)
    /** Moves one prayer's time by [minutes] (-30 to 30, 0 removes it). */
    fun setAdjust(p: Prayer, minutes: Int) = update { copy(adjust = if (minutes == 0) adjust - p else adjust + (p to minutes.coerceIn(-30, 30))) }
    fun setSound(p: Prayer, sound: String) = update { copy(sounds = if (sound == dev.falah.core.Sounds.DEFAULT) sounds - p else sounds + (p to sound)) }
    fun choosePrayerScreens(on: Boolean) = update { copy(prayerScreens = on) }
    fun setNoticeScreen(p: Prayer, on: Boolean) = update { copy(noticeScreen = if (on) noticeScreen + p else noticeScreen - p) }
    fun chooseNoticeFullScreen(on: Boolean) = update { copy(noticeFullScreen = on) }
    fun chooseSnooze(minutes: Int) = update { copy(snoozeMinutes = minutes) }
    fun setOffset(p: Prayer, minutes: Int) = update { copy(offsets = offsets + (p to minutes)) }

    companion object { const val LOG_FILE = "prayer-log.csv"
        const val TASBIH_FILE = "tasbih.csv" }
}
