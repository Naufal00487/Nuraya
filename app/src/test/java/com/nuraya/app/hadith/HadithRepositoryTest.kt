package com.nuraya.app.hadith

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class HadithRepositoryTest {
    private val repository get() = HadithRepository(ApplicationProvider.getApplicationContext())
    @Test fun exactReferenceKeepsOriginalArabicAndIndonesianTogether() = runBlocking {
        val repo = repository
        val row = repo.search("Bukhari 1").items.single()
        assertEquals("1", row.number)
        assertEquals("ce2d3dfca13ad59e0c61d84a0a8882fda446e046c042d128e1ec0fb6e4be5e1f", java.security.MessageDigest.getInstance("SHA-256").digest(row.arabic.toByteArray()).joinToString("") { "%02x".format(it) })
        assertTrue(row.indonesian.contains("niat", true))
        assertEquals(row, repo.get(row.id))
        assertEquals(2, repo.search("1").total)
    }
    @Test fun keywordSearchSupportsArabicAndPaginationWithoutDuplicates() = runBlocking {
        val repo = repository
        val first = repo.search("shalat", "bukhari")
        val second = repo.search("shalat", "bukhari", first.items.size)
        assertTrue(first.total > 40)
        assertEquals(40, first.items.size)
        assertTrue(first.items.all { it.collection == "bukhari" && it.indonesian.contains("shalat", true) })
        assertTrue(first.items.map { it.id }.intersect(second.items.map { it.id }.toSet()).isEmpty())
        assertTrue(repo.search("الأعمال بالنيات").items.any { it.collection == "bukhari" && it.number == "1" })
        assertEquals(0, repo.search("' OR 1=1 -- xyznoresult").total)
        assertEquals(0, repo.search("!").total)
    }
    @Test fun datasetCountsAndFractionalNumbersArePreserved() = runBlocking {
        val repo = repository
        assertEquals(15152, repo.sources.getInt("total"))
        assertEquals(11785, repo.sources.getInt("indonesianAvailable"))
        // Fractional references identify distinct source entries, never truncated to integers.
        val context = ApplicationProvider.getApplicationContext<Application>()
        val meta = repo.sources
        repo.search("Bukhari 1")
        android.database.sqlite.SQLiteDatabase.openDatabase(context.getDatabasePath("nuraya-hadith-${meta.getString("databaseSha256").take(12)}.sqlite").path, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT collection,number FROM hadith WHERE number LIKE '%.%' LIMIT 1", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(c.getString(1), repo.search("${c.getString(0)} ${c.getString(1)}").items.single().number)
            }
            db.rawQuery("SELECT id FROM hadith WHERE indonesian = '' LIMIT 1", null).use { c ->
                assertTrue(c.moveToFirst()); assertEquals("", repo.get(c.getLong(0))!!.indonesian)
            }
        }
    }
}
