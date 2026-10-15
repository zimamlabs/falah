package dev.falah.core

import kotlinx.datetime.LocalDate
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/** The six times of a day, as hours since local midnight (for example 12.5 is 12:30). A time that does not occur at a high latitude is null. */
data class DayTimes(val fajr: Double?, val sunrise: Double?, val dhuhr: Double, val asr: Double?, val maghrib: Double?, val isha: Double?, val adjusted: Set<Prayer> = emptySet())

enum class Prayer(val label: String, val arabic: String) { Fajr("Fajr", "الفجر"), Sunrise("Sunrise", "الشروق"), Dhuhr("Dhuhr", "الظهر"), Asr("Asr", "العصر"), Maghrib("Maghrib", "المغرب"), Isha("Isha", "العشاء") }

/** The times moved by the user's per-prayer minutes (plus or minus); a time that does not occur stays absent. */
fun DayTimes.shifted(minutes: Map<Prayer, Int>): DayTimes {
    if (minutes.values.all { it == 0 }) return this
    fun m(p: Prayer, v: Double?) = v?.plus((minutes[p] ?: 0) / 60.0)
    return DayTimes(m(Prayer.Fajr, fajr), m(Prayer.Sunrise, sunrise), dhuhr + (minutes[Prayer.Dhuhr] ?: 0) / 60.0, m(Prayer.Asr, asr), m(Prayer.Maghrib, maghrib), m(Prayer.Isha, isha), adjusted)
}

fun DayTimes.of(p: Prayer): Double? = when (p) {
    Prayer.Fajr -> fajr; Prayer.Sunrise -> sunrise; Prayer.Dhuhr -> dhuhr; Prayer.Asr -> asr; Prayer.Maghrib -> maghrib; Prayer.Isha -> isha
}

/** How the sun angles for Fajr and Isha are chosen; [ishaAfterMaghribMinutes] replaces the Isha angle when set (Umm al-Qura). */
enum class Method(val label: String, val fajrAngle: Double, val ishaAngle: Double, val ishaAfterMaghribMinutes: Int? = null) {
    Egyptian("Egyptian General Authority", 19.5, 17.5),
    MuslimWorldLeague("Muslim World League", 18.0, 17.0),
    UmmAlQura("Umm al-Qura, Makkah", 18.5, 0.0, 90),
    Karachi("University of Islamic Sciences, Karachi", 18.0, 18.0),
    NorthAmerica("Islamic Society of North America", 15.0, 15.0),
}

/**
 * What to do when the sun does not reach the Fajr or Isha angle, or reaches it too close to sunrise or sunset to be a real
 * dawn or dusk (far from the equator in summer). The rule limits Fajr and Isha to a share of the night; nothing else is invented.
 */
enum class HighLatitude(val key: String, val label: String) {
    None("none", "None (no time when the angle is not reached)"),
    MiddleOfNight("middle", "Middle of the night (half the night)"),
    OneSeventh("seventh", "One seventh of the night"),
    AngleBased("angle", "Angle based (angle divided by 60 of the night)"),
}

/** The shadow length for Asr: one times the object (most schools) or twice (Hanafi). */
enum class AsrFactor(val label: String, val factor: Double) { Standard("Standard", 1.0), Hanafi("Hanafi", 2.0) }

object PrayerTimes {
    private const val SUNRISE_ANGLE = 0.833

    /** Minutes after Maghrib that Umm al-Qura puts Isha during Ramadan (90 the rest of the year). */
    const val UMM_AL_QURA_RAMADAN_ISHA_MINUTES = 120

    /**
     * Times for [date] at [latitude], [longitude] (degrees, east positive) in a zone [utcOffsetHours] ahead of UTC (daylight saving included).
     * The sun's position (standard low-precision astronomical formulas) is taken at the moment of each event, found by repeating the
     * calculation, not once for the whole day. Umm al-Qura Isha is 120 minutes after Maghrib in Ramadan (tabular Hijri month 9).
     * Measured against an independent solar model: see ReferenceTimesTest.
     */
    fun compute(date: LocalDate, latitude: Double, longitude: Double, utcOffsetHours: Double, method: Method = Method.Egyptian, asr: AsrFactor = AsrFactor.Standard, highLatitude: HighLatitude = HighLatitude.AngleBased): DayTimes {
        val jd0 = julianDay(date)
        fun sunAt(utHours: Double) = sunPosition(jd0 + utHours / 24)
        val meridian = 12 - longitude / 15
        var noonUt = meridian
        repeat(3) { noonUt = meridian - sunAt(noonUt).second }
        val noonDecl = sunAt(noonUt).first
        /** Local clock hour when the sun is [angle] degrees above (positive) or below (negative) the horizon, before or after noon. */
        fun event(angle: Double, afterNoon: Boolean): Double? {
            var t = noonUt
            repeat(4) {
                val (decl, eqt) = sunAt(t)
                val c = (sin(rad(angle)) - sin(rad(decl)) * sin(rad(latitude))) / (cos(rad(decl)) * cos(rad(latitude)))
                if (c < -1 || c > 1) return null
                val h = deg(acos(c)) / 15
                t = meridian - eqt + (if (afterNoon) h else -h)
            }
            return t + utcOffsetHours
        }
        val asrAngle = deg(atan(1 / (asr.factor + tan(rad(kotlin.math.abs(latitude - noonDecl))))))
        val maghrib = event(-SUNRISE_ANGLE, true)
        val ishaMinutes = method.ishaAfterMaghribMinutes?.let { if (method == Method.UmmAlQura && Hijri.of(date).month == 9) UMM_AL_QURA_RAMADAN_ISHA_MINUTES else it }
        val sunrise = event(-SUNRISE_ANGLE, false)
        val fixedIsha = ishaMinutes?.let { m -> maghrib?.plus(m / 60.0) }
        var fajr = event(-method.fajrAngle, false)
        var isha = fixedIsha ?: event(-method.ishaAngle, true)
        val adjusted = mutableSetOf<Prayer>()
        // The night runs from sunset to the next sunrise (today's sunrise plus a day); without both there is no night to share out.
        if (highLatitude != HighLatitude.None && sunrise != null && maghrib != null) {
            val night = sunrise + 24 - maghrib
            fun share(angle: Double) = night * when (highLatitude) { HighLatitude.MiddleOfNight -> 0.5; HighLatitude.OneSeventh -> 1.0 / 7; else -> angle / 60.0; }
            val fajrLimit = share(method.fajrAngle)
            if (fajr == null || sunrise - fajr > fajrLimit) { fajr = sunrise - fajrLimit; adjusted += Prayer.Fajr }
            if (fixedIsha == null) {
                val ishaLimit = share(method.ishaAngle)
                if (isha == null || isha - maghrib > ishaLimit) { isha = maghrib + ishaLimit; adjusted += Prayer.Isha }
            }
        }
        return DayTimes(fajr = fajr, sunrise = sunrise, dhuhr = noonUt + utcOffsetHours, asr = event(asrAngle, true), maghrib = maghrib, isha = isha, adjusted = adjusted)
    }

    /** The direction of the Kaaba from here, in degrees clockwise from true north. */
    fun qibla(latitude: Double, longitude: Double): Double {
        val kLat = rad(21.4225); val kLon = rad(39.8262)
        val lat = rad(latitude); val dLon = kLon - rad(longitude)
        val y = sin(dLon)
        val x = cos(lat) * tan(kLat) - sin(lat) * cos(dLon)
        return (deg(atan2(y, x)) + 360) % 360
    }

    /** "05:07" for 5.12 hours; null stays null. Hours wrap around midnight. */
    fun clock(hours: Double?): String? {
        if (hours == null || hours.isNaN()) return null
        val total = Math.floorMod(Math.round(hours * 60).toInt(), 24 * 60)
        return "${(total / 60).toString().padStart(2, '0')}:${(total % 60).toString().padStart(2, '0')}"
    }

    private fun rad(d: Double) = d * PI / 180
    private fun deg(r: Double) = r * 180 / PI
    private fun fix(a: Double, b: Double) = a - b * floor(a / b)

    private fun julianDay(d: LocalDate): Double {
        var y = d.year; var m = d.monthNumber
        if (m <= 2) { y -= 1; m += 12 }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + d.dayOfMonth + b - 1524.5
    }

    /** The sun's declination in degrees and the equation of time in hours. */
    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fix(357.529 + 0.98560028 * d, 360.0)
        val q = fix(280.459 + 0.98564736 * d, 360.0)
        val l = fix(q + 1.915 * sin(rad(g)) + 0.020 * sin(rad(2 * g)), 360.0)
        val e = 23.439 - 0.00000036 * d
        val ra = fix(deg(atan2(cos(rad(e)) * sin(rad(l)), cos(rad(l)))) / 15, 24.0)
        val decl = deg(asin(sin(rad(e)) * sin(rad(l))))
        val eqt = q / 15 - ra
        return decl to (eqt - 24 * floor((eqt + 12) / 24))
    }
}
