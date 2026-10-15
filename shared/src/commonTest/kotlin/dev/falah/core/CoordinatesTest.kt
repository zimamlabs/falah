package dev.falah.core

import kotlin.test.Test
import kotlin.test.assertEquals

class CoordinatesTest {
    private fun ok(lat: Double, lon: Double) = CoordinateResult.Ok(lat, lon)
    private fun bad(e: CoordinateError) = CoordinateResult.Bad(e)

    @Test fun plainDecimals() = assertEquals(ok(30.0444, 31.2357), Coordinates.parse("30.0444", "31.2357"))
    @Test fun negativeValuesAndSigns() = assertEquals(ok(-33.87, -74.0), Coordinates.parse(" -33.87 ", "−74"))
    @Test fun commaAsTheDecimalMark() = assertEquals(ok(21.4225, 39.8262), Coordinates.parse("21,4225", "39,8262"))
    @Test fun arabicIndicDigitsAndMark() = assertEquals(ok(30.5, 31.25), Coordinates.parse("٣٠٫٥", "۳۱٫۲۵"))
    @Test fun degreeSignIsIgnored() = assertEquals(ok(51.5, -0.12), Coordinates.parse("51.5°", "-0.12°"))
    @Test fun theLimitsAreAccepted() = assertEquals(ok(90.0, -180.0), Coordinates.parse("90", "-180"))
    @Test fun latitudeBeyondNinetyIsRejected() = assertEquals(bad(CoordinateError.LatitudeRange), Coordinates.parse("90.01", "10"))
    @Test fun longitudeBeyondOneEightyIsRejected() = assertEquals(bad(CoordinateError.LongitudeRange), Coordinates.parse("10", "180.5"))
    @Test fun blankIsRejected() = assertEquals(bad(CoordinateError.Empty), Coordinates.parse("", "10"))
    @Test fun wordsAreRejected() { assertEquals(bad(CoordinateError.NotANumber), Coordinates.parse("abc", "10")); assertEquals(bad(CoordinateError.NotANumber), Coordinates.parse("NaN", "10")); assertEquals(bad(CoordinateError.NotANumber), Coordinates.parse("1e2", "10")) }
    @Test fun twoMarksAreRejected() = assertEquals(bad(CoordinateError.NotANumber), Coordinates.parse("1.2.3", "10"))
    @Test fun aMinusInTheMiddleIsRejected() = assertEquals(bad(CoordinateError.NotANumber), Coordinates.parse("10-5", "10"))
}
