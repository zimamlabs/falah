package dev.falah.app

import dev.falah.core.Str
import dev.falah.core.ThemeMode
import dev.falah.core.Lang
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import dev.falah.core.AsrFactor
import dev.falah.core.CoordinateError
import dev.falah.core.CoordinateResult
import dev.falah.core.Coordinates
import dev.falah.core.Method
import dev.falah.core.Prayer
import dev.falah.core.PrayerTimes
import dev.falah.core.of
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

@Composable
private fun Page(content: @Composable () -> Unit) {
    val sp = LocalSpacing.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = sp.gutter, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(sp.cardGap)) { content() }
}

@Composable
fun PlacePicker(s: FalahState) {
    var menu by remember { mutableStateOf(false) }
    var custom by remember { mutableStateOf(false) }
    Box {
        OutlinedButton({ menu = true }) { Icon16(); Text(" ${placeShown(s.placeName)}") }
        DropdownMenu(menu, { menu = false }) {
            Places.forEach { p -> DropdownMenuItem(text = { Text(p.shown()) }, onClick = { menu = false; s.choosePlace(p) }) }
            DropdownMenuItem(text = { Text(Str.CustomPlaceMenu.t()) }, onClick = { menu = false; custom = true })
        }
    }
    if (!s.placeChosen) Text(Str.PlaceNotChosen.t(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    if (custom) CustomPlaceDialog(s) { custom = false }
}

@Composable
private fun CustomPlaceDialog(s: FalahState, close: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("") }
    var lon by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<CoordinateError?>(null) }
    AlertDialog(
        onDismissRequest = close,
        title = { Text(Str.CustomPlaceTitle.t()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(Str.CustomPlaceHint.t(), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(name, { name = it }, label = { Text(Str.CustomPlaceName.t()) }, singleLine = true)
                OutlinedTextField(lat, { lat = it; error = null }, label = { Text(Str.Latitude.t()) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
                OutlinedTextField(lon, { lon = it; error = null }, label = { Text(Str.Longitude.t()) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
                error?.let { Text(it.message().t(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton({
                when (val r = Coordinates.parse(lat, lon)) {
                    is CoordinateResult.Ok -> { s.chooseCustomPlace(name, r.latitude, r.longitude); close() }
                    is CoordinateResult.Bad -> error = r.error
                }
            }) { Text(Str.UsePlace.t()) }
        },
        dismissButton = { TextButton(close) { Text(Str.Cancel.t()) } },
    )
}

private fun CoordinateError.message() = when (this) {
    CoordinateError.Empty -> Str.CoordEmpty
    CoordinateError.NotANumber -> Str.CoordNotNumber
    CoordinateError.LatitudeRange -> Str.CoordLatRange
    CoordinateError.LongitudeRange -> Str.CoordLonRange
}

@Composable
private fun Icon16() = androidx.compose.material3.Icon(Icons.Rounded.Place, null, Modifier.size(16.dp))

@Composable
fun TimesPage(s: FalahState) = Page {
    Text(Str.TimesTitle.t(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    PlacePicker(s)
    FalahCard {
        SectionTitle(Icons.Rounded.Schedule, s.today.longText(), Str.HijriShort.t(s.hijri.day, s.hijri.monthTitle(lang()), s.hijri.year))
        Prayer.entries.forEach { p ->
            val upcoming = !s.next.tomorrow && p == s.next.prayer
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(p.shown(), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = if (upcoming) FontWeight.Bold else FontWeight.Normal, color = if (upcoming) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                p.second()?.let { Text(it, Modifier.padding(end = 16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text(num(PrayerTimes.clock(s.times.of(p)) ?: "--:--"), style = MaterialTheme.typography.titleMedium, fontWeight = if (upcoming) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
    Text(Str.TimesDisclaimer.t(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsPage(s: FalahState, openAbout: () -> Unit) = Page {
    Text(Str.TabSettings.t(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    FalahCard {
        SectionTitle(Icons.Rounded.Place, Str.SettingsPlace.t())
        PlacePicker(s)
    }
    FalahCard {
        SectionTitle(Icons.Rounded.Tune, Str.SettingsCalc.t())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Method.entries.forEach { m -> FilterChip(s.method == m, { s.chooseMethod(m) }, { Text(m.shown()) }) } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { AsrFactor.entries.forEach { a -> FilterChip(s.asr == a, { s.chooseAsr(a) }, { Text(Str.AsrLine.t(a.shown())) }) } }
        Text(Str.HijriAdjust.t(), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(-1, 0, 1).forEach { d -> FilterChip(s.hijriAdjust == d, { s.chooseHijriAdjust(d) }, { Text(if (d > 0) Str.DayPlus.t(d) else if (d < 0) Str.DayMinus.t(-d) else Str.NoneOpt.t()) }) } }
        Text(Str.MoonNote.t(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (s.platform.canNotify) FalahCard {
        SectionTitle(Icons.Rounded.Notifications, Str.NotifyAt.t())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Prayer.entries.filter { it != Prayer.Sunrise }.forEach { p -> FilterChip(s.notifyOn(p), { s.setNotify(p, !s.notifyOn(p)) }, { Text(p.shown()) }) } }
        Text(Str.NotifyNote.t(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Prayer.entries.filter { it != Prayer.Sunrise && s.notifyOn(it) }.forEach { p ->
            Text(p.shown(), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(0, 5, 10, 15, 30).forEach { m -> FilterChip(s.offsetOf(p) == m, { s.setOffset(p, m) }, { Text(if (m == 0) Str.AtTheTime.t() else Str.MinBefore.t(m)) }) } }
        }
    }
    FalahCard {
        SectionTitle(Icons.Rounded.Palette, Str.Appearance.t())
        AppearanceControls(s)
    }
    FalahCard {
        SectionTitle(Icons.Rounded.Translate, Str.LanguageTitle.t())
        LanguageChips(s)
    }
    DataCard(s)
    FalahCard(onClick = openAbout) {
        SectionTitle(Icons.Rounded.Info, Str.AboutCardTitle.t(), Str.VersionLine.t(APP_VERSION))
        Text(Str.AboutCardText.t(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DataCard(s: FalahState) {
    var tick by remember { mutableStateOf(0) }
    var confirm by remember { mutableStateOf(false) }
    FalahCard {
        SectionTitle(Icons.Rounded.Folder, Str.YourData.t())
        Text(if (s.files.shared) Str.KeptShared.t() else Str.KeptPrivate.t(), style = MaterialTheme.typography.bodyMedium)
        Text(s.files.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(Str.DataNote.t(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        s.files.message?.let { (from, to) -> Text(Str.FolderVerifyFailed.raw(from, to), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        if (s.files.canAskAccess) Button({ s.files.askAccess() }) { Text(Str.AllowAccess.t()) }
        tick.let { }
        s.files.removableOld()?.let { old ->
            Text(Str.OldCopyAt.raw(old), style = MaterialTheme.typography.bodySmall)
            OutlinedButton({ confirm = true }) { Text(Str.RemoveOldCopy.t()) }
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text(Str.RemoveOldTitle.t()) },
        text = { Text(Str.RemoveOldText.t()) },
        confirmButton = { TextButton({ confirm = false; s.files.removeOld(); tick++ }) { Text(Str.Remove.t()) } },
        dismissButton = { TextButton({ confirm = false }) { Text(Str.KeepIt.t()) } },
    )
}

/** Language (System, English, العربية) and the digit style; both apply at once. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LanguageChips(s: FalahState) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("system" to Str.ThemeSystem.t(), "en" to Str.LangEnglish.t(), "ar" to Str.LangArabic.t()).forEach { (k, v) -> FilterChip(s.langChoice == k, { s.chooseLanguage(k) }, { Text(v) }) }
    }
    Text(Str.DigitsTitle.t(), style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(!s.arabicDigits, { s.chooseDigits(false) }, { Text(Str.DigitsWestern.text(Lang.En)) })
        FilterChip(s.arabicDigits, { s.chooseDigits(true) }, { Text(Str.DigitsArabic.text(Lang.En)) })
    }
}
