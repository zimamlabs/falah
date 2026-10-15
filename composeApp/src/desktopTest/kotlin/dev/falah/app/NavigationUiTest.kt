package dev.falah.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class NavigationUiTest {
    private fun prefs(vararg extra: Pair<String, String>) = MapPrefs(mapOf("tour.done" to "1", "seen.version" to APP_VERSION, "place" to "Cairo", "lat" to "30.0444", "lon" to "31.2357") + extra)

    @Test fun swipingLeftMovesToTheNextSectionAndRightComesBack() = runComposeUiTest {
        setContent { FalahApp(prefs(), files = MemoryFiles(), showSplash = false) }
        onNodeWithContentDescription("Open menu").assertExists()
        selectedTab("Home")
        onRoot().performTouchInput { swipeLeft() }
        waitForIdle(); selectedTab("Times")
        onRoot().performTouchInput { swipeRight() }
        waitForIdle(); selectedTab("Home")
    }

    @Test fun tappingTheBottomBarJumpsToASection() = runComposeUiTest {
        setContent { FalahApp(prefs(), files = MemoryFiles(), showSplash = false) }
        onNode(hasText("Log") and hasAnyAncestor(hasTestTag(BOTTOM_BAR_TAG))).performClick(); waitForIdle(); selectedTab("Log")
    }

    @Test fun inArabicTheSwipesAreMirrored() = runComposeUiTest {
        setContent { FalahApp(prefs("lang" to "ar"), files = MemoryFiles(), showSplash = false) }
        selectedTab("الرئيسية")
        onRoot().performTouchInput { swipeRight() }
        waitForIdle(); selectedTab("المواقيت")
    }

    @Test fun theMenuOpensAndLeadsToSettingsAndBackToASection() = runComposeUiTest {
        setContent { FalahApp(prefs(), files = MemoryFiles(), showSplash = false) }
        onNodeWithContentDescription("Open menu").performClick(); waitForIdle()
        onNode(hasText("Settings") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab) and hasAnyAncestor(hasTestTag(BOTTOM_BAR_TAG)).not()).performClick(); waitForIdle()
        onNodeWithText("Appearance", substring = true).assertExists()
        onNode(hasText("Times") and hasAnyAncestor(hasTestTag(BOTTOM_BAR_TAG))).performClick(); waitForIdle(); selectedTab("Times")
    }

    @Test fun aHiddenSectionIsNotInTheBarOrThePager() = runComposeUiTest {
        setContent { FalahApp(prefs("sections" to "home,times,log"), files = MemoryFiles(), showSplash = false) }
        onNode(hasText("Qibla") and hasAnyAncestor(hasTestTag(BOTTOM_BAR_TAG))).assertDoesNotExist()
        onNode(hasText("Tasbih") and hasAnyAncestor(hasTestTag(BOTTOM_BAR_TAG))).assertDoesNotExist()
        onNode(hasText("Log") and hasAnyAncestor(hasTestTag(BOTTOM_BAR_TAG))).assertExists()
    }

    @Test fun theStartSectionIsHonoured() = runComposeUiTest {
        setContent { FalahApp(prefs("start" to "tasbih"), files = MemoryFiles(), showSplash = false) }
        selectedTab("Tasbih")
    }

    private val tab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    @Test fun theMenuFollowsTheSharedSpecOrder() = runComposeUiTest {
        setContent { FalahApp(prefs(), files = MemoryFiles(), showSplash = false) }
        onNodeWithContentDescription("Open menu").performClick(); waitForIdle()
        fun top(text: String) = onNode(hasText(text) and tab and hasAnyAncestor(hasTestTag(BOTTOM_BAR_TAG)).not()).fetchSemanticsNode().boundsInRoot.top
        val order = listOf("Home", "Times", "Qibla", "Tasbih", "Log", "Settings", "What's new", "Show the tour again", "About").map { top(it) }
        check(order == order.sorted()) { "menu order was $order" }
        onNodeWithText("by Zimam Labs").assertExists(); onNodeWithText("Zimam Labs").assertExists()
    }

    @Test fun settingsIsAListOfLabelledSectionsInTheSharedOrder() = runComposeUiTest {
        setContent { FalahApp(prefs(), files = MemoryFiles(), showSplash = false) }
        onNodeWithContentDescription("Open menu").performClick(); waitForIdle()
        onNode(hasText("Settings") and tab and hasAnyAncestor(hasTestTag(BOTTOM_BAR_TAG)).not()).performClick(); waitForIdle()
        fun top(text: String) = onNode(hasText(text)).fetchSemanticsNode().boundsInRoot.top
        val order = listOf("Appearance", "Sections and layout", "Language", "Notifications and reminders", "Calculation", "Your data", "Privacy and security", "About Falah").map { top(it) }
        check(order == order.sorted()) { "settings order was $order" }
        onNodeWithText("Appearance").performClick(); waitForIdle()
        onNodeWithText("Colour theme").assertExists()
    }

    private fun androidx.compose.ui.test.ComposeUiTest.selectedTab(name: String) { onNode(isSelected() and hasText(name) and hasAnyAncestor(hasTestTag(BOTTOM_BAR_TAG))).assertExists("$name should be the selected tab") }
}
