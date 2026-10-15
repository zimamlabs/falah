package dev.falah.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class PrayerScreenUiTest {
    private fun prefs(vararg extra: Pair<String, String>) = MapPrefs(mapOf("tour.done" to "1", "seen.version" to APP_VERSION, "place" to "Cairo", "lat" to "30.0444", "lon" to "31.2357", "start" to "times") + extra)

    @Test fun tappingAPrayerOpensItsScreen() = runComposeUiTest {
        setContent { FalahApp(prefs(), files = MemoryFiles(), showSplash = false) }
        onNodeWithTag("times-row-Maghrib").performClick(); waitForIdle()
        onNodeWithText("How this time is calculated").assertExists()
        onNodeWithText("Maghrib is sunset.").assertExists()
        onNodeWithText("Last 30 days").assertExists()
    }

    @Test fun theOptionTurnsItOff() = runComposeUiTest {
        setContent { FalahApp(prefs("prayerscreens" to "0"), files = MemoryFiles(), showSplash = false) }
        onNodeWithTag("times-row-Maghrib").performClick(); waitForIdle()
        onNode(hasText("How this time is calculated")).assertDoesNotExist()
    }

    @Test fun sunriseHasAScreenWithoutALog() = runComposeUiTest {
        setContent { FalahApp(prefs(), files = MemoryFiles(), showSplash = false) }
        onNodeWithTag("times-row-Sunrise").performClick(); waitForIdle()
        onNodeWithText("How this time is calculated").assertExists()
        onNode(hasText("Last 30 days")).assertDoesNotExist()
    }
}
