package dev.falah.core

import kotlinx.datetime.LocalDate

data class Dhikr(val key: String, val label: String, val arabic: String, val target: Int)

val dhikrs = listOf(
    Dhikr("subhanallah", "SubhanAllah", "سبحان الله", 33),
    Dhikr("alhamdulillah", "Alhamdulillah", "الحمد لله", 33),
    Dhikr("allahuakbar", "Allahu Akbar", "الله أكبر", 34),
    Dhikr("istighfar", "Astaghfirullah", "أستغفر الله", 100),
    Dhikr("tahlil", "La ilaha illa Allah", "لا إله إلا الله", 100),
    Dhikr("salawat", "Salawat on the Prophet", "اللهم صل على محمد", 100),
)

/** Saved dhikr counts per day. Immutable; [add] returns a new log. */
data class TasbihLog(val counts: Map<Pair<LocalDate, String>, Int> = emptyMap()) {
    fun add(date: LocalDate, key: String, n: Int = 1): TasbihLog {
        val k = date to key
        val next = (counts[k] ?: 0) + n
        return TasbihLog(if (next <= 0) counts - k else counts + (k to next))
    }
    fun on(date: LocalDate, key: String): Int = counts[date to key] ?: 0
    fun dayTotal(date: LocalDate): Int = counts.filterKeys { it.first == date }.values.sum()
    fun total(key: String): Int = counts.filterKeys { it.second == key }.values.sum()
    fun grandTotal(): Int = counts.values.sum()

    /** One `date,key,count` line per day and dhikr, oldest first. */
    fun encode(): String = counts.entries.sortedWith(compareBy({ it.key.first }, { it.key.second })).joinToString("") { "${it.key.first},${it.key.second},${it.value}\n" }

    companion object {
        fun decode(text: String): TasbihLog {
            val map = mutableMapOf<Pair<LocalDate, String>, Int>()
            for (line in text.lineSequence()) {
                val p = line.trim().split(',')
                if (p.size != 3) continue
                val date = runCatching { LocalDate.parse(p[0]) }.getOrNull() ?: continue
                val n = p[2].toIntOrNull()?.takeIf { it > 0 } ?: continue
                if (dhikrs.none { it.key == p[1] }) continue
                map[date to p[1]] = n
            }
            return TasbihLog(map)
        }
    }
}
