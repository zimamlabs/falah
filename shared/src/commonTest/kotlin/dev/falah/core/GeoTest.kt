package dev.falah.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GeoTest {
    private val cities = listOf(Triple("Cairo", 30.0444, 31.2357), Triple("Giza", 30.0131, 31.2089), Triple("London", 51.5074, -0.1278))

    @Test fun distanceCairoToLondonIsAbout3500Km() = assertEquals(3500.0, Geo.distanceKm(30.0444, 31.2357, 51.5074, -0.1278), 60.0)
    @Test fun distanceToItselfIsZero() = assertEquals(0.0, Geo.distanceKm(10.0, 20.0, 10.0, 20.0), 1e-9)
    @Test fun theNearestCityWithinRangeIsNamed() = assertEquals("Giza", Geo.nearest(30.02, 31.20, cities, 25.0))
    @Test fun nothingNearEnoughGivesNull() = assertNull(Geo.nearest(40.0, 60.0, cities, 25.0))
    @Test fun invalidFixesAreRejected() { assertFalse(Geo.valid(91.0, 0.0)); assertFalse(Geo.valid(0.0, 0.0)); assertFalse(Geo.valid(10.0, 181.0)); assertTrue(Geo.valid(21.4, 39.8)) }
    @Test fun placeSourceRoundTripsAndDefaultsToManual() {
        assertEquals(PlaceSource.Manual, AppSettings().placeSource)
        val s = AppSettings(placeName = "My location", latitude = 1.5, longitude = 2.5, placeChosen = true, placeSource = PlaceSource.Gps)
        assertEquals(s, SettingsCodec.decode(SettingsCodec.encode(s)))
        assertEquals(PlaceSource.Manual, SettingsCodec.decode(mapOf("place" to "Cairo")).placeSource)
    }
}
