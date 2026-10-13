package dev.falah.app

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.io.File
import java.util.Properties

fun main() = application {
    val file = File(System.getProperty("user.home"), ".falah/settings.properties").also { it.parentFile.mkdirs() }
    val props = Properties().apply { if (file.exists()) file.inputStream().use { load(it) } }
    val prefs = object : Prefs {
        override fun get(key: String): String? = props.getProperty(key)
        override fun set(key: String, value: String) { props.setProperty(key, value); file.outputStream().use { props.store(it, null) } }
    }
    Window(onCloseRequest = ::exitApplication, title = "Falah", state = rememberWindowState(width = 460.dp, height = 820.dp)) { FalahApp(prefs) }
}
