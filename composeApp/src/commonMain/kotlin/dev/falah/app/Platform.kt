package dev.falah.app

/** What only the phone can do: ask for permission to notify, and set the alarms that fire at each prayer. */
interface Platform {
    /** Whether this system can show prayer notifications at all. */
    val canNotify: Boolean
    /** Asks for the notification permission if it is needed; [done] says whether notifications may be shown. */
    fun ensureNotifications(done: (Boolean) -> Unit)
    /** Sets the next alarm from the saved place, method and chosen prayers (or cancels it when none is chosen). */
    fun reschedule()
}

object NoPlatform : Platform {
    override val canNotify = false
    override fun ensureNotifications(done: (Boolean) -> Unit) = done(false)
    override fun reschedule() {}
}
