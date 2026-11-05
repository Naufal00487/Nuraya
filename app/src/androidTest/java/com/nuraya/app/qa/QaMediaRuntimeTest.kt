package com.nuraya.app.qa

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nuraya.app.recitation.QuranAiModel
import com.nuraya.app.recitation.ai.QuranWhisperEngine
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real Android JNI checks; no physical microphone or phone performance claim. */
@RunWith(AndroidJUnit4::class)
class QaMediaRuntimeTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    /** Requires a real, separately copied fixture; missing/incorrect files fail instead of skipping. */
    @Test(timeout = 120_000)
    fun packagedNativeEngineLoadsVerifiedModelAndHandlesSilence() = runBlocking {
        val context = instrumentation.targetContext
        val configuredPath = InstrumentationRegistry.getArguments().getString("nurayaModelPath")
        val model = configuredPath?.let(::File)
            ?: File(context.filesDir, "qa/${QuranAiModel.spec.fileName}")
        assertTrue("Copy the verified model to ${model.absolutePath} or pass -e nurayaModelPath <path>", model.isFile)
        assertEquals("Model fixture length", QuranAiModel.spec.bytes, model.length())
        val digest = MessageDigest.getInstance("SHA-256")
        model.inputStream().buffered().use { input ->
            val buffer = ByteArray(128 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        assertEquals("Model fixture checksum", QuranAiModel.spec.sha256,
            digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) })
        assertTrue("The packaged library must load on this Android ABI", QuranWhisperEngine.isSupported())
        QuranWhisperEngine(model.absolutePath).use { engine ->
            assertEquals("", withTimeout(30_000) { engine.transcribe(FloatArray(16_000)) })
            engine.cancel()
            assertEquals("Cancellation must not poison the next request", "",
                withTimeout(30_000) { engine.transcribe(FloatArray(16_000)) })
        }
        // No spoken fixture is decoded here: software-emulator timing is not a phone benchmark.
    }
}
