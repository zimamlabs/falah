package dev.falah.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.falah.core.AsrFactor
import dev.falah.core.HighLatitude
import dev.falah.core.Method
import dev.falah.core.Prayer
import dev.falah.core.Section
import dev.falah.core.SettingsCodec
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import dev.falah.core.SettingsSection
import dev.falah.core.Str
import dev.falah.core.ThemeMode

private fun SettingsSection.icon(): ImageVector = when (this) {
    SettingsSection.Appearance -> Icons.Rounded.Palette
    SettingsSection.Layout -> Icons.Rounded.Dashboard
    SettingsSection.Language -> Icons.Rounded.Translate
    SettingsSection.Calculation -> Icons.Rounded.Tune
    SettingsSection.Notifications -> Icons.Rounded.Notifications
    SettingsSection.Data -> Icons.Rounded.Folder
    SettingsSection.Privacy -> Icons.Rounded.Lock
}

fun SettingsSection.title(): Str = when (this) {
    SettingsSection.Appearance -> Str.Appearance
    SettingsSection.Layout -> Str.SectionsLayout
    SettingsSection.Language -> Str.LanguageTitle
    SettingsSection.Calculation -> Str.SettingsCalc
    SettingsSection.Notifications -> Str.NotificationsTitle
    SettingsSection.Data -> Str.YourData
    SettingsSection.Privacy -> Str.PrivacyTitle
}

@Composable
private fun SettingsSection.summary(s: FalahState): String = when (this) {
    SettingsSection.Appearance -> Str.ColourSummary.t(s.themeMode.label().t(), s.settings.accent.label().t(), s.settings.textSize.label().t())
    SettingsSection.Layout -> Str.SectionsSummary.t(s.settings.sections.size, Section.entries.size, Tab.of(s.settings.startSection).label.t())
    SettingsSection.Language -> listOf("system" to Str.ThemeSystem, "en" to Str.LangEnglish, "ar" to Str.LangArabic).first { it.first == s.langChoice }.second.t()
    SettingsSection.Calculation -> "${placeShown(s.placeName)}, ${s.method.shown()}"
    SettingsSection.Notifications -> if (s.settings.notify.isEmpty()) Str.NotificationsSummaryOff.t() else Str.NotificationsSummaryOn.t(s.settings.notify.size)
    SettingsSection.Data -> if (s.files.shared) Str.KeptShared.t() else Str.KeptPrivate.t()
    SettingsSection.Privacy -> Str.PrivacySummary.t()
}

private fun ThemeMode.label() = when (this) { ThemeMode.System -> Str.ThemeSystem; ThemeMode.Light -> Str.ThemeLight; ThemeMode.Dark -> Str.ThemeDark }

/** One tappable row of a list: icon, title, a one-line summary and an arrow. At least 56 dp high. */
@Composable
fun SettingsRow(icon: ImageVector, title: String, summary: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = LocalSpacing.current.rowMinHeight).clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Settings as a list of labelled sections; [page] is the open section (null shows the list). */
@Composable
fun SettingsPage(s: FalahState, resumed: Int, page: SettingsSection?, open: (SettingsSection?) -> Unit, openAbout: () -> Unit) = Page {
    if (page == null) {
        Text(Str.TabSettings.t(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        FalahCard {
            SettingsSection.entries.forEach { sec -> SettingsRow(sec.icon(), sec.title().t(), sec.summary(s)) { open(sec) } }
            SettingsRow(Icons.Rounded.Info, Str.AboutCardTitle.t(), Str.VersionLine.t(APP_VERSION), openAbout)
        }
    } else {
        Text(page.title().t(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        FalahCard { SettingsSectionBody(s, page, resumed) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsSectionBody(s: FalahState, page: SettingsSection, resumed: Int) {
    when (page) {
        SettingsSection.Appearance -> AppearanceControls(s)
        SettingsSection.Layout -> LayoutControls(s)
        SettingsSection.Language -> LanguageChips(s)
        SettingsSection.Calculation -> CalculationControls(s)
        SettingsSection.Notifications -> NotificationControls(s, resumed)
        SettingsSection.Data -> DataCardBody(s)
        SettingsSection.Privacy -> PrivacyBody()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LayoutControls(s: FalahState) {
    val shown = s.settings.sections
    val order = shown + (Section.entries - shown.toSet())
    Text(Str.SectionsNote.t(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    order.forEach { sec ->
        val tab = Tab.of(sec); val on = sec in shown; val index = shown.indexOf(sec)
        Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = LocalSpacing.current.rowMinHeight), verticalAlignment = Alignment.CenterVertically) {
            Icon(tab.icon, null, Modifier.padding(end = 12.dp), tint = MaterialTheme.colorScheme.primary)
            Text(tab.label.t(), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            if (on && sec != Section.Home) {
                IconButton({ s.moveSection(sec, -1) }, enabled = index > 1) { Icon(Icons.Rounded.ArrowUpward, Str.MoveUp.t()) }
                IconButton({ s.moveSection(sec, 1) }, enabled = index < shown.lastIndex) { Icon(Icons.Rounded.ArrowDownward, Str.MoveDown.t()) }
            }
            Switch(on, { s.showSection(sec, it) }, enabled = sec != Section.Home)
        }
    }
    Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = LocalSpacing.current.rowMinHeight), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(Str.PrayerScreensOption.t(), style = MaterialTheme.typography.bodyLarge)
            Text(Str.PrayerScreensNote.t(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(s.settings.prayerScreens, { s.choosePrayerScreens(it) })
    }
    Text(Str.StartScreen.t(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { shown.forEach { sec -> FilterChip(s.settings.startSection == sec, { s.chooseStart(sec) }, { Text(Tab.of(sec).label.t()) }) } }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalculationControls(s: FalahState) {
    Text(Str.SettingsPlace.t(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    PlacePicker(s)
    Text(Str.SettingsCalc.t(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Method.entries.forEach { m -> FilterChip(s.method == m, { s.chooseMethod(m) }, { Text(m.shown()) }) } }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { AsrFactor.entries.forEach { a -> FilterChip(s.asr == a, { s.chooseAsr(a) }, { Text(Str.AsrLine.t(a.shown())) }) } }
    Text(Str.HighLatTitle.t(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { HighLatitude.entries.forEach { r -> FilterChip(s.settings.highLatitude == r, { s.chooseHighLatitude(r) }, { Text(r.label) }) } }
    Text(Str.HighLatNote.t(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(Str.HijriAdjust.t(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(-1, 0, 1).forEach { d -> FilterChip(s.hijriAdjust == d, { s.chooseHijriAdjust(d) }, { Text(if (d > 0) Str.DayPlus.t(d) else if (d < 0) Str.DayMinus.t(-d) else Str.NoneOpt.t()) }) } }
    Text(Str.MoonNote.t(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(Str.AdjustTitle.t(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(Str.AdjustNote.t(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Prayer.entries.forEach { p -> AdjustRow(s, p) }
}

@Composable
private fun AdjustRow(s: FalahState, p: Prayer) {
    val minutes = s.settings.adjust[p] ?: 0
    Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = LocalSpacing.current.rowMinHeight), verticalAlignment = Alignment.CenterVertically) {
        Text(p.shown(), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        IconButton({ s.setAdjust(p, minutes - 1) }, enabled = minutes > -30) { Icon(Icons.Rounded.Remove, Str.Earlier.t()) }
        Text(if (minutes > 0) "+$minutes" else "$minutes", Modifier.widthIn(min = 40.dp), style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        IconButton({ s.setAdjust(p, minutes + 1) }, enabled = minutes < 30) { Icon(Icons.Rounded.Add, Str.Later.t()) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotificationControls(s: FalahState, resumed: Int) {
    if (!s.platform.canNotify) { Text(Str.NotificationsUnavailable.t(), style = MaterialTheme.typography.bodyMedium); return }
    ReminderStatusCard(s, resumed, onlyWhenUsed = false, showOk = true)
    Text(Str.NotifyAt.t(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Prayer.entries.filter { it != Prayer.Sunrise }.forEach { p -> FilterChip(s.notifyOn(p), { s.setNotify(p, !s.notifyOn(p)) }, { Text(p.shown()) }) } }
    Text(Str.NotifyNote.t(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Prayer.entries.filter { it != Prayer.Sunrise && s.notifyOn(it) }.forEach { p ->
        Text(p.shown(), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(0, 5, 10, 15, 30).forEach { m -> FilterChip(s.offsetOf(p) == m, { s.setOffset(p, m) }, { Text(if (m == 0) Str.AtTheTime.t() else Str.MinBefore.t(m)) }) } }
    }
    Text(Str.NoticeOption.t(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(Str.NoticeOptionNote.t(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Prayer.entries.forEach { p -> FilterChip(p in s.settings.noticeScreen, { s.setNoticeScreen(p, p !in s.settings.noticeScreen) }, { Text(p.shown()) }) } }
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        SegmentedButton(s.settings.noticeFullScreen, { s.chooseNoticeFullScreen(true) }, SegmentedButtonDefaults.itemShape(0, 2)) { Text(Str.NoticeFull.t()) }
        SegmentedButton(!s.settings.noticeFullScreen, { s.chooseNoticeFullScreen(false) }, SegmentedButtonDefaults.itemShape(1, 2)) { Text(Str.NoticeHeadsUp.t()) }
    }
    Text(Str.NoticeFullNote.t(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(Str.SnoozeTitle.t(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { SettingsCodec.snoozeChoices.forEach { m -> FilterChip(s.settings.snoozeMinutes == m, { s.chooseSnooze(m) }, { Text(Str.MinutesShort.t(m)) }) } }
}

@Composable
private fun PrivacyBody() {
    listOf(Str.PrivacyStored, Str.PrivacyPermissions, Str.PrivacyNetwork).forEach { Text(it.t(), style = MaterialTheme.typography.bodyMedium) }
}
