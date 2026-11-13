package com.nuraya.app.quranreading

import com.nuraya.app.data.Verse
import com.nuraya.app.storage.UserSettings

/** Reading aids never change the canonical Arabic text or its numbering. */
data class QuranReadingMode(
    val id: String,
    val titleId: String,
    val titleEn: String,
    val descriptionId: String,
    val descriptionEn: String,
) {
    fun title(language: String) = if (language == "en") titleEn else titleId
    fun description(language: String) = if (language == "en") descriptionEn else descriptionId
}

object QuranReadingModes {
    val all = listOf(
        QuranReadingMode("standard", "Al-Qur’an per ayat", "Qur’an by verse", "Baca ayat, dengarkan qari, dan simpan penanda.", "Read, listen and bookmark individual verses."),
        QuranReadingMode("word_tajwid", "Per kata & tajwid", "Word meanings & tajwid", "Arti tiap kata langsung terlihat bersama teks Arab bertajwid.", "Word meanings shown directly with colored Arabic tajwid."),
        QuranReadingMode("latin_tajwid", "Latin & tajwid", "Transliteration & tajwid", "Latin langsung tampil di bawah ayat, dengan tajwid berwarna pada teks Arab.", "Transliteration directly below each verse, with colored Arabic tajwid."),
        QuranReadingMode("indonesia", "Al-Qur’an Indonesia", "Indonesian Qur’an", "604 halaman berbingkai, dengan terjemahan Indonesia dan arti per kata di panel pendamping.", "604 ornamental pages, with Indonesian translation and word meanings in a companion panel."),
        QuranReadingMode("indonesia_tajwid", "Indonesia & tajwid", "Indonesian & tajwid", "604 halaman Indonesia dengan warna tajwid asli. Buka arti ayat melalui panel terjemahan.", "604 original Indonesian tajwid pages, with verse meanings in the translation panel."),
        QuranReadingMode("madinah", "Al-Qur’an Madinah", "Madinah Qur’an", "604 gambar halaman asli, dapat diperbesar. Halaman baru diunduh saat dibuka.", "604 genuine page images with zoom. New pages download when opened."),
        QuranReadingMode("madinah_tajwid", "Madinah & tajwid", "Madinah & tajwid", "604 gambar asli Madinah bertajwid warna, dengan sorotan ayat saat murottal.", "604 original Madinah tajwid images with verse highlighting during recitation."),
        QuranReadingMode("hafalan_8", "Hafalan 8 blok", "Eight-block memorization", "604 halaman edisi Hafalan dengan pembagian blok warna asli untuk mengulang bacaan.", "604 Hafalan pages with their original colored memorization blocks."),
        QuranReadingMode("tikrar", "Al-Qur’an Tikrar", "Qur’an repetition", "Latihan pengulangan di mushaf Indonesia Tajwid; atur audio dan catat pengulangan.", "Repeat verse audio 3–20 times and count your own repetitions."),
        QuranReadingMode("waqaf", "Waqaf & ibtida", "Stopping & resuming", "Baca mushaf Indonesia Tajwid dan buka panduan tanda waqaf serta ibtida.", "Recognize source stop signs and learn how to resume recitation."),
        QuranReadingMode("thematic", "Terjemah tematik", "Thematic translations", "Jelajahi ayat tentang salat, sabar, keluarga, ilmu, dan tema lainnya.", "Browse prayer, patience, family, knowledge and other themes."),
    )

    fun find(id: String): QuranReadingMode = all.firstOrNull { it.id == id } ?: all.first()
    fun opensPageReader(id: String) = PrintedMushafEdition.forMode(id) != null
    fun pageEdition(id: String) = if (id in setOf("madinah", "madinah_tajwid")) MushafEdition.MADINAH else MushafEdition.INDONESIA
    fun usesTajwid(id: String) = id in setOf("word_tajwid", "latin_tajwid", "indonesia_tajwid", "madinah_tajwid", "waqaf")
    fun settings(id: String, original: UserSettings): UserSettings = original.copy(
        translation = when (id) {
            "indonesia", "indonesia_tajwid", "thematic" -> "id"
            "madinah", "madinah_tajwid" -> "none"
            else -> original.translation
        },
        tajwidColors = if (usesTajwid(id)) original.tajwidColors else if (id in setOf("indonesia", "madinah")) false else original.tajwidColors,
    )
}

/** Exercise divisions, not page boundaries of any printed eight-block edition. */
data class MemorizationBlock(val number: Int, val verses: List<Verse>) {
    val start get() = verses.first()
    val end get() = verses.last()
    val reference get() = "${start.surah}:${start.ayah}–${end.surah}:${end.ayah}"
}

fun memorizationBlocks(verses: List<Verse>): List<MemorizationBlock> {
    if (verses.isEmpty()) return emptyList()
    val count = minOf(8, verses.size)
    return (0 until count).map { index ->
        MemorizationBlock(index + 1, verses.subList(index * verses.size / count, (index + 1) * verses.size / count))
    }
}

data class QuranTheme(val id: String, val titleId: String, val titleEn: String, val references: List<Pair<Int, Int>>) {
    fun title(language: String) = if (language == "en") titleEn else titleId
}

/** Editorial index of verse references; meanings are always loaded from the attributed corpus. */
object QuranThemes {
    val all = listOf(
        QuranTheme("faith", "Iman & tauhid", "Faith & oneness", listOf(2 to 255, 112 to 1, 112 to 2, 112 to 3, 112 to 4, 59 to 22)),
        QuranTheme("prayer", "Salat & doa", "Prayer & supplication", listOf(2 to 43, 2 to 186, 17 to 78, 20 to 14, 29 to 45)),
        QuranTheme("patience", "Sabar & ketenangan", "Patience & reassurance", listOf(2 to 153, 2 to 155, 2 to 156, 13 to 28, 94 to 5, 94 to 6)),
        QuranTheme("family", "Orang tua & keluarga", "Parents & family", listOf(17 to 23, 17 to 24, 30 to 21, 31 to 14, 66 to 6)),
        QuranTheme("knowledge", "Ilmu & belajar", "Knowledge & learning", listOf(20 to 114, 39 to 9, 58 to 11, 96 to 1, 96 to 2, 96 to 3, 96 to 4, 96 to 5)),
        QuranTheme("generosity", "Sedekah & kepedulian", "Giving & care", listOf(2 to 261, 2 to 264, 3 to 92, 76 to 8, 107 to 1, 107 to 2, 107 to 3)),
        QuranTheme("repentance", "Taubat & ampunan", "Repentance & forgiveness", listOf(3 to 135, 4 to 110, 25 to 70, 39 to 53, 66 to 8)),
        QuranTheme("character", "Akhlak & pergaulan", "Character & conduct", listOf(16 to 90, 31 to 18, 31 to 19, 49 to 11, 49 to 12, 49 to 13)),
        QuranTheme("gratitude", "Syukur & rezeki", "Gratitude & provision", listOf(2 to 152, 14 to 7, 16 to 114, 65 to 2, 65 to 3)),
        QuranTheme("fasting", "Puasa & Ramadan", "Fasting & Ramadan", listOf(2 to 183, 2 to 184, 2 to 185, 2 to 187, 97 to 1, 97 to 3)),
    )
}

/** Only symbols already present in the unchanged Uthmani source are described. */
data class WaqafSign(val mark: String, val name: String, val explanation: String)
object WaqafGuide {
    val signs = listOf(
        WaqafSign("ۘ", "م · Waqaf lazim", "Berhenti pada tanda ini untuk menjaga hubungan makna."),
        WaqafSign("ۙ", "لا · Jangan berhenti", "Lanjutkan bacaan. Jika terpaksa berhenti karena napas, ulangi dari bagian sebelumnya yang maknanya utuh."),
        WaqafSign("ۚ", "ج · Waqaf jaiz", "Boleh berhenti atau melanjutkan sesuai napas dan hubungan makna."),
        WaqafSign("ۖ", "صلى · Lanjut lebih utama", "Melanjutkan bacaan lebih utama; boleh berhenti bila diperlukan."),
        WaqafSign("ۗ", "قلى · Berhenti lebih utama", "Berhenti lebih utama; melanjutkan masih diperbolehkan."),
        WaqafSign("ۛ", "∴ · Waqaf mu‘anaqah", "Boleh melanjutkan melewati kedua tanda. Jika memilih berhenti, berhentilah pada salah satu tanda saja; jangan berhenti pada keduanya."),
        WaqafSign("ۜ", "س · Saktah", "Jeda sejenak tanpa mengambil napas pada tanda batas kata dari sumber. Ikuti contoh qari dan bimbingan guru."),
    )
    // High seen inside يبصط/بصطة is a pronunciation variant, not saktah.
    fun inText(arabic: String) = signs.filter { sign ->
        if (sign.mark == "ۜ") Regex("(?:^|\\s)ۜ(?:$|\\s)").containsMatchIn(arabic)
        else arabic.contains(sign.mark)
    }
}
