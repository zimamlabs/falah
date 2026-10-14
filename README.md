<p align="center"><img src="docs/icon.png" width="120" alt="Falah"></p>

<h1 align="center">Falah</h1>

<p align="center">Prayer times, reminders and the qibla, by <a href="https://github.com/zimamlabs">Zimam</a>.</p>

Falah (فلاح, success) shows today's five prayers and the sunrise for where you are, tells you which prayer is next and how long is left, and points to the qibla. It works offline, has no account and sends nothing anywhere.

## What is in it

- **Home**: a greeting with the Gregorian and Hijri date, the next prayer with a countdown ring, a timeline of the day with a tick for each prayer you pray, a streak and a week strip, a mini qibla card, a daily reminder (a short dua) and quick actions. Times, Qibla and Settings are pages reached from the bottom bar.
- **Prayer log**: tick the prayers you prayed; the streak and week strip follow. The log is a plain file.
- **First-run tour** and a one-time **What's new** note after each update.
- **Prayer times** computed on the phone from the sun's position: Fajr, sunrise, Dhuhr, Asr, Maghrib and Isha, accurate to about a minute.
- **Next prayer** and the time left, updated as the day goes.
- **Notifications** at the prayers you pick, set again after a restart.
- **Qibla** direction from where you are.
- **Methods**: Egyptian General Authority (default), Muslim World League, Umm al-Qura, Karachi and North America, with Standard or Hanafi Asr.
- **Places**: Cairo, Alexandria, Makkah, Madinah, Riyadh, Dubai, Istanbul, London, New York and more.

Prayer times are a calculation, not an authority: check them against your local mosque or religious authority when it matters.

## Your data

Settings and the prayer log are plain files (`settings.txt`, `prayer-log.csv`). With *All files access* they live in a visible `Zimam/Falah` folder on the phone, which survives reinstalling the app; without it they stay inside the app. When the folder changes, everything is copied and checked (file count, size, SHA-256) and the old folder is kept as a backup; nothing is deleted unless you remove it from Settings after a fresh check.

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

### Verify the Android download

Every APK is signed with Falah's own key. Android installs it without a store because the signature is valid, and shows its usual "unknown source" notice for anything that did not come from Google Play. To check a file is really ours, compare its signing certificate with this fingerprint:

```
SHA-256: CE:37:B1:30:E6:8E:27:4F:DD:62:5F:BB:EE:E3:96:60:DA:18:ED:44:47:1F:AB:CF:84:BE:DB:75:38:7A:17:22
```

```
apksigner verify --print-certs Falah_<version>_Android_arm64-v8a.apk
```

## Build

You need JDK 17 or newer and, for Android, the Android SDK (platform 36).

```
./gradlew :shared:desktopTest            # the prayer-time tests
./gradlew :composeApp:run                # run it on the desktop
./gradlew :composeApp:assembleDebug      # Android debug APKs
```

`shared` holds the calculation (plain Kotlin, tested without a screen) and `composeApp` holds the screens for both platforms.

## Roadmap

Prayer log history and a monthly view, a monthly timetable, a tasbih counter, reminder offsets, a live compass for the qibla, your own location from the phone, and a home-screen widget. A Linux edition comes after version 1.

## Licence

MIT. See [LICENSE](LICENSE).
