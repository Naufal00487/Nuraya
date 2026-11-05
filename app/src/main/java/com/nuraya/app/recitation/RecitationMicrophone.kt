package com.nuraya.app.recitation

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import java.io.IOException
import kotlin.math.min
import kotlin.math.roundToInt

/** Small boundary for real microphone capture and deterministic lifecycle regression tests. */
internal interface RecitationAudioInput : AutoCloseable {
    val sampleRate: Int
    val silenced: Boolean
    fun start()
    fun read(samples: ShortArray): Int
    fun stop()
}

internal class MicrophoneFailure(val reason: String) : IOException(reason)

internal class AndroidRecitationMicrophone(private val context: Context) {
    @SuppressLint("MissingPermission") // The session checks permission; revocation is handled by its caller.
    fun open(): RecitationAudioInput {
        val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (manager.isMicrophoneMute) throw MicrophoneFailure("audio_muted")
        // MIC is more widely supported than VOICE_RECOGNITION on vendor audio stacks.
        // 44.1 kHz is guaranteed by AudioRecord; other rates are attempted only when initialized.
        for (source in listOf(MediaRecorder.AudioSource.MIC, MediaRecorder.AudioSource.VOICE_RECOGNITION)) {
            for (rate in listOf(16_000, 48_000, 44_100)) {
                var record: AudioRecord? = null
                try {
                    val minimum = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                    if (minimum <= 0) continue
                    record = AudioRecord(source, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        maxOf(minimum * 2, rate / 5 * 2))
                    if (record.state != AudioRecord.STATE_INITIALIZED) { record.release(); continue }
                    val opened = record
                    return object : RecitationAudioInput {
                        override val sampleRate = rate
                        override val silenced: Boolean get() = manager.isMicrophoneMute ||
                            (Build.VERSION.SDK_INT >= 29 && opened.activeRecordingConfiguration?.isClientSilenced == true)
                        override fun start() {
                            opened.startRecording()
                            if (opened.recordingState != AudioRecord.RECORDSTATE_RECORDING) throw MicrophoneFailure("audio")
                        }
                        override fun read(samples: ShortArray) = opened.read(samples, 0, samples.size, AudioRecord.READ_NON_BLOCKING)
                        override fun stop() { runCatching { opened.stop() } }
                        override fun close() { stop(); opened.release() }
                    }
                } catch (error: SecurityException) {
                    record?.release()
                    throw error
                } catch (_: RuntimeException) { record?.release() }
            }
        }
        throw MicrophoneFailure("audio")
    }
}

/** Streaming box-filter resampling retains fractional position across reads and averages before decimation. */
internal class RecitationResampler(private val inputRate: Int) {
    init { require(inputRate >= RecitationPcmBuffer.SAMPLE_RATE) }
    private val ratio = inputRate.toDouble() / RecitationPcmBuffer.SAMPLE_RATE
    private var remaining = ratio
    private var weightedSum = 0.0

    fun convert(input: ShortArray, length: Int): ShortArray {
        require(length in 0..input.size)
        if (inputRate == RecitationPcmBuffer.SAMPLE_RATE) return input.copyOf(length)
        val output = ShortArray((length / ratio).toInt() + 2)
        var size = 0
        for (i in 0 until length) {
            var available = 1.0
            while (available > 1e-9) {
                val portion = min(available, remaining)
                weightedSum += input[i] * portion
                available -= portion
                remaining -= portion
                if (remaining < 1e-9) {
                    output[size++] = (weightedSum / ratio).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    weightedSum = 0.0
                    remaining = ratio
                }
            }
        }
        return output.copyOf(size)
    }
}
