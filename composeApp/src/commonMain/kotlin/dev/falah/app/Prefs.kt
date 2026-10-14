package dev.falah.app

import dev.falah.core.SettingsFile

/** A tiny key-value store the platforms provide (shared preferences on Android, a file on desktop). */
interface Prefs {
    fun get(key: String): String?
    fun set(key: String, value: String)
    fun all(): Map<String, String>
}

/** Plain in-memory values, for the desktop (which saves them through [MirroredPrefs]) and for tests. */
class MapPrefs(initial: Map<String, String> = emptyMap()) : Prefs {
    private val values = initial.toMutableMap()
    override fun get(key: String) = values[key]
    override fun set(key: String, value: String) { values[key] = value }
    override fun all(): Map<String, String> = values.toMap()
}

/**
 * Keeps a readable copy of every setting in the data folder (`settings.txt`). The copy is what survives a reinstall:
 * when the app starts with no place saved and a copy exists, the copy is loaded.
 */
class MirroredPrefs(private val inner: Prefs, private val files: AppFiles) : Prefs {
    init {
        if (inner.get("place") == null) files.read(SETTINGS_FILE)?.let { text -> SettingsFile.decode(text).forEach { (k, v) -> inner.set(k, v) } }
        mirror()
    }
    override fun get(key: String) = inner.get(key)
    override fun set(key: String, value: String) { inner.set(key, value); mirror() }
    override fun all() = inner.all()
    fun mirror() = files.write(SETTINGS_FILE, SettingsFile.encode(inner.all()))

    companion object { const val SETTINGS_FILE = "settings.txt" }
}
