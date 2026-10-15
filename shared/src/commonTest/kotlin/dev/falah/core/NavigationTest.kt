package dev.falah.core

import kotlin.test.Test
import kotlin.test.assertEquals

class NavigationTest {
    @Test fun backClosesTheMenuFirst() = assertEquals(BackAction.CloseDrawer, Navigation.back(true, true, Section.Log))
    @Test fun aSettingsPageGoesBackToTheSettingsList() = assertEquals(BackAction.LeaveSettingsSection, Navigation.back(false, true, Section.Log, inSettingsSection = true))
    @Test fun theMenuStillComesBeforeASettingsPage() = assertEquals(BackAction.CloseDrawer, Navigation.back(true, true, Section.Log, inSettingsSection = true))
    @Test fun thenLeavesSettingsOrAbout() = assertEquals(BackAction.LeaveSubScreen, Navigation.back(false, true, Section.Log))
    @Test fun thenReturnsToHome() = assertEquals(BackAction.GoHome, Navigation.back(false, false, Section.Tasbih))
    @Test fun thenExits() = assertEquals(BackAction.Exit, Navigation.back(false, false, Section.Home))

    private val all = Section.entries
    @Test fun pageOfFindsTheSectionAmongTheShownOnes() = assertEquals(2, Navigation.pageOf(listOf(Section.Home, Section.Times, Section.Log), Section.Log))
    @Test fun aHiddenSectionFallsBackToTheFirstPage() = assertEquals(0, Navigation.pageOf(listOf(Section.Home, Section.Times), Section.Log))
    @Test fun hidingASectionBeforeTheCurrentOneKeepsYouOnIt() = assertEquals(1, Navigation.pageAfterChange(all, 3, listOf(Section.Home, Section.Tasbih, Section.Log)))
    @Test fun hidingTheCurrentSectionGoesHome() = assertEquals(0, Navigation.pageAfterChange(all, 3, listOf(Section.Home, Section.Times, Section.Log)))
}
