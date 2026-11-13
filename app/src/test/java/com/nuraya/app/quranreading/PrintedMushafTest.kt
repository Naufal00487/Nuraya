package com.nuraya.app.quranreading

import android.app.Application
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.data.QuranRepository
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PrintedMushafTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()
    private fun repo(edition: PrintedMushafEdition) = MushafImages(context, printedEdition = edition)

    @Test fun sevenEditionsHave604PinnedPagesAndCorrectDestinationsForEveryVerse() {
        val canonical = QuranRepository(context).allVerses.map { it.surah to it.ayah }
        val root = context.assets.open("printed_mushaf/catalog.json").bufferedReader().use { JSONObject(it.readText()) }
        assertEquals(PrintedMushafEdition.COMMIT, root.getString("commit"))
        assertEquals(7, root.getJSONArray("editions").length())
        for (edition in PrintedMushafEdition.entries) {
            val source = repo(edition)
            assertEquals((1..604).toList(), source.pages.map { it.number })
            assertEquals(604, source.pages.map { it.gitBlobSha1 }.distinct().size)
            assertTrue(source.pages.all { it.url == PrintedMushafEdition.BASE_URL + edition.id + "/%03d.png".format(java.util.Locale.US, it.number) })
            assertEquals(canonical, source.references.map { it.surah to it.ayah })
            assertEquals((1..604).toSet(), source.references.map { it.page }.toSet())
            for (ref in source.references) assertEquals(ref.page, source.pageFor(ref.surah, ref.ayah))
            for (surah in 1..114) {
                val page = source.pageFor(surah, 1)
                assertTrue(source.references.any { it.surah == surah && it.ayah == 1 && it.page == page })
            }
            assertEquals(604, source.pageFor(114, 1))
            assertTrue(source.totalBytes > 1_000_000)
        }
    }

    @Test fun geometryCoversEveryVerseAndUsesTheMatchingPrintedIndex() {
        for (edition in PrintedMushafEdition.entries) {
            val source = repo(edition)
            if (edition.mapping == null) {
                assertTrue((1..604).all { source.regionsFor(it).isEmpty() })
                continue
            }
            val mapped = (1..604).flatMap { page -> source.regionsFor(page).onEach { region ->
                assertEquals(page, source.pageFor(region.surah, region.ayah))
                assertTrue(region.rectangles.isNotEmpty())
                region.rectangles.forEach { box ->
                    assertTrue(box.left >= 0 && box.top >= 0 && box.right <= edition.width && box.bottom <= edition.height)
                    assertTrue(box.right > box.left && box.bottom > box.top)
                }
            } }
            assertEquals(6236, mapped.size)
            assertEquals(6236, mapped.map { it.surah to it.ayah }.distinct().size)
        }
        val word = repo(PrintedMushafEdition.WORD_TAJWID)
        assertEquals(219, word.pageFor(10, 95))
        assertEquals(listOf(MushafRect(21, 1572, 242, 1708), MushafRect(21, 1708, 1023, 1844)), word.regionsFor(219).single { it.surah == 10 && it.ayah == 95 }.rectangles)
        val madinah = repo(PrintedMushafEdition.MADINAH)
        assertEquals(567, madinah.pageFor(69, 35))
        assertEquals(listOf(MushafRect(12, 1556, 469, 1684)), madinah.regionsFor(567).single { it.surah == 69 && it.ayah == 35 }.rectangles)
        assertEquals(568, MushafImages(context).pageFor(69, 35)) // Deliberately different legacy print layout.
    }

    @Test fun bundledArtworksRetainTheirOriginalBytesDimensionsAndTransparency() = runBlocking {
        val hashes = mutableSetOf<String>()
        for (edition in PrintedMushafEdition.entries) {
            val source = repo(edition)
            for (page in listOf(1, 2, 3, 7, 8, 604)) {
                val bytes = context.assets.open("printed_mushaf/${edition.id}/%03d.png".format(java.util.Locale.US, page)).use { it.readBytes() }
                assertTrue(source.verified(page, bytes))
                assertFalse(source.verified(page, bytes + 0.toByte()))
                val bitmap = source.load(page)
                assertEquals(edition.width, bitmap.width)
                assertEquals(edition.height, bitmap.height)
                assertEquals(0, Color.alpha(bitmap.getPixel(0, 0)))
                bitmap.recycle()
            }
            hashes += source.pages[2].gitBlobSha1
        }
        assertEquals("Each printed edition must have its own artwork", 7, hashes.size)
    }

    @Test fun imageCoordinatesSurviveLetterboxingLandscapeAndZoomWithoutSelectingMargins() {
        for ((width, height) in listOf(411f to 891f, 891f to 411f, 822f to 1676f)) {
            val placement = MushafImagePlacement.fit(1080, 2202, width, height)
            val box = MushafRect(21, 1572, 242, 1708)
            val (x, y) = placement.imagePoint(placement.left + 100 * placement.scale, placement.top + 1600 * placement.scale)
            assertTrue(box.contains(x, y))
            assertEquals(100f, x, .001f)
            assertEquals(1600f, y, .001f)
            val margin = placement.imagePoint(0f, 0f)
            assertFalse(box.contains(margin.first, margin.second))
        }
    }
}
