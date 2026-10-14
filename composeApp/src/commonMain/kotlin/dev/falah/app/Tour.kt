package dev.falah.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.falah.core.Method
import dev.falah.core.Prayer
import kotlinx.coroutines.launch

/** The first-install walkthrough: what Falah does, your place, notifications, then your data and look. Skip, Next, Done. */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun Tour(s: FalahState, onDone: () -> Unit) {
    val pages = 4
    val pager = rememberPagerState { pages }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onDone) { Text("Skip") } }
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
                when (page) {
                    0 -> {
                        BrandMark(Modifier.size(120.dp))
                        Text("Welcome to Falah", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Prayer times for where you are, the next prayer with a countdown, a log of the prayers you pray, and the qibla. It works offline and sends nothing anywhere.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                    }
                    1 -> {
                        Text("Where are you?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Pick the nearest place and how your prayer times are calculated. You can change both later in Settings.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                        PlacePicker(s)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) { Method.entries.forEach { m -> FilterChip(s.method == m, { s.chooseMethod(m) }, { Text(m.label) }) } }
                    }
                    2 -> {
                        Text("Prayer notifications", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        if (s.platform.canNotify) {
                            Text("Falah can tell you when each prayer comes in. Android will ask for permission the first time you turn one on.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) { Prayer.entries.filter { it != Prayer.Sunrise }.forEach { p -> FilterChip(s.notifyOn(p), { s.setNotify(p, !s.notifyOn(p)) }, { Text(p.label) }) } }
                        } else Text("This system does not show prayer notifications from Falah; the times and the next-prayer countdown work as usual.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                    }
                    else -> {
                        Text("Your data and look", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Your settings and prayer log are plain files. Allow All files access to keep them in a visible Zimam folder that survives reinstalling the app; otherwise they stay inside the app. Nothing is ever deleted when the folder changes.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                        if (s.files.canAskAccess) OutlinedButton({ s.files.askAccess() }) { Text("Allow All files access") }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (k, v) -> FilterChip(s.themeMode == k, { s.chooseTheme(k) }, { Text(v) }) } }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center) {
            repeat(pages) { i -> Box(Modifier.padding(4.dp).size(8.dp).background(if (i == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)) }
        }
        Button({ if (pager.currentPage == pages - 1) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }, Modifier.fillMaxWidth().height(52.dp)) {
            Text(if (pager.currentPage == pages - 1) "Done" else "Next")
        }
        Spacer(Modifier.height(4.dp))
    }
}
