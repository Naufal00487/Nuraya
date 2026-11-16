package com.nuraya.app.media

/** One person per selection; explicit provider IDs pair chapter and Quran-reader playback. */
object CuratedReciters {
    data class Selection(val chapterId: String, val verseId: String, val name: String)
    val selections = listOf(
        Selection("ar.alafasy", "ar.alafasy", "Mishary Rashid Alafasy"),
        Selection("ar.abdulbasitmurattal", "ar.abdulsamad", "Abdul Basit Abdus Samad"),
        Selection("ar.muhammadsiddiqalminshawimujawwad", "ar.minshawimujawwad", "Muhammad Siddiq Al-Minshawi"),
        Selection("ar.husary", "ar.husary", "Mahmoud Khalil Al-Husary"),
        Selection("ar.abdurrahmaansudais", "ar.abdurrahmaansudais", "Abdurrahman As-Sudais"),
        Selection("ar.saudalshuraim", "ar.saoodshuraym", "Saud Al-Shuraim"),
        Selection("ar.mahermuaiqly", "ar.mahermuaiqly", "Maher Al-Muaiqly"),
        Selection("ar.shaatree", "ar.shaatree", "Abu Bakr Ash-Shatri"),
        Selection("ar.ahmedalajmi", "ar.ahmedajamy", "Ahmed Al-Ajmi"),
        Selection("ar.saadalghamdi", "ar.saadalghamdi", "Saad Al-Ghamdi"),
        Selection("ar.yasseraldossari", "ar.yasseraldossari", "Yasser Al-Dossari"),
        Selection("ar.nasseralqatami", "ar.nasseralqatami", "Nasser Al-Qatami"),
        Selection("ar.faresabbad", "ar.faresabbad", "Fares Abbad"),
        Selection("ar.aliabdurrahmanalhuthaify", "ar.hudhaify", "Ali Abdurrahman Al-Hudhaify"),
        Selection("ar.abdullahbasfar", "ar.abdullahbasfar", "Abdullah Basfar"),
        Selection("ar.haniarrifai", "ar.hanirifai", "Hani Ar-Rifai"),
        Selection("ar.muhammadayyub", "ar.muhammadayyoub", "Muhammad Ayyub"),
        Selection("ar.muhammadjibreel", "ar.muhammadjibreel", "Muhammad Jibreel"),
        Selection("ar.ibrahimalakhdar", "ar.ibrahimakhbar", "Ibrahim Akhdar"),
        Selection("ar.aymanswed", "ar.aymanswoaid", "Ayman Sowaid"),
        Selection("ar.abdullahawadaljuhani", "ar.abdullahawadaljuhani", "Abdullah Awad Al-Juhani"),
        Selection("ar.abdulmohsenalqasim", "ar.abdulmohsenalqasim", "Abdul Muhsin Al-Qasim"),
        Selection("ar.salahalbudair", "ar.salahalbudair", "Salah Al-Budair"),
        Selection("ar.mahmoudalialbanna", "ar.mahmoudalialbanna", "Mahmoud Ali Al-Banna"),
        Selection("ar.khalifaaltunaiji", "ar.khalifaaltunaiji", "Khalifa Al-Tunaiji"),
        Selection("ar.mohamedtablawi", "ar.mohamedtablawi", "Muhammad At-Tablawi"),
        Selection("ar.salahbukhatir", "ar.salahbukhatir", "Salah Bukhatir"),
        Selection("ar.khaledalqahtani", "ar.khaledalqahtani", "Khalid Al-Qahtani"),
    )
    val chapterIds = selections.map { it.chapterId }
    val verseIds = selections.map { it.verseId }
    val names = selections.flatMap { listOf(it.chapterId to it.name, it.verseId to it.name) }.toMap()
    fun verseIdFor(id: String): String? = selections.firstOrNull { it.chapterId == id || it.verseId == id }?.verseId
    fun chapterIdFor(id: String): String? = selections.firstOrNull { it.chapterId == id || it.verseId == id }?.chapterId
}
