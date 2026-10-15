package dev.falah.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import dev.falah.core.Countdown
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
import dev.falah.core.PrayerNotice
import dev.falah.core.ReleaseNotes
import dev.falah.core.Section
import dev.falah.core.Welcome
import androidx.compose.material.icons.rounded.Info
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

const val APP_VERSION = "0.5.1"

/** The places the app can show: the five sections in the pager, plus Settings and About reached from the menu. */
enum class Tab(val label: Str, val icon: ImageVector, val section: Section?) {
    Home(Str.TabHome, Icons.Rounded.Home, Section.Home), Times(Str.TabTimes, Icons.Rounded.Schedule, Section.Times), Qibla(Str.TabQibla, Icons.Rounded.Explore, Section.Qibla),
    Tasbih(Str.TabTasbih, Icons.Rounded.TouchApp, Section.Tasbih), Log(Str.TabLog, Icons.Rounded.CalendarMonth, Section.Log),
    Settings(Str.TabSettings, Icons.Rounded.Settings, null), About(Str.TabAbout, Icons.Rounded.Info, null);

    companion object { fun of(section: Section) = entries.first { it.section == section } }
}

/** [resumed] changes each time the app comes back to the front, so the data folder can be picked again after the permission screen. */
@Composable
fun FalahApp(prefs: Prefs, platform: Platform = NoPlatform, files: AppFiles = MemoryFiles(), resumed: Int = 0, showSplash: Boolean = true, active: Boolean = true, notice: PrayerNotice? = null, onNoticeDone: () -> Unit = {}) {
    val state = remember { FalahState(prefs, files, platform) }
    LaunchedEffect(resumed) {
        if (resumed > 0 && files.refresh()) {
            (prefs as? MirroredPrefs)?.mirror()
            state.reload()
        }
    }
    // One clock source: the system clock is read on every tick and the countdown is computed from the target time, so a
    // late tick never drifts. The loop only runs while the app is in front; coming back reads the clock at once.
    LaunchedEffect(active) {
        while (active) {
            val now = Clock.System.now(); state.now = now
            delay(Countdown.nextTickMillis(now.toEpochMilliseconds()))
        }
    }
    val language = I18n.resolve(state.langChoice, systemLanguage())
    CompositionLocalProvider(
        LocalAppLocale provides AppLocale(language, state.arabicDigits),
        LocalLayoutDirection provides if (language == Lang.Ar) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) { FalahTheme(state.settings) {
        var splash by remember { mutableStateOf(showSplash) }
        var welcome by remember {
            mutableStateOf(Onboarding.decide(prefs.get("seen.version").orEmpty(), prefs.get("tour.done") == "1", prefs.get("place") != null || prefs.get("notify.Fajr") != null, APP_VERSION))
        }
        var tourAgain by remember { mutableStateOf(false) }
        var jump by remember { mutableStateOf<Section?>(null) }
        var hiddenNotes by remember { mutableStateOf(prefs.get("notes.hidden") == APP_VERSION) }
        fun finishTour() { prefs.set("tour.done", "1"); prefs.set("seen.version", APP_VERSION); welcome = Welcome.Nothing; tourAgain = false }
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (welcome == Welcome.Tour || tourAgain) { PlatformBackHandler(enabled = tourAgain) { tourAgain = false }; Tour(state, ::finishTour) }
            else AppShell(
                state, resumed,
                hiddenNotes = hiddenNotes, hideNotes = { hiddenNotes = true; prefs.set("notes.hidden", APP_VERSION) },
                showTour = { tourAgain = true }, showWhatsNew = { welcome = Welcome.WhatsNew },
                jumpTo = jump, jumped = { jump = null },
            )
            if (notice != null && welcome != Welcome.Tour && !tourAgain) {
                PlatformBackHandler(enabled = true) { onNoticeDone() }
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { PrayerNoticeScreen(state, notice, { jump = Section.Qibla }, onNoticeDone) }
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
