package dev.falah.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dev.falah.core.LocationFix
import dev.falah.core.PlaceSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class LocationTest {
    private class Gps(var answer: LocationFix) : Platform {
        override val canNotify = false
        override val canLocate = true
        var asked = 0
        override fun ensureNotifications(done: (Boolean) -> Unit) = done(false)
        override fun reschedule() {}
        override fun requestLocation(done: (LocationFix) -> Unit) { asked++; done(answer) }
    }
    private fun state(p: Platform) = FalahState(MapPrefs(), MemoryFiles(), p)

    @Test fun aFixNearACityNamesTheCityAndMarksItAsGps() {
        val s = state(Gps(LocationFix.Found(30.05, 31.24))); s.useLocation()
        assertEquals("Cairo", s.placeName); assertEquals(PlaceSource.Gps, s.settings.placeSource); assertTrue(s.placeChosen)
        assertEquals(30.05, s.lat)
    }

    @Test fun aFixFarFromEveryCityIsCalledMyLocation() {
        val s = state(Gps(LocationFix.Found(3.139, 101.687))); s.useLocation()
        assertEquals("My location", s.placeName); assertEquals(3.139, s.lat)
    }

    @Test fun deniedAndUnavailableAndTimeoutNeverChooseAPlace() {
        for (answer in listOf(LocationFix.Denied, LocationFix.Unavailable, LocationFix.TimedOut)) {
            val s = state(Gps(answer)); s.useLocation()
            assertFalse(s.placeChosen, "$answer must not pick a place"); assertEquals(answer, s.locationProblem); assertFalse(s.locating)
        }
    }

    @Test fun anImpossibleFixIsNotUsed() {
        val s = state(Gps(LocationFix.Found(0.0, 0.0))); s.useLocation()
        assertFalse(s.placeChosen); assertEquals(LocationFix.Unavailable, s.locationProblem)
    }

    @Test fun aFailedFixKeepsThePlaceTheUserHad() {
        val platform = Gps(LocationFix.Denied); val s = state(platform); s.choosePlace(Places.first { it.name == "London" })
        s.useLocation()
        assertEquals("London", s.placeName); assertEquals(PlaceSource.Manual, s.settings.placeSource)
    }

    @Test fun theExplanationComesBeforeAnyRequest() = runComposeUiTest {
        val platform = Gps(LocationFix.Found(30.05, 31.24))
        setContent { FalahApp(MapPrefs(mapOf("tour.done" to "1", "seen.version" to APP_VERSION, "start" to "times")), platform, MemoryFiles(), showSplash = false) }
        onAllNodesWithText("No place chosen yet", substring = true)[0].assertExists()
        onAllNodesWithTag("place-picker")[0].performClick()
        onNodeWithText("Use my location").performClick(); waitForIdle()
        onNodeWithText("Find your place with GPS?").assertExists()
        assertEquals(0, platform.asked)
        onNodeWithText("Not now").performClick(); waitForIdle()
        assertEquals(0, platform.asked)
        onAllNodesWithTag("place-picker")[0].performClick(); onNodeWithText("Use my location").performClick(); waitForIdle()
        onNodeWithText("Continue").performClick(); waitForIdle()
        assertEquals(1, platform.asked); onAllNodesWithText("Found by GPS")[0].assertExists()
    }

    @Test fun denialShowsAMessageAndTheManualWaysStay() = runComposeUiTest {
        setContent { FalahApp(MapPrefs(mapOf("tour.done" to "1", "seen.version" to APP_VERSION, "start" to "times")), Gps(LocationFix.Denied), MemoryFiles(), showSplash = false) }
        onAllNodesWithTag("place-picker")[0].performClick(); onNodeWithText("Use my location").performClick(); onNodeWithText("Continue").performClick(); waitForIdle()
        onAllNodesWithText("Location permission was not given.", substring = true)[0].assertExists()
        onAllNodesWithText("No place chosen yet", substring = true)[0].assertExists()
        onAllNodesWithTag("place-picker")[0].performClick(); onNodeWithText("Other place, type coordinates").assertExists()
    }
}
