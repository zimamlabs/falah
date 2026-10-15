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
import dev.falah.core.ThemeMode

private val Light = lightColorScheme(
    primary = Color(0xFFFF5A1F), onPrimary = Color(0xFF1A0A02), primaryContainer = Color(0xFFFFE3D6), onPrimaryContainer = Color(0xFF3A1200),
    secondary = Color(0xFF6B5B4B), background = Color(0xFFFFF8F0), surface = Color(0xFFFFF8F0), surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF6ECDF), onSurface = Color(0xFF1A1613), onSurfaceVariant = Color(0xFF6A6258), outlineVariant = Color(0xFFE5D9C8),
)
private val Dark = darkColorScheme(
    primary = Color(0xFFFF5A1F), onPrimary = Color(0xFF1A0A02), primaryContainer = Color(0xFF3A1A0C), onPrimaryContainer = Color(0xFFFFE3D6),
    secondary = Color(0xFFD9CDB8), background = Color(0xFF0F0F0F), surface = Color(0xFF0F0F0F), surfaceContainer = Color(0xFF1A1613),
    surfaceContainerHigh = Color(0xFF241E19), onSurface = Color(0xFFF5F1E8), onSurfaceVariant = Color(0xFFAFA79A), outlineVariant = Color(0xFF3A332C),
)

@Composable
fun FalahTheme(mode: ThemeMode = ThemeMode.System, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.Dark -> true; ThemeMode.Light -> false; ThemeMode.System -> isSystemInDarkTheme() }
    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        shapes = Shapes(small = RoundedCornerShape(14.dp), medium = RoundedCornerShape(22.dp), large = RoundedCornerShape(28.dp)),
        content = content,
    )
}
