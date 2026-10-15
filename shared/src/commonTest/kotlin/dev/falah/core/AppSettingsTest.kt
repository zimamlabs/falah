package dev.falah.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.minus

class AppSettingsTest {
    @Test fun anEmptyStoreGivesTheDefaults() {
        val s = SettingsCodec.decode(emptyMap())
        assertEquals(AppSettings(), s); assertFalse(s.placeChosen)
        assertEquals(Section.entries, s.sections)
    }

    @Test fun settingsSavedByVersion051StillLoad() {
        val old = mapOf("place" to "London", "lat" to "51.5074", "lon" to "-0.1278", "method" to "MuslimWorldLeague", "asr" to "Hanafi",
            "theme" to "dark", "lang" to "ar", "digits" to "ar", "hijri.adjust" to "-1", "notify.Fajr" to "1", "notify.Isha" to "1", "offset.Fajr" to "15")
        val s = SettingsCodec.decode(old)
        assertEquals("London", s.placeName); assertTrue(s.placeChosen)
        assertEquals(Method.MuslimWorldLeague, s.method); assertEquals(AsrFactor.Hanafi, s.asr)
        assertEquals(ThemeMode.Dark, s.theme); assertEquals("ar", s.language); assertTrue(s.arabicDigits); assertEquals(-1, s.hijriAdjust)
        assertEquals(setOf(Prayer.Fajr, Prayer.Isha), s.notify); assertEquals(15, s.offsetOf(Prayer.Fajr)); assertEquals(0, s.offsetOf(Prayer.Dhuhr))
        assertEquals(Accent.Orange, s.accent); assertEquals(TextSize.Normal, s.textSize)
    }

    @Test fun everyFieldSurvivesARoundTrip() {
        val s = AppSettings(
            placeName = "Karachi", latitude = 24.8607, longitude = 67.0011, placeChosen = true, method = Method.Karachi, asr = AsrFactor.Hanafi,
            hijriAdjust = 1, theme = ThemeMode.Light, accent = Accent.Emerald, textSize = TextSize.Larger, density = Density.Compact, reduceMotion = true,
            language = "en", arabicDigits = true, sections = listOf(Section.Home, Section.Tasbih, Section.Times), startSection = Section.Tasbih,
            notify = setOf(Prayer.Maghrib), offsets = mapOf(Prayer.Maghrib to 10),
        )
        assertEquals(s, SettingsCodec.decode(SettingsCodec.encode(s)))
    }

    @Test fun aFreshInstallDoesNotWriteAPlace() = assertFalse("place" in SettingsCodec.encode(AppSettings()))

    @Test fun badValuesFallBackToTheDefaults() {
        val s = SettingsCodec.decode(mapOf("lat" to "99", "lon" to "abc", "method" to "Nope", "theme" to "x", "textsize" to "huge", "lang" to "fr", "offset.Fajr" to "7", "hijri.adjust" to "9"))
        assertEquals(AppSettings().latitude, s.latitude); assertEquals(AppSettings().longitude, s.longitude)
        assertEquals(Method.Egyptian, s.method); assertEquals(ThemeMode.System, s.theme); assertEquals(TextSize.Normal, s.textSize)
        assertEquals("system", s.language); assertEquals(0, s.offsetOf(Prayer.Fajr)); assertEquals(1, s.hijriAdjust)
    }

    @Test fun homeIsAlwaysFirstAndTheStartSectionIsAlwaysShown() {
        val s = SettingsCodec.decode(mapOf("sections" to "log,times", "start" to "tasbih"))
        assertEquals(listOf(Section.Home, Section.Log, Section.Times), s.sections)
        assertEquals(Section.Home, s.startSection)
    }

    @Test fun withSectionsKeepsHomeAndDropsDuplicates() {
        val s = AppSettings().withSections(listOf(Section.Qibla, Section.Qibla))
        assertEquals(listOf(Section.Home, Section.Qibla), s.sections)
    }
}

class TimesByTheMinuteTest {
    private val cairo = kotlinx.datetime.LocalDate(2026, 3, 21)
    private fun day() = PrayerTimes.compute(cairo, 30.0444, 31.2357, 2.0)

    @Test fun shiftedMovesOnlyTheChosenPrayers() {
        val base = day(); val moved = base.shifted(mapOf(Prayer.Maghrib to 5, Prayer.Fajr to -3))
        assertEquals(base.maghrib!! + 5 / 60.0, moved.maghrib!!, 1e-9); assertEquals(base.fajr!! - 3 / 60.0, moved.fajr!!, 1e-9)
        assertEquals(base.dhuhr, moved.dhuhr); assertEquals(base.isha, moved.isha)
    }
    @Test fun noAdjustmentGivesTheSameObject() { val base = day(); assertTrue(base.shifted(emptyMap()) === base) }
    @Test fun anAbsentTimeStaysAbsent() { val polar = PrayerTimes.compute(kotlinx.datetime.LocalDate(2026, 6, 21), 69.65, 18.96, 2.0).shifted(mapOf(Prayer.Fajr to 10)); assertEquals(null, polar.fajr) }

    @Test fun theAlertFollowsTheAdjustedTime() {
        val zone = kotlinx.datetime.TimeZone.of("Africa/Cairo"); val now = kotlinx.datetime.Instant.parse("2026-03-21T00:00:00Z")
        val plain = Schedule.next(now, zone, 30.0444, 31.2357, Method.Egyptian, AsrFactor.Standard, setOf(Prayer.Maghrib))!!
        val later = Schedule.next(now, zone, 30.0444, 31.2357, Method.Egyptian, AsrFactor.Standard, setOf(Prayer.Maghrib), adjust = mapOf(Prayer.Maghrib to 7))!!
        assertEquals(plain.atMillis + 7 * 60_000L, later.atMillis)
    }
    @Test fun theAlertFiresOnTheMinuteThatIsShown() {
        val zone = kotlinx.datetime.TimeZone.of("Africa/Cairo"); val now = kotlinx.datetime.Instant.parse("2026-03-21T00:00:00Z")
        val a = Schedule.next(now, zone, 30.0444, 31.2357, Method.Egyptian, AsrFactor.Standard, setOf(Prayer.Maghrib))!!
        assertEquals(0L, a.atMillis % 60_000L)
    }
    @Test fun adjustmentsRoundTripThroughSettings() {
        val s = AppSettings(adjust = mapOf(Prayer.Fajr to -4, Prayer.Isha to 12))
        assertEquals(s, SettingsCodec.decode(SettingsCodec.encode(s)))
        assertEquals(AppSettings(), SettingsCodec.decode(SettingsCodec.encode(s.copy(adjust = emptyMap()))))
    }
    @Test fun adjustmentsOutsideThirtyMinutesAreIgnored() = assertEquals(emptyMap(), SettingsCodec.decode(mapOf("adjust.Fajr" to "45")).adjust)
}

class CountdownTest {
    @Test fun hoursAndMinutesAboveAMinute() { val c = Countdown.of(1 * 3600 + 43 * 60 + 20L); assertEquals(Triple(1, 43, false), Triple(c.hours, c.minutes, c.inLastMinute)) }
    @Test fun justOverAMinuteStillShowsMinutes() { val c = Countdown.of(60); assertEquals(1, c.minutes); assertFalse(c.inLastMinute) }
    @Test fun secondsOnlyInTheLastMinute() { val c = Countdown.of(59); assertTrue(c.inLastMinute); assertEquals(59, c.seconds) }
    @Test fun neverNegative() = assertEquals(0, Countdown.of(-5).seconds)
    @Test fun theScreenUpdatesEverySecondOnlyInTheLastMinute() { assertEquals(400, Countdown.nextTickMillis(30, 5_600)); assertEquals(20 + 15_000, Countdown.nextTickMillis(3600, 45_000)) }
}

class PrayerScreenDataTest {
    private val today = kotlinx.datetime.LocalDate(2026, 10, 15)
    private fun yesterday() = today.minus(1, kotlinx.datetime.DateTimeUnit.DAY)

    @Test fun historyIsNewestFirstAndMarksPrayedDays() {
        val log = PrayerLog().mark(today, Prayer.Fajr, true).mark(yesterday(), Prayer.Fajr, true).mark(yesterday(), Prayer.Dhuhr, true)
        val h = log.history(Prayer.Fajr, today, 3)
        assertEquals(listOf(today, yesterday(), today.minus(2, kotlinx.datetime.DateTimeUnit.DAY)), h.map { it.first })
        assertEquals(listOf(true, true, false), h.map { it.second })
    }
    @Test fun streakOfOnePrayerCountsBackFromTodayOrYesterday() {
        val log = PrayerLog().mark(yesterday(), Prayer.Isha, true).mark(yesterday().minus(1, kotlinx.datetime.DateTimeUnit.DAY), Prayer.Isha, true)
        assertEquals(2, log.streakOf(Prayer.Isha, today)); assertEquals(3, log.mark(today, Prayer.Isha, true).streakOf(Prayer.Isha, today)); assertEquals(0, log.streakOf(Prayer.Fajr, today))
    }
    @Test fun theOptionIsOnByDefaultAndRoundTrips() {
        assertTrue(AppSettings().prayerScreens)
        val off = AppSettings(prayerScreens = false, sounds = mapOf(Prayer.Fajr to "content://media/x", Prayer.Isha to Sounds.SILENT))
        assertEquals(off, SettingsCodec.decode(SettingsCodec.encode(off)))
    }
    @Test fun unknownSoundsFallBackToTheDefault() = assertEquals(Sounds.DEFAULT, SettingsCodec.decode(mapOf("sound.Fajr" to "rm -rf")).soundOf(Prayer.Fajr))
}
