package com.nuraya.app.quranreading

/** Actual printed editions. The name selects its own artwork and page index. */
enum class PrintedMushafEdition(val id: String, val index: String, val mapping: String?, val titleId: String, val titleEn: String) {
    INDONESIA("id", "indonesia", "id", "Mushaf Indonesia", "Indonesian mushaf"),
    INDONESIA_TAJWID("id-tajwid", "indonesia", "id", "Indonesia & tajwid", "Indonesian & tajwid"),
    MADINAH("md", "madinah", "md", "Mushaf Madinah", "Madinah mushaf"),
    MADINAH_TAJWID("md-tajwid", "madinah", "md", "Madinah & tajwid", "Madinah & tajwid"),
    WORD_TAJWID("kata-tajwid", "indonesia", "kata-tajwid", "Per kata & tajwid", "Word meanings & tajwid"),
    LATIN_TAJWID("latin-tajwid", "indonesia", "kata-tajwid", "Latin & tajwid", "Transliteration & tajwid"),
    HAFALAN_8("hafalan", "indonesia", null, "Hafalan 8 blok", "Eight-block memorization");

    fun title(language: String) = if (language == "en") titleEn else titleId
    val colored get() = this !in setOf(INDONESIA, MADINAH)
    val width get() = 1080
    val height get() = if (this in setOf(WORD_TAJWID, LATIN_TAJWID)) 2202 else 1745

    companion object {
        const val COMMIT = "df1ca931ca6e83f8ff6b2d3a9308406e11994d00"
        const val BASE_URL = "https://raw.githubusercontent.com/agusibrahim/quran-image/$COMMIT/"

        fun forMode(mode: String): PrintedMushafEdition? = when (mode) {
            "indonesia" -> INDONESIA
            "indonesia_tajwid" -> INDONESIA_TAJWID
            "madinah" -> MADINAH
            "madinah_tajwid" -> MADINAH_TAJWID
            "word_tajwid" -> WORD_TAJWID
            "latin_tajwid" -> LATIN_TAJWID
            "hafalan_8" -> HAFALAN_8
            // These are exercises using the Indonesian page, not new printed editions.
            "tikrar", "waqaf" -> INDONESIA_TAJWID
            else -> null
        }
    }
}

data class MushafRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    fun contains(x: Float, y: Float) = x >= left && x < right && y >= top && y < bottom
}

data class MushafRegion(val surah: Int, val ayah: Int, val rectangles: List<MushafRect>)

/** The same fitted image transform is used for drawing and hit-testing, including letterboxing. */
data class MushafImagePlacement(val scale: Float, val left: Float, val top: Float) {
    fun imagePoint(x: Float, y: Float) = (x - left) / scale to (y - top) / scale

    companion object {
        fun fit(imageWidth: Int, imageHeight: Int, availableWidth: Float, availableHeight: Float): MushafImagePlacement {
            require(imageWidth > 0 && imageHeight > 0 && availableWidth > 0 && availableHeight > 0)
            val scale = minOf(availableWidth / imageWidth, availableHeight / imageHeight)
            return MushafImagePlacement(scale, (availableWidth - imageWidth * scale) / 2, (availableHeight - imageHeight * scale) / 2)
        }
    }
}
