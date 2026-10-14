package dev.falah.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Ink = Color(0xFF121214)
private val Cream = Color(0xFFF5F1E8)
private val Orange = Color(0xFFFF5A1F)

/** The opening screen: a crescent grows in, the Zimam dot settles beside it, then the name and "by Zimam" fade in. */
@Composable
fun Splash(onDone: () -> Unit) {
    val moon = remember { Animatable(0f) }
    val pop = remember { Animatable(0f) }
    val name = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { moon.animateTo(1f, tween(600)) }
        launch { delay(450); pop.animateTo(1f, tween(300)) }
        launch { delay(650); name.animateTo(1f, tween(380)) }
        delay(1300)
        onDone()
    }
    Column(Modifier.fillMaxSize().background(Ink), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Canvas(Modifier.size(150.dp).graphicsLayer { val k = 0.88f + 0.12f * moon.value; scaleX = k; scaleY = k }) {
            val u = size.width / 512f
            val arch = Path().apply {
                moveTo(150f * u, 400f * u)
                lineTo(150f * u, 250f * u)
                arcTo(Rect(150f * u, 144f * u, 362f * u, 356f * u), 180f, 180f, false)
                lineTo(362f * u, 400f * u)
            }
            drawPath(arch, Cream.copy(alpha = moon.value), style = Stroke(40f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
            zimamDot(Offset(256f * u, 238f * u), 76f * u * pop.value, Orange)
        }
        Spacer(Modifier.height(32.dp))
        Text("Falah", Modifier.alpha(name.value).offset(y = ((1f - name.value) * 14f).dp), color = Cream, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("by Zimam", Modifier.alpha(name.value).offset(y = ((1f - name.value) * 14f).dp), color = Orange, fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
    }
}
