package dev.falah.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PaletteTest {
    @Test fun contrastOfBlackAndWhiteIsTwentyOne() = assertEquals(21.0, Palette.contrast(0x000000, 0xFFFFFF), 0.01)

    @Test fun everyAccentHasColoursAndTheyDiffer() {
        assertEquals(Accent.entries.toSet(), Palette.accents.keys)
        assertEquals(Accent.entries.size, Palette.accents.values.map { it.dark }.toSet().size)
    }

    @Test fun everyAccentIsVisibleOnItsSurfaceInBothModes() {
        Palette.accents.forEach { (a, c) ->
            assertTrue(Palette.contrast(c.dark, Palette.DARK_BACKGROUND) >= 3.0, "$a dark on dark background")
            assertTrue(Palette.contrast(c.light, Palette.LIGHT_BACKGROUND) >= 3.0, "$a light on ivory")
        }
    }

    @Test fun textOnTheAccentIsReadableInBothModes() {
        Palette.accents.forEach { (a, c) ->
            assertTrue(Palette.contrast(c.onDark, c.dark) >= 4.0, "$a text on dark accent")
            assertTrue(Palette.contrast(c.onLight, c.light) >= 4.0, "$a text on light accent")
        }
    }
}
