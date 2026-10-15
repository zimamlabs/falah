package dev.falah.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import dev.falah.core.AccentColors
import dev.falah.core.AppSettings
import dev.falah.core.Palette
import dev.falah.core.ThemeMode

private fun baseLight(c: AccentColors) = lightColorScheme(
    primary = Color(0xFF000000 or c.light.toLong()), onPrimary = Color(0xFF000000 or c.onLight.toLong()), primaryContainer = Color(0xFF000000 or c.light.toLong()).copy(alpha = 0.16f).compositeOver(Color(0xFFFFF8F0)), onPrimaryContainer = Color(0xFF3A1200),
    secondary = Color(0xFF6B5B4B), background = Color(0xFFFFF8F0), surface = Color(0xFFFFF8F0), surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF6ECDF), onSurface = Color(0xFF1A1613), onSurfaceVariant = Color(0xFF6A6258), outlineVariant = Color(0xFFE5D9C8),
)
private fun baseDark(c: AccentColors) = darkColorScheme(
    primary = Color(0xFF000000 or c.dark.toLong()), onPrimary = Color(0xFF000000 or c.onDark.toLong()), primaryContainer = Color(0xFF000000 or c.dark.toLong()).copy(alpha = 0.22f).compositeOver(Color(0xFF1A1613)), onPrimaryContainer = Color(0xFFFFE3D6),
    secondary = Color(0xFFD9CDB8), background = Color(0xFF0F0F0F), surface = Color(0xFF0F0F0F), surfaceContainer = Color(0xFF1A1613),
    surfaceContainerHigh = Color(0xFF241E19), onSurface = Color(0xFFF5F1E8), onSurfaceVariant = Color(0xFFAFA79A), outlineVariant = Color(0xFF3A332C),
)

/** Gaps used by every screen; [dev.falah.core.Density.Comfortable] is the default and [dev.falah.core.Density.Compact] tightens them. */
data class Spacing(val gutter: Dp, val cardGap: Dp, val cardPadding: Dp, val rowMinHeight: Dp)

fun dev.falah.core.Density.spacing() = when (this) {
    dev.falah.core.Density.Comfortable -> Spacing(gutter = 18.dp, cardGap = 16.dp, cardPadding = 18.dp, rowMinHeight = 56.dp)
    dev.falah.core.Density.Compact -> Spacing(gutter = 14.dp, cardGap = 10.dp, cardPadding = 14.dp, rowMinHeight = 48.dp)
}

val LocalSpacing = compositionLocalOf { dev.falah.core.Density.Comfortable.spacing() }

@Composable
fun FalahTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.theme) { ThemeMode.Dark -> true; ThemeMode.Light -> false; ThemeMode.System -> isSystemInDarkTheme() }
    val colors = Palette.accents.getValue(settings.accent)
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * settings.textSize.scale),
        LocalSpacing provides settings.density.spacing(),
    ) {
        MaterialTheme(
            colorScheme = if (dark) baseDark(colors) else baseLight(colors),
            shapes = Shapes(small = RoundedCornerShape(14.dp), medium = RoundedCornerShape(22.dp), large = RoundedCornerShape(28.dp)),
            content = content,
        )
    }
}
