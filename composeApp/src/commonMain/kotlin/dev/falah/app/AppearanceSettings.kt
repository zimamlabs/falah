package dev.falah.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import kotlin.math.roundToInt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.falah.core.Accent
import dev.falah.core.Density
import dev.falah.core.Palette
import dev.falah.core.Str
import dev.falah.core.TextSize
import dev.falah.core.ThemeMode

/** Mode, colour theme, text size with a live preview, spacing and animations. Everything applies at once. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceControls(s: FalahState) {
    val dark = when (s.themeMode) { ThemeMode.Dark -> true; ThemeMode.Light -> false; ThemeMode.System -> androidx.compose.foundation.isSystemInDarkTheme() }
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        val modes = listOf(ThemeMode.System to Str.ThemeSystem, ThemeMode.Light to Str.ThemeLight, ThemeMode.Dark to Str.ThemeDark)
        modes.forEachIndexed { i, (k, v) -> SegmentedButton(s.themeMode == k, { s.chooseTheme(k) }, SegmentedButtonDefaults.itemShape(i, modes.size)) { Text(v.t()) } }
    }
    Label(Str.ColourTheme.t())
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Accent.entries.forEach { a ->
            val c = Palette.accents.getValue(a); val rgb = if (dark) c.dark else c.light
            val color = Color(0xFF000000 or rgb.toLong()); val selected = s.settings.accent == a; val name = a.label().t()
            Box(
                Modifier.size(48.dp).background(color, CircleShape).then(if (selected) Modifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface), CircleShape) else Modifier)
                    .selectable(selected, role = Role.RadioButton, onClick = { s.chooseAccent(a) }).semantics { contentDescription = name },
                contentAlignment = Alignment.Center,
            ) { if (selected) Icon(Icons.Rounded.Check, null, tint = Color(0xFF000000 or (if (dark) c.onDark else c.onLight).toLong())) }
        }
    }
    Label(Str.TextSizeTitle.t())
    val steps = TextSize.entries
    val sliderLabel = Str.TextSizeTitle.t()
    Slider(steps.indexOf(s.settings.textSize).toFloat(), { s.chooseTextSize(steps[it.roundToInt().coerceIn(0, steps.lastIndex)]) }, Modifier.semantics { contentDescription = sliderLabel }, valueRange = 0f..steps.lastIndex.toFloat(), steps = steps.size - 2)
    Text(s.settings.textSize.label().t(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    FalahCard(container = MaterialTheme.colorScheme.surfaceContainerHigh) { Text(Str.TextPreview.t(), style = MaterialTheme.typography.bodyLarge) }
    Label(Str.DensityTitle.t())
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        Density.entries.forEachIndexed { i, d -> SegmentedButton(s.settings.density == d, { s.chooseDensity(d) }, SegmentedButtonDefaults.itemShape(i, Density.entries.size)) { Text(d.label().t()) } }
    }
    Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = LocalSpacing.current.rowMinHeight), verticalAlignment = Alignment.CenterVertically) {
        Text(Str.MotionReduceTitle.t(), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(s.settings.reduceMotion, { s.chooseReduceMotion(it) })
    }
}

@Composable
private fun Label(text: String) = Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
