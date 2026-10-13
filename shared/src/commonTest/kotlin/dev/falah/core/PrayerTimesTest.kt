package dev.falah.core

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PrayerTimesTest {
    private val cairo = 30.0444 to 31.2357

    @Test fun theSixTimesComeInOrderThroughTheDay() {
        val t = PrayerTimes.compute(LocalDate(2026, 3, 21), cairo.first, cairo.second, 2.0)
        val all = listOf(t.fajr, t.sunrise, t.dhuhr, t.asr, t.maghrib, t.isha).map { assertNotNull(it) }
        assertEquals(all.sorted(), all)
    }

    @Test fun cairoAtTheEquinoxHasNoonAboutTwelveAndADayOfAboutTwelveHours() {
        val t = PrayerTimes.compute(LocalDate(2026, 3, 21), cairo.first, cairo.second, 2.0)
        assertTrue(t.dhuhr in 11.85..12.1, "noon was ${t.dhuhr}")
        val day = t.maghrib!! - t.sunrise!!
        assertTrue(day in 11.9..12.3, "day was $day")
    }

    @Test fun summerDaysAreLongerThanWinterDaysInCairo() {
        val summer = PrayerTimes.compute(LocalDate(2026, 6, 21), cairo.first, cairo.second, 3.0)
        val winter = PrayerTimes.compute(LocalDate(2026, 12, 21), cairo.first, cairo.second, 2.0)
        assertTrue(summer.maghrib!! - summer.sunrise!! > winter.maghrib!! - winter.sunrise!! + 3)
    }

    @Test fun hanafiAsrIsLaterThanStandardAsr() {
        val s = PrayerTimes.compute(LocalDate(2026, 10, 14), cairo.first, cairo.second, 3.0, asr = AsrFactor.Standard)
        val h = PrayerTimes.compute(LocalDate(2026, 10, 14), cairo.first, cairo.second, 3.0, asr = AsrFactor.Hanafi)
        assertTrue(h.asr!! > s.asr!!)
    }

    @Test fun ummAlQuraIshaIsNinetyMinutesAfterMaghrib() {
        val t = PrayerTimes.compute(LocalDate(2026, 1, 1), 21.4225, 39.8262, 3.0, Method.UmmAlQura)
        assertEquals(90.0, (t.isha!! - t.maghrib!!) * 60, 0.01)
    }

    @Test fun makkahNoonIsNearTwelveTwentyEveryJanuaryFirst() {
        val t = PrayerTimes.compute(LocalDate(2026, 1, 1), 21.4225, 39.8262, 3.0)
        assertTrue(t.dhuhr in 12.3..12.45, "noon was ${t.dhuhr}")
    }

    @Test fun nearThePoleFajrOnAMidsummerNightMayNotExist() {
        val t = PrayerTimes.compute(LocalDate(2026, 6, 21), 69.65, 18.96, 2.0)
        assertNull(t.fajr)
    }

    @Test fun qiblaFromKnownCities() {
        assertEquals(136.1, PrayerTimes.qibla(30.0444, 31.2357), 1.0)   // Cairo
        assertEquals(119.0, PrayerTimes.qibla(51.5074, -0.1278), 1.0)   // London
        assertEquals(58.5, PrayerTimes.qibla(40.7128, -74.0060), 1.0)   // New York
        assertEquals(292.5, PrayerTimes.qibla(35.6762, 139.6503), 1.5)  // Tokyo
    }

    @Test fun clockFormatsHoursAndWrapsAroundMidnight() {
        assertEquals("05:07", PrayerTimes.clock(5.1167))
        assertEquals("00:30", PrayerTimes.clock(24.5))
        assertEquals("23:30", PrayerTimes.clock(-0.5))
        assertNull(PrayerTimes.clock(null))
    }
}
