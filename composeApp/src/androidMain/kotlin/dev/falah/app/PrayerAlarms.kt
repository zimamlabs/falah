package dev.falah.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dev.falah.prayer.R
import dev.falah.core.AsrFactor
import dev.falah.core.Method
import dev.falah.core.Prayer
import dev.falah.core.Schedule
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone

private const val CHANNEL = "prayers"

/** Reads the saved choices (the same keys the screen writes) and sets the alarm for the next chosen prayer. */
object PrayerAlarms {
    fun chosen(prefs: android.content.SharedPreferences): Set<Prayer> =
        Prayer.entries.filter { prefs.getString("notify.${it.name}", null) == "1" }.toSet()

    fun schedule(context: Context) {
        val prefs = context.getSharedPreferences("falah", Context.MODE_PRIVATE)
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = PendingIntent.getBroadcast(context, 0, Intent(context, PrayerReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        manager.cancel(intent)
        val next = Schedule.next(
            Clock.System.now(), TimeZone.currentSystemDefault(),
            prefs.getString("lat", null)?.toDoubleOrNull() ?: 30.0444, prefs.getString("lon", null)?.toDoubleOrNull() ?: 31.2357,
            Method.entries.firstOrNull { it.name == prefs.getString("method", null) } ?: Method.Egyptian,
            AsrFactor.entries.firstOrNull { it.name == prefs.getString("asr", null) } ?: AsrFactor.Standard,
            chosen(prefs),
        ) ?: return
        prefs.edit().putString("next.prayer", next.prayer.name).apply()
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        if (exact) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.atMillis, intent)
        else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.atMillis, intent)
    }

    fun notify(context: Context, prayer: Prayer) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Prayer times", NotificationManager.IMPORTANCE_HIGH))
        val open = PendingIntent.getActivity(context, 1, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("${prayer.label}  ${prayer.arabic}")
            .setContentText("It is time for ${prayer.label}.")
            .setAutoCancel(true).setContentIntent(open).setCategory(NotificationCompat.CATEGORY_REMINDER).build()
        manager.notify(prayer.ordinal, n)
    }
}

/** Fires at a prayer: shows the notification, then sets the alarm for the one after. */
class PrayerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("falah", Context.MODE_PRIVATE)
        val prayer = Prayer.entries.firstOrNull { it.name == prefs.getString("next.prayer", null) }
        if (prayer != null && prayer in PrayerAlarms.chosen(prefs)) PrayerAlarms.notify(context, prayer)
        PrayerAlarms.schedule(context)
    }
}

/** After a restart or a change of time zone the alarm is gone: set it again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { PrayerAlarms.schedule(context) }
}
