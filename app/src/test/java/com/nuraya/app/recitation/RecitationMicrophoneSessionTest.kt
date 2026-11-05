package com.nuraya.app.recitation

import android.Manifest
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Exercises real session orchestration with controlled PCM, not a microphone/model accuracy claim. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class RecitationMicrophoneSessionTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var context: Application

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        context = ApplicationProvider.getApplicationContext()
        shadowOf(context).grantPermissions(Manifest.permission.RECORD_AUDIO)
    }

    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun microphoneCheckReceivesSoundWithoutLoadingAnyAiModel() = runTest(dispatcher) {
        val states = mutableListOf<LocalQuranRecitationState>()
        val input = FakeInput(clock = { testScheduler.currentTime })
        var modelLoads = 0
        val session = LocalQuranRecitationSession(context, states::add, { fail("A microphone check is not a completed recitation") },
            { input }, { modelLoads++; FakeDecoder() }, dispatcher, { testScheduler.currentTime })
        try {
            session.checkMicrophone()
            assertFalse(states.last().speech.listening)
            runCurrent()
            advanceTimeBy(5_100); runCurrent()
            assertEquals(0, modelLoads)
            assertTrue(input.started && input.closed)
            assertTrue(states.any { it.receivedAudio && it.detectedSpeech && it.audioLevel > 0f })
            assertEquals("microphone_ok", states.last().speech.status)
            assertFalse(states.last().speech.listening || states.last().speech.waiting)
        } finally { session.destroy(); runCurrent() }
    }

    @Test fun zeroReadDeviceEndsWithSpecificFailureInsteadOfPretendingToRecord() = runTest(dispatcher) {
        val states = mutableListOf<LocalQuranRecitationState>()
        val input = FakeInput(clock = { testScheduler.currentTime }, deliver = false)
        val session = LocalQuranRecitationSession(context, states::add, {}, { input }, { FakeDecoder() }, dispatcher, { testScheduler.currentTime })
        try {
            session.start(File(context.filesDir, "test-model"))
            advanceTimeBy(5_200); runCurrent()
            assertEquals("audio_no_frames", states.last().speech.status)
            assertTrue(states.none { it.receivedAudio })
            assertTrue(input.closed)
            assertFalse(states.last().speech.listening || states.last().speech.waiting)
        } finally { session.destroy(); runCurrent() }
    }

    @Test fun mutedInputStopsBeforeSubmittingAnyAudioToAi() = runTest(dispatcher) {
        val states = mutableListOf<LocalQuranRecitationState>()
        val input = FakeInput(clock = { testScheduler.currentTime }, isSilenced = true)
        val decoder = FakeDecoder()
        val session = LocalQuranRecitationSession(context, states::add, {}, { input }, { decoder }, dispatcher, { testScheduler.currentTime })
        try {
            session.start(File(context.filesDir, "test-model")); runCurrent()
            assertEquals("audio_muted", states.last().speech.status)
            assertEquals(0, decoder.requests)
            assertTrue(input.closed)
        } finally { session.destroy(); runCurrent() }
    }

    @Test fun actualSilenceIsDifferentFromAMicrophoneThatSendsNoFrames() = runTest(dispatcher) {
        val states = mutableListOf<LocalQuranRecitationState>()
        val input = FakeInput(clock = { testScheduler.currentTime }, amplitude = 0)
        val session = LocalQuranRecitationSession(context, states::add, {}, { input }, { FakeDecoder() }, dispatcher, { testScheduler.currentTime })
        try {
            session.checkMicrophone(); advanceTimeBy(5_100); runCurrent()
            assertEquals("microphone_quiet", states.last().speech.status)
            assertTrue(states.any { it.receivedAudio })
            assertTrue(states.none { it.detectedSpeech })
            assertTrue(input.closed)
        } finally { session.destroy(); runCurrent() }
    }

    @Test fun finishedCaptureProducesResultAndShowsProcessingTimeWhileDecoderWorks() = runTest(dispatcher) {
        val states = mutableListOf<LocalQuranRecitationState>()
        val results = mutableListOf<String>()
        val input = FakeInput(clock = { testScheduler.currentTime })
        val decoder = FakeDecoder(delayMs = 2_200)
        val session = LocalQuranRecitationSession(context, states::add, results::add, { input }, { decoder }, dispatcher, { testScheduler.currentTime })
        try {
            session.start(File(context.filesDir, "test-model")); advanceTimeBy(500); runCurrent()
            session.stopListening(); runCurrent()
            advanceTimeBy(2_500); runCurrent()
            assertEquals(listOf("بسم الله"), results)
            assertEquals("complete", states.last().speech.status)
            assertTrue(states.any { it.processingSeconds > 0 && it.speech.waiting })
            assertEquals(1, decoder.requests)
            assertTrue(decoder.sampleCount >= 3_200)
            assertTrue(input.closed)
            assertTrue(decoder.lastAudio!!.all { it == 0f })
        } finally { session.destroy(); runCurrent() }
    }

    @Test fun stalledDecoderTimesOutAndReleasesMicrophoneWithNoFalseResult() = runTest(dispatcher) {
        val states = mutableListOf<LocalQuranRecitationState>()
        val input = FakeInput(clock = { testScheduler.currentTime })
        val decoder = FakeDecoder(stall = true)
        val session = LocalQuranRecitationSession(context, states::add, { fail("Stalled decoding cannot emit a result") },
            { input }, { decoder }, dispatcher, { testScheduler.currentTime })
        try {
            session.start(File(context.filesDir, "test-model")); advanceTimeBy(500); runCurrent()
            session.stopListening(); runCurrent()
            advanceTimeBy(120_500); runCurrent()
            assertEquals("processing_timeout", states.last().speech.status)
            assertTrue(input.closed)
            assertTrue(decoder.cancels > 0)
            assertFalse(states.last().speech.listening || states.last().speech.waiting)
        } finally { session.destroy(); runCurrent() }
    }

    @Test fun cancellationClearsAudioAndPreventsDelayedDecoderResult() = runTest(dispatcher) {
        val states = mutableListOf<LocalQuranRecitationState>()
        val input = FakeInput(clock = { testScheduler.currentTime })
        val decoder = FakeDecoder(delayMs = 2_200)
        val session = LocalQuranRecitationSession(context, states::add, { fail("Cancelled attempts cannot complete") },
            { input }, { decoder }, dispatcher, { testScheduler.currentTime })
        try {
            session.start(File(context.filesDir, "test-model")); advanceTimeBy(500); runCurrent()
            session.stopListening(); runCurrent()
            // Allow the session's next capture poll to submit the final PCM,
            // then cancel while the delayed decoder actually owns the samples.
            advanceTimeBy(200); runCurrent()
            assertEquals(1, decoder.requests)
            session.cancel(); advanceTimeBy(3_000); runCurrent()
            assertEquals("ready", states.last().speech.status)
            assertTrue(input.closed)
            assertTrue(decoder.cancels > 0)
            assertTrue(decoder.lastAudio!!.all { it == 0f })
        } finally { session.destroy(); runCurrent() }
    }

    private class FakeInput(
        private val clock: () -> Long,
        private val deliver: Boolean = true,
        private val amplitude: Short = 2_000,
        private val isSilenced: Boolean = false,
    ) : RecitationAudioInput {
        override val sampleRate = 16_000
        override val silenced get() = isSilenced
        var started = false
        var closed = false
        private var stopped = false
        private var lastRead = -100L
        override fun start() { started = true }
        override fun read(samples: ShortArray): Int {
            if (!deliver || stopped || clock() - lastRead < 100) return 0
            lastRead = clock()
            samples.fill(amplitude)
            return samples.size
        }
        override fun stop() { stopped = true }
        override fun close() { stop(); closed = true }
    }

    private class FakeDecoder(private val delayMs: Long = 0, private val stall: Boolean = false) : RecitationDecoder {
        var requests = 0
        var cancels = 0
        var sampleCount = 0
        var lastAudio: FloatArray? = null
        override suspend fun transcribe(pcm: FloatArray): String {
            requests++; sampleCount = pcm.size; lastAudio = pcm
            if (stall) awaitCancellation()
            delay(delayMs)
            return "بسم الله"
        }
        override fun cancel() { cancels++ }
        override fun close() = Unit
    }
}
