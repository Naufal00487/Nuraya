package com.nuraya.app.learning

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

data class QuerySpeechState(val listening: Boolean = false, val waiting: Boolean = false,
    val transcript: String = "", val status: String = "ready", val onDevice: Boolean = false, val level: Float = 0f)

internal interface QueryRecognizer {
    fun listen(listener: RecognitionListener, intent: Intent)
    fun stop()
    fun destroy()
}
internal interface QueryRecognizerFactory {
    fun available(local: Boolean): Boolean
    fun create(local: Boolean): QueryRecognizer
}
private class AndroidQueryRecognizerFactory(private val context: Context) : QueryRecognizerFactory {
    override fun available(local: Boolean) = runCatching {
        if (local) Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        else SpeechRecognizer.isRecognitionAvailable(context)
    }.getOrDefault(false)
    override fun create(local: Boolean): QueryRecognizer {
        val recognizer = if (local && Build.VERSION.SDK_INT >= 31) SpeechRecognizer.createOnDeviceSpeechRecognizer(context) else SpeechRecognizer.createSpeechRecognizer(context)
        return object : QueryRecognizer {
            override fun listen(listener: RecognitionListener, intent: Intent) { recognizer.setRecognitionListener(listener); recognizer.startListening(intent) }
            override fun stop() = recognizer.stopListening()
            override fun destroy() { runCatching { recognizer.cancel() }; recognizer.destroy() }
        }
    }
}

/** Indonesian query dictation. This session never loads or prompts the Quran recitation model. */
class QuerySpeechSession internal constructor(context: Context, private val onState: (QuerySpeechState) -> Unit,
    private val onFinal: (List<String>) -> Unit, private val factory: QueryRecognizerFactory) {
    constructor(context: Context, onState: (QuerySpeechState) -> Unit, onFinal: (List<String>) -> Unit) :
        this(context, onState, onFinal, AndroidQueryRecognizerFactory(context.applicationContext))
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: QueryRecognizer? = null
    private var state = QuerySpeechState()
    private var generation = 0
    private var destroyed = false
    private var timer: Runnable? = null
    val onDeviceAvailable get() = factory.available(true)
    val systemAvailable get() = factory.available(false)
    private fun publish(value: QuerySpeechState) { state = value; onState(value) }
    fun start(useSystem: Boolean = false) {
        check(Looper.myLooper() == Looper.getMainLooper())
        if (destroyed || state.listening || state.waiting) return
        val local = !useSystem && onDeviceAvailable
        if (!factory.available(local)) { publish(QuerySpeechState(status = "unavailable")); return }
        release()
        val request = ++generation
        publish(QuerySpeechState(listening = true, status = "starting", onDevice = local))
        try {
            val backend = factory.create(local); recognizer = backend
            backend.listen(object : RecognitionListener {
                private fun active() = !destroyed && request == generation
                override fun onReadyForSpeech(params: Bundle?) { if (active()) publish(state.copy(status = "listening")) }
                override fun onBeginningOfSpeech() { if (active()) publish(state.copy(status = "listening")) }
                override fun onRmsChanged(rmsdB: Float) { if (active()) publish(state.copy(level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f))) }
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() { if (active()) { publish(state.copy(listening = false, waiting = true, status = "processing")); timeout(request, 15_000) } }
                override fun onError(error: Int) {
                    if (!active()) return
                    val reason = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "audio"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "permission"
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "network"
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "no_match"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "busy"
                        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "language_unavailable"
                        else -> "failed"
                    }
                    finish(state.copy(listening = false, waiting = false, status = reason))
                }
                override fun onResults(results: Bundle?) {
                    if (!active()) return
                    val choices = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty().map { it.trim().take(1000) }.filter(String::isNotBlank).distinct().take(5)
                    finish(state.copy(listening = false, waiting = false, transcript = choices.firstOrNull().orEmpty(), status = if (choices.isEmpty()) "no_match" else "complete"))
                    if (choices.isNotEmpty()) onFinal(choices)
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    if (!active()) return
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { publish(state.copy(transcript = it.take(1000))) }
                }
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            }, Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, local)
            })
            if (generation == request && recognizer != null) timeout(request, 30_000)
        } catch (_: SecurityException) { finish(state.copy(listening = false, waiting = false, status = "permission")) }
        catch (_: RuntimeException) { finish(state.copy(listening = false, waiting = false, status = "failed")) }
    }
    fun stopListening() {
        if (!state.listening) return
        val request = generation
        try {
            recognizer?.stop()
            if (request == generation) { publish(state.copy(listening = false, waiting = true, status = "processing")); timeout(request, 15_000) }
        } catch (_: RuntimeException) { cancel("failed") }
    }
    private fun timeout(request: Int, ms: Long) {
        timer?.let(handler::removeCallbacks)
        timer = Runnable { if (generation == request) cancel("timeout") }.also { handler.postDelayed(it, ms) }
    }
    private fun finish(value: QuerySpeechState) { generation++; release(); publish(value) }
    fun cancel(reason: String = "ready") {
        generation++; release()
        publish(if (reason == "ready") QuerySpeechState() else state.copy(listening = false, waiting = false, status = reason))
    }
    private fun release() { timer?.let(handler::removeCallbacks); timer = null; recognizer?.let { runCatching { it.destroy() } }; recognizer = null }
    fun destroy() { destroyed = true; generation++; release() }
}
