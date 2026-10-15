package dev.falah.app

/** What only the phone can do: ask for permission to notify, and set the alarms that fire at each prayer. */
interface Platform {
    /** Whether this system can show prayer notifications at all. */
    val canNotify: Boolean
    /** Asks for the notification permission if it is needed; [done] says whether notifications may be shown. */
    fun ensureNotifications(done: (Boolean) -> Unit)
    /** Sets the next alarm from the saved place, method and chosen prayers (or cancels it when none is chosen). */
    fun reschedule()
    /** Hides the notification of [prayer] and shows it again in [minutes]. */
    fun snooze(prayer: dev.falah.core.Prayer, minutes: Int, prayerAtMillis: Long) {}
    /** Hides the notification of [prayer]. */
    fun dismissNotification(prayer: dev.falah.core.Prayer) {}
    /** What may stop reminders from working, or null where there is nothing to check. */
    fun reminderStatus(): dev.falah.core.ReminderStatus? = null
    /** Opens the system screen where the user can fix [problem]. */
    fun openReminderSetting(problem: dev.falah.core.ReminderProblem) {}
    /** Whether the phone can pick a notification sound for a prayer. */
    val canPickSound: Boolean get() = false
    /** Opens the phone's sound picker; [done] gets the chosen sound ([dev.falah.core.Sounds.SILENT] for none), or is not called if the user cancelled. */
    fun pickSound(current: String, done: (String) -> Unit) {}
    /** The title of a picked sound, or null when unknown. */
    fun soundTitle(value: String): String? = null
    /** Whether this device has a compass sensor. */
    val hasCompass: Boolean get() = false
    /** Starts reporting the heading in degrees from true north, with the sensor accuracy (0 unreliable to 3 high); stop with [stopCompass]. */
    fun startCompass(latitude: Double, longitude: Double, onHeading: (Double, Int) -> Unit) {}
    fun stopCompass() {}
}

object NoPlatform : Platform {
    override val canNotify = false
    override fun ensureNotifications(done: (Boolean) -> Unit) = done(false)
    override fun reschedule() {}
}
