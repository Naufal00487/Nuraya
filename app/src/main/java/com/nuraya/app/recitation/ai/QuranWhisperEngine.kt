package com.nuraya.app.recitation.ai

import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Local Whisper inference. Construct on Dispatchers.IO after the caller has verified the model hash.
 * Audio and recognized speech are never sent to a server. Each request uses fresh decoder context,
 * with no expected-verse prompt. Results are fallible text recognition, not tajwid assessment.
 */
class QuranWhisperEngine(modelPath: String) : AutoCloseable {
    private val handle: AtomicLong
    private val worker = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "nuraya-quran-inference").apply { isDaemon = true }
    }

    init {
        check(isSupported()) { "Local Quran AI is unavailable on this device" }
        require(File(modelPath).isFile) { "Download the Quran speech model first" }
        val nativeHandle = nativeCreate(modelPath)
        if (nativeHandle == 0L) throw IOException("Unable to load the Quran speech model")
        handle = AtomicLong(nativeHandle)
    }

    /** PCM must be normalized mono 16 kHz; 0.1–90 seconds. Cancellation interrupts native work. */
    suspend fun transcribe(pcm16k: FloatArray): String {
        require(pcm16k.size in 1_600..1_440_000) { "Expected 0.1 to 90 seconds of 16 kHz mono PCM" }
        val id = handle.get()
        check(id != 0L) { "Quran speech engine is closed" }
        val generation = nativeGeneration(id)
        val snapshot = pcm16k.copyOf()
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { nativeCancel(id) }
            try {
                worker.execute {
                    try {
                        if (!continuation.isActive) return@execute
                        val result = nativeTranscribe(id, snapshot, generation,
                            Runtime.getRuntime().availableProcessors().coerceIn(1, 4))
                            .toString(Charsets.UTF_8).trim()
                        if (continuation.isActive) continuation.resume(result)
                    } catch (error: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(error)
                    } finally {
                        snapshot.fill(0f)
                    }
                }
            } catch (error: RejectedExecutionException) {
                snapshot.fill(0f)
                if (continuation.isActive) continuation.resumeWithException(IllegalStateException("Quran speech engine is closed", error))
            }
        }
    }

    /** Invalidates running/queued requests. A subsequent transcribe call gets a fresh generation. */
    fun cancel() { handle.get().takeIf { it != 0L }?.let(::nativeCancel) }

    /** Safe during inference: native storage is released after the active request exits. */
    override fun close() {
        val id = handle.getAndSet(0L)
        if (id != 0L) nativeRelease(id)
        // Do not discard queued Runnables: they must complete their suspended callers with an error.
        worker.shutdown()
    }

    private external fun nativeCreate(path: String): Long
    private external fun nativeGeneration(id: Long): Long
    private external fun nativeTranscribe(id: Long, samples: FloatArray, generation: Long, threads: Int): ByteArray
    private external fun nativeCancel(id: Long)
    private external fun nativeRelease(id: Long)

    companion object {
        private val nativeAvailable by lazy {
            runCatching { System.loadLibrary("nuraya_quran_ai") }.isSuccess
        }
        fun isSupported(): Boolean = nativeAvailable
    }
}
