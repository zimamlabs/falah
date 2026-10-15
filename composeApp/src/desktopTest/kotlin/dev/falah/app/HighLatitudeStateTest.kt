package dev.falah.app

import dev.falah.core.Prayer
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HighLatitudeStateTest {
    private fun state(lat: Double, lon: Double, rule: String) = FalahState(MapPrefs(mapOf("place" to "X", "lat" to "$lat", "lon" to "$lon", "highlat" to rule, "method" to "MuslimWorldLeague")), MemoryFiles(), NoPlatform)
    private fun at(s: FalahState, hours: Double) { s.now = s.today.atStartOfDayIn(TimeZone.currentSystemDefault()).plus(Math.round(hours * 3600), DateTimeUnit.SECOND) }
    private fun afterIsha(s: FalahState) { s.now = LocalDate(2026, 6, 21).atStartOfDayIn(TimeZone.currentSystemDefault()).plus(12 * 3600L, DateTimeUnit.SECOND); at(s, (s.times.isha ?: s.times.maghrib!!) + 0.2) }

    @Test fun withoutARuleTheNextPrayerAfterIshaIsNeverAMadeUpFajr() {
        val s = state(51.5074, -0.1278, "none"); afterIsha(s)
        assertNull(s.times.fajr)
        val n = assertNotNull(s.next); assertTrue(n.tomorrow); assertEquals(Prayer.Dhuhr, n.prayer)
    }

    @Test fun withTheDefaultRuleLondonHasFajrTomorrowAndItIsMarked() {
        val s = state(51.5074, -0.1278, "angle"); afterIsha(s)
        assertNotNull(s.times.fajr); assertTrue(Prayer.Fajr in s.times.adjusted)
        assertEquals(Prayer.Fajr, s.next!!.prayer)
    }

    @Test fun midnightSunHasNoFajrOrIshaEvenWithARuleAndTheNextPrayerIsARealOne() {
        val s = state(69.65, 18.96, "angle"); s.now = LocalDate(2026, 6, 21).atStartOfDayIn(TimeZone.currentSystemDefault()).plus(23 * 3600L, DateTimeUnit.SECOND)
        assertNull(s.times.fajr); assertNull(s.times.isha)
        val n = assertNotNull(s.next); assertTrue(n.tomorrow); assertTrue(n.prayer != Prayer.Fajr && n.prayer != Prayer.Isha)
    }
}
