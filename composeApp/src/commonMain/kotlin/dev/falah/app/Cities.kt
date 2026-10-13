package dev.falah.app

/** Places to choose from; "Custom" lets you type any latitude and longitude. */
data class Place(val name: String, val latitude: Double, val longitude: Double)

val Places = listOf(
    Place("Cairo", 30.0444, 31.2357), Place("Alexandria", 31.2001, 29.9187), Place("Giza", 30.0131, 31.2089), Place("Mansoura", 31.0409, 31.3785),
    Place("Makkah", 21.4225, 39.8262), Place("Madinah", 24.4672, 39.6111), Place("Riyadh", 24.7136, 46.6753), Place("Jeddah", 21.4858, 39.1925),
    Place("Dubai", 25.2048, 55.2708), Place("Amman", 31.9454, 35.9284), Place("Jerusalem", 31.7683, 35.2137), Place("Istanbul", 41.0082, 28.9784),
    Place("London", 51.5074, -0.1278), Place("New York", 40.7128, -74.0060),
)
