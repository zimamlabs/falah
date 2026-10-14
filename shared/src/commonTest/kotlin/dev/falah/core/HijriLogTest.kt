package dev.falah.core

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class HijriLogTest {
    private val today = LocalDate(2026, 10, 14)

    @Test fun knownHijriDates() {
        assertEquals(HijriDate(1446, 1, 1), Hijri.of(LocalDate(2024, 7, 8)))
        assertEquals(HijriDate(1420, 9, 24), Hijri.of(LocalDate(2000, 1, 1)))
        assertEquals(HijriDate(1445, 9, 1), Hijri.of(LocalDate(2024, 3, 11)))
    }
    @Test fun adjustmentMovesTheDay() = assertEquals(HijriDate(1446, 1, 1), Hijri.of(LocalDate(2024, 7, 7), 1)) // sighting calendars can start a day earlier than the tabular one
    @Test fun daysAdvanceAndMonthsRollOver() {
        var d = LocalDate(2025, 1, 1); var prev = Hijri.of(d)
        repeat(800) {
            d = LocalDate.fromEpochDays(d.toEpochDays() + 1)
            val h = Hijri.of(d)
            assertTrue(h.month in 1..12 && h.day in 1..30, "$d -> $h")
            val sameMonth = h.year == prev.year && h.month == prev.month && h.day == prev.day + 1
            val nextMonth = h.day == 1 && (h.month == prev.month % 12 + 1) && (prev.day == 29 || prev.day == 30)
            assertTrue(sameMonth || nextMonth, "$prev then $h")
            prev = h
        }
    }
    @Test fun monthNames() { assertEquals("Ramadan", HijriDate(1446, 9, 1).monthName); assertEquals("رمضان", HijriDate(1446, 9, 1).monthArabic) }

    @Test fun markingAndStreaks() {
        var log = PrayerLog()
        for (back in 0..2) for (p in loggedPrayers) log = log.mark(LocalDate.fromEpochDays(today.toEpochDays() - back), p, true)
        assertEquals(3, log.streak(today))
        log = log.mark(today, Prayer.Asr, false)
        assertFalse(log.isFull(today)); assertEquals(2, log.streak(today)) // today is still open, yesterday back counts
        assertEquals(2, log.bestStreak())
    }
    @Test fun aGapBreaksTheStreak() {
        var log = PrayerLog()
        for (back in listOf(0, 1, 3)) for (p in loggedPrayers) log = log.mark(LocalDate.fromEpochDays(today.toEpochDays() - back), p, true)
        assertEquals(2, log.streak(today)); assertEquals(2, log.bestStreak())
    }
    @Test fun sunriseIsNeverLogged() = assertEquals(PrayerLog(), PrayerLog().mark(today, Prayer.Sunrise, true))
    @Test fun unmarkingTheLastOneRemovesTheDay() = assertEquals(PrayerLog(), PrayerLog().mark(today, Prayer.Fajr, true).mark(today, Prayer.Fajr, false))
    @Test fun weekStripHasSevenDaysOldestFirst() {
        val log = PrayerLog().mark(today, Prayer.Fajr, true)
        val w = log.week(today)
        assertEquals(7, w.size); assertEquals(LocalDate(2026, 10, 8), w.first().first); assertEquals(today to 1, w.last())
    }
    @Test fun monthTotalsStopAtToday() {
        val log = PrayerLog().mark(LocalDate(2026, 10, 1), Prayer.Fajr, true).mark(LocalDate(2026, 10, 2), Prayer.Dhuhr, true)
        assertEquals(2 to 14 * 5, log.monthTotals(2026, 10, today))
    }
    @Test fun encodeDecodeRoundTripAndIgnoresJunk() {
        var log = PrayerLog()
        log = log.mark(today, Prayer.Fajr, true).mark(today, Prayer.Isha, true).mark(LocalDate(2026, 10, 1), Prayer.Asr, true)
        assertEquals(log, PrayerLog.decode(log.encode()))
        assertEquals(log, PrayerLog.decode(log.encode() + "bad line\n2026-13-40,Fajr\n2026-10-01,Nonsense\n"))
    }
    @Test fun duaIsStableForADayAndRotates() {
        assertEquals(Duas.of(today), Duas.of(today))
        assertNotEquals(Duas.of(today), Duas.of(LocalDate(2026, 10, 15)))
        assertTrue(Duas.all.all { it.arabic.isNotBlank() && it.english.isNotBlank() })
    }
}
