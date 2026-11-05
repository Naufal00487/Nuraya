package com.nuraya.app.qa

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.data.QuranOpeningPolicy
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Regression checks use the actual packaged source, including both basmalah forms. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class QuranOpeningPolicyTest {
    private lateinit var source: List<JSONObject>

    @Before fun loadSource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val corpus = context.assets.open("quran.json").bufferedReader(Charsets.UTF_8)
            .use { JSONObject(it.readText()) }.getJSONArray("verses")
        source = List(corpus.length()) { corpus.getJSONObject(it) }
    }

    private fun raw(surah: Int, ayah: Int) = source.single {
        it.getInt("surah") == surah && it.getInt("ayah") == ayah
    }.getString("arabic")

    @Test fun all114OpeningsKeepExactSourceAndOnly112HaveUnnumberedIntros() {
        var introCount = 0
        (1..114).forEach { surah ->
            val original = raw(surah, 1)
            val result = QuranOpeningPolicy.split(surah, 1, original)
            if (surah == 1 || surah == 9) {
                assertNull("No unnumbered basmalah at $surah:1", result.openingBasmalah)
                assertEquals(original, result.verseText)
            } else {
                assertNotNull("Missing unnumbered basmalah at $surah:1", result.openingBasmalah)
                assertEquals("Source code points changed at $surah:1",
                    original, "${result.openingBasmalah} ${result.verseText}")
                assertTrue(result.verseText.isNotBlank())
                assertFalse(result.verseText.startsWith("بِسْمِ ٱللَّهِ"))
                assertFalse(result.verseText.startsWith("بِّسْمِ ٱللَّهِ"))
                introCount++
            }
        }
        assertEquals(112, introCount)
    }

    @Test fun knownFirstAyatAreTheActualVerses() {
        mapOf(
            2 to "الٓمٓ",
            3 to "الٓمٓ",
            55 to "ٱلرَّحْمَٰنُ",
            95 to "وَٱلتِّينِ وَٱلزَّيْتُونِ",
            97 to "إِنَّآ أَنزَلْنَٰهُ فِى لَيْلَةِ ٱلْقَدْرِ",
            112 to "قُلْ هُوَ ٱللَّهُ أَحَدٌ",
            113 to "قُلْ أَعُوذُ بِرَبِّ ٱلْفَلَقِ",
            114 to "قُلْ أَعُوذُ بِرَبِّ ٱلنَّاسِ",
        ).forEach { (surah, expected) ->
            assertEquals("Wrong first verse at $surah:1", expected,
                QuranOpeningPolicy.split(surah, 1, raw(surah, 1)).verseText)
        }
    }

    @Test fun fatihaBasmalahRemainsNumberedVerseOneAndTawbahHasNoIntro() {
        assertEquals("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
            QuranOpeningPolicy.split(1, 1, raw(1, 1)).verseText)
        assertTrue(QuranOpeningPolicy.split(9, 1, raw(9, 1)).verseText.startsWith("بَرَآءَةٌ"))
        assertNull(QuranOpeningPolicy.split(1, 1, raw(1, 1)).openingBasmalah)
        assertNull(QuranOpeningPolicy.split(9, 1, raw(9, 1)).openingBasmalah)
    }

    @Test fun everyOtherAyahIncludingNaml30IsUntouched() {
        source.filter { it.getInt("ayah") != 1 }.forEach { item ->
            val original = item.getString("arabic")
            val result = QuranOpeningPolicy.split(item.getInt("surah"), item.getInt("ayah"), original)
            assertEquals(original, result.verseText)
            assertNull(result.openingBasmalah)
        }
        val naml = QuranOpeningPolicy.split(27, 30, raw(27, 30))
        assertTrue(naml.verseText.contains("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"))
        assertEquals(raw(27, 30), naml.verseText)
    }

    @Test fun exactSourceDiacriticsAtTinAndQadrArePreserved() {
        listOf(95, 97).forEach { surah ->
            val result = QuranOpeningPolicy.split(surah, 1, raw(surah, 1))
            assertTrue(result.openingBasmalah!!.startsWith("بِّسْمِ"))
            assertEquals(raw(surah, 1), "${result.openingBasmalah} ${result.verseText}")
        }
    }

    @Test fun alreadySeparatedTextAndUnknownVariantsAreNeverGuessed() {
        listOf("الٓمٓ", "بسم الله الرحمن الرحيم الم", "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ").forEach { original ->
            val result = QuranOpeningPolicy.split(2, 1, original)
            assertEquals(original, result.verseText)
            assertNull(result.openingBasmalah)
        }
    }
}
