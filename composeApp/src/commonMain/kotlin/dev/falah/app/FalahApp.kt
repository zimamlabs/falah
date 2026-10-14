package dev.falah.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime

const val APP_VERSION = "0.2.2"

@Composable
fun FalahApp(prefs: Prefs, platform: Platform = NoPlatform) {
    FalahTheme {
        var splash by remember { mutableStateOf(true) }
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Today(prefs, platform)
            AnimatedVisibility(splash, exit = fadeOut()) { Splash { splash = false } }
        }
    }
}

@Composable
private fun Today(prefs: Prefs, platform: Platform) {
    var placeName by remember { mutableStateOf(prefs.get("place") ?: "Cairo") }
    var lat by remember { mutableStateOf(prefs.get("lat")?.toDoubleOrNull() ?: 30.0444) }
    var lon by remember { mutableStateOf(prefs.get("lon")?.toDoubleOrNull() ?: 31.2357) }
    var method by remember { mutableStateOf(Method.entries.firstOrNull { it.name == prefs.get("method") } ?: Method.Egyptian) }
    var asr by remember { mutableStateOf(AsrFactor.entries.firstOrNull { it.name == prefs.get("asr") } ?: AsrFactor.Standard) }
    var now by remember { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(Unit) { while (true) { delay(20_000); now = Clock.System.now() } }

    val zone = TimeZone.currentSystemDefault()
    val local = now.toLocalDateTime(zone)
    val offset = zone.offsetAt(now).totalSeconds / 3600.0
    val times = PrayerTimes.compute(local.date, lat, lon, offset, method, asr)
    val hourNow = local.hour + local.minute / 60.0
    val upcoming = Prayer.entries.filter { it != Prayer.Sunrise }.firstOrNull { (times.of(it) ?: -1.0) > hourNow }
    var menu by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Falah", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Text("${local.date.dayOfMonth}/${local.date.monthNumber}/${local.date.year}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                TextButton(onClick = { menu = true }) { Text(placeName) }
                DropdownMenu(menu, { menu = false }) {
                    Places.forEach { p ->
                        DropdownMenuItem(text = { Text(p.name) }, onClick = {
                            menu = false; placeName = p.name; lat = p.latitude; lon = p.longitude
                            prefs.set("place", p.name); prefs.set("lat", p.latitude.toString()); prefs.set("lon", p.longitude.toString()); platform.reschedule()
                        })
                    }
                }
            }
        }

        Panel {
            Text(upcoming?.let { "Next: ${it.label}" } ?: "Fajr is next, tomorrow", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            val nextAt = upcoming?.let { times.of(it) }
            if (nextAt != null) {
                val minutes = ((nextAt - hourNow) * 60).toInt().coerceAtLeast(0)
                Text("in ${minutes / 60} h ${minutes % 60} min  ·  ${PrayerTimes.clock(nextAt)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
        }

        Panel {
            Prayer.entries.forEach { p ->
                val t = times.of(p)
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(p.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = if (p == upcoming) FontWeight.Bold else FontWeight.Normal, color = if (p == upcoming) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    Text(p.arabic, Modifier.padding(end = 16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(PrayerTimes.clock(t) ?: "--:--", style = MaterialTheme.typography.titleMedium, fontWeight = if (p == upcoming) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        if (platform.canNotify) Panel {
            Text("Notify me at", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Prayer.entries.filter { it != Prayer.Sunrise }.forEach { p ->
                    var on by remember { mutableStateOf(prefs.get("notify.${p.name}") == "1") }
                    FilterChip(on, {
                        val turnOn = !on
                        if (turnOn) platform.ensureNotifications { ok -> if (ok) { on = true; prefs.set("notify.${p.name}", "1"); platform.reschedule() } }
                        else { on = false; prefs.set("notify.${p.name}", "0"); platform.reschedule() }
                    }, { Text(p.label) })
                }
            }
            Text("A notification at the time of each prayer you pick.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        val qibla = PrayerTimes.qibla(lat, lon)
        Panel {
            Text("Qibla", style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("↑", Modifier.rotate(qibla.toFloat()).padding(end = 16.dp), style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
                Column {
                    Text("${qibla.toInt()}° from true north", style = MaterialTheme.typography.titleMedium)
                    Text("The arrow points the way when the top of the phone faces north.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Panel {
            Text("Calculation", style = MaterialTheme.typography.titleSmall)
            Method.entries.forEach { m ->
                FilterChip(method == m, { method = m; prefs.set("method", m.name); platform.reschedule() }, { Text(m.label) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AsrFactor.entries.forEach { a -> FilterChip(asr == a, { asr = a; prefs.set("asr", a.name); platform.reschedule() }, { Text("Asr: ${a.label}") }) }
            }
        }

        Text("Falah $APP_VERSION  ·  by Zimam  ·  MIT licence", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun Panel(content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    }
}
