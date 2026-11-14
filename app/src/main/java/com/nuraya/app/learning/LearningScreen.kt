package com.nuraya.app.learning

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuraya.app.NurayaApplication
import com.nuraya.app.ui.QuranArabicFont
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private fun learningText(language: String, id: String, en: String) = if (language == "en") en else id

@Composable
fun LearningScreen(language: String, onBack: () -> Unit, onRead: (Int, Int) -> Unit = { _, _ -> }) {
    val application = LocalContext.current.applicationContext as NurayaApplication
    val repository = remember(application) { LocalLearningRepository(application, application.quran, application.study) }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var question by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var answer by remember { mutableStateOf<LocalLearningResponse?>(null) }
    var failure by remember { mutableStateOf(false) }
    var semantic by rememberSaveable { mutableStateOf(true) }
    var voice by remember { mutableStateOf(false) }
    fun search(value: String) {
        if (loading || value.trim().length !in 2..1000) return
        question = value
        keyboard?.hide()
        loading = true
        failure = false
        answer = null
        scope.launch {
            try { answer = repository.find(value, semantic) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { failure = true }
            finally { loading = false }
        }
    }

    if (voice) VoiceVerseSearchSheet(application, language, onDismiss = { voice = false }) { recognized -> voice = false; search(recognized) }
    Column(Modifier.fillMaxSize().testTag("learning_screen").verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, learningText(language, "Kembali", "Back")) }
            Column(Modifier.padding(start = 8.dp)) {
                Text(learningText(language, "TEMAN BELAJAR", "LEARNING COMPANION"), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
                Text(learningText(language, "Temukan rujukannya", "Find the source"), style = MaterialTheme.typography.headlineSmall)
            }
        }
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, null, Modifier.size(30.dp))
                Text(learningText(language, "Tanya dengan bahasamu sendiri", "Ask in your own words"), style = MaterialTheme.typography.titleLarge)
                Text(learningText(language,
                    "Cari berdasarkan makna, kata kunci, atau bacakan potongan ayat Arab. Temukan ayat, tafsir Kemenag, dan pelajaran tajwid. Rujukan dicari di HP tanpa akun atau koneksi internet; pertanyaan tidak dikirim ke server.",
                    "Find verses, Kemenag tafsir, or tajwid lessons. Sources are searched on your phone without an account or internet; questions are not sent to a server."))
                Text(learningText(language,
                    "Hasil berupa kutipan sumber yang mungkin relevan, bukan jawaban AI generatif atau fatwa.",
                    "Results are potentially relevant source passages, not generative AI answers or religious rulings."), style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(semantic, onClick = { semantic = true }, enabled = !loading, label = { Text(learningText(language, "Pencarian makna", "Meaning search")) }, modifier = Modifier.testTag("search_semantic"))
            FilterChip(!semantic, onClick = { semantic = false }, enabled = !loading, label = { Text(learningText(language, "Kata kunci", "Keywords")) }, modifier = Modifier.testTag("search_keywords"))
        }
        TextButton(onClick = { voice = true }, enabled = !loading, modifier = Modifier.fillMaxWidth().testTag("learning_voice_search")) {
            Icon(Icons.Outlined.Mic, null); Spacer(Modifier.width(8.dp)); Text(learningText(language, "Cari ayat lewat suara", "Find a verse by voice"))
        }
        OutlinedTextField(value = question, onValueChange = { question = it.take(1000) }, enabled = !loading,
            label = { Text(learningText(language, "Apa yang ingin dipelajari?", "What would you like to learn?")) },
            placeholder = { Text(learningText(language, "Contoh: hukum nun mati bertemu ba", "Example: explain patience in the Quran")) },
            minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth().testTag("learning_query"),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { search(question) }),
            supportingText = { Text("${question.length}/1000") }, shape = RoundedCornerShape(18.dp))
        Button(onClick = { search(question) }, enabled = !loading && question.trim().length >= 2,
            modifier = Modifier.fillMaxWidth().testTag("learning_search")) {
            if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Icon(Icons.Outlined.Search, null)
            Spacer(Modifier.width(10.dp))
            Text(learningText(language, if (loading) "Mencari rujukan…" else "Cari rujukan", if (loading) "Finding sources…" else "Find sources"))
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val examples = if (language == "en") listOf("What is iqlab?", "Verses about patience", "Explain 2:255")
                else listOf("Apa itu iqlab?", "Ayat tentang sabar", "Jelaskan 2:255")
            examples.forEach { example -> AssistChip(onClick = { search(example) }, enabled = !loading, label = { Text(example) }) }
        }
        if (failure) Text(learningText(language, "Data belajar belum dapat dibuka. Coba lagi atau buka ayat melalui menu Al-Qur’an.",
            "Learning data could not be opened. Retry or open a verse from the Quran section."), color = MaterialTheme.colorScheme.error)
        answer?.let { response ->
            if (response.status != "matches") {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(learningText(language, "Belum ada rujukan yang sesuai", "No matching sources yet"), style = MaterialTheme.typography.titleMedium)
                        Text(when (response.status) {
                            "invalid_reference" -> learningText(language, "Nomor ayat tidak ditemukan. Gunakan surah:ayat, misalnya 2:255, atau Al-Fatihah ayat 3.", "That verse number was not found. Use chapter:verse, such as 2:255, or Al-Fatihah verse 3.")
                            "outside_scope" -> learningText(language, "Teman belajar hanya mencari materi Al-Qur’an dan tajwid. Coba topik seperti sabar, salat, atau iqlab.", "This companion only searches Quran and tajwid learning material. Try patience, prayer, or iqlab.")
                            else -> learningText(language, "Coba sederhanakan topik, sebutkan istilah tajwid, atau tulis nomor ayat. Hasil kosong bukan berarti topik tersebut tidak dibahas dalam Al-Qur’an.", "Try a shorter topic, a tajwid term, or a verse number. No search results does not mean the Quran does not address the topic.")
                        })
                    }
                }
            } else {
                Text(learningText(language, "Rujukan untuk dipelajari", "Sources to study"), style = MaterialTheme.typography.titleLarge)
                response.lessons.forEach { lesson ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(learningText(language, "MATERI TAJWID NURAYA", "NURAYA TAJWID LESSON"), style = MaterialTheme.typography.labelSmall)
                            Text(if (language == "en") lesson.titleEn else lesson.titleId, style = MaterialTheme.typography.titleMedium)
                            SelectionContainer { Text(if (language == "en") lesson.explanationEn else lesson.explanationId) }
                            Text(lesson.sourceTitle, style = MaterialTheme.typography.labelSmall)
                            TextButton(onClick = { onRead(lesson.surah, lesson.ayah) }) {
                                Text(learningText(language, "Buka contoh ${lesson.surah}:${lesson.ayah}", "Open example ${lesson.surah}:${lesson.ayah}"))
                            }
                        }
                    }
                }
                response.matches.forEach { source ->
                    val verse = source.verse
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("${application.quran.surahs.first { it.id == verse.surah }.name} · ${verse.surah}:${verse.ayah}",
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            SelectionContainer { Text(verse.arabic, Modifier.fillMaxWidth(), fontFamily = QuranArabicFont, fontSize = 28.sp,
                                lineHeight = 48.sp, textAlign = TextAlign.Right, style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl)) }
                            SelectionContainer { Text(if (language == "en") verse.english else verse.indonesian) }
                            Text(if (language == "en") "Arabic: Tanzil · Translation: Marmaduke Pickthall (1930)"
                                else "Arab: Tanzil · Terjemahan: Kementerian Agama RI, QuranEnc v1.0.1", style = MaterialTheme.typography.labelSmall)
                            source.tafsirExcerpt?.let { excerpt ->
                                HorizontalDivider()
                                Text(learningText(language, "KUTIPAN TAFSIR · BAHASA INDONESIA", "TAFSIR EXCERPT · INDONESIAN"), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                                SelectionContainer { Text(excerpt) }
                                if (source.excerptTruncated) Text(learningText(language, "Sebagian kutipan ditampilkan. Buka ayat untuk membaca tafsir lengkap.",
                                    "An excerpt is shown. Open the verse to read the complete tafsir."), style = MaterialTheme.typography.bodySmall)
                                Text(source.tafsirSource.orEmpty(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { onRead(verse.surah, verse.ayah) }, modifier = Modifier.testTag("learning_open_${verse.surah}_${verse.ayah}")) {
                                Text(learningText(language, "Buka ayat, tafsir lengkap & audio", "Open verse, full tafsir & audio"))
                            }
                        }
                    }
                }
            }
        }
        Text(learningText(language,
            "Kecocokan pencarian belum tentu menjawab seluruh pertanyaan. Baca konteks lengkap dan diskusikan persoalan yang rumit atau diperdebatkan dengan guru.",
            "A search match may not answer the whole question. Read the full context and discuss complex or disputed matters with a qualified teacher."),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
    }
}
