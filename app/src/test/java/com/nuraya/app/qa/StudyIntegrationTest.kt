package com.nuraya.app.qa

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.data.QuranRepository
import com.nuraya.app.storage.SettingsRepository
import com.nuraya.app.storage.UserSettings
import com.nuraya.app.study.StudyVerse
import com.nuraya.app.study.StudyRepository
import com.nuraya.app.study.StudyWord
import com.nuraya.app.study.TajwidSpan
import com.nuraya.app.ui.annotatedQuranText
import com.nuraya.app.ui.TajwidPalette
import java.security.MessageDigest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Cross-feature checks run against packaged data and the actual persistent settings. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class StudyIntegrationTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun addingStudyFeaturesDoesNotReplaceTheVerifiedCanonicalCorpus() {
        val bytes = context.assets.open("quran.json").use { it.readBytes() }
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        assertEquals("Study imports must preserve the verified 1.0.1 source text",
            "f41cef112e2efd581d3ea15ad7229a713d18122a6e0fbec4ddb0c00b7d1cae9c", digest)

        val quran = QuranRepository(context)
        assertEquals(6236, quran.allVerses.size)
        assertEquals(112, quran.allVerses.count { it.openingBasmalah != null })
        assertEquals("الٓمٓ", quran.verse(2, 1)?.arabic)
        assertNull(quran.verse(1, 1)?.openingBasmalah)
        assertNull(quran.verse(9, 1)?.openingBasmalah)
        assertTrue(quran.verse(27, 30)!!.arabic.contains("بِسْمِ ٱللَّهِ"))
    }

    @Test fun colorPreferenceStartsEnabledAndPersistsAlongsideExistingPreferences() = runBlocking {
        assertTrue(UserSettings().tajwidColors)
        val repository = SettingsRepository(context)
        repository.setLanguage("en")
        repository.setTranslation("id")
        repository.setArabicSize(38f)
        repository.setDarkMode(true)
        assertTrue("Existing installations without the new preference default to enabled",
            repository.settings.first().tajwidColors)

        repository.setTajwidColors(false)
        repository.setFollowRecitation(false)
        repository.setReaderQari("ar.alafasy")

        val restored = SettingsRepository(context).settings.first()
        assertFalse(restored.tajwidColors)
        assertFalse(restored.followRecitation)
        assertEquals("ar.alafasy", restored.readerQariId)
        assertEquals("en", restored.language)
        assertEquals("id", restored.translation)
        assertEquals(38f, restored.arabicSize, 0f)
        assertTrue(restored.darkMode)

        repository.setTajwidColors(true)
        assertTrue(SettingsRepository(context).settings.first().tajwidColors)
    }

    @Test fun colorsNeverChangeTextAndAnnotationsRequireTheirExactSource() {
        val study = annotationFixture()
        val colored = annotatedQuranText(study.arabic, study, colors = true, dark = false)
        assertEquals(study.arabic, colored.text)
        assertEquals(1, colored.spanStyles.size)

        val disabled = annotatedQuranText(study.arabic, study, colors = false, dark = false)
        assertEquals(study.arabic, disabled.text)
        assertTrue(disabled.spanStyles.isEmpty())

        // A visually similar string with different combining marks is not the same source.
        val differentArabic = "نّ"
        val mismatch = annotatedQuranText(differentArabic, study, colors = true, dark = false, wordPosition = 1)
        assertEquals(differentArabic, mismatch.text)
        assertTrue(mismatch.spanStyles.isEmpty())
    }

    @Test fun malformedExternalRangesDoNotCrashOrStyleUnrelatedCharacters() {
        val original = annotationFixture()
        val malformed = original.copy(
            tajwidSpans = listOf(
                TajwidSpan(-1, 2, "ghunnah"),
                TajwidSpan(0, original.arabic.length + 1, "ghunnah"),
                TajwidSpan(1, 1, "ghunnah"),
                TajwidSpan(0, original.arabic.length, "unknown-provider-rule"),
            ),
            words = original.words.map { it.copy(start = 0, end = original.arabic.length + 1) },
        )
        val result = annotatedQuranText(malformed.arabic, malformed, colors = true, dark = true, wordPosition = 1)
        assertEquals(malformed.arabic, result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test fun followingAWordRemainsVisibleWhenTajwidColorsAreDisabled() {
        val study = annotationFixture()
        val highlighted = annotatedQuranText(study.arabic, study, colors = false, dark = false, wordPosition = 1)
        assertEquals(study.arabic, highlighted.text)
        assertEquals(1, highlighted.spanStyles.size)
        assertEquals(0, highlighted.spanStyles.single().start)
        assertEquals(study.arabic.length, highlighted.spanStyles.single().end)
        assertTrue(annotatedQuranText(study.arabic, study, colors = false, dark = false, wordPosition = 2).spanStyles.isEmpty())
    }

    @Test fun sourceIdghaamNamesDoNotMistakeWithoutGhunnahForGhunnah() {
        for (dark in listOf(false, true)) {
            val idgham = TajwidPalette.color("idgham", dark)
            assertNotNull(idgham)
            assertEquals(idgham, TajwidPalette.color("idghaam_no_ghunnah", dark))
            assertEquals(idgham, TajwidPalette.color("idghaam_mutajanisayn", dark))
            assertNotEquals(TajwidPalette.color("ghunnah", dark), TajwidPalette.color("idghaam_no_ghunnah", dark))
        }
    }

    @Test fun allPackagedStudyReferencesAlignWithTheCanonicalReaderAndCarrySources() {
        val quran = QuranRepository(context)
        val repository = StudyRepository(context)
        assertTrue("The attributed offline study dataset must be packaged", repository.available)
        assertEquals(3, repository.sources.size)
        var verses = 0
        var words = 0
        var spans = 0
        val unmapped = mutableListOf<String>()
        quran.surahs.forEach { surah ->
            val chapter = repository.chapter(surah.id)
            assertEquals(surah.ayahCount, chapter.size)
            assertEquals((1..surah.ayahCount).toList(), chapter.map { it.ayah })
            chapter.forEach { study ->
                val reference = "${study.surah}:${study.ayah}"
                assertEquals(reference, quran.verse(study.surah, study.ayah)!!.arabic, study.arabic)
                assertFalse("Missing tafsir at $reference", study.tafsir.isNullOrBlank())
                assertFalse(study.tafsirSource.isNullOrBlank())
                assertFalse(study.tafsirLanguage.isNullOrBlank())
                assertFalse(study.wordsSource.isNullOrBlank())
                assertFalse(study.tajwidSource.isNullOrBlank())
                assertEquals((1..study.words.size).toList(), study.words.map { it.position })
                var previousEnd = 0
                study.words.forEach { word ->
                    assertFalse("Missing Indonesian word meaning at $reference:${word.position}", word.indonesian.isNullOrBlank())
                    assertFalse("Missing English word meaning at $reference:${word.position}", word.english.isNullOrBlank())
                    assertEquals("Word offsets must both exist or both be absent", word.start == null, word.end == null)
                    val start = word.start
                    val end = word.end
                    if (start == null || end == null) {
                        unmapped += "$reference:${word.position}"
                    } else {
                        assertTrue("Word offsets must remain ordered at $reference", start >= previousEnd)
                        assertTrue(start >= 0 && end > start && end <= study.arabic.length)
                        assertEquals("Word highlight must select the exact displayed word at $reference", word.arabic, study.arabic.substring(start, end))
                        previousEnd = end
                    }
                }
                study.tajwidSpans.forEach { span ->
                    assertTrue(span.start >= 0 && span.end > span.start && span.end <= study.arabic.length)
                    assertNotNull("Known imported rule needs a legend color: ${span.rule}", TajwidPalette.color(span.rule, false))
                }
                verses++
                words += study.words.size
                spans += study.tajwidSpans.size
            }
        }
        assertEquals(6236, verses)
        assertEquals(77429, words)
        assertEquals(60023, spans)
        assertEquals("The known different source spelling must never shift subsequent word offsets", listOf("11:13:3"), unmapped)
    }

    /** Synthetic fragment used only to exercise annotation bounds, not a Quran data source. */
    private fun annotationFixture(): StudyVerse {
        val fragment = "نَّ"
        return StudyVerse(
            surah = 1, ayah = 1, arabic = fragment,
            tajwidSpans = listOf(TajwidSpan(0, fragment.length, "ghunnah")),
            words = listOf(StudyWord(1, fragment, null, null, 0, fragment.length)),
            tafsir = null, tafsirSource = null, tafsirLanguage = null,
        )
    }
}
