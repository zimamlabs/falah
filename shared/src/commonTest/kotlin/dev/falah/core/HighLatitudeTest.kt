package dev.falah.core

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HighLatitudeTest {
    private val london = 51.5074 to -0.1278
    private fun londonDay(date: LocalDate, rule: HighLatitude, method: Method = Method.MuslimWorldLeague) =
        PrayerTimes.compute(date, london.first, london.second, if (date.monthNumber in 4..10) 1.0 else 0.0, method, highLatitude = rule)
    private val midsummer = LocalDate(2026, 6, 21)

    @Test fun withoutARuleLondonInJuneHasNoFajrOrIsha() { val t = londonDay(midsummer, HighLatitude.None); assertNull(t.fajr); assertNull(t.isha); assertTrue(t.adjusted.isEmpty()) }

    @Test fun everyRuleGivesLondonAFajrAndIshaInTheNightAndSaysItAdjustedThem() {
        for (rule in listOf(HighLatitude.MiddleOfNight, HighLatitude.OneSeventh, HighLatitude.AngleBased)) {
            val t = londonDay(midsummer, rule)
            val fajr = assertNotNull(t.fajr, "$rule fajr"); val isha = assertNotNull(t.isha, "$rule isha")
            assertTrue(fajr < t.sunrise!!, "$rule fajr before sunrise"); assertTrue(isha > t.maghrib!!, "$rule isha after maghrib")
            assertEquals(setOf(Prayer.Fajr, Prayer.Isha), t.adjusted, "$rule marks both")
        }
    }

    @Test fun middleOfTheNightIsHalfAndOneSeventhIsASeventh() {
        val t = londonDay(midsummer, HighLatitude.MiddleOfNight); val night = t.sunrise!! + 24 - t.maghrib!!
        assertEquals(night / 2, t.isha!! - t.maghrib!!, 1e-9); assertEquals(night / 2, t.sunrise!! - t.fajr!!, 1e-9)
        val seventh = londonDay(midsummer, HighLatitude.OneSeventh); assertEquals(night / 7, seventh.isha!! - seventh.maghrib!!, 1e-9)
    }

    @Test fun angleBasedUsesTheAngleOverSixty() {
        val t = londonDay(midsummer, HighLatitude.AngleBased); val night = t.sunrise!! + 24 - t.maghrib!!
        assertEquals(night * 18 / 60, t.sunrise!! - t.fajr!!, 1e-9); assertEquals(night * 17 / 60, t.isha!! - t.maghrib!!, 1e-9)
    }

    @Test fun midnightSunNeverInventsATime() {
        val t = PrayerTimes.compute(midsummer, 69.65, 18.96, 2.0, Method.MuslimWorldLeague, highLatitude = HighLatitude.AngleBased)
        assertNull(t.sunrise); assertNull(t.maghrib); assertNull(t.fajr); assertNull(t.isha)
    }

    @Test fun placesFarFromTheHighLatitudesAreUnchangedAllYearByTheAngleBasedAndMiddleRules() {
        val places = listOf(Triple(30.0444, 31.2357, 2.0) to Method.Egyptian, Triple(21.4225, 39.8262, 3.0) to Method.UmmAlQura, Triple(40.7128, -74.0060, -5.0) to Method.NorthAmerica,
            Triple(41.0082, 28.9784, 3.0) to Method.MuslimWorldLeague, Triple(-33.8688, 151.2093, 10.0) to Method.MuslimWorldLeague)
        for ((p, method) in places) {
            var day = LocalDate(2026, 1, 1)
            repeat(365) {
                val none = PrayerTimes.compute(day, p.first, p.second, p.third, method, highLatitude = HighLatitude.None)
                // One seventh caps the twilight at a seventh of the night everywhere, so it can change even mid-latitude times by design; the two default-style rules cannot.
                for (rule in listOf(HighLatitude.AngleBased, HighLatitude.MiddleOfNight)) assertEquals(none, PrayerTimes.compute(day, p.first, p.second, p.third, method, highLatitude = rule), "${p.first} $day $rule")
                day = day.plus(1, DateTimeUnit.DAY)
            }
        }
    }

    @Test fun londonOutsideSummerIsUnchanged() {
        for (date in listOf(LocalDate(2026, 3, 20), LocalDate(2026, 12, 21), LocalDate(2026, 10, 1))) assertEquals(londonDay(date, HighLatitude.None), londonDay(date, HighLatitude.AngleBased), "$date")
    }

    @Test fun theRuleSurvivesAMinuteShiftAndIsStoredInSettings() {
        val t = londonDay(midsummer, HighLatitude.AngleBased).shifted(mapOf(Prayer.Fajr to 5)); assertEquals(setOf(Prayer.Fajr, Prayer.Isha), t.adjusted)
        assertEquals(HighLatitude.AngleBased, AppSettings().highLatitude)
        val s = AppSettings(highLatitude = HighLatitude.OneSeventh); assertEquals(s, SettingsCodec.decode(SettingsCodec.encode(s)))
    }

    @Test fun printTheComparisonTable() {
        println("HIGHLAT table London MWL (Fajr/Isha as HH:MM), rule none | middle | seventh | angle")
        for (date in listOf(LocalDate(2026, 5, 10), LocalDate(2026, 5, 24), LocalDate(2026, 6, 21), LocalDate(2026, 7, 20), LocalDate(2026, 8, 10), LocalDate(2026, 9, 1))) {
            val cells = HighLatitude.entries.joinToString(" | ") { r -> londonDay(date, r).let { "${PrayerTimes.clock(it.fajr) ?: "--:--"}/${PrayerTimes.clock(it.isha) ?: "--:--"}" } }
            val d = londonDay(date, HighLatitude.None)
            println("HIGHLAT $date sunrise ${PrayerTimes.clock(d.sunrise)} sunset ${PrayerTimes.clock(d.maghrib)}: $cells")
        }
    }
}
