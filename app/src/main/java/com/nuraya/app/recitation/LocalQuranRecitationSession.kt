package com.nuraya.app.recitation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.nuraya.app.recitation.ai.QuranWhisperEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import android.os.SystemClock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.math.sqrt
import kotlin.math.abs

/** Memory-only recording, with a hard cap independent of any UI timer. */
internal class RecitationPcmBuffer(maxSeconds: Int = 90) {
    private val samples = FloatArray(maxSeconds.coerceIn(1, 90) * SAMPLE_RATE)
    private var count = 0
    private var audibleSamples = 0
    val size: Int @Synchronized get() = count
    val full: Boolean @Synchronized get() = count == samples.size

    @Synchronized fun append(input: ShortArray, length: Int): Int {
        require(length in 0..input.size)
        val accepted = minOf(length, samples.size - count)
        for (i in 0 until accepted) {
            val sample = input[i] / 32768f
            samples[count++] = sample
            if (abs(sample) > 0.003f) audibleSamples++
        }
        return accepted
    }
    @Synchronized fun snapshot(): FloatArray = samples.copyOf(count)
    @Synchronized fun hasAudibleSignal(): Boolean = audibleSamples >= SAMPLE_RATE / 5
    @Synchronized fun clear() { samples.fill(0f); count = 0; audibleSamples = 0 }

    companion object { const val SAMPLE_RATE = 16_000 }
}

data class LocalQuranRecitationState(
    val speech: SpeechSessionState = SpeechSessionState(onDevice = true),
    val elapsedSeconds: Int = 0,
    val audioLevel: Float = 0f,
    val receivedAudio: Boolean = false,
    val detectedSpeech: Boolean = false,
    val processingSeconds: Int = 0,
    val microphoneCheck: Boolean = false,
)

internal interface RecitationDecoder : AutoCloseable {
    suspend fun transcribe(pcm: FloatArray): String
    fun cancel()
}

/** Capture begins immediately; model loading/inference must never pretend to be microphone input. */
class LocalQuranRecitationSession internal constructor(
    context: Context,
    private val onState: (LocalQuranRecitationState) -> Unit,
    private val onFinal: (String) -> Unit,
    private val openInput: () -> RecitationAudioInput,
    private val loadDecoder: (File) -> RecitationDecoder,
    private val workerDispatcher: CoroutineDispatcher,
    private val clockMs: () -> Long,
) {
    constructor(context: Context, onState: (LocalQuranRecitationState) -> Unit, onFinal: (String) -> Unit) : this(
        context, onState, onFinal, { AndroidRecitationMicrophone(context.applicationContext).open() },
        { model ->
            val native = QuranWhisperEngine(model.absolutePath)
            object : RecitationDecoder {
                override suspend fun transcribe(pcm: FloatArray) = native.transcribe(pcm)
                override fun cancel() = native.cancel()
                override fun close() = native.close()
            }
        }, Dispatchers.IO, SystemClock::elapsedRealtime,
    )

    private val context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @Volatile private var activeBuffer: RecitationPcmBuffer? = null
    private var state = LocalQuranRecitationState()
    @Volatile private var generation = 0
    @Volatile private var recording = false
    @Volatile private var destroyed = false
    @Volatile private var engine: RecitationDecoder? = null
    private val engineLock = Any()
    @Volatile private var recorder: RecitationAudioInput? = null
    private val recorderLock = Any()
    private var captureJob: Job? = null
    private var inferenceJob: Job? = null
    private var limitJob: Job? = null
    private var processingClock: Job? = null

    private fun publish(value: LocalQuranRecitationState) { state = value; onState(value) }
    private fun active(current: Int) = !destroyed && generation == current

    fun start(model: File) = begin(model)

    /** Records five seconds locally without a model or speech service, then reports whether sound arrived. */
    fun checkMicrophone() = begin(null)

    private fun begin(model: File?) {
        if (destroyed || state.speech.listening || state.speech.waiting) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            publish(LocalQuranRecitationState(SpeechSessionState(status = "permission", onDevice = true)))
            return
        }
        val current = ++generation
        val checking = model == null
        val pcm = RecitationPcmBuffer(if (checking) 5 else 90)
        activeBuffer = pcm
        recording = true
        publish(LocalQuranRecitationState(SpeechSessionState(waiting = true, status = "starting", onDevice = true), microphoneCheck = checking))
        val previousCapture = captureJob
        captureJob = scope.launch(workerDispatcher) {
            previousCapture?.join()
            capture(current, pcm)
        }
        limitJob = scope.launch { delay(if (checking) 5_000 else 90_000); if (active(current)) stopListening() }
        inferenceJob = scope.launch {
            try {
                if (checking) {
                    captureJob?.join()
                    if (active(current)) finish(current, "", when {
                        pcm.size == 0 -> "audio_no_frames"
                        pcm.hasAudibleSignal() -> "microphone_ok"
                        else -> "microphone_quiet"
                    })
                    return@launch
                }
                if (engine == null) withContext(workerDispatcher) {
                    val loaded = loadDecoder(requireNotNull(model))
                    synchronized(engineLock) { if (!active(current)) loaded.close() else engine = loaded }
                }
                if (!active(current)) return@launch
                if (state.speech.listening) publish(state.copy(speech = state.speech.copy(status = "listening")))
                var lastAnalyzed = 0
                while (isActive && active(current)) {
                    val size = pcm.size
                    if (recording && (size < MIN_PARTIAL_SAMPLES || size - lastAnalyzed < PARTIAL_STEP_SAMPLES)) {
                        delay(100)
                        continue
                    }
                    if (!recording) captureJob?.join()
                    val final = !recording
                    if (!pcm.hasAudibleSignal()) {
                        if (final) { finish(current, "", if (pcm.size == 0) "audio_no_frames" else "microphone_quiet"); break }
                        lastAnalyzed = size
                        delay(100)
                        continue
                    }
                    val audio = pcm.snapshot()
                    lastAnalyzed = audio.size
                    publish(state.copy(speech = state.speech.copy(listening = !final, waiting = final,
                        status = if (final) "local_processing" else "local_listening_processing"), processingSeconds = 0))
                    processingClock = scope.launch {
                        while (isActive && active(current)) {
                            delay(1_000)
                            if (active(current)) publish(state.copy(processingSeconds = state.processingSeconds + 1))
                        }
                    }
                    val text = try {
                        // Bound a stalled inference and let cancellation abort the native decoder.
                        withTimeout(120_000) { engine?.transcribe(audio).orEmpty().take(10_000) }
                    } finally {
                        audio.fill(0f)
                        processingClock?.cancel(); processingClock = null
                    }
                    if (!active(current)) break
                    if (final) { finish(current, text, if (text.isBlank()) "no_match" else "complete"); break }
                    // If Finish was pressed during inference and no additional samples arrived,
                    // this result already covers the whole attempt; do not run the same work twice.
                    if (!recording) {
                        captureJob?.join()
                        if (active(current) && pcm.size == lastAnalyzed) {
                            finish(current, text, if (text.isBlank()) "no_match" else "complete")
                            break
                        }
                    }
                    publish(state.copy(speech = state.speech.copy(
                        transcript = text.ifBlank { state.speech.transcript }, finalResult = false,
                        status = if (recording) "listening" else "local_processing", listening = recording, waiting = !recording),
                        processingSeconds = 0))
                }
            } catch (_: TimeoutCancellationException) { fail(current, "processing_timeout")
            } catch (_: CancellationException) { // Explicit cancellation already invalidated callbacks.
            } catch (_: OutOfMemoryError) { fail(current, "model_memory")
            } catch (_: Exception) { fail(current, "model_failed") }
        }
    }

    private suspend fun capture(current: Int, pcm: RecitationPcmBuffer) {
        var input: RecitationAudioInput? = null
        var buffer: ShortArray? = null
        try {
            if (!active(current) || !recording) return
            input = openInput()
            val accepted = synchronized(recorderLock) {
                if (!active(current) || !recording) false else {
                    recorder = input
                    input.start()
                    true
                }
            }
            if (!accepted) return
            withContext(Dispatchers.Main.immediate) {
                if (active(current)) publish(state.copy(speech = state.speech.copy(listening = true, waiting = false,
                    status = if (state.microphoneCheck) "microphone_check" else if (engine == null) "listening_model_loading" else "listening")))
            }
            val converter = RecitationResampler(input.sampleRate)
            buffer = ShortArray(input.sampleRate / 10)
            var lastSamplesAt = clockMs()
            while (active(current) && recording && !pcm.full) {
                if (input.silenced) throw MicrophoneFailure("audio_muted")
                val count = input.read(buffer)
                if (!active(current) || !recording) break
                if (count < 0) throw MicrophoneFailure("audio")
                if (count == 0) {
                    if (clockMs() - lastSamplesAt >= 5_000) throw MicrophoneFailure("audio_no_frames")
                    delay(20)
                    continue
                }
                lastSamplesAt = clockMs()
                val converted = converter.convert(buffer, count)
                pcm.append(converted, converted.size)
                val rms = if (converted.isEmpty()) 0f else sqrt(converted.sumOf { val v = it / 32768.0; v * v } / converted.size).toFloat()
                val level = (rms * 12).coerceIn(0f, 1f)
                converted.fill(0); buffer.fill(0)
                val second = pcm.size / RecitationPcmBuffer.SAMPLE_RATE
                val audible = pcm.hasAudibleSignal()
                withContext(Dispatchers.Main.immediate) {
                    if (active(current)) publish(state.copy(elapsedSeconds = second, audioLevel = level,
                        receivedAudio = true, detectedSpeech = audible))
                }
                // A nonblocking reader must yield even on vendor stacks that continuously return data.
                delay(10)
            }
        } catch (_: SecurityException) {
            withContext(Dispatchers.Main.immediate) { fail(current, "permission") }
        } catch (_: CancellationException) {
        } catch (error: MicrophoneFailure) {
            withContext(Dispatchers.Main.immediate) { fail(current, error.reason) }
        } catch (_: Exception) {
            withContext(Dispatchers.Main.immediate) { fail(current, "audio") }
        } finally {
            buffer?.fill(0)
            synchronized(recorderLock) { if (recorder === input) recorder = null }
            runCatching { input?.close() }
            if (active(current)) withContext(Dispatchers.Main.immediate) {
                if (active(current)) {
                    recording = false
                    publish(state.copy(speech = state.speech.copy(listening = false, waiting = true,
                        status = if (state.microphoneCheck) "microphone_check" else "local_processing"), audioLevel = 0f))
                }
            }
        }
    }

    fun stopListening() {
        if (!recording) return
        recording = false
        synchronized(recorderLock) { runCatching { recorder?.stop() } }
        publish(state.copy(speech = state.speech.copy(listening = false, waiting = true,
            status = if (state.microphoneCheck) "microphone_check" else "local_processing"), audioLevel = 0f))
    }

    private fun finish(current: Int, text: String, status: String) {
        if (!active(current)) return
        recording = false
        activeBuffer?.clear(); activeBuffer = null
        limitJob?.cancel(); limitJob = null
        processingClock?.cancel(); processingClock = null
        publish(state.copy(speech = SpeechSessionState(transcript = text, finalResult = text.isNotBlank(),
            status = status, onDevice = true), audioLevel = 0f))
        if (text.isNotBlank()) onFinal(text)
    }

    private fun fail(current: Int, status: String) { if (active(current)) cancel(status) }

    fun cancel(status: String = "ready") {
        generation++
        recording = false
        synchronized(recorderLock) { runCatching { recorder?.stop() } }
        synchronized(engineLock) { engine?.cancel() }
        captureJob?.cancel() // Keep the job until the next start can await its microphone release.
        inferenceJob?.cancel(); inferenceJob = null
        activeBuffer?.clear(); activeBuffer = null
        limitJob?.cancel(); limitJob = null
        processingClock?.cancel(); processingClock = null
        publish(LocalQuranRecitationState(SpeechSessionState(status = status, onDevice = true)))
    }

    fun destroy() {
        destroyed = true
        cancel()
        synchronized(engineLock) { engine?.close(); engine = null }
        scope.cancel()
    }

    companion object {
        private const val MIN_PARTIAL_SAMPLES = 6 * RecitationPcmBuffer.SAMPLE_RATE
        private const val PARTIAL_STEP_SAMPLES = 6 * RecitationPcmBuffer.SAMPLE_RATE
    }
}
