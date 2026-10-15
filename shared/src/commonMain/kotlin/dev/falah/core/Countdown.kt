package dev.falah.core

/** How long is left, as the screen shows it: hours and minutes, and seconds only in the last minute. */
data class Countdown(val hours: Int, val minutes: Int, val seconds: Int, val inLastMinute: Boolean) {
    companion object {
        fun of(secondsLeft: Long): Countdown {
            val s = secondsLeft.coerceAtLeast(0)
            return if (s < 60) Countdown(0, 0, s.toInt(), true) else Countdown((s / 3600).toInt(), (s % 3600 / 60).toInt(), 0, false)
        }

        /** Milliseconds until the screen needs to update: the next second in the last minute, otherwise the next whole minute (the target is a whole minute). */
        fun nextTickMillis(secondsLeft: Long, nowMillis: Long): Long = if (secondsLeft < 60) 1000 - nowMillis % 1000 else 60_000 - nowMillis % 60_000 + 20
    }
}
