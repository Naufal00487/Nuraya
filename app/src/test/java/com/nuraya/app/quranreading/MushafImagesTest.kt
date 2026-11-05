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
