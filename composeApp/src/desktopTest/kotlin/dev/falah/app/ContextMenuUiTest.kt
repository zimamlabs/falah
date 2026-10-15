package dev.falah.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class ContextMenuUiTest {
    private fun prefs(start: String) = MapPrefs(mapOf("tour.done" to "1", "seen.version" to APP_VERSION, "place" to "Cairo", "lat" to "30.0444", "lon" to "31.2357", "start" to start))
    private fun log(files: MemoryFiles) = files.read(FalahState.LOG_FILE).orEmpty()

    @Test fun longPressOnAPrayerRowOffersDetailsAndPrayed() = runComposeUiTest {
        val files = MemoryFiles()
        setContent { FalahApp(prefs("times"), files = files, showSplash = false) }
        onNodeWithTag("times-row-Dhuhr").performTouchInput { longClick() }; waitForIdle()
        onNodeWithText("Details and history").assertExists(); onNodeWithText("Mark as prayed").assertExists()
        onNodeWithText("Mark as prayed").performClick(); waitForIdle()
        assertTrue(log(files).contains("Dhuhr"))
    }

    @Test fun unmarkingAsksFirstAndOnlyDeletesAfterConfirming() = runComposeUiTest {
        val files = MemoryFiles()
        setContent { FalahApp(prefs("times"), files = files, showSplash = false) }
        onNodeWithTag("times-row-Asr").performTouchInput { longClick() }; onNodeWithText("Mark as prayed").performClick(); waitForIdle()
        onNodeWithTag("times-row-Asr").performTouchInput { longClick() }; waitForIdle()
        onNodeWithText("Mark as not prayed").performClick(); waitForIdle()
        onNodeWithText("Delete this log entry?").assertExists(); assertTrue(log(files).contains("Asr"))
        onNodeWithText("Delete").performClick(); waitForIdle()
        assertFalse(log(files).contains("Asr"))
    }

    @Test fun tapStillOpensThePrayerScreenAndLongPressDoesNot() = runComposeUiTest {
        setContent { FalahApp(prefs("times"), files = MemoryFiles(), showSplash = false) }
        onNodeWithTag("times-row-Fajr").performTouchInput { longClick() }; waitForIdle()
        assertEquals(0, onAllNodesCount("How this time is calculated"))
        onNodeWithText("Details and history").performClick(); waitForIdle()
        onNodeWithText("How this time is calculated").assertExists()
    }

    @Test fun tasbihTapCountsAndLongPressDeletesTodayWithAConfirm() = runComposeUiTest {
        val files = MemoryFiles()
        setContent { FalahApp(prefs("tasbih"), files = files, showSplash = false) }
        onNodeWithTag("tasbih-counter").performClick(); onNodeWithTag("tasbih-counter").performClick(); waitForIdle()
        assertTrue(files.read(FalahState.TASBIH_FILE)!!.contains(",2"))
        onNodeWithTag("tasbih-counter").performTouchInput { longClick() }; waitForIdle()
        onNodeWithText("Delete today's count").performClick(); waitForIdle()
        onNodeWithText("Delete today's count?").assertExists(); assertTrue(files.read(FalahState.TASBIH_FILE)!!.contains(",2"))
        onNodeWithText("Delete").performClick(); waitForIdle()
        assertEquals("", files.read(FalahState.TASBIH_FILE))
    }

    private fun androidx.compose.ui.test.ComposeUiTest.onAllNodesCount(text: String) = onAllNodes(hasText(text)).fetchSemanticsNodes().size
}
