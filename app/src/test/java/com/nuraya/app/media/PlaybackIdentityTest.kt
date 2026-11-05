package com.nuraya.app.media

import android.content.ComponentName
import android.os.Binder
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.NurayaApplication
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/** Checks selection before a media service connects; no physical audio playback is claimed. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = NurayaApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class PlaybackIdentityTest {
    private fun playback(): PlaybackManager {
        val app = ApplicationProvider.getApplicationContext<NurayaApplication>()
        if (runCatching { androidx.work.WorkManager.getInstance(app) }.isFailure) {
            androidx.work.WorkManager.initialize(app, androidx.work.Configuration.Builder().build())
        }
        shadowOf(app).setComponentNameAndServiceForBindService(ComponentName(app, PlaybackService::class.java), Binder())
        return app.playback
    }

    @Test fun unknownSavedReciterIsRejectedRatherThanSubstitutingAlafasy() {
        val manager = playback()
        manager.playVerseRange(2, 255, 255, "ar.retirededition")
        val state = manager.state.value
        assertFalse(state.isPlaying)
        assertFalse(state.loading)
        assertEquals("ar.retirededition", state.qariId)
        assertNotEquals("ar.alafasy", state.qariId)
        assertTrue(state.error.orEmpty().contains("Pilih ulang qari"))
        assertEquals(255, state.ayahId)
    }

    @Test fun rejectingASelectionCancelsThePreviousRequestAndAllowsAnExplicitNewOne() {
        val manager = playback()
        val selected = manager.verseReciters.first { it.id != "ar.alafasy" }
        manager.playVerseRange(1, 1, 7, selected.id)
        assertEquals(selected.id, manager.state.value.qariId)
        assertTrue(manager.state.value.loading)
        manager.playVerseRange(1, 1, 1, "")
        assertFalse(manager.state.value.loading)
        assertFalse(manager.state.value.isPlaying)
        assertNotNull(manager.state.value.error)
        manager.toggle()
        assertFalse(manager.state.value.isPlaying)
        assertNotNull(manager.state.value.error)
        manager.playVerseRange(1, 1, 2, selected.id)
        assertEquals(selected.id, manager.state.value.qariId)
        assertEquals(selected.name, manager.state.value.qari)
        assertNull(manager.state.value.error)
        manager.stop()
    }
}
