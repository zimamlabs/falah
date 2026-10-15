package dev.falah.core

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** What asking the phone for its position came back with. */
sealed interface LocationFix {
    data class Found(val latitude: Double, val longitude: Double) : LocationFix
    /** The user did not allow location. */
    data object Denied : LocationFix
    /** Location is switched off, or the phone has no way to find a position right now. */
    data object Unavailable : LocationFix
    /** No position arrived in time (no GPS signal, or offline with no stored fix). */
    data object TimedOut : LocationFix
}

enum class PlaceSource(val key: String) { Manual("manual"), Gps("gps") }

object Geo {
    /** Great-circle distance in kilometres. */
    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        fun r(d: Double) = d * PI / 180
        val a = sin(r(lat2 - lat1) / 2).let { it * it } + cos(r(lat1)) * cos(r(lat2)) * sin(r(lon2 - lon1) / 2).let { it * it }
        return 2 * 6371.0 * asin(sqrt(a))
    }

    /** The name of the closest of [places] (name, latitude, longitude) within [maxKm], or null. */
    fun nearest(latitude: Double, longitude: Double, places: List<Triple<String, Double, Double>>, maxKm: Double): String? =
        places.map { it.first to distanceKm(latitude, longitude, it.second, it.third) }.filter { it.second <= maxKm }.minByOrNull { it.second }?.first

    /** A fix is only usable if the coordinates are real numbers inside the valid range. */
    fun valid(latitude: Double, longitude: Double) = latitude in -90.0..90.0 && longitude in -180.0..180.0 && !(latitude == 0.0 && longitude == 0.0)
}
