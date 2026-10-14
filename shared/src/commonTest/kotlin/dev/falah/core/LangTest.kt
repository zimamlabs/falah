package dev.falah.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LangTest {
    @Test fun systemFollowsTheDeviceLanguage() {
        assertEquals(Lang.Ar, I18n.resolve("system", "ar"))
        assertEquals(Lang.Ar, I18n.resolve(null, "ar-EG"))
        assertEquals(Lang.En, I18n.resolve(null, "fr"))
    }

    @Test fun anExplicitChoiceWinsOverTheDevice() {
        assertEquals(Lang.En, I18n.resolve("en", "ar"))
        assertEquals(Lang.Ar, I18n.resolve("ar", "en"))
    }

    @Test fun digitsBecomeArabicIndicOnlyWhenAsked() {
        assertEquals("٠٥:١٢ 2026".replace("2026", "٢٠٢٦"), I18n.digits("05:12 2026", true))
        assertEquals("05:12", I18n.digits("05:12", false))
        assertEquals("abc ٣٤° N", I18n.digits("abc 34° N", true))
    }

    @Test fun placeholdersAreFilledInOrder() {
        assertEquals("3 of 5", I18n.fill("{0} of {1}", arrayOf(3, 5)))
        assertEquals("a-a", I18n.fill("{0}-{0}", arrayOf("a")))
    }

    @Test fun everyStringHasTheSamePlaceholdersInBothLanguages() {
        val slot = Regex("\\{\\d+\\}")
        for (s in Str.entries) {
            assertTrue(s.en.isNotBlank() && s.ar.isNotBlank(), "${s.name} is empty")
            if (s == Str.NotifTitle) continue // the Arabic title shows only the Arabic name, on purpose
            assertEquals(slot.findAll(s.en).map { it.value }.toSortedSet(), slot.findAll(s.ar).map { it.value }.toSortedSet(), "${s.name} placeholders differ")
        }
    }

    @Test fun arabicTextReallyIsArabic() {
        val arabic = Regex("[\\u0600-\\u06FF]")
        for (s in Str.entries) if (s != Str.LangEnglish && s != Str.DigitsWestern && s != Str.HijriShort) assertTrue(arabic.containsMatchIn(s.ar) || s.ar.all { !it.isLetter() } , "${s.name} has no Arabic")
    }

    @Test fun datesFormatInBothLanguages() {
        val d = kotlinx.datetime.LocalDate(2026, 10, 14)
        assertEquals("Wednesday, 14 October 2026", d.longText(Lang.En))
        assertEquals("الأربعاء، 14 أكتوبر 2026", d.longText(Lang.Ar))
    }

    @Test fun theArabicWeekStartsOnSaturday() {
        val cells = MonthGrid.cells(2026, 10, kotlinx.datetime.DayOfWeek.SATURDAY)
        // 1 October 2026 is a Thursday: five days after Saturday
        assertEquals(5, cells.indexOfFirst { it != null })
        assertEquals(0, cells.size % 7)
    }

    @Test fun releaseNotesComeInBothLanguages() {
        assertTrue(ReleaseNotes.forVersion("0.5.0", Lang.Ar).first().contains("عربية"))
        assertTrue(ReleaseNotes.forVersion("0.5.0", Lang.En).first().startsWith("An Arabic"))
        assertEquals(ReleaseNotes.forVersion("0.4.1", Lang.En).size, ReleaseNotes.forVersion("0.4.1", Lang.Ar).size)
    }

    @Test fun everyPrayerMethodAndHijriMonthHasAnArabicName() {
        Prayer.entries.forEach { assertEquals(it.arabic, it.title(Lang.Ar)); assertEquals(it.label, it.title(Lang.En)) }
        Method.entries.forEach { assertTrue(it.title(Lang.Ar).isNotBlank() && it.title(Lang.En) != it.title(Lang.Ar)) }
        assertEquals(12, Hijri.monthNamesArabic.size)
        assertEquals("رمضان", HijriDate(1448, 9, 1).monthTitle(Lang.Ar))
    }
}
