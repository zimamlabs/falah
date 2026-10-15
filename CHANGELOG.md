# Changelog

## 0.6.0

- **Navigation.** Swipe between Home, Times, Qibla, Tasbih and Log; a side menu (mark, sections, Settings, What's new, Tour, About); Back closes the menu, leaves Settings, returns Home, then exits.
- **Settings as a list of sections:** Appearance (mode, six colour themes, text size with preview, spacing, reduce animations), Sections and layout (show, hide, reorder, start section), Language, Notifications and reminders, Calculation, Your data, Privacy and security. Settings are one typed object; keys from 0.5.1 are read unchanged.
- **Time to the second.** The countdown to the next prayer reads HH:MM:SS from the system clock without drift and pauses in the background. Any prayer can be moved by up to 30 minutes; the alarm fires on the minute shown.
- **The prayer notification opens the prayer's screen** (Prayed, Snooze, Skip, Qibla), optionally full screen over the lock screen, with a sound choice per prayer. Tapping a prayer in the list opens the same screen (option). One card, "Reminders may not work", covers notifications off, exact alarms off, full-screen not allowed and battery optimisation. Reminders are not set while no place is chosen, and a late alarm no longer announces a past prayer.
- **Places.** "Use my location" (GPS, with an explanation and your permission first; approximate is enough), type any latitude and longitude, and a notice instead of a silent Cairo default.
- **Accuracy.** The sun is positioned at each event (largest difference from an independent precise model 0.05 minutes, was 2.1), Umm al-Qura Isha is 120 minutes in Ramadan, and a high-latitude rule (angle based by default, middle of the night, one seventh, or none) gives London in summer a Fajr and Isha. The invented 05:00 Fajr in the countdown is gone.
- **Safer saving.** A failed save shows a message; a data file that cannot be read is never overwritten.
- Android only for now; desktop packages are no longer built. New English and Arabic texts are unreviewed by a native reader.

## 0.5.1

- **A new app icon.** The launcher icon, splash and in-app mark follow the Zimam family, on a warm dark background.

## 0.5.0

- **Arabic and right-to-left.** Falah now has an Arabic interface with a mirrored layout (Settings, Language: System, English or العربية, applied at once). Prayer names, Hijri and Gregorian dates, the duas, the tour, the notes, About and the prayer notifications are all in Arabic. Digits can be Western (0 1 2 3) or Arabic-Indic (٠ ١ ٢ ٣). In Arabic the week in the log starts on Saturday. The compass is not mirrored.

## 0.4.1

- **A new look.** Every screen follows the icon: warm near-black or ivory, the Zimam orange as the one accent, a soft glow behind the hero, large rounded cards with a hairline border and the small orange diamond.
- **An About page** with the mark on a glow, what Falah is, the tour and what's new, a privacy card and links to the repository, releases, issues and licence.
- **README refreshed** with a banner, badges and a screenshots section.

## 0.4.0

- **Prayer log history.** A Log page with a month calendar of rings, the month's totals, streaks, and any past day you can open and edit.
- **Monthly timetable.** Times has a Month view with every day and all six times; the time-zone offset follows clock changes.
- **Tasbih.** A counter with six dhikr, a round each, saved totals per day and all time (`tasbih.csv` in your data folder).
- **Reminder offsets.** For each prayer you pick, notify at the time or 5, 10, 15 or 30 minutes before.
- **Live qibla compass.** The dial follows the phone's sensor (corrected for magnetic declination), says how far to turn and when you face the qibla, asks you to calibrate when the sensor is unsure, and falls back to the fixed bearing when there is no sensor.

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
