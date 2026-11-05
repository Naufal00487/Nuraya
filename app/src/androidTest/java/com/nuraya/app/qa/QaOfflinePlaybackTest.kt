package com.nuraya.app.qa

import android.content.ComponentName
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.util.concurrent.ListenableFuture
import com.nuraya.app.MainActivity
import com.nuraya.app.media.PlaybackService
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.sin
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A synthetic tone exists only in the test sandbox; it is never Quran content. */
@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
@RunWith(AndroidJUnit4::class)
class QaOfflinePlaybackTest {
    @get:Rule val activity = ActivityScenarioRule(MainActivity::class.java)

    @Test fun localTestToneDecodesAndMediaControlsWorkWithoutNetwork() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val file = File(context.cacheDir, "qa-synthetic-tone-not-quran.wav")
        createTestTone(file)
        val futureRef = AtomicReference<ListenableFuture<MediaController>>()
        instrumentation.runOnMainSync {
            futureRef.set(MediaController.Builder(context,
                SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync())
        }
        val controller = futureRef.get().get(45, TimeUnit.SECONDS)
        fun onMain(block: () -> Unit) = instrumentation.runOnMainSync(block)
        fun await(description: String, condition: () -> Boolean) {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(45)
            while (System.nanoTime() < deadline) {
                val result = AtomicReference(false)
                onMain { result.set(condition()) }
                if (result.get()) return
                Thread.sleep(100)
            }
            fail(description)
        }
        try {
            onMain {
                controller.setMediaItem(MediaItem.Builder()
                    .setMediaId("qa-synthetic-audio")
                    .setUri(Uri.fromFile(file))
                    .setMediaMetadata(MediaMetadata.Builder()
                        .setTitle("QA synthetic tone — not Quran")
                        .build())
                    .build())
                controller.prepare()
                controller.play()
            }
            await("Local WAV must reach READY and play") {
                controller.playbackState == Player.STATE_READY && controller.isPlaying
            }
            onMain {
                assertNull(controller.playerError)
                assertTrue(controller.duration in 29_000L..31_000L)
                controller.pause()
            }
            await("Pause must stop playback") { !controller.isPlaying }
            onMain {
                controller.seekTo(10_000)
                controller.setPlaybackSpeed(1.25f)
                controller.repeatMode = Player.REPEAT_MODE_ONE
            }
            await("Seek, speed and repeat must update the real media session") {
                controller.currentPosition in 9_500L..10_500L &&
                    controller.playbackParameters.speed == 1.25f &&
                    controller.repeatMode == Player.REPEAT_MODE_ONE
            }
            onMain { controller.play() }
            await("Resume must restart playback") { controller.isPlaying }
        } finally {
            onMain { controller.stop(); controller.clearMediaItems(); controller.release() }
            file.delete()
        }
    }

    private fun createTestTone(file: File) {
        val sampleRate = 16_000
        val samples = sampleRate * 30
        val dataBytes = samples * 2
        val out = ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN)
        out.put("RIFF".toByteArray()).putInt(36 + dataBytes).put("WAVE".toByteArray())
        out.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
        out.putInt(sampleRate).putInt(sampleRate * 2).putShort(2).putShort(16)
        out.put("data".toByteArray()).putInt(dataBytes)
        repeat(samples) { i -> out.putShort((sin(2 * Math.PI * 220 * i / sampleRate) * 1000).toInt().toShort()) }
        file.writeBytes(out.array())
    }
}
