package dev.falah.core

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant

data class TimetableRow(val date: LocalDate, val times: DayTimes)

object Timetable {
    /** Every day of the month with its prayer times; the time-zone offset is taken for each day, so a clock change mid-month is followed. */
    fun month(year: Int, month: Int, zone: TimeZone, latitude: Double, longitude: Double, method: Method, asr: AsrFactor): List<TimetableRow> {
        val rows = mutableListOf<TimetableRow>()
        var d = LocalDate(year, month, 1)
        while (d.monthNumber == month) {
            val offset = zone.offsetAt(LocalDateTime(d, LocalTime(12, 0)).toInstant(zone)).totalSeconds / 3600.0
            rows += TimetableRow(d, PrayerTimes.compute(d, latitude, longitude, offset, method, asr))
            d = d.plus(1, DateTimeUnit.DAY)
        }
        return rows
    }
}

object MonthGrid {
    /** The days of a month laid out in weeks (null pads before the 1st and after the last day), weeks starting Monday or Sunday. */
    fun cells(year: Int, month: Int, weekStartsMonday: Boolean = true): List<LocalDate?> {
        val first = LocalDate(year, month, 1)
        val lead = if (weekStartsMonday) first.dayOfWeek.ordinal else (first.dayOfWeek.ordinal + 1) % 7
        val cells = MutableList<LocalDate?>(lead) { null }
        var d = first
        while (d.monthNumber == month) { cells += d; d = d.plus(1, DateTimeUnit.DAY) }
        while (cells.size % 7 != 0) cells += null
        return cells
    }

    fun previous(year: Int, month: Int): Pair<Int, Int> = if (month == 1) year - 1 to 12 else year to month - 1
    fun next(year: Int, month: Int): Pair<Int, Int> = if (month == 12) year + 1 to 1 else year to month + 1
}
