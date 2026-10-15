package dev.falah.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.falah.core.Prayer
import dev.falah.core.PrayerTimes
import dev.falah.core.Sounds
import dev.falah.core.Str
import dev.falah.core.loggedPrayers
import dev.falah.core.of

/** Set by the app frame when "Open a screen for each prayer" is on; rows call it with the prayer that was tapped. */
val LocalPrayerOpener = compositionLocalOf<((Prayer) -> Unit)?> { null }

/** A prayer's own page: its time, the log state, reminder and sound, how the time is calculated, and the last 30 days. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrayerScreen(s: FalahState, prayer: Prayer) = Page {
    val time = s.times.of(prayer)
    val clock = num(PrayerTimes.clock(time) ?: "--:--")
    FalahCard(container = MaterialTheme.colorScheme.primaryContainer, diamond = true, glowing = true) {
        Text(prayer.shown(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        prayer.second()?.let { Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)) }
        Text(clock, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        val isNext = !s.next.tomorrow && s.next.prayer == prayer
        Text(
            when { isNext -> Str.PrayerNextIn.t(countdownText(s.countdown)); time != null && time <= s.hourNow -> Str.PrayerPassedAt.t(clock); else -> Str.PrayerTimeToday.t(clock) },
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
    if (prayer in loggedPrayers) FalahCard {
        ToggleRow(Str.PrayerLogState.t(), s.prayed(prayer)) { s.mark(prayer, it) }
    }
    if (s.platform.canNotify && prayer != Prayer.Sunrise) FalahCard {
        SectionTitle(Icons.Rounded.Alarm, Str.PrayerReminder.t())
        ToggleRow(Str.PrayerReminder.t(), s.notifyOn(prayer)) { s.setNotify(prayer, it) }
        if (s.notifyOn(prayer)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(0, 5, 10, 15, 30).forEach { m -> FilterChip(s.offsetOf(prayer) == m, { s.setOffset(prayer, m) }, { Text(if (m == 0) Str.AtTheTime.t() else Str.MinBefore.t(m)) }) } }
            SoundChoice(s, prayer)
        }
    }
    FalahCard {
        SectionTitle(Icons.Rounded.Info, Str.PrayerHowCalculated.t())
        CalculationNotes(s, prayer).forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
    if (prayer in loggedPrayers) FalahCard {
        val history = s.log.history(prayer, s.today)
        SectionTitle(Icons.Rounded.History, Str.PrayerHistory.t(), Str.PrayerHistoryCount.t(history.count { it.second }, history.size))
        val streak = s.log.streakOf(prayer, s.today)
        if (streak > 0) Text(Str.PrayerStreakLine.t(streak), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            history.reversed().forEach { (_, done) ->
                Box(Modifier.size(14.dp).background(if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape))
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, on: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = LocalSpacing.current.rowMinHeight), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(on, change)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SoundChoice(s: FalahState, prayer: Prayer) {
    val current = s.settings.soundOf(prayer)
    val custom = current.startsWith("content://") || current.startsWith("file://")
    SectionTitle(Icons.Rounded.MusicNote, Str.PrayerSound.t())
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(current == Sounds.DEFAULT, { s.setSound(prayer, Sounds.DEFAULT) }, { Text(Str.SoundDefault.t()) })
        FilterChip(current == Sounds.ALARM, { s.setSound(prayer, Sounds.ALARM) }, { Text(Str.SoundAlarm.t()) })
        FilterChip(current == Sounds.SILENT, { s.setSound(prayer, Sounds.SILENT) }, { Text(Str.SoundSilent.t()) })
        if (s.platform.canPickSound) FilterChip(custom, { s.platform.pickSound(current) { s.setSound(prayer, it) } }, { Text(if (custom) s.platform.soundTitle(current) ?: Str.SoundPick.t() else Str.SoundPick.t()) })
    }
    Text(Str.SoundNote.t(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun CalculationNotes(s: FalahState, prayer: Prayer): List<String> = buildList {
    add(Str.CalcMethodLine.t(s.method.shown()))
    add(Str.CalcPlaceLine.t(placeShown(s.placeName), "%.3f".format(s.lat), "%.3f".format(s.lon)))
    add(when (prayer) {
        Prayer.Fajr -> Str.CalcFajrLine.t(s.method.fajrAngle)
        Prayer.Sunrise -> Str.CalcSunriseLine.t()
        Prayer.Dhuhr -> Str.CalcDhuhrLine.t()
        Prayer.Asr -> Str.CalcAsrLine.t(s.asr.factor.toInt())
        Prayer.Maghrib -> Str.CalcMaghribLine.t()
        Prayer.Isha -> s.method.ishaAfterMaghribMinutes?.let { Str.CalcIshaFixedLine.t(it) } ?: Str.CalcIshaAngleLine.t(s.method.ishaAngle)
    })
    s.settings.adjust[prayer]?.let { add(Str.CalcAdjustedLine.t(if (it > 0) "+$it" else "$it")) }
}
