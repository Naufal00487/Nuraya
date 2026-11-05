package com.nuraya.app.recitation

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

data class SpeechSessionState(
    val listening: Boolean = false,
    val waiting: Boolean = false,
    val transcript: String = "",
    val finalResult: Boolean = false,
    val status: String = "ready",
    val onDevice: Boolean = false,
)

/** One explicit user-started utterance. Never restarts the microphone in the background. */
class RecitationSpeechSession(
    context: Context,
    private val onState: (SpeechSessionState) -> Unit,
    private val onFinal: (String) -> Unit,
) {
    private val context = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var generation = 0
    private var destroyed = false
    private var state = SpeechSessionState()
    private var timeout: Runnable? = null

    val onDeviceAvailable: Boolean get() = Build.VERSION.SDK_INT >= 31 &&
        runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(context) }.getOrDefault(false)
    val systemAvailable: Boolean get() = runCatching { SpeechRecognizer.isRecognitionAvailable(context) }.getOrDefault(false)

    private fun publish(next: SpeechSessionState) { state = next; onState(next) }

    fun start(allowSystemProvider: Boolean) {
        check(Looper.myLooper() == Looper.getMainLooper())
        if (destroyed || state.listening || state.waiting) return
        val local = onDeviceAvailable && !allowSystemProvider
        if (!local && !allowSystemProvider) { publish(SpeechSessionState(status = "consent")); return }
        if (!local && !systemAvailable) { publish(SpeechSessionState(status = "unavailable")); return }
        releaseRecognizer()
        val current = ++generation
        publish(SpeechSessionState(listening = true, status = "starting", onDevice = local))
        try {
            val service = if (local && Build.VERSION.SDK_INT >= 31) SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                else SpeechRecognizer.createSpeechRecognizer(context)
            recognizer = service
            service.setRecognitionListener(object : RecognitionListener {
                private fun active() = !destroyed && generation == current
                override fun onReadyForSpeech(params: Bundle?) { if (active()) publish(state.copy(status = "listening")) }
                override fun onBeginningOfSpeech() { if (active()) publish(state.copy(status = "listening")) }
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() { if (active()) { publish(state.copy(listening = false, waiting = true, status = "processing")); scheduleTimeout(current) } }
                override fun onError(error: Int) {
                    if (!active()) return
                    val reason = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "audio"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "permission"
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "network"
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "no_match"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "busy"
                        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "arabic_unavailable"
                        else -> "failed"
                    }
                    finish(state.copy(listening = false, waiting = false, finalResult = false, status = reason))
                }
                override fun onResults(results: Bundle?) {
                    if (!active()) return
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().take(10_000)
                    finish(state.copy(listening = false, waiting = false, transcript = text, finalResult = text.isNotBlank(), status = if (text.isBlank()) "no_match" else "complete"))
                    if (text.isNotBlank()) onFinal(text)
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    if (!active()) return
                    val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().take(10_000)
                    if (text.isNotBlank()) publish(state.copy(transcript = text, finalResult = false))
                }
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            service.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, local)
                // Do not send the expected verse as a bias prompt: that can conceal a reader's omissions.
            })
            // A recognizer may fail to call onEndOfSpeech. Limit each deliberate attempt to 90 seconds.
            if (generation == current && recognizer != null) {
                timeout = Runnable { if (generation == current) cancel("timeout") }.also { handler.postDelayed(it, 90_000) }
            }
        } catch (_: SecurityException) {
            finish(state.copy(listening = false, waiting = false, status = "permission"))
        } catch (_: RuntimeException) {
            finish(state.copy(listening = false, waiting = false, status = "failed"))
        }
    }

    fun stopListening() {
        if (!state.listening) return
        val current = generation
        try {
            recognizer?.stopListening()
            if (generation == current) {
                publish(state.copy(listening = false, waiting = true, status = "processing"))
                scheduleTimeout(current)
            }
        } catch (_: RuntimeException) { cancel("failed") }
    }

    private fun scheduleTimeout(current: Int) {
        timeout?.let(handler::removeCallbacks)
        timeout = Runnable { if (generation == current) cancel("timeout") }.also { handler.postDelayed(it, 15_000) }
    }

    private fun finish(next: SpeechSessionState) {
        generation++
        releaseRecognizer()
        publish(next)
    }

    fun cancel(reason: String = "ready") {
        generation++
        releaseRecognizer()
        publish(SpeechSessionState(status = reason))
    }

    private fun releaseRecognizer() {
        timeout?.let(handler::removeCallbacks); timeout = null
        recognizer?.let { service -> runCatching { service.cancel() }; runCatching { service.destroy() } }
        recognizer = null
    }

    fun destroy() { destroyed = true; generation++; releaseRecognizer() }
}
