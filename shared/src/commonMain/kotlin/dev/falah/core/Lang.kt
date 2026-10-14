package dev.falah.core

/** The language the screens are shown in. */
enum class Lang { En, Ar }

object I18n {
    private const val ARABIC_INDIC = "٠١٢٣٤٥٦٧٨٩"

    /** [choice] is the saved setting ("en", "ar", anything else means follow the system); [systemLanguage] is the device's language code. */
    fun resolve(choice: String?, systemLanguage: String): Lang = when (choice) {
        "en" -> Lang.En
        "ar" -> Lang.Ar
        else -> if (systemLanguage.lowercase().startsWith("ar")) Lang.Ar else Lang.En
    }

    /** Western digits become Arabic-Indic digits when [arabic] is true; everything else is left alone. */
    fun digits(text: String, arabic: Boolean): String =
        if (!arabic) text else buildString(text.length) { for (c in text) append(if (c in '0'..'9') ARABIC_INDIC[c - '0'] else c) }

    /** Replaces {0}, {1}, ... with [args]. */
    fun fill(template: String, args: Array<out Any?>): String {
        var out = template
        args.forEachIndexed { i, a -> out = out.replace("{$i}", a.toString()) }
        return out
    }
}

val gregorianMonthsEn = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
val gregorianMonthsAr = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
val weekdaysEn = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
val weekdaysAr = listOf("الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد")
/** One-letter weekday headings, Monday first. */
val weekdayLettersEn = listOf("M", "T", "W", "T", "F", "S", "S")
val weekdayLettersAr = listOf("ن", "ث", "ر", "خ", "ج", "س", "ح")
