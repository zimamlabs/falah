# Changelog

## 0.3.0

- **New icon.** The final Falah icon (a mosque and the sun in a mihrab arch) on the launcher, the splash screen and the Settings page.
- **A real Home.** Greeting, Gregorian and Hijri date, the next prayer with a countdown ring, a timeline of the day with a tick for each prayer prayed, a streak and week strip, a mini qibla card, a daily dua and quick actions. Times, Qibla and Settings are separate pages in a bottom bar.
- **Prayer log.** Tick the prayers you pray; streaks and the week strip follow.
- **One Zimam folder.** Settings and the log are plain files in `Zimam/Falah` when All files access is allowed (survives reinstalling), otherwise inside the app. Copies are checked by count, size and SHA-256, the old folder is kept as a backup, nothing is overwritten, and removing the old folder is a button in Settings that checks again first.
- **First-run tour and What's new.** A short tour on a fresh install (reopen it from Settings), and a one-time note with a Close button after each update.
- **Settings:** theme (system, light, dark) and a Hijri date adjustment of a day either way.

## 0.2.2

- A better splash screen: the name sits clearly apart from the logo, and the logo and name ease in.

## 0.2.1

- **New icon.** A mihrab arch with the Zimam dot, on the app icon and the splash screen.

## 0.2.0

- **Notifications at prayer times.** Pick which prayers to be told about (Fajr, Dhuhr, Asr, Maghrib, Isha); Falah sets an alarm for the next one, shows a notification when it comes, and sets the one after. Alarms are set again after a restart or a change of time zone. On Android 13 and later the first pick asks for permission to show notifications; if the system does not allow exact alarms, the notification can come a few minutes late.

## 0.1.0

- First release: today's prayer times (Fajr, sunrise, Dhuhr, Asr, Maghrib, Isha) for a chosen place, the next prayer and the time left, the qibla direction, five calculation methods with Standard or Hanafi Asr, a splash screen and the Zimam look.
