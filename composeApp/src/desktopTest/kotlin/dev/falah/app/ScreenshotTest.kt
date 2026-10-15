package dev.falah.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Draws screens offscreen into build/screenshots so layout can be looked at without a device, on a normal phone and on
 * a small phone at 130 percent text, in English and Arabic. It also proves each screen composes without throwing.
 */
@OptIn(ExperimentalComposeUiApi::class)
class ScreenshotTest {
    private fun prefs(vararg extra: Pair<String, String>) = MapPrefs(mapOf("tour.done" to "1", "seen.version" to APP_VERSION, "place" to "Cairo", "lat" to "30.0444", "lon" to "31.2357") + extra)

    private fun shot(name: String, width: Int, height: Int, fontScale: Float = 1f, vararg settings: Pair<String, String>) {
        val scene = ImageComposeScene(width * 2, height * 2, Density(2f, fontScale)) { FalahApp(prefs(*settings), files = MemoryFiles(), showSplash = false) }
        try {
            scene.render(0)
            scene.render(4_000_000_000L) // past the opening splash
            val data = scene.render(4_100_000_000L).encodeToData(EncodedImageFormat.PNG)!!
            File("build/screenshots").apply { mkdirs() }.resolve("$name.png").writeBytes(data.bytes)
            assertTrue(data.bytes.size > 1_000)
        } finally { scene.close() }
    }

    @Test fun everySectionRendersOnANormalPhone() {
        for (section in listOf("home", "times", "qibla", "tasbih", "log")) shot("phone-$section", 412, 860, 1f, "start" to section)
    }

    @Test fun everySectionRendersOnASmallPhoneWithLargeText() {
        for (section in listOf("home", "times", "qibla", "tasbih", "log")) shot("small-$section", 360, 640, 1.3f, "start" to section)
    }

    @Test fun arabicRendersRightToLeft() {
        for (section in listOf("home", "times", "log")) shot("ar-$section", 412, 860, 1f, "start" to section, "lang" to "ar")
    }
}
