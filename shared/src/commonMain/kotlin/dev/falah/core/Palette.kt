package dev.falah.core

import kotlin.math.pow

/** The accent colours of one palette as 0xRRGGBB: the colour on dark surfaces, on light surfaces, and the text colour on top of each. */
data class AccentColors(val dark: Int, val onDark: Int, val light: Int, val onLight: Int)

object Palette {
    private const val INK = 0x1A0A02
    private const val WHITE = 0xFFFFFF

    /** Each accent defined once. Orange on dark is the Zimam orange; on light it is a little deeper so it stays readable on ivory. */
    val accents: Map<Accent, AccentColors> = mapOf(
        Accent.Orange to AccentColors(0xFF5A1F, INK, 0xE04A10, WHITE),
        Accent.Emerald to AccentColors(0x2FBF8F, INK, 0x0E7C5A, WHITE),
        Accent.Sapphire to AccentColors(0x5B9BFF, INK, 0x1F5FD0, WHITE),
        Accent.Rose to AccentColors(0xFF6B8A, INK, 0xC8305A, WHITE),
        Accent.Violet to AccentColors(0xA98BFF, INK, 0x6A45D6, WHITE),
        Accent.Gold to AccentColors(0xE8B339, INK, 0x8F6A00, WHITE),
    )

    const val DARK_BACKGROUND = 0x0F0F0F
    const val LIGHT_BACKGROUND = 0xFFF8F0

    private fun channel(v: Int): Double { val c = v / 255.0; return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4) }
    private fun luminance(rgb: Int) = 0.2126 * channel(rgb shr 16 and 0xFF) + 0.7152 * channel(rgb shr 8 and 0xFF) + 0.0722 * channel(rgb and 0xFF)

    /** WCAG contrast ratio between two 0xRRGGBB colours, 1 to 21. */
    fun contrast(a: Int, b: Int): Double {
        val (hi, lo) = luminance(a).let { x -> luminance(b).let { y -> if (x >= y) x to y else y to x } }
        return (hi + 0.05) / (lo + 0.05)
    }
}
