package dev.falah.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import dev.falah.core.I18n
import dev.falah.core.Lang
import dev.falah.core.Method
import dev.falah.core.AsrFactor
import dev.falah.core.Dhikr
import dev.falah.core.Prayer
import dev.falah.core.Accent
import dev.falah.core.Density
import dev.falah.core.Str
import dev.falah.core.TextSize
import dev.falah.core.title

/** The language and digits the screens use right now. */
class AppLocale(val lang: Lang, val arabicDigits: Boolean)

val LocalAppLocale = staticCompositionLocalOf { AppLocale(Lang.En, false) }

/** The device's language code, for the "System" language choice. */
expect fun systemLanguage(): String

@Composable fun lang(): Lang = LocalAppLocale.current.lang

/** The text in the current language, with the digits in the chosen style. */
@Composable fun Str.t(vararg args: Any?): String {
    val l = LocalAppLocale.current
    return I18n.digits(text(l.lang, *args), l.arabicDigits)
}

/** Same, but digits are left as they are: for text that holds a file path. */
@Composable fun Str.raw(vararg args: Any?): String = text(LocalAppLocale.current.lang, *args)

/** A number or time shown with the chosen digits. */
@Composable fun num(value: Any?): String = I18n.digits(value.toString(), LocalAppLocale.current.arabicDigits)

@Composable fun Prayer.shown(): String = title(lang())
/** The other-language name shown beneath a prayer: Arabic under the English name, nothing in the Arabic interface. */
@Composable fun Prayer.second(): String? = if (lang() == Lang.En) arabic else null
@Composable fun Method.shown(): String = title(lang())
@Composable fun AsrFactor.shown(): String = title(lang())
@Composable fun Dhikr.shown(): String = title(lang())
@Composable fun Place.shown(): String = if (lang() == Lang.Ar) arabic else name
@Composable fun placeShown(name: String): String = Places.firstOrNull { it.name == name }?.shown() ?: when (name) { Str.GpsPlaceName.en -> Str.GpsPlaceName.t(); Str.CustomPlaceDefaultName.en -> Str.CustomPlaceDefaultName.t(); else -> name }

fun Accent.label() = when (this) { Accent.Orange -> Str.AccentOrange; Accent.Emerald -> Str.AccentEmerald; Accent.Sapphire -> Str.AccentSapphire; Accent.Rose -> Str.AccentRose; Accent.Violet -> Str.AccentViolet; Accent.Gold -> Str.AccentGold }
fun TextSize.label() = when (this) { TextSize.Small -> Str.TextSmall; TextSize.Normal -> Str.TextNormal; TextSize.Large -> Str.TextLarge; TextSize.Larger -> Str.TextLarger }
fun Density.label() = if (this == Density.Comfortable) Str.DensityComfortable else Str.DensityCompact

/** "01:43:20", with the chosen digits. */
@Composable fun countdownText(c: dev.falah.core.Countdown): String = num(c.text())
