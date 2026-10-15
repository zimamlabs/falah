package dev.falah.core

enum class CoordinateError { Empty, NotANumber, LatitudeRange, LongitudeRange }

sealed interface CoordinateResult {
    data class Ok(val latitude: Double, val longitude: Double) : CoordinateResult
    data class Bad(val error: CoordinateError) : CoordinateResult
}

/** Parses a typed latitude and longitude: Western or Arabic-Indic digits, "." "," or "٫" as the decimal mark, an optional degree sign. */
object Coordinates {
    private val number = Regex("^[+-]?(\\d+(\\.\\d*)?|\\.\\d+)$")

    private fun normalise(text: String): String = buildString {
        for (c in text.trim()) append(
            when (c) {
                in '٠'..'٩' -> '0' + (c - '٠')
                in '۰'..'۹' -> '0' + (c - '۰')
                ',', '٫' -> '.'
                '−', '–' -> '-'
                '°' -> ""
                else -> c
            }
        )
    }.trim()

    private fun one(text: String): Double? = normalise(text).takeIf { number.matches(it) }?.toDoubleOrNull()

    fun parse(latitude: String, longitude: String): CoordinateResult {
        if (latitude.isBlank() || longitude.isBlank()) return CoordinateResult.Bad(CoordinateError.Empty)
        val lat = one(latitude) ?: return CoordinateResult.Bad(CoordinateError.NotANumber)
        val lon = one(longitude) ?: return CoordinateResult.Bad(CoordinateError.NotANumber)
        if (lat < -90 || lat > 90) return CoordinateResult.Bad(CoordinateError.LatitudeRange)
        if (lon < -180 || lon > 180) return CoordinateResult.Bad(CoordinateError.LongitudeRange)
        return CoordinateResult.Ok(lat, lon)
    }
}
