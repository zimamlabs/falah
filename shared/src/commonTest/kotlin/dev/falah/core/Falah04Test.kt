package dev.falah.core

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Falah04Test {
    private val cairo = TimeZone.of("Africa/Cairo")

    @Test fun anOffsetMovesTheAlertEarlier() {
        val now = Instant.parse("2026-03-21T00:00:00Z")
        val at = Schedule.next(now, cairo, 30.0444, 31.2357, Method.Egyptian, AsrFactor.Standard, setOf(Prayer.Maghrib))!!
        val early = Schedule.next(now, cairo, 30.0444, 31.2357, Method.Egyptian, AsrFactor.Standard, setOf(Prayer.Maghrib), mapOf(Prayer.Maghrib to 15))!!
        assertEquals(at.atMillis - 15 * 60_000L, early.atMillis); assertEquals(15, early.minutesBefore); assertEquals(0, at.minutesBefore)
    }
    @Test fun anAlertAlreadyPastTodayRollsToTomorrow() {
        // Right after Fajr minus 30 minutes passed but before Fajr itself: that alert is gone, the next one is Fajr's of tomorrow.
        val times = PrayerTimes.compute(LocalDate(2026, 3, 21), 30.0444, 31.2357, 2.0, Method.Egyptian, AsrFactor.Standard)
        val fajrMillis = Instant.parse("2026-03-20T22:00:00Z").toEpochMilliseconds() + (times.fajr!! * 3_600_000).toLong()
        val now = Instant.fromEpochMilliseconds(fajrMillis - 10 * 60_000L)
        val a = Schedule.next(now, cairo, 30.0444, 31.2357, Method.Egyptian, AsrFactor.Standard, setOf(Prayer.Fajr), mapOf(Prayer.Fajr to 30))!!
        assertTrue(a.atMillis > fajrMillis + 12 * 3_600_000L)
    }
    @Test fun negativeOffsetsAreTreatedAsZero() {
        val now = Instant.parse("2026-03-21T00:00:00Z")
        val a = Schedule.next(now, cairo, 30.0444, 31.2357, Method.Egyptian, AsrFactor.Standard, setOf(Prayer.Dhuhr), mapOf(Prayer.Dhuhr to -5))!!
        assertEquals(0, a.minutesBefore)
    }

    @Test fun monthTimetableHasEveryDayInOrder() {
        val rows = Timetable.month(2026, 10, cairo, 30.0444, 31.2357, Method.Egyptian, AsrFactor.Standard)
        assertEquals(31, rows.size); assertEquals(LocalDate(2026, 10, 1), rows.first().date); assertEquals(LocalDate(2026, 10, 31), rows.last().date)
        rows.forEach { r -> val t = r.times; assertTrue(t.fajr!! < t.sunrise!! && t.sunrise!! < t.dhuhr && t.dhuhr < t.asr!! && t.asr!! < t.maghrib!! && t.maghrib!! < t.isha!!, "${r.date}") }
        assertEquals(29, Timetable.month(2028, 2, cairo, 30.0, 31.0, Method.Egyptian, AsrFactor.Standard).size)
    }
    @Test fun monthGridPadsToWholeWeeks() {
        val c = MonthGrid.cells(2026, 10) // 1 Oct 2026 is a Thursday
        assertEquals(0, c.size % 7); assertEquals(3, c.indexOfFirst { it != null }); assertEquals(LocalDate(2026, 10, 31), c.last { it != null })
        assertEquals(4, MonthGrid.cells(2026, 10, weekStartsMonday = false).indexOfFirst { it != null })
        assertEquals(2025 to 12, MonthGrid.previous(2026, 1)); assertEquals(2027 to 1, MonthGrid.next(2026, 12))
    }

    @Test fun tasbihCountsPerDayAndDhikr() {
        val d = LocalDate(2026, 10, 14)
        var log = TasbihLog()
        repeat(33) { log = log.add(d, "subhanallah") }
        log = log.add(d, "tahlil", 5).add(LocalDate(2026, 10, 13), "subhanallah", 10)
        assertEquals(33, log.on(d, "subhanallah")); assertEquals(38, log.dayTotal(d)); assertEquals(43, log.total("subhanallah")); assertEquals(48, log.grandTotal())
        assertEquals(log, TasbihLog.decode(log.encode()))
        assertEquals(log, TasbihLog.decode(log.encode() + "junk\n2026-10-14,unknown,5\n2026-10-14,tahlil,-2\n"))
    }
    @Test fun tasbihNeverGoesNegative() = assertEquals(TasbihLog(), TasbihLog().add(LocalDate(2026, 10, 14), "tahlil", 1).add(LocalDate(2026, 10, 14), "tahlil", -5))

    @Test fun compassMath() {
        assertEquals(90.0, Compass.relative(135.0, 45.0)); assertEquals(350.0, Compass.relative(10.0, 20.0))
        assertTrue(Compass.aligned(2.0)); assertTrue(Compass.aligned(358.0)); assertFalse(Compass.aligned(10.0))
        assertEquals(10.0, Compass.trueHeading(5.0, 5.0)); assertEquals(355.0, Compass.trueHeading(5.0, -10.0))
        assertEquals(355.0, Compass.smooth(350.0, 0.0, 0.5)) // wraps the short way through north
        assertEquals(4.0, Compass.smooth(null, 4.0))
    }
}
