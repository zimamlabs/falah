package dev.falah.core

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** The five prayers (sunrise is a time, not a prayer you log). */
val loggedPrayers: List<Prayer> = Prayer.entries.filter { it != Prayer.Sunrise }

/** Which prayers were prayed on which days. Immutable; [mark] returns a new log. */
data class PrayerLog(val days: Map<LocalDate, Set<Prayer>> = emptyMap()) {
    fun done(date: LocalDate): Set<Prayer> = days[date].orEmpty()
    fun count(date: LocalDate): Int = done(date).size
    fun isFull(date: LocalDate): Boolean = count(date) == loggedPrayers.size

    fun mark(date: LocalDate, prayer: Prayer, prayed: Boolean): PrayerLog {
        if (prayer == Prayer.Sunrise) return this
        val next = if (prayed) done(date) + prayer else done(date) - prayer
        return PrayerLog(if (next.isEmpty()) days - date else days + (date to next))
    }

    /** The last [length] days ending on [today], oldest first, each with how many prayers were prayed. */
    fun week(today: LocalDate, length: Int = 7): List<Pair<LocalDate, Int>> =
        (length - 1 downTo 0).map { back -> today.minus(back, DateTimeUnit.DAY).let { it to count(it) } }

    /** The last [length] days ending on [today], newest first, with whether [prayer] was prayed on each. */
    fun history(prayer: Prayer, today: LocalDate, length: Int = 30): List<Pair<LocalDate, Boolean>> =
        (0 until length).map { back -> today.minus(back, DateTimeUnit.DAY).let { it to (prayer in done(it)) } }

    /** Days in a row, ending today if prayed today, otherwise yesterday, on which [prayer] was prayed. */
    fun streakOf(prayer: Prayer, today: LocalDate): Int {
        var day = if (prayer in done(today)) today else today.minus(1, DateTimeUnit.DAY)
        var n = 0
        while (prayer in done(day)) { n++; day = day.minus(1, DateTimeUnit.DAY) }
        return n
    }

    /** Days in a row with all five prayed, ending today if today is complete, otherwise yesterday (today is still open). */
    fun streak(today: LocalDate): Int {
        var day = if (isFull(today)) today else today.minus(1, DateTimeUnit.DAY)
        var n = 0
        while (isFull(day)) { n++; day = day.minus(1, DateTimeUnit.DAY) }
        return n
    }

    /** Longest run of full days anywhere in the log. */
    fun bestStreak(): Int {
        var best = 0
        for (start in days.keys.filter { isFull(it) }.sorted()) {
            if (isFull(start.minus(1, DateTimeUnit.DAY))) continue
            var n = 0; var d = start
            while (isFull(d)) { n++; d = d.plus(1, DateTimeUnit.DAY) }
            if (n > best) best = n
        }
        return best
    }

    /** Prayers prayed and prayers possible in a month, counting only days up to [today]. */
    fun monthTotals(year: Int, month: Int, today: LocalDate): Pair<Int, Int> {
        var prayed = 0; var possible = 0
        var d = LocalDate(year, month, 1)
        while (d.monthNumber == month && d <= today) { prayed += count(d); possible += loggedPrayers.size; d = d.plus(1, DateTimeUnit.DAY) }
        return prayed to possible
    }

    /** One `date,Prayer` line per prayed prayer, oldest first. */
    fun encode(): String = days.toSortedMap().entries.joinToString("") { (d, set) -> loggedPrayers.filter { it in set }.joinToString("") { "$d,${it.name}\n" } }

    companion object {
        fun decode(text: String): PrayerLog {
            val map = mutableMapOf<LocalDate, Set<Prayer>>()
            for (line in text.lineSequence()) {
                val parts = line.trim().split(',')
                if (parts.size != 2) continue
                val date = runCatching { LocalDate.parse(parts[0]) }.getOrNull() ?: continue
                val prayer = loggedPrayers.firstOrNull { it.name == parts[1] } ?: continue
                map[date] = map[date].orEmpty() + prayer
            }
            return PrayerLog(map)
        }
    }
}
