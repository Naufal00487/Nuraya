package com.nuraya.app.media

/** One known edition per person, paired only with portraits attributed by Quran.com. */
object CuratedReciters {
    val chapterIds = listOf("ar.alafasy", "ar.abdulbasitmurattal",
        "ar.haniarrifai", "ar.muhammadsiddiqalminshawimujawwad", "ar.saudalshuraim")
    val verseIds = listOf("ar.alafasy", "ar.abdulsamad", "ar.abdurrahmaansudais", "ar.shaatree",
        "ar.hanirifai", "ar.husary", "ar.saoodshuraym")
    val names = mapOf("ar.alafasy" to "Mishary Rashid Alafasy", "ar.abdulsamad" to "Abdul Basit Abdus Samad",
        "ar.abdurrahmaansudais" to "Abdurrahman As-Sudais", "ar.shaatree" to "Abu Bakr Ash-Shatri",
        "ar.hanirifai" to "Hani Ar-Rifai", "ar.husary" to "Mahmoud Khalil Al-Husary", "ar.saoodshuraym" to "Saud Al-Shuraim")
}
