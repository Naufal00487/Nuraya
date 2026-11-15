package com.nuraya.app.learning

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
@LooperMode(LooperMode.Mode.PAUSED)
class QuerySpeechSessionTest {
    private class Backend : QueryRecognizer {
        lateinit var listener: RecognitionListener
        lateinit var intent: Intent
        var stops = 0; var destroyed = false
        override fun listen(listener: RecognitionListener, intent: Intent) { this.listener = listener; this.intent = intent }
        override fun stop() { stops++ }
        override fun destroy() { destroyed = true }
    }
    private class Factory(var local: Boolean = true, var system: Boolean = true) : QueryRecognizerFactory {
        val created = mutableListOf<Pair<Boolean, Backend>>()
        override fun available(local: Boolean) = if (local) this.local else system
        override fun create(local: Boolean): QueryRecognizer = Backend().also { created += local to it }
    }
    private val factory = Factory()
    private var state = QuerySpeechState()
    private val results = mutableListOf<List<String>>()
    private val session = QuerySpeechSession(ApplicationProvider.getApplicationContext<Application>(), { state = it }, { results += it }, factory)
    private fun bundle(vararg words: String) = Bundle().apply { putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf(*words)) }

    @Test fun dictationRequestsIndonesianAndReturnsEditableAlternativesOnlyAtFinal() {
        session.start()
        val backend = factory.created.single().second
        assertEquals("id-ID", backend.intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
        assertEquals(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM, backend.intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL))
        assertEquals(5, backend.intent.getIntExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 0))
        assertTrue(backend.intent.getBooleanExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false))
        backend.listener.onPartialResults(bundle("cari surat"))
        assertEquals("cari surat", state.transcript)
        assertTrue(results.isEmpty())
        backend.listener.onResults(bundle("cari surat Yasim", "cari surat Yasin", "cari surat Yasin", " "))
        assertEquals(listOf("cari surat Yasim", "cari surat Yasin"), results.single())
        assertFalse(state.listening); assertFalse(state.waiting); assertTrue(backend.destroyed)
        backend.listener.onResults(bundle("stale result"))
        assertEquals(1, results.size)
    }

    @Test fun unsupportedOfflineLanguageRequiresAnExplicitSystemRetry() {
        session.start()
        factory.created.single().second.listener.onError(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)
        assertEquals("language_unavailable", state.status)
        assertTrue(state.onDevice)
        assertEquals(1, factory.created.size)
        session.start(useSystem = true)
        assertFalse(factory.created.last().first)
        assertEquals("id-ID", factory.created.last().second.intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
        assertFalse(factory.created.last().second.intent.getBooleanExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true))
        session.destroy()
    }

    @Test fun systemOnlyDevicesStillDictateIndonesianAndNoProviderHasActionableState() {
        factory.local = false
        session.start()
        assertFalse(factory.created.single().first)
        assertEquals("id-ID", factory.created.single().second.intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
        session.cancel()
        factory.system = false
        session.start()
        assertEquals("unavailable", state.status)
        assertFalse(state.listening)
    }

    @Test fun closingOrDestroyingTheSheetDiscardsLateResultsAndReleasesMicrophone() {
        session.start()
        val first = factory.created.single().second
        session.cancel()
        first.listener.onResults(bundle("Yasin"))
        assertTrue(first.destroyed); assertTrue(results.isEmpty())
        session.start()
        val second = factory.created.last().second
        session.destroy()
        second.listener.onPartialResults(bundle("Yasin"))
        second.listener.onResults(bundle("Yasin"))
        assertTrue(second.destroyed); assertTrue(results.isEmpty())
        session.start()
        assertEquals(2, factory.created.size)
    }

    @Test fun finishWaitsForTranscriptAndAStalledProviderTimesOut() {
        session.start()
        val backend = factory.created.single().second
        session.stopListening()
        assertEquals(1, backend.stops); assertTrue(state.waiting); assertFalse(state.listening)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(16))
        assertEquals("timeout", state.status); assertFalse(state.waiting); assertTrue(backend.destroyed)
        backend.listener.onResults(bundle("late"))
        assertTrue(results.isEmpty())
    }

    @Test fun permissionAndEmptyResultsStopRecordingWithoutRunningASearch() {
        session.start()
        factory.created.last().second.listener.onError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
        assertEquals("permission", state.status); assertFalse(state.listening)
        session.start()
        factory.created.last().second.listener.onResults(bundle("  "))
        assertEquals("no_match", state.status); assertTrue(results.isEmpty())
    }
}
