package dev.falah.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.falah.core.Countdown
import dev.falah.core.NoticeClock
import dev.falah.core.Prayer
import dev.falah.core.PrayerNotice
import dev.falah.core.Str
import dev.falah.core.loggedPrayers
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** What opens when a prayer notification is tapped (or comes up by itself): the prayer, a live clock, and four things to do. */
@Composable
fun PrayerNoticeScreen(s: FalahState, notice: PrayerNotice, openQibla: () -> Unit, close: () -> Unit) {
    val prayer = notice.prayer
    val at = Instant.fromEpochMilliseconds(notice.prayerAtMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    var marked by remember(notice) { mutableStateOf(prayer in loggedPrayers && prayer in s.log.done(at.date)) }
    LaunchedEffect(notice) { if (notice.markPrayed && !marked) { s.markOn(at.date, prayer, true); marked = true } }
    val clock = NoticeClock.of(notice.prayerAtMillis, s.now.toEpochMilliseconds())
    val snooze = s.settings.snoozeMinutes

    Column(
        Modifier.fillMaxSize().glow(MaterialTheme.colorScheme.primary, 0.35f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text(Str.NoticeTimeIs.t(prayer.shown()), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Text(prayer.shown(), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        prayer.second()?.let { Text(it, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary) }
        Text(num("%02d:%02d".format(at.hour, at.minute)), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
        Text(if (clock is NoticeClock.Before) Str.NoticeLeft.t() else Str.NoticeSince.t(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(num(Countdown.of(clock.seconds).text()), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, maxLines = 1, softWrap = false)
        Spacer(Modifier.height(12.dp))
        if (prayer in loggedPrayers) {
            if (marked) Row(Icons.Rounded.CheckCircle, Str.NoticeMarked.t())
            else Button({ s.markOn(at.date, prayer, true); marked = true }, Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(Str.NoticePrayed.t()) }
        }
        if (s.platform.canNotify) OutlinedButton({ s.platform.snooze(prayer, snooze, notice.prayerAtMillis); close() }, Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(Str.NoticeSnooze.t(snooze)) }
        TextButton({ s.platform.dismissNotification(prayer); close() }, Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(Str.NoticeSkip.t()) }
        TextButton({ openQibla(); close() }, Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Icon(Icons.Rounded.Explore, null); Text(Str.NoticeQibla.t(), Modifier.padding(start = 8.dp)) }
    }
}

@Composable
private fun Row(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}
