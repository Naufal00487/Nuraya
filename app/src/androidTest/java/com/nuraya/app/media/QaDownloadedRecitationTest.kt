package com.nuraya.app.media

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import com.nuraya.app.NurayaApplication
import com.nuraya.app.MainActivity
import com.nuraya.app.storage.DownloadEntity
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.runner.RunWith

/** A real, licensed recitation fixture exercises the production offline lookup and player. */
@RunWith(AndroidJUnit4::class)
class QaDownloadedRecitationTest {
    @get:Rule val activity = ActivityScenarioRule(MainActivity::class.java)

    @Test fun downloadedRecitationPlaysOfflineWithSeekingAndPause() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as NurayaApplication
        val surah = app.quran.surahs.first { it.id == 1 }
        val qari = app.quran.qaris.first { it.id == "ar.alafasy" }
        val id = "${qari.id}:${surah.id}"
        val dao = app.database.downloadDao()
        val originalRow = dao.get(id)
        val audio = File(app.filesDir, "audio/ar.alafasy-1.mp3")
        val backup = File(app.cacheDir, "qa-original-recitation-${System.nanoTime()}.mp3")
        val hadOriginal = audio.isFile
        if (hadOriginal) audio.copyTo(backup)
        val originalHistory = app.database.libraryDao().get("audio_history:$id")
        try {
            val fixture = instrumentation.context.assets.open("alafasy1.mp3").use { it.readBytes() }
            val checksum = MessageDigest.getInstance("SHA-256").digest(fixture).joinToString("") { "%02x".format(it) }
            assertEquals("7c2e67cbde506d25f7e181c3ce876c2a9943a5529ee24a753b2856075a4567fa", checksum)
            audio.parentFile!!.mkdirs()
            audio.writeBytes(fixture)
            dao.upsert(DownloadEntity(id, qari.id, surah.id, "QA: real Al-Fatihah fixture",
                status = "complete", progress = 100, filePath = audio.absolutePath))
            assertEquals(audio.canonicalPath, app.downloads.localFile(qari.id, surah.id)?.canonicalPath)

            instrumentation.runOnMainSync { app.playback.play(surah, qari) }
            await("Real downloaded MP3 did not start") {
                val state = app.playback.state.value
                assertNull("Playback error: ${state.error}", state.error)
                state.isPlaying && state.durationMs in 49_000L..55_000L && state.positionMs > 0L
            }
            assertEquals("ar.alafasy", app.playback.state.value.qariId)
            assertEquals(1, app.playback.state.value.surahId)
            instrumentation.runOnMainSync { app.playback.setSpeed(1.25f); app.playback.setRepeat(true) }
            await("Speed/repeat did not propagate") {
                app.playback.state.value.speed == 1.25f && app.playback.state.value.repeat
            }
            instrumentation.runOnMainSync { app.playback.seekTo(20_000L) }
            await("Seek did not advance the recitation") { app.playback.state.value.positionMs >= 19_000L }
            instrumentation.runOnMainSync { app.playback.toggle() }
            await("Pause did not propagate") { !app.playback.state.value.isPlaying }
            instrumentation.runOnMainSync { app.playback.toggle() }
            await("Resume did not propagate") { app.playback.state.value.isPlaying }
        } finally {
            instrumentation.runOnMainSync { app.playback.setRepeat(false); app.playback.setSpeed(1f); app.playback.stop() }
            if (hadOriginal) backup.copyTo(audio, overwrite = true) else audio.delete()
            backup.delete()
            if (originalRow == null) dao.delete(id) else dao.upsert(originalRow)
            if (originalHistory == null) app.database.libraryDao().delete("audio_history:$id")
            else app.database.libraryDao().upsert(originalHistory)
        }
    }

    private fun await(message: String, test: () -> Boolean) {
        val until = SystemClock.elapsedRealtime() + 60_000L
        while (SystemClock.elapsedRealtime() < until) {
            if (test()) return
            SystemClock.sleep(100L)
        }
        assertTrue(message, test())
    }
}
