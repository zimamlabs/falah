package dev.falah.core

/** A prayer notification that was tapped (or whose full-screen screen was shown): which prayer, when its time is, and what to do at once. */
data class PrayerNotice(val prayer: Prayer, val prayerAtMillis: Long, val markPrayed: Boolean = false)

/** Turns the extras of an Android intent into a [PrayerNotice]. Anything unknown or incomplete is ignored. */
object NoticeRoute {
    const val ACTION_OPEN = "dev.falah.action.OPEN_PRAYER"
    const val ACTION_PRAYED = "dev.falah.action.PRAYED"
    const val EXTRA_PRAYER = "prayer"
    const val EXTRA_AT = "at"

    fun parse(action: String?, prayerName: String?, atMillis: Long?): PrayerNotice? {
        if (action != ACTION_OPEN && action != ACTION_PRAYED) return null
        val prayer = Prayer.entries.firstOrNull { it.name == prayerName } ?: return null
        if (atMillis == null || atMillis <= 0) return null
        return PrayerNotice(prayer, atMillis, markPrayed = action == ACTION_PRAYED && prayer != Prayer.Sunrise)
    }
}

/** What the notice screen shows: time left before the prayer (a reminder sent early) or time since the prayer's time. */
sealed interface NoticeClock {
    val seconds: Long
    data class Before(override val seconds: Long) : NoticeClock
    data class Since(override val seconds: Long) : NoticeClock

    companion object {
        fun of(prayerAtMillis: Long, nowMillis: Long): NoticeClock {
            val diff = (prayerAtMillis - nowMillis).let { if (it > 0) (it + 999) / 1000 else it / 1000 }
            return if (diff > 0) Before(diff) else Since(-diff)
        }
    }
}

/** The "reminders may not work" checks, each true when everything is fine. */
data class ReminderStatus(val notifications: Boolean, val exactAlarms: Boolean, val fullScreen: Boolean, val batteryUnrestricted: Boolean) {
    val allGood get() = notifications && exactAlarms && batteryUnrestricted
    /** Problems the user can fix, most serious first. Full-screen is only a problem when the user wants the full screen. */
    fun problems(wantsFullScreen: Boolean): List<ReminderProblem> = buildList {
        if (!notifications) add(ReminderProblem.NotificationsOff)
        if (!exactAlarms) add(ReminderProblem.ExactAlarmsOff)
        if (wantsFullScreen && !fullScreen) add(ReminderProblem.FullScreenOff)
        if (!batteryUnrestricted) add(ReminderProblem.BatteryOptimised)
    }
}

enum class ReminderProblem { NotificationsOff, ExactAlarmsOff, FullScreenOff, BatteryOptimised }
