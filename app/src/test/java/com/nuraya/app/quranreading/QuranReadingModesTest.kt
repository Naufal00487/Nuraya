package com.nuraya.app.quranreading

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.data.QuranRepository
import com.nuraya.app.storage.UserSettings
import com.nuraya.app.study.StudyRepository
import com.nuraya.app.ui.annotatedStudyWord
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class QuranReadingModesTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    @Test fun pageReadersKeepEditionsSeparateAndPracticeModesKeepTheirOwnReader() {
        for (id in listOf("indonesia", "indonesia_tajwid")) {
            assertTrue(QuranReadingModes.opensPageReader(id))
            assertEquals(MushafEdition.INDONESIA, QuranReadingModes.pageEdition(id))
        }
        for (id in listOf("madinah", "madinah_tajwid")) {
            assertTrue(QuranReadingModes.opensPageReader(id))
            assertEquals(MushafEdition.MADINAH, QuranReadingModes.pageEdition(id))
        }
        for (id in listOf("standard", "word_tajwid", "latin_tajwid", "hafalan_8", "tikrar", "waqaf", "bahriyah", "thematic")) {
            assertFalse(QuranReadingModes.opensPageReader(id))
        }
    }

    @Test fun learningBlocksPreserveEverySelectedVerseOnceAndInOrder() {
        val repo = QuranRepository(context)
        for (verses in listOf(repo.verses(1), repo.verses(2).subList(4, 20), repo.juz(30), emptyList())) {
            val blocks = memorizationBlocks(verses)
            assertEquals(minOf(8, verses.size), blocks.size)
            assertEquals(verses, blocks.flatMap { it.verses })
            assertEquals((1..blocks.size).toList(), blocks.map { it.number })
            assertTrue(blocks.all { it.verses.isNotEmpty() })
            if (blocks.isNotEmpty()) assertTrue(blocks.maxOf { it.verses.size } - blocks.minOf { it.verses.size } <= 1)
        }
    }

    @Test fun readingModesDoNotOverwritePersonalPreferencesOrIgnoreColorOptOut() {
        assertEquals(12, QuranReadingModes.all.size)
        assertEquals(12, QuranReadingModes.all.map { it.id }.toSet().size)
        val preferences = UserSettings(translation = "en", arabicSize = 42f, tajwidColors = false)
        for (mode in QuranReadingModes.all) {
            val result = QuranReadingModes.settings(mode.id, preferences)
            assertEquals(42f, result.arabicSize, 0f)
            assertFalse(result.tajwidColors)
            assertEquals("en", preferences.translation)
        }
        assertEquals("id", QuranReadingModes.settings("indonesia_tajwid", preferences).translation)
        assertEquals("none", QuranReadingModes.settings("madinah", preferences).translation)
        assertEquals(preferences, QuranReadingModes.settings("standard", preferences))
    }

    @Test fun thematicIndexContainsOnlyRealReferencesWithIndonesianTranslations() {
        val repo = QuranRepository(context)
        assertTrue(QuranThemes.all.size >= 10)
        for (theme in QuranThemes.all) {
            assertEquals(theme.references.size, theme.references.distinct().size)
            for ((surah, ayah) in theme.references) {
                val verse = repo.verse(surah, ayah)
                assertNotNull("${theme.id}:$surah:$ayah", verse)
                assertTrue(verse!!.indonesian.isNotBlank())
            }
        }
    }

    @Test fun wordColorsAreClippedOnlyToExactSourceWordAndHonorDisabledColors() {
        val study = StudyRepository(context).verse(1, 1)!!
        for (word in study.words) {
            val styled = annotatedStudyWord(study, word, true, false)
            assertEquals(word.arabic, styled.text)
            assertTrue(styled.spanStyles.all { it.start >= 0 && it.end <= word.arabic.length })
            assertTrue(annotatedStudyWord(study, word, false, false).spanStyles.isEmpty())
        }
        val word = study.words.first()
        assertTrue(annotatedStudyWord(study, word.copy(start = -1), true, false).spanStyles.isEmpty())
        assertTrue(annotatedStudyWord(study, word.copy(arabic = word.arabic + "x"), true, false).spanStyles.isEmpty())
    }

    @Test fun tikrarCountsPersistAndStaySeparateForEachVerse() {
        val store = TikrarProgress(context)
        store.reset(1, 1); store.reset(1, 2)
        repeat(3) { store.increment(1, 1) }
        val restored = TikrarProgress(context)
        assertEquals(3, restored.count(1, 1))
        assertEquals(0, restored.count(1, 2))
        restored.reset(1, 1)
        assertEquals(0, store.count(1, 1))
    }

    @Test fun stopAnnotationsPreserveCanonicalLettersAndExcludeSeenInsideWords() {
        val quran = QuranRepository(context)
        val source = WaqafAnnotations(context)
        assertTrue(source.available)
        var count = 0
        for (verse in quran.allVerses) {
            val stops = source.forVerse(verse)
            count += stops.size
            val shown = annotatedWaqafText(androidx.compose.ui.text.AnnotatedString(verse.arabic), stops)
            assertEquals(verse.arabic, shown.text.replace(Regex(" [ۖۗۘۙۚۛۜ]"), ""))
        }
        assertEquals(4362, count)
        assertEquals(listOf("ۛ", "ۛ"), source.forVerse(quran.verse(2, 2)!!).map { it.mark })
        for (verse in listOf(quran.verse(2, 245)!!, quran.verse(7, 69)!!)) {
            assertFalse(WaqafGuide.inText(verse.arabic).any { it.mark == "ۜ" })
            assertFalse(source.forVerse(verse).any { it.mark == "ۜ" })
        }
        assertTrue(source.forVerse(quran.verse(75, 27)!!).any { it.mark == "ۜ" })
        assertFalse(source.verified(quran.verse(12, 39)!!))
        assertTrue(source.forVerse(quran.verse(12, 39)!!).isEmpty())
    }
}
