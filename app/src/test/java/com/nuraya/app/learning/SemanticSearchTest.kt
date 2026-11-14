package com.nuraya.app.learning

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.data.QuranRepository
import com.nuraya.app.study.StudyRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class SemanticSearchTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val quran = QuranRepository(context)
    private val study = StudyRepository(context)
    private val repository = LocalLearningRepository(context, quran, study)

    @Test fun meaningSearchRetrievesAttributedVersesForEverydayTopics() = runBlocking {
        val cases = linkedMapOf(
            "hati tenang mengingat Allah" to setOf(13 to 28),
            "berbuat baik kepada orang tua" to setOf(17 to 23, 17 to 24, 31 to 14, 46 to 15, 4 to 36, 2 to 83),
            "jangan menyerah pada ampunan Allah" to setOf(39 to 53, 12 to 87),
            "menjaga lisan dari gosip" to setOf(49 to 12, 104 to 1),
            "aku ingin belajar sabar" to setOf(2 to 153, 2 to 45, 74 to 7, 11 to 115, 8 to 46),
            "kesulitan akan berlalu" to setOf(94 to 5, 94 to 6, 65 to 7),
            "cemas karena takut miskin" to setOf(17 to 31, 2 to 268, 6 to 151),
        )
        val failed = mutableListOf<String>()
        for ((query, expected) in cases) {
            val result = repository.find(query, semantic = true)
            val refs = result.matches.map { it.verse.surah to it.verse.ayah }
            println("SEMANTIC $query -> $refs")
            if (refs.none { it in expected }) failed += "$query -> $refs"
            for (match in result.matches) {
                assertEquals(quran.verse(match.verse.surah, match.verse.ayah), match.verse)
                match.tafsirExcerpt?.let { assertTrue(study.verse(match.verse.surah, match.verse.ayah)!!.tafsir!!.contains(it)) }
            }
        }
        assertTrue("Topics without expected sources: $failed", failed.isEmpty())
    }

    @Test fun unknownQueriesAndInvalidReferencesDoNotProduceInventedAnswers() = runBlocking {
        assertEquals("no_results", repository.find("zxqvzzzz", true).status)
        assertEquals("invalid_reference", repository.find("114:7", true).status)
        assertEquals("outside_scope", repository.find("buat kode python", true).status)
        assertEquals(quran.verse(2, 255), repository.find("Al-Baqarah ayat 255", true).matches.single().verse)
        assertTrue(SemanticVerseIndex(context).scores("aku ingin belajar sabar").all { it.isFinite() && it in -1f..1f })
    }

    @Test fun spokenArabicFragmentsFindAnExactVerseBeyondItsBeginning() {
        val search = SpokenVerseSearch(quran)
        assertEquals(2 to 255, search.find("لا تأخذه سنة ولا نوم له ما في السماوات وما في الأرض").first().let { it.surah to it.ayah })
        assertTrue(search.find("فإن مع العسر يسرا").any { it.surah == 94 && it.ayah == 5 })
        assertTrue(search.find("قل هو الله أحد").any { it.surah == 112 && it.ayah == 1 })
        assertTrue(search.find("الله").isEmpty())
        assertTrue(search.find("هذا نص تجريبي ليس من القرآن").isEmpty())
        assertEquals(31, search.find("فبأي آلاء ربكما تكذبان").let { matches ->
            // The result is deliberately bounded, but repeated verses are not collapsed into one.
            assertEquals(8, matches.size)
            quran.allVerses.count { it.surah == 55 && it.arabic == quran.verse(55, 13)!!.arabic }
        })
    }
}
