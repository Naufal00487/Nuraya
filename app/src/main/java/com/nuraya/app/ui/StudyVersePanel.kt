package com.nuraya.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuraya.app.study.StudyVerse
import com.nuraya.app.study.StudyWord

/** Colors decorate verified source spans only; unknown rules keep the normal text color. */
object TajwidPalette {
    fun color(rule: String, dark: Boolean): Color? = when (rule.lowercase(java.util.Locale.ROOT)) {
        "ghunnah", "ikhfa", "ikhfa_shafawi" -> if (dark) Color(0xFF9BE3AB) else Color(0xFF28733F)
        "idgham", "idghaam_ghunnah", "idghaam_no_ghunnah", "idghaam_mutajanisayn", "idghaam_mutaqaribayn", "idghaam_shafawi", "lam_shamsiyyah" -> if (dark) Color(0xFFFFC68C) else Color(0xFF975418)
        "iqlab" -> if (dark) Color(0xFFD4B5FA) else Color(0xFF794CA8)
        "qalqalah" -> if (dark) Color(0xFF8DCBF1) else Color(0xFF226C9C)
        "madd", "madd_2", "madd_246", "madd_6", "madd_munfasil", "madd_muttasil" -> if (dark) Color(0xFFFFA7AD) else Color(0xFFB03443)
        "hamzat_wasl", "silent" -> if (dark) Color(0xFFB5BFC6) else Color(0xFF65717C)
        else -> null
    }
}

fun annotatedQuranText(arabic: String, study: StudyVerse?, colors: Boolean, dark: Boolean, wordPosition: Int = 0): AnnotatedString = buildAnnotatedString {
    append(arabic)
    // Annotation offsets are meaningful only for the exact displayed source string.
    if (study?.arabic != arabic) return@buildAnnotatedString
    if (colors) study.tajwidSpans.forEach { span ->
        if (span.start >= 0 && span.end > span.start && span.end <= arabic.length) {
            TajwidPalette.color(span.rule, dark)?.let { addStyle(SpanStyle(color = it), span.start, span.end) }
        }
    }
    if (wordPosition > 0) study.words.firstOrNull { it.position == wordPosition }?.let { word ->
        val start = word.start
        val end = word.end
        if (start != null && end != null && start >= 0 && end > start && end <= arabic.length) {
            addStyle(SpanStyle(background = if (dark) Color(0xFF465C3B) else Color(0xFFFFE9A7)), start, end)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TajwidLegend(language: String, dark: Boolean) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("ghunnah" to "Ghunnah / Ikhfa", "idgham" to "Idgham / Syamsiyyah", "iqlab" to "Iqlab", "qalqalah" to "Qalqalah", "madd" to "Mad", "silent" to label(language, "Hamzah wasal / huruf tidak dibaca", "Hamzat wasl / silent letters")).forEach { (rule, title) ->
            Text("● $title", style = MaterialTheme.typography.labelSmall, color = TajwidPalette.color(rule, dark)!!)
        }
    }
    Text(label(language, "Warna membantu mengenali hukum; panjang dan cara membaca dipelajari bersama contoh audio/guru.",
        "Colors identify rules; learn their duration and pronunciation with audio examples and a teacher."),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(label(language, "Hamzah wasal dibaca ketika memulai kata, dengan harakat sesuai kaidah; bunyinya gugur ketika disambung dari kata sebelumnya. Warna abu-abu juga dipakai untuk anotasi huruf yang tidak dibaca.",
        "Hamzat wasl is pronounced when starting a word, with the vowel prescribed by its rule; its sound is omitted when joining from the preceding word. Gray also marks annotated silent letters."),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudyVersePanel(study: StudyVerse?, language: String, initiallyShowWords: Boolean = false, tajwidColors: Boolean = false, dark: Boolean = false) {
    var wordsOpen by rememberSaveable(study?.surah, study?.ayah, initiallyShowWords) { mutableStateOf(initiallyShowWords) }
    var tafsirOpen by rememberSaveable(study?.surah, study?.ayah) { mutableStateOf(false) }
    if (study == null) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (study.words.isNotEmpty()) FilterChip(selected = wordsOpen, onClick = { wordsOpen = !wordsOpen },
            label = { Text(label(language, "Arti per kata", "Word meanings")) })
        if (!study.tafsir.isNullOrBlank()) FilterChip(selected = tafsirOpen, onClick = { tafsirOpen = !tafsirOpen },
            label = { Text(label(language, "Tafsir", "Tafsir")) })
    }
    if (wordsOpen) {
        if (language == "id" && study.words.any { it.indonesian == null && it.english != null }) {
            Text("Kata tanpa terjemahan Indonesia memakai terjemahan Inggris dari sumber.", style = MaterialTheme.typography.bodySmall)
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                study.words.forEach { word ->
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Column(Modifier.widthIn(min = 76.dp, max = 170.dp).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(annotatedStudyWord(study, word, tajwidColors, dark), fontFamily = QuranArabicFont, fontSize = 28.sp, lineHeight = 46.sp,
                                textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl))
                            val meaning = if (language == "id") word.indonesian ?: word.english else word.english ?: word.indonesian
                            Text(meaning.orEmpty(), textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr))
                        }
                    }
                }
            }
        }
        Text(study.wordsSource.orEmpty(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
    }
    if (tafsirOpen) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(study.tafsirSource.orEmpty(), style = MaterialTheme.typography.titleSmall)
                if (!study.tafsirLanguage.isNullOrBlank()) Text(study.tafsirLanguage.orEmpty(), style = MaterialTheme.typography.labelSmall)
                Text(study.tafsir.orEmpty(), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/** Apply only exact source offsets; an unmapped word keeps its original text and no guessed colors. */
fun annotatedStudyWord(study: StudyVerse, word: StudyWord, colors: Boolean, dark: Boolean): AnnotatedString {
    val start = word.start
    val end = word.end
    if (start == null || end == null || start < 0 || end <= start || end > study.arabic.length ||
        study.arabic.substring(start, end) != word.arabic) return AnnotatedString(word.arabic)
    return annotatedQuranText(study.arabic, study, colors, dark).subSequence(start, end)
}
