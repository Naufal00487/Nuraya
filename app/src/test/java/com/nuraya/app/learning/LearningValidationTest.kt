package com.nuraya.app.learning

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.data.QuranRepository
import java.io.ByteArrayInputStream
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class LearningValidationTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val quran = QuranRepository(context)

    @Test fun unconfiguredBuildCannotSendQuestions() = runBlocking {
        val repository = LearningRepository(context, quran)
        assertFalse(repository.configured)
        try {
            repository.ask("Explain 1:1", "en")
            fail("An unconfigured build must not issue a request")
        } catch (expected: IllegalArgumentException) {
            assertEquals("unconfigured", expected.message)
        }
    }

    @Test fun citationsMustExistInActualCorpus() {
        val invalid = """{"answer":"Unreviewed explanation","citations":[{"surah":1,"ayah":8}]}"""
        assertThrows(IOException::class.java) { LearningRepository.parseAnswer(invalid, quran) }
    }

    @Test fun citationsRejectCoercedNumericTypes() {
        listOf("1.1", "\"1\"", "true").forEach { value ->
            val invalid = """{"answer":"Unreviewed explanation","citations":[{"surah":$value,"ayah":1}]}"""
            assertThrows(IOException::class.java) { LearningRepository.parseAnswer(invalid, quran) }
        }
    }

    @Test fun generatedQuotationAndUrlAreNotUsedAsQuranSource() {
        val response = """{"answer":"Check the original source.","citations":[{"surah":1,"ayah":1,"quote":"invented fixture, not religious text","sourceUrl":"https://invalid.example"}]}"""
        val parsed = LearningRepository.parseAnswer(response, quran)
        assertEquals(listOf(QuranCitation(1, 1)), parsed.citations)
        assertEquals("https://quran.com/1/1", parsed.citations.single().sourceUrl)
        assertNotNull(quran.verse(parsed.citations.single().surah, parsed.citations.single().ayah))
    }

    @Test fun boundedInputRejectsOversizedResponse() {
        assertEquals(12, LearningRepository.readBounded(ByteArrayInputStream(ByteArray(12)), 12).size)
        assertThrows(IOException::class.java) {
            LearningRepository.readBounded(ByteArrayInputStream(ByteArray(13)), 12)
        }
    }
}
