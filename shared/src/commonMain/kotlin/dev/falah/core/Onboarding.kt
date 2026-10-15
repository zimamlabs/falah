package dev.falah.core

/** What to show when Falah opens: the first-install tour, the one-time notes after an update, or nothing. */
enum class Welcome { Tour, WhatsNew, Nothing }

object Onboarding {
    /** `0.3.0` > `0.2.2`; missing or non-numeric parts count as 0. */
    fun compare(a: String, b: String): Int {
        val x = a.split('.').map { it.toIntOrNull() ?: 0 }
        val y = b.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) {
            val d = x.getOrElse(i) { 0 }.compareTo(y.getOrElse(i) { 0 })
            if (d != 0) return d
        }
        return 0
    }

    /**
     * [lastSeen] is the version whose notes were closed last (empty when never), [tourDone] whether the tour was
     * finished or skipped, [hasData] whether settings already exist (an update from a version that did not store
     * these two values), [current] the installed version.
     */
    fun decide(lastSeen: String, tourDone: Boolean, hasData: Boolean, current: String): Welcome = when {
        !tourDone && lastSeen.isEmpty() && !hasData -> Welcome.Tour
        lastSeen.isNotEmpty() && compare(current, lastSeen) <= 0 -> Welcome.Nothing // same version, or a downgrade
        else -> Welcome.WhatsNew
    }
}

/** Short notes per version, taken from the changelog; shown once after an update. Each note is English then Arabic. */
object ReleaseNotes {
    private val notes: Map<String, List<Pair<String, String>>> = mapOf(
        "0.5.1" to listOf("A new app icon." to "أيقونة جديدة للتطبيق."),
        "0.5.0" to listOf(
            "An Arabic interface with a right-to-left layout. Choose the language and the digits in Settings." to "واجهة عربية بتخطيط من اليمين إلى اليسار. اختر اللغة والأرقام من الإعدادات.",
        ),
        "0.4.1" to listOf(
            "A new look that follows the icon: warm dark or ivory, orange accent, glow and rounded cards." to "مظهر جديد يتبع الأيقونة: داكن دافئ أو عاجي، ولمسة برتقالية، وتوهّج، وبطاقات مستديرة.",
            "An About page with the tour, what's new, privacy and links." to "صفحة «حول» فيها الجولة والجديد والخصوصية والروابط.",
        ),
        "0.4.0" to listOf(
            "A prayer log with a month calendar, and a monthly timetable." to "سجل للصلاة بتقويم شهري، وجدول مواقيت شهري.",
            "A tasbih counter that saves your totals." to "عدّاد تسبيح يحفظ مجاميعك.",
            "Choose to be notified a few minutes before a prayer, and a live compass for the qibla." to "اختر التنبيه قبل الصلاة بدقائق، وبوصلة مباشرة للقبلة.",
        ),
        "0.3.0" to listOf(
            "A Home page with the next prayer, today's times and shortcuts." to "صفحة رئيسية فيها الصلاة القادمة ومواقيت اليوم واختصارات.",
            "A first-run tour, and these notes after each update." to "جولة تعريفية عند أول تشغيل، وهذه الملاحظات بعد كل تحديث.",
            "Settings can live in one Zimam folder (Zimam/Falah). Copies are checked and nothing is ever deleted." to "يمكن حفظ الإعدادات في مجلد زمام (Zimam/Falah). تُفحص النسخ ولا يُحذف شيء أبدًا.",
        ),
        "0.2.2" to listOf("A better opening screen." to "شاشة افتتاحية أفضل."),
        "0.2.1" to listOf("A new icon." to "أيقونة جديدة."),
        "0.2.0" to listOf("A notification at each prayer you pick." to "إشعار عند كل صلاة تختارها."),
    )

    /** The notes for [version], or for the newest older version that has some, in [lang]. */
    fun forVersion(version: String, lang: Lang = Lang.En): List<String> =
        (notes[version] ?: notes.keys.filter { Onboarding.compare(it, version) <= 0 }.maxWithOrNull { a, b -> Onboarding.compare(a, b) }?.let { notes[it] }.orEmpty())
            .map { if (lang == Lang.Ar) it.second else it.first }
}

/** The settings mirror kept in the Zimam folder: one `key=value` per line, newline and backslash escaped. */
object SettingsFile {
    private fun esc(s: String) = s.replace("\\", "\\\\").replace("\n", "\\n")
    private fun unesc(s: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) { out.append(if (s[i + 1] == 'n') '\n' else s[i + 1]); i += 2 } else { out.append(c); i++ }
        }
        return out.toString()
    }

    fun encode(values: Map<String, String>): String = values.toSortedMap().entries.joinToString("") { "${esc(it.key)}=${esc(it.value)}\n" }

    fun decode(text: String): Map<String, String> = text.lineSequence().mapNotNull { line ->
        val at = line.indexOf('=')
        if (at <= 0) null else unesc(line.substring(0, at)) to unesc(line.substring(at + 1))
    }.toMap()
}
