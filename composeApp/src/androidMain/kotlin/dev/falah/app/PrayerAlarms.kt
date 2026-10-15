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
import dev.falah.core.NoticeRoute
import dev.falah.core.Prayer
import dev.falah.core.ReminderStatus
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
        // No place chosen yet: do not set reminders for a placeholder city.
        if (prefs.getString("place", null) == null) return
        val next = Schedule.next(
            Clock.System.now(), TimeZone.currentSystemDefault(),
            prefs.getString("lat", null)?.toDoubleOrNull() ?: 30.0444, prefs.getString("lon", null)?.toDoubleOrNull() ?: 31.2357,
            Method.entries.firstOrNull { it.name == prefs.getString("method", null) } ?: Method.Egyptian,
            AsrFactor.entries.firstOrNull { it.name == prefs.getString("asr", null) } ?: AsrFactor.Standard,
            chosen(prefs),
            Prayer.entries.associateWith { prefs.getString("offset.${it.name}", null)?.toIntOrNull() ?: 0 },
            Prayer.entries.associateWith { prefs.getString("adjust.${it.name}", null)?.toIntOrNull()?.coerceIn(-30, 30) ?: 0 },
        ) ?: return
        prefs.edit().putString("next.prayer", next.prayer.name).putInt("next.before", next.minutesBefore).putLong("next.fire", next.atMillis).putLong("next.at", next.atMillis + next.minutesBefore * 60_000L).apply()
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        if (exact) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.atMillis, intent)
        else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.atMillis, intent)
    }

    /** A channel per sound, because Android fixes a channel's sound when it is created. The default channel keeps its old id. */
    private fun channelFor(sound: String?, name: String): NotificationChannel {
        val audio = android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION).build()
        return when {
            sound == "silent" -> NotificationChannel("$CHANNEL-silent", name, NotificationManager.IMPORTANCE_HIGH).apply { setSound(null, null) }
            sound == "alarm" -> NotificationChannel("$CHANNEL-alarm", name, NotificationManager.IMPORTANCE_HIGH).apply { setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM), audio) }
            sound != null && (sound.startsWith("content://") || sound.startsWith("file://")) ->
                NotificationChannel("$CHANNEL-${sound.hashCode()}", name, NotificationManager.IMPORTANCE_HIGH).apply { setSound(android.net.Uri.parse(sound), audio) }
            else -> NotificationChannel(CHANNEL, name, NotificationManager.IMPORTANCE_HIGH)
        }
    }

    /** An intent that opens the prayer's own screen. The data address makes each prayer, time and action a different PendingIntent. */
    private fun screenIntent(context: Context, prayer: Prayer, atMillis: Long, action: String) =
        Intent(context, MainActivity::class.java).setAction(action).setData(android.net.Uri.parse("falah://notice/${prayer.name}/$atMillis/$action"))
            .putExtra(NoticeRoute.EXTRA_PRAYER, prayer.name).putExtra(NoticeRoute.EXTRA_AT, atMillis)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    private fun broadcast(context: Context, cls: Class<*>, prayer: Prayer, atMillis: Long, action: String) = PendingIntent.getBroadcast(
        context, prayer.ordinal, Intent(context, cls).setAction(action).setData(android.net.Uri.parse("falah://notice/${prayer.name}/$atMillis/$action"))
            .putExtra(NoticeRoute.EXTRA_PRAYER, prayer.name).putExtra(NoticeRoute.EXTRA_AT, atMillis), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    fun fullScreenAllowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < 34 || (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).canUseFullScreenIntent()

    /** Shows the prayer notification. Tapping it always opens the prayer's screen; if the user wants it, the screen also comes up by itself (full-screen intent where the phone allows it, otherwise a heads-up notification). */
    fun notify(context: Context, prayer: Prayer, minutesBefore: Int = 0, prayerAtMillis: Long = System.currentTimeMillis()) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val prefs = context.getSharedPreferences("falah", Context.MODE_PRIVATE)
        val lang = I18n.resolve(prefs.getString("lang", null), java.util.Locale.getDefault().language)
        val arabicDigits = prefs.getString("digits", null) == "ar"
        fun t(s: Str, vararg args: Any?) = I18n.digits(s.text(lang, *args), arabicDigits)
        val channel = channelFor(prefs.getString("sound.${prayer.name}", null), t(Str.NotifChannel))
        manager.createNotificationChannel(channel)
        val snoozeMinutes = prefs.getString("snooze", null)?.toIntOrNull() ?: 10
        val open = PendingIntent.getActivity(context, prayer.ordinal, screenIntent(context, prayer, prayerAtMillis, NoticeRoute.ACTION_OPEN), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(t(Str.NotifTitle, prayer.label, prayer.arabic))
            .setContentText(if (minutesBefore > 0) t(Str.NotifBefore, prayer.title(lang), minutesBefore) else t(Str.NotifNow, prayer.title(lang)))
            .setAutoCancel(true).setContentIntent(open).setCategory(NotificationCompat.CATEGORY_REMINDER).setPriority(NotificationCompat.PRIORITY_HIGH)
        if (prayer != Prayer.Sunrise) builder.addAction(0, t(Str.NoticePrayed), PendingIntent.getActivity(context, 100 + prayer.ordinal, screenIntent(context, prayer, prayerAtMillis, NoticeRoute.ACTION_PRAYED), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        builder.addAction(0, t(Str.NoticeSnooze, snoozeMinutes), broadcast(context, SnoozeReceiver::class.java, prayer, prayerAtMillis, "dev.falah.action.SNOOZE"))
        builder.addAction(0, t(Str.NoticeSkip), broadcast(context, DismissReceiver::class.java, prayer, prayerAtMillis, "dev.falah.action.DISMISS"))
        val wantsScreen = prefs.getString("notice.${prayer.name}", null).let { it == "1" || it == null && prayer != Prayer.Sunrise }
        if (wantsScreen && prefs.getString("notice.mode", null) != "heads-up" && fullScreenAllowed(context)) builder.setFullScreenIntent(open, true)
        manager.notify(prayer.ordinal, builder.build())
    }

    /** Hides the notification and sets one alarm [minutes] from now that shows it again. */
    fun snooze(context: Context, prayer: Prayer, minutes: Int, prayerAtMillis: Long) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(prayer.ordinal)
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val at = System.currentTimeMillis() + minutes * 60_000L
        val pi = broadcast(context, SnoozeReceiver::class.java, prayer, prayerAtMillis, "dev.falah.action.SNOOZE_FIRE")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    fun status(context: Context): ReminderStatus {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val power = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return ReminderStatus(
            notifications = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled(),
            exactAlarms = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms(),
            fullScreen = fullScreenAllowed(context),
            batteryUnrestricted = power.isIgnoringBatteryOptimizations(context.packageName),
        )
    }
}

/** Snooze button, and the alarm that ends a snooze. */
class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayer = Prayer.entries.firstOrNull { it.name == intent.getStringExtra(NoticeRoute.EXTRA_PRAYER) } ?: return
        val at = intent.getLongExtra(NoticeRoute.EXTRA_AT, System.currentTimeMillis())
        val prefs = context.getSharedPreferences("falah", Context.MODE_PRIVATE)
        if (intent.action == "dev.falah.action.SNOOZE_FIRE") PrayerAlarms.notify(context, prayer, 0, at)
        else PrayerAlarms.snooze(context, prayer, prefs.getString("snooze", null)?.toIntOrNull() ?: 10, at)
    }
}

/** Skip button: just hides the notification. */
class DismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayer = Prayer.entries.firstOrNull { it.name == intent.getStringExtra(NoticeRoute.EXTRA_PRAYER) } ?: return
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(prayer.ordinal)
    }
}

/** Fires at a prayer: shows the notification, then sets the alarm for the one after. */
class PrayerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("falah", Context.MODE_PRIVATE)
        val prayer = Prayer.entries.firstOrNull { it.name == prefs.getString("next.prayer", null) }
        // An alarm that arrives long after its moment (phone off, Doze) is stale: do not announce a prayer that is long past.
        val stale = System.currentTimeMillis() - prefs.getLong("next.fire", 0L) > 10 * 60_000L && prefs.getLong("next.fire", 0L) > 0
        if (prayer != null && prayer in PrayerAlarms.chosen(prefs) && !stale) PrayerAlarms.notify(context, prayer, prefs.getInt("next.before", 0), prefs.getLong("next.at", System.currentTimeMillis()))
        PrayerAlarms.schedule(context)
    }
}

/** After a restart or a change of time zone the alarm is gone: set it again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { PrayerAlarms.schedule(context) }
}
