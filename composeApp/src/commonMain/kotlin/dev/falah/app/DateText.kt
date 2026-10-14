package dev.falah.app

import androidx.compose.runtime.Composable
import dev.falah.core.Lang
import dev.falah.core.longText
import dev.falah.core.weekdayLettersAr
import dev.falah.core.weekdayLettersEn
import dev.falah.core.weekdayName
import kotlinx.datetime.LocalDate

@Composable fun LocalDate.longText(): String = num(longText(lang()))
@Composable fun LocalDate.weekdayName(): String = weekdayName(lang())
@Composable fun LocalDate.weekdayLetter(): String = (if (lang() == Lang.Ar) weekdayLettersAr else weekdayLettersEn)[dayOfWeek.ordinal]
