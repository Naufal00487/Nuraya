package com.nuraya.app.recitation

import org.junit.Assert.*
import org.junit.Test

class RecitationMatcherTest {
    @Test fun markedArabicMatchesPlainRecognitionWithoutClaimingDiacriticAccuracy() {
        val result = RecitationMatcher.compare("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", "بسم الله الرحمن الرحيم", true)
        assertEquals(4, result.matchedCount)
        assertTrue(result.extraWords.isEmpty())
    }

    @Test fun partialUtteranceNeverMarksUnreadTailAsWrongOrMissing() {
        val result = RecitationMatcher.compare("الحمد لله رب العالمين", "الحمد لله", false)
        assertEquals(listOf(WordMatch.MATCHED, WordMatch.MATCHED, WordMatch.PENDING, WordMatch.PENDING), result.words.map { it.match })
        assertFalse(result.finalResult)
    }

    @Test fun partialRecognitionMistakeDoesNotBecomeAnErrorAccusation() {
        val result = RecitationMatcher.compare("الحمد لله رب العالمين", "الحمد لنا رب", false)
        assertTrue(result.words.all { it.match in setOf(WordMatch.MATCHED, WordMatch.PENDING) })
        assertTrue(result.extraWords.isEmpty())
    }

    @Test fun missingFinalWordDoesNotShiftTheRemainingWords() {
        val result = RecitationMatcher.compare("الحمد لله رب العالمين", "الحمد رب العالمين", true)
        assertEquals(listOf(WordMatch.MATCHED, WordMatch.NOT_HEARD, WordMatch.MATCHED, WordMatch.MATCHED), result.words.map { it.match })
    }

    @Test fun unexpectedSpokenWordDoesNotShiftTheVerse() {
        val result = RecitationMatcher.compare("الحمد لله رب العالمين", "الحمد لله نعم رب العالمين", true)
        assertEquals(4, result.matchedCount)
        assertEquals(listOf("نعم"), result.extraWords)
    }

    @Test fun repeatedWordsRequireRepeatedSpeech() {
        val result = RecitationMatcher.compare("قل قل هو", "قل هو", true)
        assertEquals(2, result.matchedCount)
        assertEquals(1, result.words.count { it.match == WordMatch.NOT_HEARD })
        assertEquals(WordMatch.MATCHED, result.words.last().match)
    }

    @Test fun displayRangesKeepOriginalDiacriticsAndSkipStandalonePauseMarks() {
        val text = "قُلْ ۖ هُوَ ٱللَّهُ"
        val result = RecitationMatcher.compare(text, "قل هو الله", true)
        assertEquals(3, result.words.size)
        result.words.forEach { assertEquals(it.text, text.substring(it.start, it.end)) }
        assertEquals(3, result.matchedCount)
    }

    @Test fun emptyOrNonArabicInterimResultsDoNotInventMatches() {
        val result = RecitationMatcher.compare("قل هو الله", "hello 123", false)
        assertEquals(0, result.matchedCount)
        assertTrue(result.words.all { it.match == WordMatch.PENDING })
    }

    @Test fun finalSubstitutionIsMarkedForReview() {
        val result = RecitationMatcher.compare("قل هو الله", "قل هي الله", true)
        assertEquals(listOf(WordMatch.MATCHED, WordMatch.DIFFERENT, WordMatch.MATCHED), result.words.map { it.match })
    }

    @Test fun commonUthmaniFormsAcceptOrdinarySpeechTranscription() {
        val cases = listOf(
            "ٱلْعَٰلَمِينَ" to "العالمين", "ٱلرَّحْمَٰنِ" to "الرحمن",
            "ذَٰلِكَ" to "ذلك", "ٱلصَّلَوٰةَ" to "الصلاة",
            "ٱلزَّكَوٰةَ" to "الزكاة", "ٱلْحَيَوٰةَ" to "الحياة",
            "وَٰحِدٌ" to "واحد",
        )
        cases.forEach { (source, recognized) ->
            val result = RecitationMatcher.compare(source, recognized, true)
            assertEquals(source, WordMatch.MATCHED, result.words.single().match)
            assertEquals(source, result.words.single().text)
        }
    }

    @Test fun normalAlefIsNeverRemovedAndConsonantWawIsNeverReplaced() {
        assertEquals(WordMatch.DIFFERENT, RecitationMatcher.compare("قَالَ", "قل", true).words.single().match)
        assertEquals(WordMatch.DIFFERENT, RecitationMatcher.compare("وَٰحِدٌ", "أحد", true).words.single().match)
    }
}
