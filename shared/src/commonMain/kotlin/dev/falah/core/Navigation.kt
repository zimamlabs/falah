package dev.falah.core

/** What the back button or gesture does, in order: close the menu, leave Settings or About, return to Home, then leave the app. */
enum class BackAction { CloseDrawer, LeaveSettingsSection, LeaveSubScreen, GoHome, Exit }

/** The pages of Settings, as a list of labelled rows. */
enum class SettingsSection { Appearance, Layout, Language, Notifications, Calculation, Data, Privacy }

object Navigation {
    fun back(drawerOpen: Boolean, inSubScreen: Boolean, current: Section, inSettingsSection: Boolean = false): BackAction = when {
        drawerOpen -> BackAction.CloseDrawer
        inSettingsSection -> BackAction.LeaveSettingsSection
        inSubScreen -> BackAction.LeaveSubScreen
        current != Section.Home -> BackAction.GoHome
        else -> BackAction.Exit
    }

    /** The pager page of [target] among the shown [sections], or the first page when it is not shown. */
    fun pageOf(sections: List<Section>, target: Section): Int = sections.indexOf(target).coerceAtLeast(0)

    /** The page to stay on after the shown sections changed: the same section if still shown, otherwise the first page. */
    fun pageAfterChange(old: List<Section>, page: Int, new: List<Section>): Int = old.getOrNull(page)?.let { pageOf(new, it) } ?: 0
}
