package dev.falah.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The small orange diamond from the icon, used as a quiet signature on cards. */
@Composable
fun Diamond(modifier: Modifier = Modifier, size: Dp = 9.dp) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier.size(size)) { rotate(45f) { drawRoundRect(color, cornerRadius = CornerRadius(this.size.width * 0.18f)) } }
}

/** A soft orange glow behind an area, like the sun behind the dome in the icon. */
fun Modifier.glow(color: Color, strength: Float = 0.30f): Modifier = drawBehind {
    drawRect(Brush.radialGradient(listOf(color.copy(alpha = strength), Color.Transparent), center = Offset(size.width * 0.78f, size.height * 0.15f), radius = size.maxDimension * 0.85f))
}

/** The one card look used everywhere: large rounded corners, a hairline border, softly raised, the same padding. */
@Composable
fun FalahCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    container: Color = MaterialTheme.colorScheme.surfaceContainer,
    diamond: Boolean = false,
    glowing: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = container)
    val elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    val shape = MaterialTheme.shapes.large
    val body: @Composable ColumnScope.() -> Unit = {
        Box(if (glowing) Modifier.glow(MaterialTheme.colorScheme.primary) else Modifier) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
            if (diamond) Diamond(Modifier.align(Alignment.TopEnd).padding(14.dp))
        }
    }
    if (onClick != null) Card(onClick, modifier.fillMaxWidth(), shape = shape, colors = colors, elevation = elevation, border = border, content = body)
    else Card(modifier.fillMaxWidth(), shape = shape, colors = colors, elevation = elevation, border = border, content = body)
}

@Composable
fun SectionTitle(icon: ImageVector, text: String, trailing: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(30.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        trailing?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

/** A ring that fills as [progress] goes from 0 to 1, with [content] in the middle. */
@Composable
fun Ring(progress: Float, size: Dp, stroke: Dp, track: Color, fill: Color, content: @Composable () -> Unit = {}) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val w = stroke.toPx()
            val inset = w / 2
            val arc = Size(this.size.width - w, this.size.height - w)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(w))
            if (progress > 0f) drawArc(fill, -90f, 360f * progress.coerceIn(0f, 1f), false, Offset(inset, inset), arc, style = Stroke(w, cap = StrokeCap.Round))
        }
        content()
    }
}

@Composable
fun ActionTile(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FalahCard(modifier, onClick) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

fun hoursAndMinutes(minutes: Int): String = if (minutes >= 60) "${minutes / 60} h ${minutes % 60} min" else "$minutes min"

