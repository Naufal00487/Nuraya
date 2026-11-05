package com.nuraya.app.qa

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.storage.DownloadEntity
import com.nuraya.app.storage.LibraryRepository
import com.nuraya.app.storage.NurayaDatabase
import com.nuraya.app.storage.SettingsRepository
import com.nuraya.app.storage.UserEntry
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Uses SQLite/DataStore, not an in-memory replacement for the production repositories. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class QaLocalPersistenceTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun bookmarksFavoritesAndDownloadsSurviveDatabaseCloseAndReopen() = runBlocking {
        val filename = "qa-persistence-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, NurayaDatabase::class.java, filename).build()
        var db = open()
        try {
            val library = LibraryRepository(db.libraryDao())
            library.save("bookmark", "2:255", "Al-Baqarah", "Ayat 255")
            library.save("audio_favorite", "ar.alafasy:1", "Al-Fatihah", "Alafasy")
            library.save("playlist", "ar.alafasy:1", "Koleksi", "Al-Fatihah")
            db.downloadDao().upsert(DownloadEntity(
                id = "ar.alafasy:1", qariId = "ar.alafasy", surahId = 1,
                title = "Al-Fatihah", status = "failed", progress = 17,
                error = "network_unavailable",
            ))
            db.close()
            db = open()
            val restored = LibraryRepository(db.libraryDao())
            assertEquals(setOf("bookmark", "audio_favorite", "playlist"), restored.entries.first().map { it.type }.toSet())
            assertEquals("2:255", restored.observe("bookmark").first().single().key)
            assertEquals("Al-Fatihah", restored.observe("audio_favorite").first().single().title)
            val download = db.downloadDao().get("ar.alafasy:1")
            assertNotNull(download)
            assertEquals("failed", download!!.status)
            assertEquals(17, download.progress)
            assertEquals("network_unavailable", download.error)
        } finally {
            db.close()
            context.deleteDatabase(filename)
        }
    }

    @Test fun favoriteToggleIsReversibleAndHistoryClearPreservesCollections() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, NurayaDatabase::class.java).build()
        try {
            val library = LibraryRepository(db.libraryDao())
            assertTrue(library.toggle("bookmark", "1:1", "Al-Fatihah"))
            assertEquals(1, library.observe("bookmark").first().size)
            assertFalse(library.toggle("bookmark", "1:1", "Al-Fatihah"))
            assertTrue(library.observe("bookmark").first().isEmpty())
            library.save("bookmark", "1:2", "Al-Fatihah")
            library.save("audio_favorite", "ar.alafasy:1", "Al-Fatihah")
            library.save("read_history", "1:1", "Al-Fatihah")
            library.save("audio_history", "ar.alafasy:1", "Al-Fatihah")
            // A database from an older release can still contain removed video
            // content. It must never reappear as a current collection item.
            db.libraryDao().upsert(UserEntry("video_history:qa-video", "video_history", "qa-video", "Legacy fixture"))
            db.libraryDao().upsert(UserEntry("video_saved:qa-video", "video_saved", "qa-video", "Legacy fixture"))
            assertFalse(library.entries.first().any { it.type.startsWith("video") })
            library.clearHistory()
            assertEquals(setOf("bookmark", "audio_favorite"), library.entries.first().map { it.type }.toSet())
        } finally { db.close() }
    }

    @Test fun historyIsBoundedAndInvalidCollectionInputIsRejected() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, NurayaDatabase::class.java).build()
        try {
            val library = LibraryRepository(db.libraryDao())
            repeat(305) { library.save("read_history", "qa-$it", "QA fixture") }
            assertEquals(300, library.observe("read_history").first().size)
            expectInvalid { library.save("not_a_supported_type", "x", "x") }
            expectInvalid { library.save("video_saved", "x", "Removed content") }
            expectInvalid { library.observe("video_history") }
            expectInvalid { library.save("bookmark", "", "x") }
            expectInvalid { library.toggle("bookmark", "x".repeat(501), "x") }
        } finally { db.close() }
    }

    @Test fun settingsPersistAcrossRepositoryInstancesAndValidateValues() = runBlocking {
        val settings = SettingsRepository(context)
        settings.setLanguage("en")
        settings.setTranslation("none")
        settings.setArabicSize(40f)
        settings.setDarkMode(true)
        val restored = SettingsRepository(context).settings.first()
        assertEquals("en", restored.language)
        assertEquals("none", restored.translation)
        assertEquals(40f, restored.arabicSize, 0f)
        assertTrue(restored.darkMode)
        settings.setArabicSize(999f)
        assertEquals(48f, settings.settings.first().arabicSize, 0f)
        settings.setArabicSize(0f)
        assertEquals(22f, settings.settings.first().arabicSize, 0f)
        expectInvalid { settings.setLanguage("invalid") }
        expectInvalid { settings.setTranslation("invalid") }
        expectInvalid { settings.setArabicSize(Float.NaN) }
        assertEquals("en", settings.settings.first().language)
        assertEquals("none", settings.settings.first().translation)
    }

    private suspend fun expectInvalid(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected validation to reject input")
        } catch (_: IllegalArgumentException) {
            // Expected: invalid input must not alter persistent data.
        }
    }
}
