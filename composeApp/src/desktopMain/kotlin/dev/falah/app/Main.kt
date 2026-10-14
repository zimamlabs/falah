package dev.falah.app

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import okio.FileSystem
import okio.Path.Companion.toPath
import java.io.File
import java.util.Properties

fun main() = application {
    val home = System.getProperty("user.home")
    // The old ~/.falah folder is copied into ~/Zimam/Falah and kept as the backup; settings.txt there replaces the old properties file.
    val files = FolderFiles(FileSystem.SYSTEM, "$home/.falah".toPath(), "$home/Zimam/Falah".toPath(), sharedAllowed = { true })
    val legacy = File(home, ".falah/settings.properties")
    val seed = if (files.read(MirroredPrefs.SETTINGS_FILE) == null && legacy.isFile) Properties().apply { legacy.inputStream().use { load(it) } }.stringPropertyNames().associateWith { n -> legacy.let { _ -> Properties().apply { legacy.inputStream().use { load(it) } }.getProperty(n) } } else emptyMap()
    val prefs = MirroredPrefs(MapPrefs(seed), files)
    Window(onCloseRequest = ::exitApplication, title = "Falah", state = rememberWindowState(width = 460.dp, height = 820.dp)) { FalahApp(prefs, files = files) }
}
