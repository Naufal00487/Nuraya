package com.nuraya.app.storage

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.data.QuranRepository
import com.nuraya.app.media.VerseAudioCatalog
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class VerseAudioHistoryTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun repeatedPlaybackUpdatesOneRowAndVerseHistorySurvivesReopening() = runBlocking {
        val filename = "verse-history-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, NurayaDatabase::class.java, filename).build()
        var db = open()
        try {
            val library = LibraryRepository(db.libraryDao())
            repeat(20) { library.save("verse_audio_history", "ar.husary:2:255", "Latest $it", "Husary", "ar.husary") }
            library.save("verse_audio_history", "ar.husary:2:256", "Next verse", "Husary", "ar.husary")
            db.libraryDao().upsert(UserEntry("verse_audio_history:ar.alafasy:1:1", "verse_audio_history", "ar.alafasy:1:1", "Older saved verse", extra = "ar.alafasy"))
            assertEquals(3, library.observe("verse_audio_history").first().size)
            db.close()
            db = open()
            val restored = LibraryRepository(db.libraryDao())
            val rows = restored.entries.first()
            assertEquals(3, rows.size)
            assertEquals("Latest 19", rows.single { it.key == "ar.husary:2:255" }.title)
            assertTrue(rows.any { it.key == "ar.alafasy:1:1" })
        } finally { db.close(); context.deleteDatabase(filename) }
    }

    @Test fun explicitHistoryClearIncludesVerseAudioAndPreservesCollections() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, NurayaDatabase::class.java).build()
        try {
            val library = LibraryRepository(db.libraryDao())
            library.save("bookmark", "2:255", "Bookmark")
            library.save("audio_favorite", "ar.husary:2", "Favourite")
            library.save("playlist", "ar.husary:2", "Playlist")
            library.save("read_history", "2:255", "Reading")
            library.save("audio_history", "ar.husary:2", "Surah audio")
            library.save("verse_audio_history", "ar.husary:2:255", "Verse audio", extra = "ar.husary")
            db.libraryDao().upsert(UserEntry("video_saved:old", "video_saved", "old", "Legacy item"))
            db.libraryDao().upsert(UserEntry("video_history:old", "video_history", "old", "Legacy history"))
            library.clearHistory()
            assertEquals(setOf("bookmark", "audio_favorite", "playlist"), library.entries.first().map { it.type }.toSet())
            assertTrue(library.observe("verse_audio_history").first().isEmpty())
            assertNotNull(db.libraryDao().get("video_saved:old"))
            assertNull(db.libraryDao().get("video_history:old"))
        } finally { db.close() }
    }

    @Test fun historyResolutionKeepsTheExactQariAndVerseAndRejectsConflicts() {
        val quran = QuranRepository(context)
        val qaris = VerseAudioCatalog(context).reciters.map { it.id }.toSet()
        fun entry(key: String, extra: String = "ar.husary", type: String = "verse_audio_history") =
            UserEntry("$type:$key", type, key, "Saved verse", extra = extra)
        fun resolve(value: UserEntry) = resolveVerseAudioHistory(value, qaris) { s, a -> quran.verse(s, a) != null }
        assertEquals(VerseAudioHistory("ar.husary", 2, 255), resolve(entry("ar.husary:2:255")))
        assertEquals(VerseAudioHistory("ar.husary", 2, 255), resolve(entry("ar.husary:2:255", extra = "")))
        listOf(entry("ar.husary:255:2"), entry("ar.husary:1:8"), entry("ar.husary:2:0"),
            entry("ar.husary:2"), entry("ar.husary:2:255:extra"), entry("ar.unknown:2:255", "ar.unknown"),
            entry("ar.husary:2:255", "ar.alafasy"), entry("ar.husary:2:255", type = "audio_history"))
            .forEach { assertNull(it.key, resolve(it)) }
    }
}
