package dev.falah.app

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
import dev.falah.core.AsrFactor
import dev.falah.core.Method
import dev.falah.core.Prayer
import dev.falah.core.PrayerTimes
import dev.falah.core.of
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

@Composable
private fun Page(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { content() }
}

@Composable
fun PlacePicker(s: FalahState) {
    var menu by remember { mutableStateOf(false) }
    Box {
        OutlinedButton({ menu = true }) { Icon16(); Text(" ${s.placeName}") }
        DropdownMenu(menu, { menu = false }) {
            Places.forEach { p -> DropdownMenuItem(text = { Text(p.name) }, onClick = { menu = false; s.choosePlace(p) }) }
        }
    }
}

@Composable
private fun Icon16() = androidx.compose.material3.Icon(Icons.Rounded.Place, null, Modifier.size(16.dp))

@Composable
fun TimesPage(s: FalahState) = Page {
    Text("Prayer times", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    PlacePicker(s)
    FalahCard {
        SectionTitle(Icons.Rounded.Schedule, s.today.longText(), "${s.hijri.day} ${s.hijri.monthName} ${s.hijri.year}")
        Prayer.entries.forEach { p ->
            val upcoming = !s.next.tomorrow && p == s.next.prayer
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(p.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = if (upcoming) FontWeight.Bold else FontWeight.Normal, color = if (upcoming) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                Text(p.arabic, Modifier.padding(end = 16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(PrayerTimes.clock(s.times.of(p)) ?: "--:--", style = MaterialTheme.typography.titleMedium, fontWeight = if (upcoming) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
    Text("Prayer times are a calculation, not an authority: check them against your local mosque or religious authority when it matters.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun QiblaPage(s: FalahState) = Page {
    Text("Qibla", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    PlacePicker(s)
    val qibla = PrayerTimes.qibla(s.lat, s.lon)
    FalahCard {
        SectionTitle(Icons.Rounded.Explore, "Direction of the Kaaba")
        Text("N", Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Ring(1f, 220.dp, 6.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.outlineVariant) {
                Text("↑", Modifier.rotate(qibla.toFloat()), style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
        Text("${qibla.toInt()}° from true north", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text("The arrow points the way when the top of the phone faces north. A live compass comes in the next version.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsPage(s: FalahState, openAbout: () -> Unit) = Page {
    Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    FalahCard {
        SectionTitle(Icons.Rounded.Place, "Place")
        PlacePicker(s)
    }
    FalahCard {
        SectionTitle(Icons.Rounded.Tune, "Calculation")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Method.entries.forEach { m -> FilterChip(s.method == m, { s.chooseMethod(m) }, { Text(m.label) }) } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { AsrFactor.entries.forEach { a -> FilterChip(s.asr == a, { s.chooseAsr(a) }, { Text("Asr: ${a.label}") }) } }
        Text("Hijri date adjustment", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(-1, 0, 1).forEach { d -> FilterChip(s.hijriAdjust == d, { s.chooseHijriAdjust(d) }, { Text(if (d > 0) "+$d day" else if (d < 0) "$d day" else "None") }) } }
        Text("Moon-sighting calendars can start a day apart from the calculated one.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (s.platform.canNotify) FalahCard {
        SectionTitle(Icons.Rounded.Notifications, "Notify me at")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Prayer.entries.filter { it != Prayer.Sunrise }.forEach { p -> FilterChip(s.notifyOn(p), { s.setNotify(p, !s.notifyOn(p)) }, { Text(p.label) }) } }
        Text("A notification at the time of each prayer you pick, or a few minutes before it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Prayer.entries.filter { it != Prayer.Sunrise && s.notifyOn(it) }.forEach { p ->
            Text(p.label, style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(0, 5, 10, 15, 30).forEach { m -> FilterChip(s.offsetOf(p) == m, { s.setOffset(p, m) }, { Text(if (m == 0) "At the time" else "$m min before") }) } }
        }
    }
    FalahCard {
        SectionTitle(Icons.Rounded.Palette, "Appearance")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (k, v) -> FilterChip(s.themeMode == k, { s.chooseTheme(k) }, { Text(v) }) } }
    }
    DataCard(s)
    FalahCard(onClick = openAbout) {
        SectionTitle(Icons.Rounded.Info, "About Falah", "version $APP_VERSION")
        Text("The tour, what's new, privacy and links.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DataCard(s: FalahState) {
    var tick by remember { mutableStateOf(0) }
    var confirm by remember { mutableStateOf(false) }
    FalahCard {
        SectionTitle(Icons.Rounded.Folder, "Your data")
        Text(if (s.files.shared) "Kept in the Zimam folder:" else "Kept inside the app:", style = MaterialTheme.typography.bodyMedium)
        Text(s.files.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Settings and your prayer log are plain files. In the Zimam folder they survive reinstalling the app.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        s.files.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        if (s.files.canAskAccess) Button({ s.files.askAccess() }) { Text("Allow All files access") }
        tick.let { }
        s.files.removableOld()?.let { old ->
            Text("The earlier private copy is still at $old. It is checked and safe to remove.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton({ confirm = true }) { Text("Remove the old private copy") }
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Remove the old copy?") },
        text = { Text("Everything in it was copied to the Zimam folder and checked again just now. This deletes only the old private folder.") },
        confirmButton = { TextButton({ confirm = false; s.files.removeOld(); tick++ }) { Text("Remove") } },
        dismissButton = { TextButton({ confirm = false }) { Text("Keep it") } },
    )
}
