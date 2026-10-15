package dev.falah.app

/** A place. [Places] are the built-in choices; any other latitude and longitude is typed in through the place menu. */
data class Place(val name: String, val latitude: Double, val longitude: Double, val arabic: String = name)

val Places = listOf(
    Place("Cairo", 30.0444, 31.2357, "القاهرة"), Place("Alexandria", 31.2001, 29.9187, "الإسكندرية"), Place("Giza", 30.0131, 31.2089, "الجيزة"), Place("Mansoura", 31.0409, 31.3785, "المنصورة"),
    Place("Makkah", 21.4225, 39.8262, "مكة المكرمة"), Place("Madinah", 24.4672, 39.6111, "المدينة المنورة"), Place("Riyadh", 24.7136, 46.6753, "الرياض"), Place("Jeddah", 21.4858, 39.1925, "جدة"),
    Place("Dubai", 25.2048, 55.2708, "دبي"), Place("Amman", 31.9454, 35.9284, "عمّان"), Place("Jerusalem", 31.7683, 35.2137, "القدس"), Place("Istanbul", 41.0082, 28.9784, "إسطنبول"),
    Place("London", 51.5074, -0.1278, "لندن"), Place("New York", 40.7128, -74.0060, "نيويورك"),
)
