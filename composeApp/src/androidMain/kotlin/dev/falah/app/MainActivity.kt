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
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import okio.FileSystem
import okio.Path.Companion.toPath

class MainActivity : ComponentActivity() {
    private var pending: ((Boolean) -> Unit)? = null
    private var resumed by mutableIntStateOf(0)
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> pending?.invoke(granted); pending = null }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val store = getSharedPreferences("falah", Context.MODE_PRIVATE)
        val prefs = object : Prefs {
            override fun get(key: String): String? = store.getString(key, null)
            override fun set(key: String, value: String) { store.edit().putString(key, value).apply() }
            override fun all(): Map<String, String> = store.all.mapNotNull { (k, v) -> (v as? String)?.let { k to it } }.toMap()
        }
        val files = FolderFiles(
            FileSystem.SYSTEM,
            (filesDir.path + "/Zimam/Falah").toPath(),
            (Environment.getExternalStorageDirectory().path + "/Zimam/Falah").toPath(),
            sharedAllowed = { Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager() },
            canAsk = { Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager() },
            ask = { startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName"))) },
        )
        val mirrored = MirroredPrefs(prefs, files)
        val platform = object : Platform {
            override val canNotify = true
            override fun ensureNotifications(done: (Boolean) -> Unit) {
                if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) done(true)
                else { pending = done; permission.launch(Manifest.permission.POST_NOTIFICATIONS) }
            }
            override fun reschedule() = PrayerAlarms.schedule(this@MainActivity)
        }
        platform.reschedule()
        setContent { FalahApp(mirrored, platform, files, resumed) }
    }

    override fun onResume() {
        super.onResume()
        resumed++
    }
}
