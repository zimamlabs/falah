package dev.falah.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.falah.core.ReminderProblem
import dev.falah.core.Str

private fun ReminderProblem.text() = when (this) {
    ReminderProblem.NotificationsOff -> Str.StatusNotifications
    ReminderProblem.ExactAlarmsOff -> Str.StatusExact
    ReminderProblem.FullScreenOff -> Str.StatusFullScreen
    ReminderProblem.BatteryOptimised -> Str.StatusBattery
}

/** The problems found right now; re-read each time the app comes back to the front ([resumed] changes). */
@Composable
private fun problemsNow(s: FalahState, resumed: Int): List<ReminderProblem> {
    val status = remember(resumed, s.settings.notify, s.settings.noticeScreen) { s.platform.reminderStatus() } ?: return emptyList()
    return status.problems(wantsFullScreen = s.settings.noticeFullScreen && s.settings.noticeScreen.isNotEmpty())
}

/** One card, "Reminders may not work", with a row and a button to the right system screen for each problem. Shows nothing when all is well, or when no reminder is on and [onlyWhenUsed]. */
@Composable
fun ReminderStatusCard(s: FalahState, resumed: Int, onlyWhenUsed: Boolean, showOk: Boolean = false) {
    val problems = problemsNow(s, resumed)
    if (onlyWhenUsed && s.settings.notify.isEmpty()) return
    if (problems.isEmpty()) { if (showOk && s.platform.reminderStatus() != null) Text(Str.StatusOk.t(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary); return }
    FalahCard(container = MaterialTheme.colorScheme.errorContainer) {
        SectionTitle(Icons.Rounded.Warning, Str.StatusTitle.t())
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            problems.forEach { p ->
                Text(p.text().t(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                OutlinedButton({ s.platform.openReminderSetting(p) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(Str.StatusOpen.t()) }
            }
        }
    }
}
