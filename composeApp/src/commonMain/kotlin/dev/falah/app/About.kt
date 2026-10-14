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
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.falah.core.ReleaseNotes

private const val REPO = "https://github.com/zimamlabs/falah"

@Composable
fun AboutPage(s: FalahState, showTour: () -> Unit) {
    val links = LocalUriHandler.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        FalahCard(glowing = true, diamond = true) {
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BrandMark(Modifier.size(112.dp))
                Text("Falah", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Prayer times, reminders and the qibla", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Version $APP_VERSION", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
        Text(
            "Falah (فلاح, success) shows today's five prayers and the sunrise for where you are, the next prayer with a countdown, a log of the prayers you pray, a monthly timetable, a tasbih counter and the qibla with a live compass. It works offline.",
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        FalahCard(onClick = showTour) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle(Icons.Rounded.PlayCircle, "Show the tour again")
            }
        }
        FalahCard {
            SectionTitle(Icons.Rounded.NewReleases, "What's new in $APP_VERSION")
            ReleaseNotes.forVersion(APP_VERSION).forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium) }
        }
        FalahCard(diamond = true) {
            SectionTitle(Icons.Rounded.Lock, "Private by design")
            Text("No account, no tracking, nothing leaves your phone. Your settings and prayer log stay in ${if (s.files.shared) "the Zimam folder" else "the app"}: ${s.files.location}", style = MaterialTheme.typography.bodyMedium)
        }
        FalahCard {
            SectionTitle(Icons.Rounded.Code, "Links")
            LinkRow(Icons.Rounded.Code, "Repository") { links.openUri(REPO) }
            LinkRow(Icons.Rounded.Download, "Releases") { links.openUri("$REPO/releases") }
            LinkRow(Icons.Rounded.BugReport, "Report an issue") { links.openUri("$REPO/issues") }
            LinkRow(Icons.Rounded.Description, "Licence (MIT)") { links.openUri("$REPO/blob/main/LICENSE") }
        }
        Text("by Zimam Labs", Modifier.fillMaxWidth().padding(bottom = 8.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun LinkRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick, Modifier.fillMaxWidth()) {
        androidx.compose.material3.Icon(icon, null, Modifier.size(18.dp))
        Text("  $label", Modifier.weight(1f), textAlign = TextAlign.Start)
    }
}
