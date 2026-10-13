package dev.falah.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private var pending: ((Boolean) -> Unit)? = null
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> pending?.invoke(granted); pending = null }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val store = getSharedPreferences("falah", Context.MODE_PRIVATE)
        val prefs = object : Prefs {
            override fun get(key: String): String? = store.getString(key, null)
            override fun set(key: String, value: String) { store.edit().putString(key, value).apply() }
        }
        val platform = object : Platform {
            override val canNotify = true
            override fun ensureNotifications(done: (Boolean) -> Unit) {
                if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) done(true)
                else { pending = done; permission.launch(Manifest.permission.POST_NOTIFICATIONS) }
            }
            override fun reschedule() = PrayerAlarms.schedule(this@MainActivity)
        }
        platform.reschedule()
        setContent { FalahApp(prefs, platform) }
    }
}
