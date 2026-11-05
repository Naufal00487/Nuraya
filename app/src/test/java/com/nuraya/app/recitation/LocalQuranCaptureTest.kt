package com.nuraya.app.recitation

import org.junit.Assert.*
import org.junit.Test

class LocalQuranCaptureTest {
    @Test fun longInputStopsAtTheRecordingLimitAndConvertsPcmWithoutOverflow() {
        val buffer = RecitationPcmBuffer(maxSeconds = 1)
        val samples = ShortArray(20_000) { if (it % 2 == 0) Short.MIN_VALUE else Short.MAX_VALUE }
        assertEquals(16_000, buffer.append(samples, samples.size))
        assertTrue(buffer.full)
        assertEquals(0, buffer.append(samples, samples.size))
        val pcm = buffer.snapshot()
        assertEquals(16_000, pcm.size)
        assertEquals(-1f, pcm[0], 0f)
        assertEquals(32767f / 32768, pcm[1], 0f)
    }

    @Test fun snapshotsDoNotExposeRecordingStorageAndClearRemovesPreviousAttempt() {
        val buffer = RecitationPcmBuffer()
        buffer.append(shortArrayOf(2000, 4000), 2)
        val snapshot = buffer.snapshot()
        snapshot[0] = 0f
        assertTrue(buffer.snapshot()[0] > 0)
        buffer.clear()
        assertEquals(0, buffer.size)
        assertFalse(buffer.hasAudibleSignal())
        buffer.append(shortArrayOf(8000), 1)
        assertEquals(1, buffer.snapshot().size)
        assertEquals(8000f / 32768, buffer.snapshot().single(), 0f)
    }

    @Test fun silenceAndSingleClickCannotBeSubmittedAsRecitation() {
        val buffer = RecitationPcmBuffer()
        buffer.append(ShortArray(16_000), 16_000)
        buffer.append(shortArrayOf(Short.MAX_VALUE), 1)
        assertFalse(buffer.hasAudibleSignal())
        buffer.append(ShortArray(3_200) { 2000 }, 3_200)
        assertTrue(buffer.hasAudibleSignal())
    }

    @Test fun failedAudioReadCannotAdvanceRecordingBuffer() {
        val buffer = RecitationPcmBuffer()
        assertThrows(IllegalArgumentException::class.java) { buffer.append(ShortArray(32), -3) }
        assertThrows(IllegalArgumentException::class.java) { buffer.append(ShortArray(32), 33) }
        assertEquals(0, buffer.size)
    }

    @Test fun fallbackMicrophoneRatesKeepDurationAmplitudeAndReadBoundaries() {
        for (rate in listOf(16_000, 44_100, 48_000)) {
            val source = ShortArray(rate) { ((it % 200) * 100 - 10_000).toShort() }
            val whole = RecitationResampler(rate).convert(source, source.size)
            val streaming = RecitationResampler(rate)
            val chunks = source.asList().chunked(997).flatMap { chunk ->
                streaming.convert(chunk.toShortArray(), chunk.size).toList()
            }.toShortArray()
            assertEquals(16_000, whole.size)
            assertArrayEquals(whole, chunks)
            val dc = RecitationResampler(rate).convert(ShortArray(rate) { 4_000 }, rate)
            assertTrue(dc.all { it == 4_000.toShort() })
        }
    }
}
