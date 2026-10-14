package dev.falah.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.DateTimeUnit

data class HijriDate(val year: Int, val month: Int, val day: Int) {
    val monthName: String get() = Hijri.monthNames[month - 1]
    fun monthTitle(lang: Lang): String = if (lang == Lang.Ar) monthArabic else monthName
    val monthArabic: String get() = Hijri.monthNamesArabic[month - 1]
}

/**
 * The tabular (arithmetic) Islamic calendar. The moon-sighting calendars each country announces can differ by a day,
 * so [of] takes an adjustment of a day or two that the user can set.
 */
object Hijri {
    val monthNames = listOf("Muharram", "Safar", "Rabi' al-awwal", "Rabi' al-thani", "Jumada al-ula", "Jumada al-akhira", "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhu al-qi'da", "Dhu al-hijja")
    val monthNamesArabic = listOf("محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة")

    /** Julian day number of 1 Muharram 1 AH (civil epoch). */
    private const val EPOCH = 1948440L

    private fun jdn(date: LocalDate): Long = date.toEpochDays() + 2440588L

    fun of(date: LocalDate, adjustDays: Int = 0): HijriDate {
        val shifted = if (adjustDays == 0) date else date.plus(adjustDays, DateTimeUnit.DAY)
        var l = jdn(shifted) - EPOCH + 10632
        val n = (l - 1) / 10631
        l = l - 10631 * n + 354
        val j = ((10985 - l) / 5316) * ((50 * l) / 17719) + (l / 5670) * ((43 * l) / 15238)
        l = l - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
        val m = (24 * l) / 709
        val d = l - (709 * m) / 24
        val y = 30 * n + j - 30
        return HijriDate(y.toInt(), m.toInt(), d.toInt())
    }
}
