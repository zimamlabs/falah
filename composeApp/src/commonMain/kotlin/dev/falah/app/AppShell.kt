package dev.falah.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
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
import androidx.compose.runtime.CompositionLocalProvider
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
import dev.falah.core.SettingsSection
import dev.falah.core.Str
import kotlinx.coroutines.launch

const val BOTTOM_BAR_TAG = "bottomBar"

/**
 * The frame around every screen: a menu (drawer) from the start edge, a swipeable pager over the shown sections,
 * the bottom bar, and Settings or About on top of the pager when opened from the menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppShell(state: FalahState, resumed: Int, hiddenNotes: Boolean, hideNotes: () -> Unit, showTour: () -> Unit, showWhatsNew: () -> Unit, jumpTo: Section? = null, jumped: () -> Unit = {}) {
    val sections = state.settings.sections
    val reduceMotion = state.settings.reduceMotion
    val pager = rememberPagerState(initialPage = Navigation.pageOf(sections, state.settings.startSection)) { sections.size }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var sub by remember { mutableStateOf<Tab?>(null) }
    var prayerOpen by remember { mutableStateOf<dev.falah.core.Prayer?>(null) }
    var settingsPage by remember { mutableStateOf<SettingsSection?>(null) }

    // Keep the same section on screen when the user shows or hides sections in Settings.
    var shown by remember { mutableStateOf(sections) }
    LaunchedEffect(sections) {
        if (sections != shown) { val page = Navigation.pageAfterChange(shown, pager.currentPage, sections); shown = sections; pager.scrollToPage(page) }
    }

    val current = sections.getOrElse(pager.currentPage) { Section.Home }
    fun goTo(section: Section) {
        sub = null; prayerOpen = null
        scope.launch {
            drawer.close()
            val page = sections.indexOf(section)
            if (page >= 0) { if (reduceMotion) pager.scrollToPage(page) else pager.animateScrollToPage(page) }
        }
    }
    fun open(tab: Tab) { tab.section?.let(::goTo) ?: run { sub = tab; prayerOpen = null; if (tab == Tab.Settings) settingsPage = null; scope.launch { drawer.close() } } }

    LaunchedEffect(jumpTo) { jumpTo?.let { if (it in sections) goTo(it); jumped() } }

    PlatformBackHandler(enabled = drawer.isOpen || sub != null || prayerOpen != null || current != Section.Home) {
        when (Navigation.back(drawer.isOpen, sub != null || prayerOpen != null, current, inSettingsSection = sub == Tab.Settings && settingsPage != null)) {
            BackAction.CloseDrawer -> scope.launch { drawer.close() }
            BackAction.LeaveSettingsSection -> settingsPage = null
            BackAction.LeaveSubScreen -> if (prayerOpen != null) prayerOpen = null else sub = if (sub == Tab.About) Tab.Settings else null
            BackAction.GoHome -> goTo(Section.Home)
            BackAction.Exit -> Unit
        }
    }

    ModalNavigationDrawer(
        drawerContent = { MenuSheet(sections, current.takeIf { sub == null }, sub, ::open, { scope.launch { drawer.close() }; showWhatsNew() }, { scope.launch { drawer.close() }; showTour() }) },
        drawerState = drawer,
        // Swiping in from the edge opens the menu on the first page only; on other pages a swipe moves between sections.
        gesturesEnabled = drawer.isOpen,
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { TopBar(if (prayerOpen != null) null else if (sub == Tab.Settings) settingsPage?.title() ?: Str.TabSettings else sub?.label ?: Tab.of(current).label, { scope.launch { drawer.open() } }) },
            bottomBar = {
                NavigationBar(Modifier.testTag(BOTTOM_BAR_TAG), containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    sections.forEach { sec ->
                        val t = Tab.of(sec)
                        NavigationBarItem(sub == null && prayerOpen == null && current == sec, { goTo(sec) }, { Icon(t.icon, null) }, label = { Text(t.label.t(), maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary, indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)))
                    }
                }
            },
        ) { inner ->
            Column(Modifier.padding(inner)) {
                if (state.unreadable.isNotEmpty() || state.saveFailed) StorageBanner(state)
                Box(Modifier.weight(1f)) {
                    if (sub == null && pager.currentPage == 0) DrawerEdge { scope.launch { drawer.open() } }
                    CompositionLocalProvider(LocalPrayerOpener provides if (state.settings.prayerScreens) { p -> prayerOpen = p } else null) {
                    when (if (prayerOpen != null) null else sub) {
                        Tab.Settings -> SettingsPage(state, resumed, settingsPage, { settingsPage = it }) { sub = Tab.About }
                        Tab.About -> AboutPage(state, showTour)
                        else -> if (prayerOpen != null) PrayerScreen(state, prayerOpen!!) else HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1, key = { sections[it].key }) { page ->
                            SectionPage(sections[page], state, resumed, active = page == pager.currentPage, hiddenNotes, hideNotes, sections, ::open)
                        }
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
        Section.Home -> HomePage(state, resumed, sections, open, hiddenNotes, hideNotes)
        Section.Times -> TimesTab(state)
        Section.Qibla -> CompassPage(state, resumed, active)
        Section.Tasbih -> TasbihPage(state)
        Section.Log -> LogPage(state)
    }
}

@Composable
private fun TopBar(title: Str?, openMenu: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(openMenu, Modifier.size(48.dp)) { Icon(Icons.Rounded.Menu, Str.MenuOpen.t()) }
        title?.let { Text(it.t(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
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

/** The side menu, in the order of the shared Zimam spec: header, sections, divider, Settings, What's new, Tour, About, footer. */
@Composable
private fun MenuSheet(sections: List<Section>, current: Section?, sub: Tab?, open: (Tab) -> Unit, whatsNew: () -> Unit, tour: () -> Unit) {
    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) { BrandMark(Modifier.size(40.dp)) }
                Column {
                    Text(Str.AppName.t(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(Str.MenuByZimam.t(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Text(Str.VersionLine.t(APP_VERSION), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            sections.forEach { sec -> val t = Tab.of(sec); MenuItem(t.label.t(), t.icon, current == sec) { open(t) } }
            HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
            MenuItem(Str.TabSettings.t(), Tab.Settings.icon, sub == Tab.Settings) { open(Tab.Settings) }
            MenuItem(Str.MenuWhatsNew.t(), Icons.Rounded.NewReleases, false, whatsNew)
            MenuItem(Str.ShowTour.t(), Icons.Rounded.PlayCircle, false, tour)
            MenuItem(Str.TabAbout.t(), Tab.About.icon, sub == Tab.About) { open(Tab.About) }
            Text(Str.MenuFooter.t(), Modifier.padding(horizontal = 28.dp, vertical = 16.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MenuItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) = NavigationDrawerItem(
    { Text(label) }, selected, onClick, Modifier.padding(horizontal = 16.dp).heightIn(min = 56.dp), icon = { Icon(icon, null) },
    colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary),
)

/**
 * The pager takes every horizontal swipe, so on the first section a thin strip at the start edge opens the menu
 * (the menu button always works). The strip sits on the side the reading direction starts from.
 */
@Composable
private fun BoxScope.DrawerEdge(onOpen: () -> Unit) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val threshold = with(LocalDensity.current) { 40.dp.toPx() }
    Box(
        Modifier.align(Alignment.CenterStart).width(24.dp).fillMaxHeight().pointerInput(rtl) {
            var dragged = 0f
            detectHorizontalDragGestures(onDragStart = { dragged = 0f }, onDragEnd = { dragged = 0f }) { change, amount ->
                change.consume()
                dragged += if (rtl) -amount else amount
                if (dragged > threshold) { dragged = Float.NEGATIVE_INFINITY; onOpen() }
            }
        },
    )
}
