package dev.falah.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CustomPlaceTest {
    @Test fun aFreshInstallSaysNoPlaceIsChosenYet() = assertFalse(FalahState(MapPrefs(), MemoryFiles(), NoPlatform).placeChosen)

    @Test fun aTypedPlaceIsUsedAndSaved() {
        val prefs = MapPrefs(); val s = FalahState(prefs, MemoryFiles(), NoPlatform)
        s.chooseCustomPlace(" Karachi ", 24.8607, 67.0011)
        assertTrue(s.placeChosen)
        assertEquals(Triple("Karachi", 24.8607, 67.0011), Triple(s.placeName, s.lat, s.lon))
        assertEquals("24.8607", prefs.get("lat")); assertEquals("67.0011", prefs.get("lon")); assertEquals("Karachi", prefs.get("place"))
    }

    @Test fun anEmptyNameGetsADefaultName() {
        val s = FalahState(MapPrefs(), MemoryFiles(), NoPlatform); s.chooseCustomPlace("  ", 1.0, 2.0)
        assertEquals("My place", s.placeName)
    }

    @Test fun aSavedPlaceCountsAsChosenAfterRestart() = assertTrue(FalahState(MapPrefs(mapOf("place" to "Cairo")), MemoryFiles(), NoPlatform).placeChosen)
}
