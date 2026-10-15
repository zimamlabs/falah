package dev.falah.app

import dev.falah.core.AppSettings
import dev.falah.core.SettingsCodec

/** Reads and writes [AppSettings] through the platform key-value store; only keys whose value changed are written. */
class SettingsRepository(private val prefs: Prefs) {
    fun load(): AppSettings = SettingsCodec.decode(prefs.all())

    fun save(old: AppSettings, new: AppSettings) {
        val before = SettingsCodec.encode(old); val after = SettingsCodec.encode(new)
        after.forEach { (k, v) -> if (before[k] != v) prefs.set(k, v) }
    }
}
