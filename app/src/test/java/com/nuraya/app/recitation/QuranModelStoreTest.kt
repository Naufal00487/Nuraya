package com.nuraya.app.recitation

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class QuranModelStoreTest {
    private val fixedUrl = "https://raw.githubusercontent.com/Naufal00487/Nuraya/1234567890abcdef1234567890abcdef12345678/model.bin"
    private val digest = "1234567890abcdef".repeat(4)
    private val spec = QuranModelSpec(fixedUrl, digest, 1_000_000, "model-test.bin")

    @Test fun modelsRequireImmutableHttpsSourceChecksumAndBoundedSafeFilename() {
        spec.validate()
        for (invalid in listOf(
            spec.copy(url = fixedUrl.replace("https:", "http:")),
            spec.copy(url = fixedUrl.replace("raw.githubusercontent.com", "example.org")),
            spec.copy(url = fixedUrl.replace("1234567890abcdef1234567890abcdef12345678", "main")),
            spec.copy(url = fixedUrl + "?token=test"),
            spec.copy(fileName = "../model.bin"),
            spec.copy(bytes = 1_000_000_000),
            spec.copy(sha256 = "0".repeat(64)),
        )) assertThrows(IllegalArgumentException::class.java) { invalid.validate() }
    }

    @Test fun existingModelMustMatchContentsNotOnlySizeBeforeReady() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val bytes = ByteArray(1_000_000) { (it % 127).toByte() }
        val actualHash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        val store = QuranModelStore(context, spec.copy(sha256 = actualHash, fileName = "verified-test.bin"))
        store.modelFile.parentFile!!.mkdirs()
        try {
            store.modelFile.writeBytes(bytes)
            store.inspect()
            assertEquals(ModelPhase.READY, store.state.value.phase)
            bytes[0] = 99
            store.modelFile.writeBytes(bytes)
            store.inspect()
            assertEquals(ModelPhase.MISSING, store.state.value.phase)
        } finally { store.modelFile.delete() }
    }

    @Test fun interruptedModelIsNeverExposedAsReady() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val store = QuranModelStore(context, spec.copy(fileName = "partial-test.bin"))
        store.modelFile.parentFile!!.mkdirs()
        try {
            store.modelFile.writeBytes(ByteArray(100))
            store.inspect()
            assertEquals(ModelPhase.MISSING, store.state.value.phase)
        } finally { store.modelFile.delete() }
    }

    @Test fun publishedBuildHasAConfiguredImmutableModel() {
        QuranAiModel.spec.validate()
    }
}
