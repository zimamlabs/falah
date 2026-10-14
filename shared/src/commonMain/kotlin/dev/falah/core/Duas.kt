package dev.falah.core

import kotlinx.datetime.LocalDate

data class Dua(val arabic: String, val english: String, val source: String)

/** A short reminder for each day: the same one all day, a different one tomorrow. */
object Duas {
    val all = listOf(
        Dua("رَبِّ اشْرَحْ لِي صَدْرِي وَيَسِّرْ لِي أَمْرِي", "My Lord, expand my chest and ease my task for me.", "Quran 20:25-26"),
        Dua("رَبِّ زِدْنِي عِلْمًا", "My Lord, increase me in knowledge.", "Quran 20:114"),
        Dua("رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ", "Our Lord, give us good in this world and good in the next, and protect us from the punishment of the Fire.", "Quran 2:201"),
        Dua("حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ", "Allah is enough for us, and the best disposer of affairs.", "Quran 3:173"),
        Dua("رَبَّنَا لَا تُؤَاخِذْنَا إِن نَّسِينَا أَوْ أَخْطَأْنَا", "Our Lord, do not hold us to account if we forget or make a mistake.", "Quran 2:286"),
        Dua("رَّبِّ ارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا", "My Lord, have mercy on them as they raised me when I was small.", "Quran 17:24"),
        Dua("لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", "There is no power and no strength except with Allah.", "Hadith"),
        Dua("سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ الْعَظِيمِ", "Glory and praise to Allah, glory to Allah the Almighty.", "Hadith"),
        Dua("أَسْتَغْفِرُ اللَّهَ الْعَظِيمَ وَأَتُوبُ إِلَيْهِ", "I ask Allah the Almighty for forgiveness and turn to Him.", "Hadith"),
        Dua("اللَّهُمَّ أَعِنِّي عَلَى ذِكْرِكَ وَشُكْرِكَ وَحُسْنِ عِبَادَتِكَ", "O Allah, help me remember You, thank You and worship You well.", "Abu Dawud"),
        Dua("اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا وَرِزْقًا طَيِّبًا وَعَمَلًا مُتَقَبَّلًا", "O Allah, I ask You for useful knowledge, good provision and accepted deeds.", "Ibn Majah"),
        Dua("اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ", "O Allah, send prayers and peace on our Prophet Muhammad.", "Hadith"),
    )

    fun of(date: LocalDate): Dua = all[(date.toEpochDays() % all.size + all.size) % all.size]
}
