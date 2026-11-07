package com.nuraya.app.qa

import android.app.Application
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.ui.QariPortraitCatalog
import java.security.MessageDigest
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class QariPortraitTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    @Test fun allBundledPhotographsMatchTheirRecordedSourceHashesAndDecode() {
        val catalog = QariPortraitCatalog(context)
        assertEquals(134, catalog.entries.size)
        assertEquals(8,catalog.entries.count{!it.id.startsWith("user_")})
        catalog.entries.forEach { entry ->
            val bytes = context.assets.open(entry.asset).use { it.readBytes() }
            assertEquals(entry.bytes, bytes.size.toLong())
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            assertEquals(entry.id, entry.sha256, digest)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            assertTrue(entry.id, bounds.outWidth > 0 && bounds.outHeight > 0)
            if(entry.id.startsWith("user_"))assertEquals("Dokumen foto qari dari penyusun",entry.sourceName)
            else assertTrue(entry.sourcePage.startsWith("https://quran.com/reciters/"))
        }
    }

    @Test fun aliasesBelongToRealAudioEditionIdsWithoutFuzzyNameMatching() {
        val catalog = QariPortraitCatalog(context)
        val qariIds = listOf("reciters.json" to "qaris", "verse_reciters.json" to "reciters").flatMap { (file, key) ->
            context.assets.open(file).bufferedReader().use { reader ->
                val root = JSONObject(reader.readText())
                val array = root.optJSONArray(key) ?: root.getJSONArray("qaris")
                List(array.length()) { array.getJSONObject(it).getString("id") }
            }
        }.toSet()
        catalog.entries.forEach { entry -> entry.qariIds.forEach { id ->
            assertTrue("Missing edition $id", id in qariIds)
            assertEquals(entry.id, catalog.find(id)?.id)
        } }
        assertEquals("minshawi", catalog.find("ar.muhammadsiddiqalminshawimujawwad")?.id)
        assertEquals("rifai", catalog.find("ar.haniarrifai")?.id)
        assertEquals("user_099",catalog.find("ar.nabilarrifai")?.id)
        assertEquals("user_067",catalog.find("ar.mahmoodalrifai")?.id)
        assertNull(catalog.find("ar.mahmoudsayedeltayeb"))
        assertNull(catalog.find("ar.muhammadsulaimanpatel"))
        assertNull(catalog.find("Hani Rifai"))
        assertNull(catalog.find(""))
    }
}
