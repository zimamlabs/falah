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
data class DayTimes(val fajr: Double?, val sunrise: Double?, val dhuhr: Double, val asr: Double?, val maghrib: Double?, val isha: Double?)

enum class Prayer(val label: String, val arabic: String) { Fajr("Fajr", "الفجر"), Sunrise("Sunrise", "الشروق"), Dhuhr("Dhuhr", "الظهر"), Asr("Asr", "العصر"), Maghrib("Maghrib", "المغرب"), Isha("Isha", "العشاء") }

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

/** The shadow length for Asr: one times the object (most schools) or twice (Hanafi). */
enum class AsrFactor(val label: String, val factor: Double) { Standard("Standard", 1.0), Hanafi("Hanafi", 2.0) }

object PrayerTimes {
    private const val SUNRISE_ANGLE = 0.833

    /**
     * Times for [date] at [latitude], [longitude] (degrees, east positive) in a zone [utcOffsetHours] ahead of UTC (daylight saving included).
     * Uses the sun's position from the standard low-precision astronomical formulas, accurate to about a minute.
     */
    fun compute(date: LocalDate, latitude: Double, longitude: Double, utcOffsetHours: Double, method: Method = Method.Egyptian, asr: AsrFactor = AsrFactor.Standard): DayTimes {
        val jd = julianDay(date) - longitude / (15 * 24)
        val (decl, eqt) = sunPosition(jd)
        val noon = 12 + utcOffsetHours - longitude / 15 - eqt
        fun t(angle: Double): Double? {
            val c = (-sin(rad(angle)) - sin(rad(decl)) * sin(rad(latitude))) / (cos(rad(decl)) * cos(rad(latitude)))
            return if (c < -1 || c > 1) null else deg(acos(c)) / 15
        }
        val sunriseT = t(SUNRISE_ANGLE)
        val maghrib = sunriseT?.let { noon + it }
        val asrAngle = -deg(atan(1 / (asr.factor + tan(rad(kotlin.math.abs(latitude - decl))))))
        val asrT = t(asrAngle)
        return DayTimes(
            fajr = t(method.fajrAngle)?.let { noon - it },
            sunrise = sunriseT?.let { noon - it },
            dhuhr = noon,
            asr = asrT?.let { noon + it },
            maghrib = maghrib,
            isha = method.ishaAfterMaghribMinutes?.let { m -> maghrib?.plus(m / 60.0) } ?: t(method.ishaAngle)?.let { noon + it },
        )
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
