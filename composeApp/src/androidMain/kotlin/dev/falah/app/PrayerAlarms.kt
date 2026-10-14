package dev.falah.app

import dev.falah.core.I18n
import dev.falah.core.Str
import dev.falah.core.title
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
            Prayer.entries.associateWith { prefs.getString("offset.${it.name}", null)?.toIntOrNull() ?: 0 },
        ) ?: return
        prefs.edit().putString("next.prayer", next.prayer.name).putInt("next.before", next.minutesBefore).apply()
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        if (exact) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.atMillis, intent)
        else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.atMillis, intent)
    }

    fun notify(context: Context, prayer: Prayer, minutesBefore: Int = 0) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val prefs = context.getSharedPreferences("falah", Context.MODE_PRIVATE)
        val lang = I18n.resolve(prefs.getString("lang", null), java.util.Locale.getDefault().language)
        val arabicDigits = prefs.getString("digits", null) == "ar"
        fun t(s: Str, vararg args: Any?) = I18n.digits(s.text(lang, *args), arabicDigits)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, t(Str.NotifChannel), NotificationManager.IMPORTANCE_HIGH))
        val open = PendingIntent.getActivity(context, 1, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(t(Str.NotifTitle, prayer.label, prayer.arabic))
            .setContentText(if (minutesBefore > 0) t(Str.NotifBefore, prayer.title(lang), minutesBefore) else t(Str.NotifNow, prayer.title(lang)))
            .setAutoCancel(true).setContentIntent(open).setCategory(NotificationCompat.CATEGORY_REMINDER).build()
        manager.notify(prayer.ordinal, n)
    }
}

/** Fires at a prayer: shows the notification, then sets the alarm for the one after. */
class PrayerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("falah", Context.MODE_PRIVATE)
        val prayer = Prayer.entries.firstOrNull { it.name == prefs.getString("next.prayer", null) }
        if (prayer != null && prayer in PrayerAlarms.chosen(prefs)) PrayerAlarms.notify(context, prayer, prefs.getInt("next.before", 0))
        PrayerAlarms.schedule(context)
    }
}

/** After a restart or a change of time zone the alarm is gone: set it again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { PrayerAlarms.schedule(context) }
}
