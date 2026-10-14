package dev.falah.app

actual fun systemLanguage(): String = java.util.Locale.getDefault().language
