package dev.falah.app

import dev.falah.core.AppSettings
import dev.falah.core.Prayer
import dev.falah.core.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettingsRepositoryTest {
    @Test fun changingOneSettingWritesOnlyThatKey() {
        val prefs = MapPrefs(); val repo = SettingsRepository(prefs)
        repo.save(AppSettings(), AppSettings(theme = ThemeMode.Dark))
        assertEquals(mapOf("theme" to "dark"), prefs.all())
    }

    @Test fun savedChangesLoadBack() {
        val prefs = MapPrefs(); val repo = SettingsRepository(prefs)
        var s = AppSettings()
        val next = s.copy(placeName = "Doha", latitude = 25.28, longitude = 51.53, placeChosen = true, notify = setOf(Prayer.Fajr)); repo.save(s, next); s = next
        assertEquals(s, repo.load())
    }

    @Test fun aFreshInstallLeavesNoPlaceOrReminderKeys() {
        val state = FalahState(MapPrefs(), MemoryFiles(), NoPlatform); state.chooseTheme(ThemeMode.Light)
        assertNull(state.prefs.get("place")); assertNull(state.prefs.get("notify.Fajr"))
    }

    @Test fun theStateReloadsWhatWasSaved() {
        val prefs = MapPrefs(); FalahState(prefs, MemoryFiles(), NoPlatform).chooseCustomPlace("Doha", 25.28, 51.53)
        val again = FalahState(prefs, MemoryFiles(), NoPlatform)
        assertEquals("Doha", again.placeName); assertEquals(25.28, again.lat)
    }
}
