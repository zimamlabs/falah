package dev.falah.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OnboardingTest {
    @Test fun aFreshInstallGetsTheTour() = assertEquals(Welcome.Tour, Onboarding.decide("", false, false, "0.3.0"))
    @Test fun theSameVersionShowsNothing() = assertEquals(Welcome.Nothing, Onboarding.decide("0.3.0", true, true, "0.3.0"))
    @Test fun anUpdateShowsTheNotesOnce() = assertEquals(Welcome.WhatsNew, Onboarding.decide("0.2.2", true, true, "0.3.0"))
    @Test fun aDowngradeShowsNothing() = assertEquals(Welcome.Nothing, Onboarding.decide("0.3.0", true, true, "0.2.2"))
    @Test fun anUpdateFromAVersionThatStoredNothingShowsTheNotes() = assertEquals(Welcome.WhatsNew, Onboarding.decide("", false, true, "0.3.0"))
    @Test fun versionsCompareNumerically() { assertTrue(Onboarding.compare("0.10.0", "0.9.9") > 0); assertEquals(0, Onboarding.compare("1.0", "1.0.0")) }
    @Test fun notesFallBackToTheNewestOlderVersion() { assertTrue(ReleaseNotes.forVersion("0.3.0").isNotEmpty()); assertEquals(ReleaseNotes.forVersion("0.2.2"), ReleaseNotes.forVersion("0.2.5")) }
    @Test fun theNewIconNoteHasBothLanguages() {
        assertEquals(listOf("A new app icon."), ReleaseNotes.forVersion("0.5.1", Lang.En))
        assertTrue(ReleaseNotes.forVersion("0.5.1", Lang.Ar).single().contains("أيقونة"))
    }
    @Test fun settingsRoundTripWithOddCharacters() {
        val m = mapOf("place" to "New York", "note" to "a=b\nc\\d", "notify.Fajr" to "1")
        assertEquals(m, SettingsFile.decode(SettingsFile.encode(m)))
    }
}
