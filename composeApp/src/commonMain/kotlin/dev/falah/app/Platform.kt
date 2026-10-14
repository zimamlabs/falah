package dev.falah.app

/** What only the phone can do: ask for permission to notify, and set the alarms that fire at each prayer. */
interface Platform {
    /** Whether this system can show prayer notifications at all. */
    val canNotify: Boolean
    /** Asks for the notification permission if it is needed; [done] says whether notifications may be shown. */
    fun ensureNotifications(done: (Boolean) -> Unit)
    /** Sets the next alarm from the saved place, method and chosen prayers (or cancels it when none is chosen). */
    fun reschedule()
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
