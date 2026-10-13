package dev.falah.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val store = getSharedPreferences("falah", Context.MODE_PRIVATE)
        val prefs = object : Prefs {
            override fun get(key: String): String? = store.getString(key, null)
            override fun set(key: String, value: String) { store.edit().putString(key, value).apply() }
        }
        setContent { FalahApp(prefs) }
    }
}
