package dev.falah.core

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.offsetAt
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** One moment to notify: which prayer, and when (milliseconds since 1970). */
data class Alert(val prayer: Prayer, val atMillis: Long)

object Schedule {
    /**
     * The next of the chosen [prayers] after [now], looking today and then the next two days.
     * Sunrise can be chosen too (some people want a warning before it).
     */
    fun next(
        now: Instant, zone: TimeZone, latitude: Double, longitude: Double, method: Method, asr: AsrFactor, prayers: Set<Prayer>,
    ): Alert? {
        if (prayers.isEmpty()) return null
        var date = now.toLocalDateTime(zone).date
        repeat(3) {
            val start = date.atStartOfDayIn(zone)
            val offset = zone.offsetAt(start).totalSeconds / 3600.0
            val times = PrayerTimes.compute(date, latitude, longitude, offset, method, asr)
            val upcoming = Prayer.entries.filter { it in prayers }.mapNotNull { p -> times.of(p)?.let { Alert(p, start.toEpochMilliseconds() + (it * 3_600_000).toLong()) } }
                .filter { it.atMillis > now.toEpochMilliseconds() }
                .minByOrNull { it.atMillis }
            if (upcoming != null) return upcoming
            date = date.plus(1, DateTimeUnit.DAY)
        }
        return null
    }
}
