package dev.falah.app

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.graphics.painter.BitmapPainter

@Composable
actual fun BrandMark(modifier: Modifier) {
    val painter = remember { BitmapPainter(useResource("brand_mark.png", ::loadImageBitmap)) }
    Image(painter, "Falah", modifier)
}
