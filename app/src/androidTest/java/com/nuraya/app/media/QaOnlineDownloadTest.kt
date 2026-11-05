package com.nuraya.app.media

import android.os.SystemClock
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nuraya.app.MainActivity
import com.nuraya.app.NurayaApplication
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Explicit live-source test: pass -e nuraya.online true to the instrumentation runner. */
@RunWith(AndroidJUnit4::class)
class QaOnlineDownloadTest {
    @get:Rule val activity = ActivityScenarioRule(MainActivity::class.java)

    @Test fun realWorkerDownloadsPlaysPrivateFileAndDeletesIt() = runBlocking {
        assumeTrue("External-network check requires nuraya.online=true",
            InstrumentationRegistry.getArguments().getString("nuraya.online") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as NurayaApplication
        val qari = app.quran.qaris.first { it.id == "ar.alafasy" }
        val surah = app.quran.surahs.first { it.id == 1 }
        val id = "${qari.id}:${surah.id}"
        val dao = app.database.downloadDao()
        val previous = dao.get(id)
        assumeTrue("Another download is active for this recitation",
            previous?.status !in setOf("queued", "downloading"))
        val audio = File(app.filesDir, "audio/ar.alafasy-1.mp3")
        val backup = File(app.cacheDir, "qa-online-original-${System.nanoTime()}.mp3")
        val hadFile = audio.isFile
        if (hadFile) audio.copyTo(backup)
        val history = app.database.libraryDao().get("audio_history:$id")
        try {
            instrumentation.runOnMainSync { app.playback.stop() }
            app.downloads.delete(id)
            app.downloads.enqueue(surah, qari)
            val completed = withTimeout(240_000L) {
                app.downloads.downloads.first { rows ->
                    rows.any { it.id == id && it.status in setOf("complete", "failed", "cancelled") }
                }.first { it.id == id }
            }
            assertEquals("Live worker result: ${completed.error}", "complete", completed.status)
            assertEquals(100, completed.progress)
            val downloaded = app.downloads.localFile(qari.id, surah.id)
            assertNotNull("Complete row must resolve to its private audio file", downloaded)
            assertTrue("Real recitation should contain more than 1 KiB", downloaded!!.length() > 1024L)

            // The production manager prefers this validated private file over the HTTPS URL.
            instrumentation.runOnMainSync { app.playback.play(surah, qari) }
            val deadline = SystemClock.elapsedRealtime() + 60_000L
            while (!app.playback.state.value.isPlaying && SystemClock.elapsedRealtime() < deadline) {
                assertNull(app.playback.state.value.error)
                SystemClock.sleep(100L)
            }
            assertTrue("Downloaded recitation did not start", app.playback.state.value.isPlaying)
            assertTrue("MP3 duration should be available", app.playback.state.value.durationMs > 30_000L)
            instrumentation.runOnMainSync { app.playback.stop() }
            app.downloads.delete(id)
            assertNull("Delete must remove persisted download row", dao.get(id))
            assertFalse("Delete must remove private media file", audio.exists())
            assertNull(app.downloads.localFile(qari.id, surah.id))
        } finally {
            instrumentation.runOnMainSync { app.playback.stop() }
            app.downloads.cancel(id)
            app.downloads.delete(id)
            if (hadFile) { audio.parentFile!!.mkdirs(); backup.copyTo(audio, overwrite = true) }
            backup.delete()
            if (previous != null) dao.upsert(previous)
            if (history == null) app.library.remove("audio_history", id)
            else app.library.save(history.type, history.key, history.title, history.subtitle, history.extra)
        }
    }
}
