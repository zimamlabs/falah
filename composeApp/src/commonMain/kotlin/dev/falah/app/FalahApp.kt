package dev.falah.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import dev.falah.core.I18n
import dev.falah.core.Lang
import dev.falah.core.Str
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.falah.core.Onboarding
import dev.falah.core.ReleaseNotes
import dev.falah.core.Welcome
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

const val APP_VERSION = "0.5.1"

enum class Tab(val label: Str, val icon: ImageVector) {
    Home(Str.TabHome, Icons.Rounded.Home), Times(Str.TabTimes, Icons.Rounded.Schedule), Qibla(Str.TabQibla, Icons.Rounded.Explore), Tasbih(Str.TabTasbih, Icons.Rounded.TouchApp), Log(Str.TabLog, Icons.Rounded.CalendarMonth),
    Settings(Str.TabSettings, Icons.Rounded.Settings), About(Str.TabAbout, Icons.Rounded.Settings), // reached from Home and Settings, not shown in the bar
}

/** [resumed] changes each time the app comes back to the front, so the data folder can be picked again after the permission screen. */
@Composable
fun FalahApp(prefs: Prefs, platform: Platform = NoPlatform, files: AppFiles = MemoryFiles(), resumed: Int = 0) {
    val state = remember { FalahState(prefs, files, platform) }
    LaunchedEffect(resumed) {
        if (resumed > 0 && files.refresh()) {
            (prefs as? MirroredPrefs)?.mirror()
            state.reload()
        }
    }
    LaunchedEffect(Unit) { while (true) { delay(15_000); state.now = Clock.System.now() } }
    val language = I18n.resolve(state.langChoice, systemLanguage())
    CompositionLocalProvider(
        LocalAppLocale provides AppLocale(language, state.arabicDigits),
        LocalLayoutDirection provides if (language == Lang.Ar) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) { FalahTheme(state.themeMode) {
        var splash by remember { mutableStateOf(true) }
        var tab by remember { mutableStateOf(Tab.Home) }
        var welcome by remember {
            mutableStateOf(Onboarding.decide(prefs.get("seen.version").orEmpty(), prefs.get("tour.done") == "1", prefs.get("place") != null || prefs.get("notify.Fajr") != null, APP_VERSION))
        }
        var tourAgain by remember { mutableStateOf(false) }
        var hiddenNotes by remember { mutableStateOf(prefs.get("notes.hidden") == APP_VERSION) }
        fun finishTour() { prefs.set("tour.done", "1"); prefs.set("seen.version", APP_VERSION); welcome = Welcome.Nothing; tourAgain = false }
        PlatformBackHandler(enabled = tab != Tab.Home && !tourAgain) { tab = if (tab == Tab.About) Tab.Settings else Tab.Home }
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (welcome == Welcome.Tour || tourAgain) Tour(state, ::finishTour)
            else Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                        Tab.entries.filter { it != Tab.Settings && it != Tab.About }.forEach { t -> NavigationBarItem(tab == t, { tab = t }, { Icon(t.icon, t.label.t()) }, label = { Text(t.label.t()) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary, indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))) }
                    }
                },
            ) { inner ->
                Column(Modifier.padding(inner)) {
                if (state.unreadable.isNotEmpty() || state.saveFailed) {
                    Text(
                        (if (state.unreadable.isNotEmpty()) Str.DataUnreadable else Str.SaveFailed).t(),
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer).padding(12.dp),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        Tab.Home -> HomePage(state, { tab = it }, hiddenNotes) { hiddenNotes = true; prefs.set("notes.hidden", APP_VERSION) }
                        Tab.Times -> TimesTab(state)
                        Tab.Qibla -> CompassPage(state, resumed)
                        Tab.Tasbih -> TasbihPage(state)
                        Tab.Log -> LogPage(state)
                        Tab.Settings -> SettingsPage(state) { tab = Tab.About }
                        Tab.About -> AboutPage(state) { tourAgain = true }
                    }
                }
                }
            }
            if (welcome == Welcome.WhatsNew && !splash) AlertDialog(
                onDismissRequest = {},
                title = { Text(Str.WhatsNewIn.t(APP_VERSION)) },
                text = { Column { ReleaseNotes.forVersion(APP_VERSION, lang()).forEach { Text("•  ${num(it)}") } } },
                confirmButton = { TextButton({ prefs.set("seen.version", APP_VERSION); prefs.set("tour.done", "1"); welcome = Welcome.Nothing }) { Text(Str.Close.t()) } },
            )
            AnimatedVisibility(splash, exit = fadeOut()) { Splash { splash = false } }
        }
    } }
}
