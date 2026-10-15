package dev.falah.core

enum class ThemeMode(val key: String) { System("system"), Light("light"), Dark("dark") }

/** Accent palettes. Each is defined once in the app theme; the key is what is stored. */
enum class Accent(val key: String) { Orange("orange"), Emerald("emerald"), Sapphire("sapphire"), Rose("rose"), Violet("violet"), Gold("gold") }

enum class TextSize(val key: String, val scale: Float) { Small("small", 0.9f), Normal("normal", 1.0f), Large("large", 1.15f), Larger("larger", 1.3f) }

enum class Density(val key: String) { Comfortable("comfortable"), Compact("compact") }

/** The main sections the user can show, hide and reorder. Home is always shown. */
enum class Section(val key: String) { Home("home"), Times("times"), Qibla("qibla"), Tasbih("tasbih"), Log("log") }

/**
 * Every setting the app has, in one typed object (the single source of truth). Stored as plain `key=value` text with
 * the same keys earlier versions used, so nothing needs migrating; keys that did not exist yet fall back to the defaults.
 */
data class AppSettings(
    val placeName: String = "Cairo",
    val latitude: Double = 30.0444,
    val longitude: Double = 31.2357,
    /** False until the user picked or typed a place (the Cairo default is then only a placeholder). */
    val placeChosen: Boolean = false,
    /** How the place was set: picked or typed by the user, or found by the phone's location. */
    val placeSource: PlaceSource = PlaceSource.Manual,
    val method: Method = Method.Egyptian,
    val asr: AsrFactor = AsrFactor.Standard,
    val highLatitude: HighLatitude = HighLatitude.AngleBased,
    val hijriAdjust: Int = 0,
    val theme: ThemeMode = ThemeMode.System,
    val accent: Accent = Accent.Orange,
    val textSize: TextSize = TextSize.Normal,
    val density: Density = Density.Comfortable,
    val reduceMotion: Boolean = false,
    /** "system", "en" or "ar". */
    val language: String = "system",
    val arabicDigits: Boolean = false,
    /** Sections shown in the bar and the menu, in order. Home is always first. */
    val sections: List<Section> = Section.entries,
    val startSection: Section = Section.Home,
    val notify: Set<Prayer> = emptySet(),
    /** Minutes before each prayer the notification fires. */
    val offsets: Map<Prayer, Int> = emptyMap(),
    /** Minutes added to (or taken from) each prayer's time, -30 to 30. */
    val adjust: Map<Prayer, Int> = emptyMap(),
    /** Tapping a prayer in the list opens that prayer's own screen. */
    val prayerScreens: Boolean = true,
    /** The notification sound per prayer: [Sounds.DEFAULT], [Sounds.SILENT], [Sounds.ALARM] or the address of a sound the user picked. */
    val sounds: Map<Prayer, String> = emptyMap(),
    /** The prayers that bring up their screen when the time comes (the five prayers by default, not sunrise). Tapping a notification always opens it. */
    val noticeScreen: Set<Prayer> = loggedPrayers.toSet(),
    /** True: use a full-screen screen where the phone allows it; false: a normal heads-up notification. */
    val noticeFullScreen: Boolean = true,
    /** Minutes the Snooze button waits. */
    val snoozeMinutes: Int = 10,
) {
    fun soundOf(p: Prayer): String = sounds[p] ?: Sounds.DEFAULT

    fun offsetOf(p: Prayer): Int = offsets[p] ?: 0

    fun withSections(chosen: List<Section>): AppSettings {
        val list = (listOf(Section.Home) + chosen.filter { it != Section.Home }).distinct()
        return copy(sections = list, startSection = startSection.takeIf { it in list } ?: Section.Home)
    }
}

object Sounds {
    const val DEFAULT = "default"
    const val SILENT = "silent"
    const val ALARM = "alarm"
    fun valid(v: String) = v == DEFAULT || v == SILENT || v == ALARM || v.startsWith("content://") || v.startsWith("file://")
}

object SettingsCodec {
    private val reminderOffsets = listOf(0, 5, 10, 15, 30)
    val snoozeChoices = listOf(5, 10, 15, 20, 30)

    fun decode(values: Map<String, String>): AppSettings {
        val d = AppSettings()
        fun <T> pick(all: Iterable<T>, key: String, name: (T) -> String, default: T) = all.firstOrNull { name(it) == values[key] } ?: default
        val lat = values["lat"]?.toDoubleOrNull()?.takeIf { it in -90.0..90.0 } ?: d.latitude
        val lon = values["lon"]?.toDoubleOrNull()?.takeIf { it in -180.0..180.0 } ?: d.longitude
        val sections = values["sections"]?.split(",")?.mapNotNull { k -> Section.entries.firstOrNull { it.key == k } }
        return AppSettings(
            placeName = values["place"]?.takeIf { it.isNotBlank() } ?: d.placeName,
            latitude = lat, longitude = lon,
            placeChosen = values["place"] != null,
            placeSource = if (values["place.source"] == "gps") PlaceSource.Gps else PlaceSource.Manual,
            method = pick(Method.entries, "method", { it.name }, d.method),
            highLatitude = pick(HighLatitude.entries, "highlat", { it.key }, d.highLatitude),
            asr = pick(AsrFactor.entries, "asr", { it.name }, d.asr),
            hijriAdjust = values["hijri.adjust"]?.toIntOrNull()?.coerceIn(-1, 1) ?: 0,
            theme = pick(ThemeMode.entries, "theme", { it.key }, d.theme),
            accent = pick(Accent.entries, "accent", { it.key }, d.accent),
            textSize = pick(TextSize.entries, "textsize", { it.key }, d.textSize),
            density = pick(Density.entries, "density", { it.key }, d.density),
            reduceMotion = values["motion"] == "reduced",
            language = values["lang"]?.takeIf { it in listOf("system", "en", "ar") } ?: d.language,
            arabicDigits = values["digits"] == "ar",
            sections = if (sections.isNullOrEmpty()) d.sections else d.withSections(sections).sections,
            startSection = pick(Section.entries, "start", { it.key }, d.startSection),
            notify = Prayer.entries.filter { values["notify.${it.name}"] == "1" }.toSet(),
            adjust = Prayer.entries.mapNotNull { p -> values["adjust.${p.name}"]?.toIntOrNull()?.takeIf { it in -30..30 && it != 0 }?.let { p to it } }.toMap(),
            noticeScreen = if (values.keys.any { it.startsWith("notice.") && it != "notice.mode" }) Prayer.entries.filter { values["notice.${it.name}"] == "1" }.toSet() else d.noticeScreen,
            noticeFullScreen = values["notice.mode"] != "heads-up",
            snoozeMinutes = values["snooze"]?.toIntOrNull()?.takeIf { it in snoozeChoices } ?: d.snoozeMinutes,
            prayerScreens = values["prayerscreens"] != "0",
            sounds = Prayer.entries.mapNotNull { p -> values["sound.${p.name}"]?.takeIf { Sounds.valid(it) && it != Sounds.DEFAULT }?.let { p to it } }.toMap(),
            offsets = Prayer.entries.mapNotNull { p -> values["offset.${p.name}"]?.toIntOrNull()?.takeIf { it in reminderOffsets }?.let { p to it } }.toMap(),
        ).let { it.copy(startSection = it.startSection.takeIf { s -> s in it.sections } ?: Section.Home) }
    }

    /** All keys for [s]. The place key is written only once a place was chosen, so a fresh install stays "no place yet". */
    fun encode(s: AppSettings): Map<String, String> = buildMap {
        if (s.placeChosen) { put("place", s.placeName); put("place.source", s.placeSource.key); put("lat", s.latitude.toString()); put("lon", s.longitude.toString()) }
        put("method", s.method.name); put("asr", s.asr.name); put("highlat", s.highLatitude.key); put("hijri.adjust", s.hijriAdjust.toString())
        put("theme", s.theme.key); put("accent", s.accent.key); put("textsize", s.textSize.key); put("density", s.density.key)
        put("motion", if (s.reduceMotion) "reduced" else "full")
        put("lang", s.language); put("digits", if (s.arabicDigits) "ar" else "en")
        put("sections", s.sections.joinToString(",") { it.key }); put("start", s.startSection.key)
        Prayer.entries.forEach { put("notify.${it.name}", if (it in s.notify) "1" else "0") }
        s.offsets.forEach { (p, m) -> put("offset.${p.name}", m.toString()) }
        Prayer.entries.forEach { put("notice.${it.name}", if (it in s.noticeScreen) "1" else "0") }
        put("notice.mode", if (s.noticeFullScreen) "full" else "heads-up"); put("snooze", s.snoozeMinutes.toString())
        put("prayerscreens", if (s.prayerScreens) "1" else "0")
        Prayer.entries.forEach { put("sound.${it.name}", s.soundOf(it)) }
        Prayer.entries.forEach { put("adjust.${it.name}", (s.adjust[it] ?: 0).toString()) }
    }
}
