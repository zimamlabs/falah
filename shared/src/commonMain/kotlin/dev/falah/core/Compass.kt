package dev.falah.core


object Compass {
    fun normalize(deg: Double): Double = ((deg % 360.0) + 360.0) % 360.0

    /** How far to turn from [heading] to face [qibla], 0 up to 360 (clockwise). */
    fun relative(qibla: Double, heading: Double): Double = normalize(qibla - heading)

    /** Facing the qibla within [tolerance] degrees either way. */
    fun aligned(relative: Double, tolerance: Double = 4.0): Boolean = normalize(relative).let { it <= tolerance || it >= 360.0 - tolerance }

    /** Moves [previous] a fraction [alpha] of the way to [next] along the shortest turn, so the needle does not jitter or spin across north. */
    fun smooth(previous: Double?, next: Double, alpha: Double = 0.2): Double {
        if (previous == null) return normalize(next)
        val delta = ((next - previous + 540.0) % 360.0) - 180.0
        return normalize(previous + alpha * delta)
    }

    /** True north from a magnetic reading and the local declination (east is positive). */
    fun trueHeading(magnetic: Double, declination: Double): Double = normalize(magnetic + declination)

}
