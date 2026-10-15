package dev.falah.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.falah.core.BackAction
import dev.falah.core.Navigation
import dev.falah.core.Section
import dev.falah.core.Str
import kotlinx.coroutines.launch

const val BOTTOM_BAR_TAG = "bottomBar"

/**
 * The frame around every screen: a menu (drawer) from the start edge, a swipeable pager over the shown sections,
 * the bottom bar, and Settings or About on top of the pager when opened from the menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppShell(state: FalahState, resumed: Int, hiddenNotes: Boolean, hideNotes: () -> Unit, showTour: () -> Unit, showWhatsNew: () -> Unit) {
    val sections = state.settings.sections
    val reduceMotion = state.settings.reduceMotion
    val pager = rememberPagerState(initialPage = Navigation.pageOf(sections, state.settings.startSection)) { sections.size }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var sub by remember { mutableStateOf<Tab?>(null) }

    // Keep the same section on screen when the user shows or hides sections in Settings.
    var shown by remember { mutableStateOf(sections) }
    LaunchedEffect(sections) {
        if (sections != shown) { val page = Navigation.pageAfterChange(shown, pager.currentPage, sections); shown = sections; pager.scrollToPage(page) }
    }

    val current = sections.getOrElse(pager.currentPage) { Section.Home }
    fun goTo(section: Section) {
        sub = null
        scope.launch {
            drawer.close()
            val page = sections.indexOf(section)
            if (page >= 0) { if (reduceMotion) pager.scrollToPage(page) else pager.animateScrollToPage(page) }
        }
    }
    fun open(tab: Tab) { tab.section?.let(::goTo) ?: run { sub = tab; scope.launch { drawer.close() } } }

    PlatformBackHandler(enabled = drawer.isOpen || sub != null || current != Section.Home) {
        when (Navigation.back(drawer.isOpen, sub != null, current)) {
            BackAction.CloseDrawer -> scope.launch { drawer.close() }
            BackAction.LeaveSubScreen -> sub = if (sub == Tab.About) Tab.Settings else null
            BackAction.GoHome -> goTo(Section.Home)
            BackAction.Exit -> Unit
        }
    }

    ModalNavigationDrawer(
        drawerContent = { MenuSheet(sections, current.takeIf { sub == null }, sub, ::open, { scope.launch { drawer.close() }; showWhatsNew() }, { scope.launch { drawer.close() }; showTour() }) },
        drawerState = drawer,
        // Swiping in from the edge opens the menu on the first page only; on other pages a swipe moves between sections.
        gesturesEnabled = drawer.isOpen || (pager.currentPage == 0 && sub == null),
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { TopBar(sub?.label ?: Tab.of(current).label, { scope.launch { drawer.open() } }) },
            bottomBar = {
                NavigationBar(Modifier.testTag(BOTTOM_BAR_TAG), containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    sections.forEach { sec ->
                        val t = Tab.of(sec)
                        NavigationBarItem(sub == null && current == sec, { goTo(sec) }, { Icon(t.icon, null) }, label = { Text(t.label.t(), maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary, indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)))
                    }
                }
            },
        ) { inner ->
            Column(Modifier.padding(inner)) {
                if (state.unreadable.isNotEmpty() || state.saveFailed) StorageBanner(state)
                Box(Modifier.weight(1f)) {
                    when (sub) {
                        Tab.Settings -> SettingsPage(state) { sub = Tab.About }
                        Tab.About -> AboutPage(state, showTour)
                        else -> HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1, key = { sections[it].key }) { page ->
                            SectionPage(sections[page], state, resumed, active = page == pager.currentPage, hiddenNotes, hideNotes, sections, ::open)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionPage(section: Section, state: FalahState, resumed: Int, active: Boolean, hiddenNotes: Boolean, hideNotes: () -> Unit, sections: List<Section>, open: (Tab) -> Unit) {
    when (section) {
        Section.Home -> HomePage(state, sections, open, hiddenNotes, hideNotes)
        Section.Times -> TimesTab(state)
        Section.Qibla -> CompassPage(state, resumed, active)
        Section.Tasbih -> TasbihPage(state)
        Section.Log -> LogPage(state)
    }
}

@Composable
private fun TopBar(title: Str, openMenu: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(openMenu, Modifier.size(48.dp)) { Icon(Icons.Rounded.Menu, Str.MenuOpen.t()) }
        Text(title.t(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StorageBanner(state: FalahState) {
    Text(
        (if (state.unreadable.isNotEmpty()) Str.DataUnreadable else Str.SaveFailed).t(),
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer).padding(12.dp),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer,
    )
}

@Composable
private fun MenuSheet(sections: List<Section>, current: Section?, sub: Tab?, open: (Tab) -> Unit, whatsNew: () -> Unit, tour: () -> Unit) {
    ModalDrawerSheet {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BrandMark(Modifier.size(56.dp))
            Text(Str.AppName.t(), style = MaterialTheme.typography.titleLarge)
            Text(Str.AboutTagline.t(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val colors = NavigationDrawerItemDefaults.colors()
        sections.forEach { sec ->
            val t = Tab.of(sec)
            NavigationDrawerItem({ Text(t.label.t()) }, current == sec, { open(t) }, Modifier.padding(NavigationDrawerItemDefaults.ItemPadding), icon = { Icon(t.icon, null) }, colors = colors)
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 24.dp))
        NavigationDrawerItem({ Text(Str.TabSettings.t()) }, sub == Tab.Settings, { open(Tab.Settings) }, Modifier.padding(NavigationDrawerItemDefaults.ItemPadding), icon = { Icon(Tab.Settings.icon, null) })
        NavigationDrawerItem({ Text(Str.TabAbout.t()) }, sub == Tab.About, { open(Tab.About) }, Modifier.padding(NavigationDrawerItemDefaults.ItemPadding), icon = { Icon(Tab.About.icon, null) })
        NavigationDrawerItem({ Text(Str.MenuWhatsNew.t()) }, false, whatsNew, Modifier.padding(NavigationDrawerItemDefaults.ItemPadding), icon = { Icon(Icons.Rounded.NewReleases, null) })
        NavigationDrawerItem({ Text(Str.ShowTour.t()) }, false, tour, Modifier.padding(NavigationDrawerItemDefaults.ItemPadding), icon = { Icon(Icons.Rounded.PlayCircle, null) })
    }
}
