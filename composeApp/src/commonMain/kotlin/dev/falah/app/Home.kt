package dev.falah.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.falah.core.Duas
import dev.falah.core.Prayer
import dev.falah.core.PrayerTimes
import dev.falah.core.ReleaseNotes
import dev.falah.core.loggedPrayers
import dev.falah.core.of

@Composable
fun HomePage(s: FalahState, go: (Tab) -> Unit, whatsNewHidden: Boolean, hideWhatsNew: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Header(s)
        Hero(s)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile(Icons.Rounded.Schedule, "Times", Modifier.weight(1f)) { go(Tab.Times) }
            ActionTile(Icons.Rounded.Explore, "Qibla", Modifier.weight(1f)) { go(Tab.Qibla) }
            ActionTile(Icons.Rounded.TouchApp, "Tasbih", Modifier.weight(1f)) { go(Tab.Tasbih) }
            ActionTile(Icons.Rounded.Settings, "Settings", Modifier.weight(1f)) { go(Tab.Settings) }
        }
        Timeline(s)
        WeekCard(s)
        QiblaMini(s) { go(Tab.Qibla) }
        DuaCard(s)
        if (!whatsNewHidden) WhatsNewCard(hideWhatsNew)
        Text("Falah $APP_VERSION  ·  by Zimam  ·  MIT licence", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
    }
}

@Composable
private fun Header(s: FalahState) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${greeting(s.local.hour)}, السلام عليكم", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(s.placeName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        val h = s.hijri
        Text("${s.today.longText()}  ·  ${h.day} ${h.monthName} ${h.year} AH", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Hero(s: FalahState) {
    val n = s.next
    val left = s.minutesLeft()
    FalahCard(container = MaterialTheme.colorScheme.primaryContainer, diamond = true, glowing = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (n.tomorrow) "NEXT PRAYER, TOMORROW" else "NEXT PRAYER", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                Text(n.prayer.label, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(n.prayer.arabic, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                Text(PrayerTimes.clock(n.at % 24.0) ?: "--:--", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Ring(n.progress, 124.dp, 10.dp, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f), MaterialTheme.colorScheme.primary) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${left / 60}:${(left % 60).toString().padStart(2, '0')}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("left", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun Timeline(s: FalahState) {
    FalahCard {
        SectionTitle(Icons.Rounded.Today, "Today", "${loggedPrayers.count { s.prayed(it) }} of 5 prayed")
        Prayer.entries.forEach { p ->
            val t = s.times.of(p)
            val upcoming = !s.next.tomorrow && p == s.next.prayer
            val past = t != null && t <= s.hourNow
            Row(
                Modifier.fillMaxWidth().then(if (upcoming) Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f), MaterialTheme.shapes.medium) else Modifier).padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(10.dp).then(if (upcoming) Modifier.background(MaterialTheme.colorScheme.primary, CircleShape) else Modifier.border(2.dp, if (past) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.secondary, CircleShape)))
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.label, style = MaterialTheme.typography.bodyLarge, fontWeight = if (upcoming) FontWeight.Bold else FontWeight.Normal, color = if (past && !upcoming) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                    Text(p.arabic, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(PrayerTimes.clock(t) ?: "--:--", style = MaterialTheme.typography.titleMedium, fontWeight = if (upcoming) FontWeight.Bold else FontWeight.Normal)
                if (p == Prayer.Sunrise) Spacer(Modifier.size(48.dp))
                else IconButton({ s.mark(p, !s.prayed(p)) }) {
                    Icon(if (s.prayed(p)) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, if (s.prayed(p)) "Prayed" else "Mark as prayed", tint = if (s.prayed(p)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun WeekCard(s: FalahState) {
    val streak = s.log.streak(s.today)
    FalahCard {
        SectionTitle(Icons.Rounded.LocalFireDepartment, if (streak > 0) "$streak day streak" else "This week", "best ${s.log.bestStreak()}")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            s.log.week(s.today).forEach { (day, count) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Ring(count / 5f, 36.dp, 4.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.primary) {
                        if (count == 5) Icon(Icons.Rounded.CheckCircle, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        else Text("$count", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(day.weekdayLetter(), style = MaterialTheme.typography.labelSmall, fontWeight = if (day == s.today) FontWeight.Bold else FontWeight.Normal, color = if (day == s.today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun QiblaMini(s: FalahState, open: () -> Unit) {
    val qibla = PrayerTimes.qibla(s.lat, s.lon)
    FalahCard(onClick = open) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("↑", Modifier.rotate(qibla.toFloat()).padding(end = 16.dp), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text("Qibla", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("${qibla.toInt()}° from true north", style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Rounded.Explore, null, tint = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun DuaCard(s: FalahState) {
    val dua = Duas.of(s.today)
    FalahCard(diamond = true) {
        SectionTitle(Icons.Rounded.AutoStories, "A reminder for today")
        Text(dua.arabic, Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(dua.english, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Text(dua.source, Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun WhatsNewCard(hide: () -> Unit) {
    FalahCard {
        SectionTitle(Icons.Rounded.NewReleases, "New in $APP_VERSION")
        ReleaseNotes.forVersion(APP_VERSION).forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(hide) { Text("Hide") } }
    }
}

