package dev.falah.app

import dev.falah.core.Prayer
import dev.falah.core.PrayerTimes
import dev.falah.core.of
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CountdownStateTest {
    private val zone = TimeZone.currentSystemDefault()
    private fun state() = FalahState(MapPrefs(mapOf("place" to "Cairo", "lat" to "30.0444", "lon" to "31.2357")), MemoryFiles(), NoPlatform)
    private fun at(s: FalahState, hours: Double): Instant = s.today.atStartOfDayIn(zone).plus(Math.round(hours * 3600), DateTimeUnit.SECOND)

    @Test fun afterIshaTheNextPrayerIsTomorrowsFajrAndTheCountdownCrossesMidnight() {
        val s = state(); val isha = s.times.of(Prayer.Isha)!!
        s.now = at(s, isha + 0.5)
        assertEquals(Prayer.Fajr, s.next!!.prayer); assertTrue(s.next!!.tomorrow)
        val left = s.secondsLeft()
        assertTrue(left in 3_600..14 * 3_600L, "left was $left")
        assertEquals(24 * 3600L - Math.round((isha + 0.5) * 3600) + Math.round(s.next!!.at % 24 * 60) * 60, left, "from now to tomorrow's Fajr minute")
    }

    @Test fun theCountdownIsComputedFromTheTargetSoALateTickChangesNothingButTheClock() {
        val s = state(); val dhuhr = s.times.of(Prayer.Dhuhr)!!
        s.now = at(s, dhuhr - 1.0); val one = s.secondsLeft()
        s.now = Instant.fromEpochMilliseconds(s.now.toEpochMilliseconds() + 37_000); val later = s.secondsLeft()
        assertEquals(37L, one - later)
    }

    @Test fun theCountdownReachesZeroOnTheDisplayedMinute() {
        val s = state(); val asr = s.times.of(Prayer.Asr)!!
        s.now = at(s, Math.round(asr * 60) / 60.0 - 5.0 / 3600)
        assertEquals(Prayer.Asr, s.next!!.prayer); assertEquals(5L, s.secondsLeft())
        assertEquals("00:00:05", s.countdown.text())
    }

    @Test fun theMinuteShownAndTheCountdownTargetAgree() {
        val s = state(); val maghrib = s.times.of(Prayer.Maghrib)!!
        val shown = PrayerTimes.clock(maghrib)!!.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        s.now = at(s, 0.0 + shown / 60.0 - 0.25)
        assertEquals(900L, s.secondsLeft())
    }
}
