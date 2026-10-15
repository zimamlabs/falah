package dev.falah.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PrayerNoticeTest {
    @Test fun aTappedNotificationRoutesToItsPrayer() = assertEquals(PrayerNotice(Prayer.Maghrib, 1_000L), NoticeRoute.parse(NoticeRoute.ACTION_OPEN, "Maghrib", 1_000L))
    @Test fun thePrayedButtonAlsoMarksIt() = assertTrue(NoticeRoute.parse(NoticeRoute.ACTION_PRAYED, "Fajr", 5L)!!.markPrayed)
    @Test fun sunriseCannotBeMarkedPrayed() = assertFalse(NoticeRoute.parse(NoticeRoute.ACTION_PRAYED, "Sunrise", 5L)!!.markPrayed)
    @Test fun otherActionsAndBadExtrasAreIgnored() {
        assertNull(NoticeRoute.parse("android.intent.action.MAIN", "Fajr", 5L)); assertNull(NoticeRoute.parse(null, "Fajr", 5L))
        assertNull(NoticeRoute.parse(NoticeRoute.ACTION_OPEN, "Nope", 5L)); assertNull(NoticeRoute.parse(NoticeRoute.ACTION_OPEN, null, 5L))
        assertNull(NoticeRoute.parse(NoticeRoute.ACTION_OPEN, "Fajr", null)); assertNull(NoticeRoute.parse(NoticeRoute.ACTION_OPEN, "Fajr", 0L))
    }

    @Test fun beforeThePrayerItCountsDown() = assertEquals(NoticeClock.Before(300), NoticeClock.of(prayerAtMillis = 1_300_000, nowMillis = 1_000_000))
    @Test fun afterThePrayerItCountsUp() = assertEquals(NoticeClock.Since(192), NoticeClock.of(prayerAtMillis = 1_000_000, nowMillis = 1_192_000))
    @Test fun atTheExactMomentItStartsCountingUpFromZero() = assertEquals(NoticeClock.Since(0), NoticeClock.of(2_000, 2_000))
    @Test fun partialSecondsRoundTowardsTheMoment() { assertEquals(NoticeClock.Before(1), NoticeClock.of(2_000, 1_500)); assertEquals(NoticeClock.Since(0), NoticeClock.of(2_000, 2_400)) }

    @Test fun theFiveNoticeScreensAreOnByDefaultAndSunriseIsOff() {
        val s = AppSettings(); assertEquals(loggedPrayers.toSet(), s.noticeScreen); assertFalse(Prayer.Sunrise in s.noticeScreen); assertTrue(s.noticeFullScreen); assertEquals(10, s.snoozeMinutes)
    }
    @Test fun noticeSettingsRoundTrip() {
        val s = AppSettings(noticeScreen = setOf(Prayer.Fajr, Prayer.Sunrise), noticeFullScreen = false, snoozeMinutes = 20)
        assertEquals(s, SettingsCodec.decode(SettingsCodec.encode(s)))
        assertEquals(emptySet(), SettingsCodec.decode(SettingsCodec.encode(AppSettings(noticeScreen = emptySet()))).noticeScreen)
    }
    @Test fun anOldInstallKeepsTheDefaults() = assertEquals(AppSettings().noticeScreen, SettingsCodec.decode(mapOf("place" to "Cairo")).noticeScreen)
    @Test fun badSnoozeFallsBack() = assertEquals(10, SettingsCodec.decode(mapOf("snooze" to "7")).snoozeMinutes)

    @Test fun statusOnlyReportsWhatMatters() {
        val ok = ReminderStatus(true, true, false, true)
        assertTrue(ok.allGood); assertEquals(emptyList(), ok.problems(wantsFullScreen = false)); assertEquals(listOf(ReminderProblem.FullScreenOff), ok.problems(true))
        assertEquals(listOf(ReminderProblem.NotificationsOff, ReminderProblem.ExactAlarmsOff, ReminderProblem.BatteryOptimised), ReminderStatus(false, false, true, false).problems(true))
    }
}
