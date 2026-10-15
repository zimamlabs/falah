package dev.falah.core

/** How long is left, always as hours, minutes and seconds (HH:MM:SS). */
data class Countdown(val hours: Int, val minutes: Int, val seconds: Int) {
    /** "01:43:20"; the screen turns the digits into Arabic-Indic ones when that setting is on. */
    fun text(): String = "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"

    companion object {
        fun of(secondsLeft: Long): Countdown {
            val s = secondsLeft.coerceAtLeast(0)
            return Countdown((s / 3600).toInt(), (s % 3600 / 60).toInt(), (s % 60).toInt())
        }

        /** Milliseconds from [nowMillis] to the next whole second of the clock. Recomputed from the clock each time, so it never drifts. */
        fun nextTickMillis(nowMillis: Long): Long = 1000 - nowMillis % 1000 + 2
    }
}
