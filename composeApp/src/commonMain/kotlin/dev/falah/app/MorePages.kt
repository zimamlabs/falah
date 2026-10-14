package dev.falah.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.falah.core.Compass
import dev.falah.core.MonthGrid
import dev.falah.core.Prayer
import dev.falah.core.PrayerTimes
import dev.falah.core.Timetable
import dev.falah.core.dhikrs
import dev.falah.core.loggedPrayers
import dev.falah.core.of
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

private val monthNames = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

@Composable
private fun MonthHeader(year: Int, month: Int, change: (Int, Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton({ MonthGrid.previous(year, month).let { change(it.first, it.second) } }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Previous month") }
        Text("${monthNames[month - 1]} $year", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        IconButton({ MonthGrid.next(year, month).let { change(it.first, it.second) } }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Next month") }
    }
}

/** Times: today's list, or the whole month as a timetable. */
@Composable
fun TimesTab(s: FalahState) {
    var month by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            SegmentedButton(!month, { month = false }, SegmentedButtonDefaults.itemShape(0, 2)) { Text("Today") }
            SegmentedButton(month, { month = true }, SegmentedButtonDefaults.itemShape(1, 2)) { Text("Month") }
        }
        Box(Modifier.weight(1f)) { if (month) TimetablePage(s) else TimesPage(s) }
    }
}

@Composable
fun TimetablePage(s: FalahState) {
    var ym by remember { mutableStateOf(s.today.year to s.today.monthNumber) }
    val rows = remember(ym, s.lat, s.lon, s.method, s.asr) { Timetable.month(ym.first, ym.second, TimeZone.currentSystemDefault(), s.lat, s.lon, s.method, s.asr) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MonthHeader(ym.first, ym.second) { y, m -> ym = y to m }
        PlacePicker(s)
        FalahCard(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth()) {
                Text("Day", Modifier.weight(0.8f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Prayer.entries.forEach { Text(it.label.take(5), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
            }
            Column(Modifier.verticalScroll(rememberScrollState())) {
                rows.forEach { r ->
                    val isToday = r.date == s.today
                    Row(Modifier.fillMaxWidth().then(if (isToday) Modifier.background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small) else Modifier).padding(vertical = 5.dp)) {
                        Text("${r.date.dayOfMonth} ${r.date.weekdayName().take(3)}", Modifier.weight(0.8f), style = MaterialTheme.typography.bodySmall, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                        Prayer.entries.forEach { Text(PrayerTimes.clock(r.times.of(it)) ?: "--", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center) }
                    }
                }
            }
        }
    }
}

/** The history of prayers by month: a calendar of rings, totals and a day you can open and edit. */
@Composable
fun LogPage(s: FalahState) {
    var ym by remember { mutableStateOf(s.today.year to s.today.monthNumber) }
    var open by remember { mutableStateOf<LocalDate?>(s.today) }
    val (prayed, possible) = s.log.monthTotals(ym.first, ym.second, s.today)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Prayer log", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        FalahCard {
            MonthHeader(ym.first, ym.second) { y, m -> ym = y to m }
            Row(Modifier.fillMaxWidth()) { listOf("M", "T", "W", "T", "F", "S", "S").forEach { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            MonthGrid.cells(ym.first, ym.second).chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    week.forEach { day ->
                        Box(Modifier.weight(1f).padding(2.dp).aspectRatio(1f), contentAlignment = Alignment.Center) {
                            if (day != null && day <= s.today) {
                                val n = s.log.count(day)
                                Box(Modifier.fillMaxSize().clickable { open = day }.then(if (day == open) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier), contentAlignment = Alignment.Center) {
                                    Ring(n / 5f, 34.dp, 4.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.primary) {
                                        Text("${day.dayOfMonth}", style = MaterialTheme.typography.labelSmall, fontWeight = if (day == s.today) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            } else if (day != null) Text("${day.dayOfMonth}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
        FalahCard {
            SectionTitle(Icons.Rounded.CalendarMonth, "This month", if (possible > 0) "${prayed * 100 / possible}%" else "")
            Text("$prayed of $possible prayers prayed", style = MaterialTheme.typography.bodyLarge)
            Text("Streak ${s.log.streak(s.today)} days  ·  best ${s.log.bestStreak()}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        open?.let { day ->
            FalahCard {
                SectionTitle(Icons.Rounded.CheckCircle, day.longText())
                loggedPrayers.forEach { p ->
                    val on = p in s.log.done(day)
                    Row(Modifier.fillMaxWidth().clickable { s.markOn(day, p, !on) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(p.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Icon(if (on) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null, tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun TasbihPage(s: FalahState) {
    var selected by remember { mutableStateOf(dhikrs.first()) }
    var round by remember { mutableStateOf(0) }
    var confirmReset by remember { mutableStateOf(false) }
    val todayCount = s.tasbih.on(s.today, selected.key)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Tasbih", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            dhikrs.forEach { d -> FilterChip(selected == d, { selected = d; round = 0 }, { Text(d.label) }) }
        }
        FalahCard {
            Text(selected.arabic, Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Ring(round / selected.target.toFloat(), 220.dp, 12.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.primary) {
                    Box(Modifier.size(180.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape).clickable { s.tap(selected.key); round = if (round + 1 >= selected.target) 0 else round + 1 }, contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$round", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("of ${selected.target}  ·  tap", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Today ${todayCount}  ·  all time ${s.tasbih.total(selected.key)}", style = MaterialTheme.typography.bodyMedium)
                TextButton({ confirmReset = true }) { Icon(Icons.Rounded.Refresh, null, Modifier.size(16.dp)); Text(" Restart round") }
            }
        }
        FalahCard {
            Text("Today: ${s.tasbih.dayTotal(s.today)} in total  ·  all time: ${s.tasbih.grandTotal()}", style = MaterialTheme.typography.bodyMedium)
            Text("Every tap is saved to tasbih.csv in your data folder.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("Restart this round?") },
        text = { Text("The round goes back to 0. The totals already saved are not changed.") },
        confirmButton = { TextButton({ confirmReset = false; round = 0 }) { Text("Restart") } },
        dismissButton = { TextButton({ confirmReset = false }) { Text("Cancel") } },
    )
}

/** Qibla with a live compass when the phone has one; otherwise the fixed bearing from north. */
@Composable
fun CompassPage(s: FalahState, resumed: Int) {
    val qibla = PrayerTimes.qibla(s.lat, s.lon)
    val hasCompass = s.platform.hasCompass
    var heading by remember { mutableStateOf<Double?>(null) }
    var accuracy by remember { mutableStateOf(3) }
    DisposableEffect(resumed, hasCompass, s.lat, s.lon) {
        if (hasCompass) s.platform.startCompass(s.lat, s.lon) { h, a -> heading = Compass.smooth(heading, h); accuracy = a }
        onDispose { s.platform.stopCompass() }
    }
    val relative = heading?.let { Compass.relative(qibla, it) }
    val aligned = relative?.let { Compass.aligned(it) } == true
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Qibla", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        PlacePicker(s)
        FalahCard {
            SectionTitle(Icons.Rounded.Explore, "Direction of the Kaaba", "${qibla.toInt()}° from true north")
            Text("N", Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Ring(1f, 240.dp, 6.dp, if (aligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, if (aligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant) {
                    // With a compass the whole dial turns with the phone and the arrow shows how far to turn; without one the arrow is fixed against north.
                    Text("↑", Modifier.rotate((relative ?: qibla).toFloat()), style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            when {
                !hasCompass -> Text("This phone has no compass sensor, so the arrow shows the bearing from north: face north and turn to the arrow.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                heading == null -> Text("Waiting for the compass. Hold the phone flat.", style = MaterialTheme.typography.bodyMedium)
                aligned -> Text("You are facing the Qibla.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                else -> Text("Turn ${relative!!.toInt().let { if (it > 180) "left ${360 - it}" else "right $it" }}° to face the Qibla. Hold the phone flat.", style = MaterialTheme.typography.bodyMedium)
            }
            if (hasCompass && accuracy < 2) Text("The compass needs calibrating: move the phone in a figure 8, and keep it away from magnets and metal.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
