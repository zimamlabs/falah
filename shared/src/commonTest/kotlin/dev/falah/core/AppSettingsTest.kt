package dev.falah.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
