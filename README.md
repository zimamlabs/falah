<p align="center"><img src="docs/icon.png" width="120" alt="Falah"></p>

<h1 align="center">Falah</h1>

<p align="center">Prayer times, reminders and the qibla, by <a href="https://github.com/zimamlabs">Zimam Labs</a>.</p>

<p align="center">
  <a href="../../releases/latest"><img alt="Release" src="https://img.shields.io/github/v/release/zimamlabs/falah?color=FF5A1F&labelColor=1A1613"></a>
  <a href="../../releases"><img alt="Downloads" src="https://img.shields.io/github/downloads/zimamlabs/falah/total?color=FF5A1F&labelColor=1A1613"></a>
  <a href="LICENSE"><img alt="Licence: MIT" src="https://img.shields.io/badge/licence-MIT-FF5A1F?labelColor=1A1613"></a>
  <img alt="Android" src="https://img.shields.io/badge/Android-8.0%2B-FF5A1F?labelColor=1A1613">
</p>

## What it is

Android today, Linux later. Falah (فلاح, success) shows today's five prayers and the sunrise for where you are, tells you which prayer is next and how long is left, keeps a log of the prayers you pray, and points to the qibla. It works offline, has no account and sends nothing anywhere.

## Features

- **Navigation**: swipe between Home, Times, Qibla, Tasbih and Log, or use the bottom bar or the side menu (sections, Settings, What's new, Tour, About). Choose which sections show, their order and the one the app opens on.
- **Home**: a greeting with the Gregorian and Hijri date, the next prayer with a countdown in hours, minutes and seconds, a timeline of the day with a tick for each prayer, a streak and a week strip, a mini qibla card, a short dua, and a warning card if reminders may not work.
- **Prayer times** computed on the phone from the sun's position, shown to the minute, with a per-prayer adjustment of up to 30 minutes, five methods, Standard or Hanafi Asr, and a high-latitude rule (see Calculation).
- **Places**: use your location (GPS, with your permission), pick one of the built-in cities, or type any latitude and longitude. Nothing is assumed: until you choose, a notice says so and no reminder is set.
- **Prayer notifications** with Prayed, Snooze and Skip buttons. Tapping one opens that prayer's screen (time, live clock, Qibla), optionally full screen over the lock screen. Minutes before the prayer and a sound (phone default, alarm, silent or one you pick) are set per prayer. Tapping a prayer in the list opens the same screen (an option).
- **Prayer log**: tick the prayers you prayed; streaks, a month calendar and any past day can be edited. The log is a plain file, and a save that fails or a file that cannot be read is reported and never overwritten.
- **Monthly timetable** with all six times.
- **Tasbih**: a counter with six common dhikr and totals saved per day.
- **Live qibla compass** on phones with a compass sensor, with the bearing from north otherwise.
- **Appearance**: System, Light or Dark, six colour themes, four text sizes, comfortable or compact spacing, reduced animations; English and Arabic with right-to-left layout and an Arabic-Indic digits choice.
- **Your data** in a plain-file Zimam folder you can open and back up, **Privacy**: no account, no ads, no tracking and no internet permission.

Prayer times are a calculation, not an authority: check them against your local mosque or religious authority when it matters.

## Languages

English and Arabic (العربية), with a right-to-left layout. Settings has a Language choice (System, English, العربية) and a digit style (0 1 2 3 or ٠ ١ ٢ ٣); both apply at once. Prayer names, Hijri and Gregorian dates, the duas, the tour, About and the prayer notifications are all in Arabic.

## Screenshots

Screenshots: coming with the next release.

## Download

Get the file for your phone from the [latest release](../../releases/latest). Next to every file is a checksum file with the same name plus `.sha256`.

| System | File |
|---|---|
| Android phone (64-bit, almost every phone since 2016) | `Falah_<version>_Android_arm64-v8a.apk` |
| Android, older 32-bit | `Falah_<version>_Android_armeabi-v7a.apk` |
| Android emulator, Chromebook | `Falah_<version>_Android_x86_64.apk` |

```
sha256sum -c Falah_<version>_Android_arm64-v8a.apk.sha256
```

## Verify

Every APK is signed with Falah's own key. Android installs it without a store because the signature is valid, and shows its usual "unknown source" notice for anything that did not come from Google Play. To check a file is really ours, compare its signing certificate with this fingerprint:

```
SHA-256: CE:37:B1:30:E6:8E:27:4F:DD:62:5F:BB:EE:E3:96:60:DA:18:ED:44:47:1F:AB:CF:84:BE:DB:75:38:7A:17:22
```

```
apksigner verify --print-certs Falah_<version>_Android_arm64-v8a.apk
```

## Where your data lives

Settings and the prayer log are plain files (`settings.txt`, `prayer-log.csv`). With *All files access* they live in a visible `Zimam/Falah` folder on the phone, which survives reinstalling the app; without it they stay inside the app. When the folder changes, everything is copied and checked (file count, size, SHA-256) and the old folder is kept as a backup; nothing is deleted unless you remove it from Settings after a fresh check.

## Privacy

No account, no ads, no tracking. Falah keeps your data on your phone in plain files you can open anywhere, and sends nothing over the network.

## Calculation

- Methods: Egyptian General Authority (default), Muslim World League, Umm al-Qura, Karachi and ISNA; Asr standard (shadow one times the object) or Hanafi (two times). Maghrib is sunset, sunrise is the sun's upper edge at 0.833 degrees. Umm al-Qura puts Isha 90 minutes after Maghrib, and 120 minutes in Ramadan (tabular Hijri month 9, which can differ by a day from the announced calendar).
- The sun is positioned at the moment of each event, found by repeating the calculation. Times are shown to the nearest minute, and you can move any prayer by up to 30 minutes in Settings.
- Accuracy: `ReferenceTimesTest` compares 26 days across Cairo, Makkah, London, New York, Istanbul and Sydney with an independent implementation of a more precise solar model; the largest difference is 0.05 minutes (3 seconds). That is a cross-check, not a published timetable, so compare with your local authority's table before relying on it, and use the adjustment if they differ.
- High latitudes: where the sun does not reach the Fajr or Isha angle (for example London in June), or reaches it too near sunrise or sunset, Falah limits the time to a share of the night by the rule you choose (angle based by default, middle of the night, one seventh, or none). A time moved by a rule is marked on its screen; near the poles in midsummer, where the sun does not set, no Fajr or Isha is shown.
- Times follow the phone's time zone and clock.

## Build

You need JDK 17 or newer and, for Android, the Android SDK (platform 36).

```
./gradlew :shared:desktopTest            # the prayer-time tests
./gradlew :composeApp:desktopTest        # the storage tests
./gradlew :composeApp:run                # run it on the desktop (development only, not packaged)
./gradlew :composeApp:assembleDebug      # Android debug APKs
```

`shared` holds the calculation (plain Kotlin, tested without a screen) and `composeApp` holds the screens for both platforms.

## Roadmap

A home-screen widget, a missed-prayer (qada) tracker, Ramadan mode, more calculation methods and a city search, long-press menus everywhere, and a native-speaker review of the Arabic. A Linux edition comes after version 1.

## Licence

MIT. See [LICENSE](LICENSE).
