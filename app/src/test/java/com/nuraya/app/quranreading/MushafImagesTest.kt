package com.nuraya.app.quranreading

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.data.QuranRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MushafImagesTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    @Test fun indonesiaPagesKeepTheirSourceIdentityAndNeverOverwriteSavedMadinahPages() = runBlocking {
        val indonesia = MushafImages(context, MushafEdition.INDONESIA)
        val madinah = MushafImages(context)
        assertEquals(6236, indonesia.references.map { it.surah to it.ayah }.distinct().size)
        assertEquals((1..604).toList(), indonesia.references.map { it.page }.distinct())
        val quran = QuranRepository(context)
        indonesia.references.forEach { assertNotNull(quran.verse(it.surah, it.ayah)) }
        for (page in listOf(1, 2, 3, 604)) {
            val bytes = context.assets.open("mushaf/indonesia/%03d.png".format(java.util.Locale.US, page)).use { it.readBytes() }
            assertTrue(indonesia.verified(page, bytes))
            assertFalse(madinah.verified(page, bytes))
            val bitmap = indonesia.load(page)
            assertEquals(641, bitmap.width)
            assertEquals(917, bitmap.height)
            bitmap.recycle()
            bytes[0] = (bytes[0].toInt() xor 1).toByte()
            assertFalse(indonesia.verified(page, bytes))
        }
        madinah.saveOffline(2)
        val oldPage = java.io.File(context.filesDir, "madinah-saved-v1/002.png")
        val original = oldPage.readBytes()
        indonesia.saveOffline(2)
        val newPage = java.io.File(context.filesDir, "indonesia-saved-v1/002.png")
        assertTrue(indonesia.verified(2, newPage.readBytes()))
        assertArrayEquals(original, oldPage.readBytes())
        for (entry in indonesia.pages) {
            assertTrue(MushafEdition.INDONESIA.accepts(java.net.URL(entry.url)))
            assertFalse(MushafEdition.MADINAH.accepts(java.net.URL(entry.url)))
        }
        assertFalse(MushafEdition.INDONESIA.accepts(java.net.URL(indonesia.pages.first().url.replace("https:", "http:"))))
    }

    @Test fun allCanonicalVersesHaveOneValidMadinahPageAndEveryPageIsAddressable() {
        val images = MushafImages(context)
        val quran = QuranRepository(context)
        assertEquals(6236, images.references.map { it.surah to it.ayah }.distinct().size)
        assertEquals((1..604).toList(), images.references.map { it.page }.distinct())
        images.references.forEach { assertNotNull(quran.verse(it.surah, it.ayah)) }
        assertEquals(1, images.pageFor(1, 1))
        assertEquals(2, images.pageFor(2, 1))
        assertEquals(3, images.pageFor(2, 6))
        assertEquals(604, images.pageFor(114, 6))
        assertEquals(2, images.firstReference(2).surah)
        assertEquals(1, images.firstReference(2).ayah)
    }

    @Test fun explicitlySavedPageRetainsTheExactVerifiedSourceBytes() = runBlocking {
        val images = MushafImages(context)
        images.saveOffline(2)
        val saved = java.io.File(context.filesDir, "madinah-saved-v1/002.png")
        assertTrue(saved.isFile)
        assertTrue(images.verified(2, saved.readBytes()))
        images.saveOffline(2)
        assertEquals(1, saved.parentFile!!.listFiles()!!.count { it.name == "002.png" })
        assertTrue(saved.parentFile!!.listFiles()!!.none { it.extension == "tmp" })
    }

    @Test fun bundledOriginalPagesMatchPinnedTreeAndCorruptionIsRejected() = runBlocking {
        val images = MushafImages(context)
        for (page in listOf(1, 2, 3, 604)) {
            val name = "%03d".format(java.util.Locale.US, page) + if (page == 3) ".jpg" else ".png"
            val bytes = context.assets.open("mushaf/$name").use { it.readBytes() }
            assertTrue(images.verified(page, bytes))
            bytes[0] = (bytes[0].toInt() xor 1).toByte()
            assertFalse(images.verified(page, bytes))
            val bitmap = images.load(page)
            assertTrue(bitmap.width in 100..1600)
            assertTrue(bitmap.height > bitmap.width)
        }
    }
}
