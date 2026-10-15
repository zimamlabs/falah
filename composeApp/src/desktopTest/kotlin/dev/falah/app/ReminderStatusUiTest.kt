package dev.falah.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dev.falah.core.ReminderProblem
import dev.falah.core.ReminderStatus
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class ReminderStatusUiTest {
    private class Fake(val status: ReminderStatus) : Platform {
        override val canNotify = true
        val opened = mutableListOf<ReminderProblem>()
        override fun ensureNotifications(done: (Boolean) -> Unit) = done(true)
        override fun reschedule() {}
        override fun reminderStatus() = status
        override fun openReminderSetting(problem: ReminderProblem) { opened += problem }
    }
    private fun prefs(vararg extra: Pair<String, String>) = MapPrefs(mapOf("tour.done" to "1", "seen.version" to APP_VERSION, "place" to "Cairo", "lat" to "30.0444", "lon" to "31.2357", "notify.Fajr" to "1") + extra)

    @Test fun oneCardListsEveryProblemWithAButtonToTheRightSetting() = runComposeUiTest {
        val platform = Fake(ReminderStatus(notifications = false, exactAlarms = false, fullScreen = false, batteryUnrestricted = false))
        setContent { FalahApp(prefs(), platform, MemoryFiles(), showSplash = false) }
        onNodeWithText("Reminders may not work").assertExists()
        onNodeWithText("Notifications are off for Falah, so no reminder can show.").assertExists()
        onNodeWithText("Exact alarms are off, so reminders can come a few minutes late.").assertExists()
        onNodeWithText("Full-screen prayer screens are not allowed, so you get a normal notification instead.").assertExists()
        onAllNodesWithText("Open settings").assertCountEquals(4)
        onAllNodesWithText("Open settings")[1].performClick()
        assertEquals(listOf(ReminderProblem.ExactAlarmsOff), platform.opened)
    }

    @Test fun nothingShowsWhenEverythingIsFine() = runComposeUiTest {
        setContent { FalahApp(prefs(), Fake(ReminderStatus(true, true, true, true)), MemoryFiles(), showSplash = false) }
        onNode(hasText("Reminders may not work")).assertDoesNotExist()
    }

    @Test fun noCardWhenNoReminderIsOn() = runComposeUiTest {
        val p = MapPrefs(mapOf("tour.done" to "1", "seen.version" to APP_VERSION, "place" to "Cairo", "lat" to "30.0444", "lon" to "31.2357"))
        setContent { FalahApp(p, Fake(ReminderStatus(false, false, false, false)), MemoryFiles(), showSplash = false) }
        onNode(hasText("Reminders may not work")).assertDoesNotExist()
    }
}
