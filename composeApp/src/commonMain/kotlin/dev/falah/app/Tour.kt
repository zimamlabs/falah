package dev.falah.app

import dev.falah.core.Str
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onDone) { Text(Str.Skip.t()) } }
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
                when (page) {
                    0 -> {
                        BrandMark(Modifier.size(120.dp))
                        Text(Str.TourWelcome.t(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(Str.TourWelcomeBody.t(), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                    }
                    1 -> {
                        Text(Str.TourWhere.t(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(Str.TourWhereBody.t(), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                        PlacePicker(s)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) { Method.entries.forEach { m -> FilterChip(s.method == m, { s.chooseMethod(m) }, { Text(m.shown()) }) } }
                    }
                    2 -> {
                        Text(Str.TourNotifTitle.t(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        if (s.platform.canNotify) {
                            Text(Str.TourNotifBody.t(), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) { Prayer.entries.filter { it != Prayer.Sunrise }.forEach { p -> FilterChip(s.notifyOn(p), { s.setNotify(p, !s.notifyOn(p)) }, { Text(p.shown()) }) } }
                        } else Text(Str.TourNoNotif.t(), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                    }
                    else -> {
                        Text(Str.TourDataTitle.t(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(Str.TourDataBody.t(), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                        if (s.files.canAskAccess) OutlinedButton({ s.files.askAccess() }) { Text(Str.AllowAccess.t()) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("system" to Str.ThemeSystem, "light" to Str.ThemeLight, "dark" to Str.ThemeDark).forEach { (k, v) -> FilterChip(s.themeMode == k, { s.chooseTheme(k) }, { Text(v.t()) }) } }
                        LanguageChips(s)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center) {
            repeat(pages) { i -> Box(Modifier.padding(4.dp).size(8.dp).background(if (i == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)) }
        }
        Button({ if (pager.currentPage == pages - 1) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }, Modifier.fillMaxWidth().height(52.dp)) {
            Text(if (pager.currentPage == pages - 1) Str.Done.t() else Str.Next.t())
        }
        Spacer(Modifier.height(4.dp))
    }
}
