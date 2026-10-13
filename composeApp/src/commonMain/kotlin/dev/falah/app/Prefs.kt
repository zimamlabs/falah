package dev.falah.app

/** A tiny key-value store the platforms provide (shared preferences on Android, a file on desktop). */
interface Prefs {
    fun get(key: String): String?
    fun set(key: String, value: String)
}
