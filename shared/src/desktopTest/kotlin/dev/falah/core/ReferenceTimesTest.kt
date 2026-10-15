package dev.falah.core

import kotlinx.datetime.LocalDate
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * F-02: compares the app's times with `reference-times.csv`.
 *
 * The CSV is an INDEPENDENT CROSS-CHECK, not a published table: a separate implementation of the NOAA/Meeus solar
 * position (apparent longitude, aberration, full equation of time) with each event solved at its own instant.
 * It proves the app's low-precision sun model stays close to a better one; it does not prove agreement with any authority.
 */
class ReferenceTimesTest {
    private class Row(val city: String, val date: LocalDate, val lat: Double, val lon: Double, val offset: Double, val method: Method, val minutes: Map<Prayer, Double?>)

    private fun rows(): List<Row> {
        val text = ReferenceTimesTest::class.java.getResourceAsStream("/reference-times.csv")!!.bufferedReader().readText()
        return text.lines().filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("city,") }.map { line ->
            val c = line.split(",")
            fun v(i: Int) = c[i].toDoubleOrNull()
            Row(c[0], LocalDate.parse(c[1]), c[2].toDouble(), c[3].toDouble(), c[4].toDouble(), Method.valueOf(c[5]),
                mapOf(Prayer.Fajr to v(6), Prayer.Sunrise to v(7), Prayer.Dhuhr to v(8), Prayer.Asr to v(9), Prayer.Maghrib to v(10), Prayer.Isha to v(11)))
        }
    }

    /** The largest difference, in minutes, per prayer, over every row where the reference has a value. */
    private fun errors(): Map<Prayer, Pair<Double, String>> {
        val worst = mutableMapOf<Prayer, Pair<Double, String>>()
        for (r in rows()) {
            val t = PrayerTimes.compute(r.date, r.lat, r.lon, r.offset, r.method)
            for ((p, ref) in r.minutes) {
                if (ref == null) continue
                val mine = t.of(p) ?: error("${r.city} ${r.date} $p: the app has no time, the reference has ${ref / 60}")
                val diff = abs(mine * 60 - ref)
                if (diff > (worst[p]?.first ?: -1.0)) worst[p] = diff to "${r.city} ${r.date}"
            }
        }
        return worst
    }

    @Test fun measuredErrorStaysInsideTheDocumentedTolerance() {
        val worst = errors()
        println("F-02 measured error (minutes, worst row):")
        worst.forEach { (p, w) -> println("  %-8s %.2f  %s".format(p, w.first, w.second)) }
        worst.forEach { (p, w) -> assertTrue(w.first <= TOLERANCE_MINUTES, "$p is ${w.first} minutes off in ${w.second}") }
    }

    @Test fun whereTheReferenceHasNoFajrOrIshaTheAppHasNoneEither() {
        val london = rows().single { it.city == "London" && it.date.monthNumber == 6 }
        val t = PrayerTimes.compute(london.date, london.lat, london.lon, london.offset, london.method)
        assertTrue(london.minutes[Prayer.Fajr] == null && t.fajr == null && t.isha == null)
    }

    private companion object { const val TOLERANCE_MINUTES = 2.5 // measured worst case 2.09 (Isha, London 2026-09-23); tighten when the algorithm improves }
}
