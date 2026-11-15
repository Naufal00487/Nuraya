package com.nuraya.app.media

import android.content.ComponentName
import android.os.Binder
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.MainActivity
import com.nuraya.app.NurayaApplication
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = NurayaApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ReadingActivityLifecycleTest {
    private fun app(): NurayaApplication {
        val app = ApplicationProvider.getApplicationContext<NurayaApplication>()
        if (runCatching { androidx.work.WorkManager.getInstance(app) }.isFailure)
            androidx.work.WorkManager.initialize(app, androidx.work.Configuration.Builder().build())
        shadowOf(app).setComponentNameAndServiceForBindService(ComponentName(app, PlaybackService::class.java), Binder())
        return app
    }

    @Test fun backgroundingTheRealActivityCancelsPendingReadingAudio() {
        val app = app()
        val activity = Robolectric.buildActivity(MainActivity::class.java).create().start().resume()
        app.playback.playVerseRange(36, 1, 3, "ar.alafasy")
        assertTrue(app.playback.state.value.verseMode)
        activity.pause().stop()
        assertEquals(PlaybackState(), app.playback.state.value)
        activity.destroy()
    }

    @Test fun backgroundingTheRealActivityKeepsTheMurottalSelection() {
        val app = app()
        val activity = Robolectric.buildActivity(MainActivity::class.java).create().start().resume()
        app.playback.play(app.quran.surahs[35], app.quran.qaris.first())
        activity.pause().stop()
        assertEquals(36, app.playback.state.value.surahId)
        assertFalse(app.playback.state.value.verseMode)
        app.playback.stop()
        activity.destroy()
    }
}
