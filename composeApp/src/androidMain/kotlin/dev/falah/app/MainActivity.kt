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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dev.falah.core.Compass
import dev.falah.core.LocationFix
import dev.falah.core.NoticeRoute
import dev.falah.core.PrayerNotice
import dev.falah.core.ReminderProblem
import okio.FileSystem
import okio.Path.Companion.toPath

class MainActivity : ComponentActivity() {
    private var pending: ((Boolean) -> Unit)? = null
    private var resumed by mutableIntStateOf(0)
    private var inFront by mutableStateOf(false)
    private var notice by mutableStateOf<PrayerNotice?>(null)
    private var soundDone: ((String) -> Unit)? = null
    private val soundPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.let { if (Build.VERSION.SDK_INT >= 33) it.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java) else @Suppress("DEPRECATION") it.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI) }
            soundDone?.invoke(uri?.toString() ?: dev.falah.core.Sounds.SILENT)
        }
        soundDone = null
    }
    private var locationDone: ((LocationFix) -> Unit)? = null
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) findPosition() else finishLocation(LocationFix.Denied)
    }

    private fun finishLocation(fix: LocationFix) { locationDone?.invoke(fix); locationDone = null }

    /** One position, from a recent stored fix when there is one, otherwise a single fresh update with a 30 second limit. */
    @android.annotation.SuppressLint("MissingPermission")
    private fun findPosition() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val providers = (if (fine) listOf(android.location.LocationManager.GPS_PROVIDER) else emptyList()) + android.location.LocationManager.NETWORK_PROVIDER
        val enabled = providers.filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        val stored = (providers + android.location.LocationManager.PASSIVE_PROVIDER).mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
        if (stored != null && System.currentTimeMillis() - stored.time < 10 * 60_000L) { finishLocation(LocationFix.Found(stored.latitude, stored.longitude)); return }
        if (enabled.isEmpty()) { finishLocation(stored?.let { LocationFix.Found(it.latitude, it.longitude) } ?: LocationFix.Unavailable); return }
        val handler = android.os.Handler(mainLooper)
        var finished = false
        fun done(fix: LocationFix) { if (!finished) { finished = true; handler.removeCallbacksAndMessages(null); finishLocation(fix) } }
        handler.postDelayed({ done(stored?.let { LocationFix.Found(it.latitude, it.longitude) } ?: LocationFix.TimedOut) }, 30_000)
        val provider = enabled.first()
        runCatching {
            if (Build.VERSION.SDK_INT >= 30) lm.getCurrentLocation(provider, null, mainExecutor) { loc -> done(loc?.let { LocationFix.Found(it.latitude, it.longitude) } ?: LocationFix.TimedOut) }
            else @Suppress("DEPRECATION") lm.requestSingleUpdate(provider, object : android.location.LocationListener {
                override fun onLocationChanged(location: android.location.Location) = done(LocationFix.Found(location.latitude, location.longitude))
                @Deprecated("old callback") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }, mainLooper)
        }.onFailure { done(LocationFix.Unavailable) }
    }

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
            override fun snooze(prayer: dev.falah.core.Prayer, minutes: Int, prayerAtMillis: Long) = PrayerAlarms.snooze(this@MainActivity, prayer, minutes, prayerAtMillis)
            override fun dismissNotification(prayer: dev.falah.core.Prayer) = (getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager).cancel(prayer.ordinal)
            override fun reminderStatus() = PrayerAlarms.status(this@MainActivity)
            override fun openReminderSetting(problem: ReminderProblem) {
                val pkg = Uri.parse("package:$packageName")
                val intent = when (problem) {
                    ReminderProblem.NotificationsOff -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    ReminderProblem.ExactAlarmsOff -> if (Build.VERSION.SDK_INT >= 31) Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg) else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)
                    ReminderProblem.FullScreenOff -> if (Build.VERSION.SDK_INT >= 34) Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg) else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)
                    ReminderProblem.BatteryOptimised -> Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                }
                runCatching { startActivity(intent) }.onFailure { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)) }
            }
            override val canLocate = true
            override fun requestLocation(done: (LocationFix) -> Unit) {
                locationDone = done
                val granted = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any { ContextCompat.checkSelfPermission(this@MainActivity, it) == PackageManager.PERMISSION_GRANTED }
                if (granted) findPosition() else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
            override val canPickSound = true
            override fun pickSound(current: String, done: (String) -> Unit) {
                soundDone = done
                val existing = current.takeIf { it.startsWith("content://") || it.startsWith("file://") }?.let(Uri::parse)
                soundPicker.launch(Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER)
                    .putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_NOTIFICATION or android.media.RingtoneManager.TYPE_ALARM)
                    .putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing)
                    .putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true))
            }
            override fun soundTitle(value: String): String? = runCatching { android.media.RingtoneManager.getRingtone(this@MainActivity, Uri.parse(value)).getTitle(this@MainActivity) }.getOrNull()
            private var listener: SensorEventListener? = null
            private val sensors get() = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            override val hasCompass: Boolean get() = sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
            override fun startCompass(latitude: Double, longitude: Double, onHeading: (Double, Int) -> Unit) {
                stopCompass()
                val sensor = sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) ?: return
                val declination = GeomagneticField(latitude.toFloat(), longitude.toFloat(), 0f, System.currentTimeMillis()).declination.toDouble()
                val matrix = FloatArray(9); val orientation = FloatArray(3)
                listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        SensorManager.getRotationMatrixFromVector(matrix, event.values)
                        SensorManager.getOrientation(matrix, orientation)
                        onHeading(Compass.trueHeading(Math.toDegrees(orientation[0].toDouble()), declination), event.accuracy)
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }.also { sensors.registerListener(it, sensor, SensorManager.SENSOR_DELAY_UI) }
            }
            override fun stopCompass() { listener?.let { sensors.unregisterListener(it) }; listener = null }
        }
        platform.reschedule()
        handle(intent)
        setContent { FalahApp(mirrored, platform, files, resumed, active = inFront, notice = notice, onNoticeDone = { closeNotice() }) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handle(intent)
    }

    /** A tap on a prayer notification (or its full-screen intent) opens that prayer's screen, over the lock screen if needed. */
    private fun handle(intent: Intent?) {
        val found = NoticeRoute.parse(intent?.action, intent?.getStringExtra(NoticeRoute.EXTRA_PRAYER), intent?.getLongExtra(NoticeRoute.EXTRA_AT, 0L))
        notice = found ?: return
        (getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager).cancel(found.prayer.ordinal)
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true) }
        else @Suppress("DEPRECATION") window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
    }

    private fun closeNotice() {
        notice = null
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(false); setTurnScreenOn(false) }
    }

    override fun onResume() {
        super.onResume()
        resumed++
        inFront = true
    }

    override fun onPause() {
        inFront = false
        super.onPause()
    }
}
