package dev.falah.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dev.falah.core.Prayer
import dev.falah.core.PrayerNotice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class NoticeScreenUiTest {
    private fun prefs() = MapPrefs(mapOf("tour.done" to "1", "seen.version" to APP_VERSION, "place" to "Cairo", "lat" to "30.0444", "lon" to "31.2357"))
    private val nowMs get() = Clock.System.now().toEpochMilliseconds()

    private class RecordingPlatform : Platform {
        override val canNotify = true
        val snoozed = mutableListOf<Pair<Prayer, Int>>(); val dismissed = mutableListOf<Prayer>()
        override fun ensureNotifications(done: (Boolean) -> Unit) = done(true)
        override fun reschedule() {}
        override fun snooze(prayer: Prayer, minutes: Int, prayerAtMillis: Long) { snoozed += prayer to minutes }
        override fun dismissNotification(prayer: Prayer) { dismissed += prayer }
    }

    @Test fun beforeTheTimeItCountsDownAndAfterItCountsUp() = runComposeUiTest {
        var notice by androidx.compose.runtime.mutableStateOf<PrayerNotice?>(PrayerNotice(Prayer.Maghrib, nowMs + 300_000))
        setContent { FalahApp(prefs(), files = MemoryFiles(), showSplash = false, notice = notice, onNoticeDone = { notice = null }) }
        onNodeWithText("Time left").assertExists(); onNodeWithText("It is time for Maghrib").assertExists()
        notice = PrayerNotice(Prayer.Maghrib, nowMs - 192_000); waitForIdle()
        onNodeWithText("Since the time began").assertExists()
    }

    @Test fun prayedMarksTheLogAndSkipClosesWithoutMarking() = runComposeUiTest {
        val state = intArrayOf(0); var notice: PrayerNotice? = PrayerNotice(Prayer.Fajr, nowMs - 60_000)
        val platform = RecordingPlatform(); val files = MemoryFiles()
        setContent { FalahApp(prefs(), platform, files, showSplash = false, notice = notice, onNoticeDone = { notice = null; state[0]++ }) }
        onNodeWithText("Skip").performClick(); waitForIdle()
        assertEquals(listOf(Prayer.Fajr), platform.dismissed); assertEquals(1, state[0]); assertEquals(null, files.read(FalahState.LOG_FILE))
    }

    @Test fun snoozeUsesTheChosenMinutesAndCloses() = runComposeUiTest {
        var notice: PrayerNotice? = PrayerNotice(Prayer.Dhuhr, nowMs - 5_000); val platform = RecordingPlatform()
        setContent { FalahApp(MapPrefs(mapOf("tour.done" to "1", "seen.version" to APP_VERSION, "place" to "Cairo", "snooze" to "15")), platform, MemoryFiles(), showSplash = false, notice = notice, onNoticeDone = { notice = null }) }
        onNodeWithText("Snooze 15 min").performClick(); waitForIdle()
        assertEquals(listOf(Prayer.Dhuhr to 15), platform.snoozed)
    }

    @Test fun thePrayedButtonOnTheNotificationMarksItAtOnce() = runComposeUiTest {
        val files = MemoryFiles()
        setContent { FalahApp(prefs(), RecordingPlatform(), files, showSplash = false, notice = PrayerNotice(Prayer.Isha, nowMs - 1_000, markPrayed = true), onNoticeDone = {}) }
        waitForIdle(); onNodeWithText("Marked as prayed").assertExists()
        assertTrue(files.read(FalahState.LOG_FILE)!!.contains("Isha"))
    }

    @Test fun sunriseHasNoPrayedButton() = runComposeUiTest {
        setContent { FalahApp(prefs(), RecordingPlatform(), MemoryFiles(), showSplash = false, notice = PrayerNotice(Prayer.Sunrise, nowMs), onNoticeDone = {}) }
        onNode(hasText("Prayed")).assertDoesNotExist(); onNodeWithText("Qibla direction").assertExists()
    }
}
