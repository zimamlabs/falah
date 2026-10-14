package dev.falah.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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

const val APP_VERSION = "0.3.0"

enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Rounded.Home), Times("Times", Icons.Rounded.Schedule), Qibla("Qibla", Icons.Rounded.Explore), Settings("Settings", Icons.Rounded.Settings),
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
    FalahTheme(state.themeMode) {
        var splash by remember { mutableStateOf(true) }
        var tab by remember { mutableStateOf(Tab.Home) }
        var welcome by remember {
            mutableStateOf(Onboarding.decide(prefs.get("seen.version").orEmpty(), prefs.get("tour.done") == "1", prefs.get("place") != null || prefs.get("notify.Fajr") != null, APP_VERSION))
        }
        var tourAgain by remember { mutableStateOf(false) }
        var hiddenNotes by remember { mutableStateOf(prefs.get("notes.hidden") == APP_VERSION) }
        fun finishTour() { prefs.set("tour.done", "1"); prefs.set("seen.version", APP_VERSION); welcome = Welcome.Nothing; tourAgain = false }
        PlatformBackHandler(enabled = tab != Tab.Home && !tourAgain) { tab = Tab.Home }
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (welcome == Welcome.Tour || tourAgain) Tour(state, ::finishTour)
            else Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                        Tab.entries.forEach { t -> NavigationBarItem(tab == t, { tab = t }, { Icon(t.icon, t.label) }, label = { Text(t.label) }) }
                    }
                },
            ) { inner ->
                Box(Modifier.padding(inner)) {
                    when (tab) {
                        Tab.Home -> HomePage(state, { tab = it }, hiddenNotes) { hiddenNotes = true; prefs.set("notes.hidden", APP_VERSION) }
                        Tab.Times -> TimesPage(state)
                        Tab.Qibla -> QiblaPage(state)
                        Tab.Settings -> SettingsPage(state) { tourAgain = true }
                    }
                }
            }
            if (welcome == Welcome.WhatsNew && !splash) AlertDialog(
                onDismissRequest = {},
                title = { Text("What's new in $APP_VERSION") },
                text = { androidx.compose.foundation.layout.Column { ReleaseNotes.forVersion(APP_VERSION).forEach { Text("•  $it") } } },
                confirmButton = { TextButton({ prefs.set("seen.version", APP_VERSION); prefs.set("tour.done", "1"); welcome = Welcome.Nothing }) { Text("Close") } },
            )
            AnimatedVisibility(splash, exit = fadeOut()) { Splash { splash = false } }
        }
    }
}
