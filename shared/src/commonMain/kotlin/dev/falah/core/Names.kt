package dev.falah.core

/** The name of a prayer in [lang]: Arabic in the Arabic interface, the transliteration in English. */
fun Prayer.title(lang: Lang): String = if (lang == Lang.Ar) arabic else label

fun Method.title(lang: Lang): String = when (this) {
    Method.Egyptian -> Str.MethodEgyptian
    Method.MuslimWorldLeague -> Str.MethodMwl
    Method.UmmAlQura -> Str.MethodUmmAlQura
    Method.Karachi -> Str.MethodKarachi
    Method.NorthAmerica -> Str.MethodNorthAmerica
}.text(lang)

fun AsrFactor.title(lang: Lang): String = (if (this == AsrFactor.Hanafi) Str.AsrHanafi else Str.AsrStandard).text(lang)

fun Dhikr.title(lang: Lang): String = if (lang == Lang.Ar) arabic else label

/** "Monday, 14 October 2026" or "الاثنين، 14 أكتوبر 2026". */
fun kotlinx.datetime.LocalDate.longText(lang: Lang): String =
    if (lang == Lang.Ar) "${weekdaysAr[dayOfWeek.ordinal]}، $dayOfMonth ${gregorianMonthsAr[monthNumber - 1]} $year"
    else "${weekdaysEn[dayOfWeek.ordinal]}, $dayOfMonth ${gregorianMonthsEn[monthNumber - 1]} $year"

fun kotlinx.datetime.LocalDate.weekdayName(lang: Lang): String = (if (lang == Lang.Ar) weekdaysAr else weekdaysEn)[dayOfWeek.ordinal]

fun greetingStr(hour: Int): Str = when { hour < 12 -> Str.GreetMorning; hour < 17 -> Str.GreetAfternoon; else -> Str.GreetEvening }

fun monthTitle(month: Int, lang: Lang): String = (if (lang == Lang.Ar) gregorianMonthsAr else gregorianMonthsEn)[month - 1]
