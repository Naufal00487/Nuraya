package com.nuraya.app.recitation

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** A fixed, public model artifact; never accept a user-supplied URL or model filename. */
data class QuranModelSpec(val url: String, val sha256: String, val bytes: Long, val fileName: String) {
    fun validate() {
        val source = URL(url)
        require(source.protocol == "https" && source.host == "raw.githubusercontent.com" && source.port == -1)
        require(source.userInfo == null && source.query == null && source.ref == null)
        require(Regex("/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+/[a-f0-9]{40}/[A-Za-z0-9_.-]+\\.bin").matches(source.path))
        require(sha256.matches(Regex("[a-f0-9]{64}")) && sha256.any { it != '0' })
        require(bytes in 1_000_000..150_000_000)
        require(fileName.matches(Regex("[A-Za-z0-9_.-]+\\.bin")) && !fileName.contains(".."))
    }
}

// Converted model passed real audio/JNI checks; bytes and SHA-256 are pinned to an immutable public artifact.
object QuranAiModel {
    val spec = QuranModelSpec(
        url = "https://raw.githubusercontent.com/Naufal00487/Nuraya/1d5a31aa2e60dbb9af70981e606a9c06b1b18c3e/nuraya-quran-whisper-base-q5_1.bin",
        sha256 = "1de4498dc16fdee2045e67a97d73cda4624017640dbe5ddf809f3130da7c234a",
        bytes = 59_707_625,
        fileName = "nuraya-quran-whisper-base-q5_1.bin",
    )
}

enum class ModelPhase { CHECKING, MISSING, DOWNLOADING, VERIFYING, READY, PAUSED, ERROR }
data class QuranModelState(val phase: ModelPhase = ModelPhase.CHECKING, val downloaded: Long = 0, val error: String? = null)

/** Checksum-verified, private on-device model storage. Only model bytes are downloaded; audio never leaves the device. */
class QuranModelStore(context: Context, val spec: QuranModelSpec = QuranAiModel.spec) {
    private val folder = File(context.noBackupFilesDir, "quran-ai")
    val modelFile: File get() = File(folder, spec.fileName)
    private val partialFile: File get() = File(folder, spec.fileName + ".part")
    private val mutex = Mutex()
    private val cancelled = AtomicBoolean(false)
    private val connection = AtomicReference<HttpURLConnection?>()
    private val mutableState = MutableStateFlow(QuranModelState())
    val state: StateFlow<QuranModelState> = mutableState

    suspend fun inspect() = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                spec.validate()
                mutableState.value = QuranModelState(ModelPhase.CHECKING)
                val valid = modelFile.isFile && modelFile.length() == spec.bytes && sha256(modelFile) == spec.sha256
                mutableState.value = if (valid) QuranModelState(ModelPhase.READY, spec.bytes)
                    else QuranModelState(ModelPhase.MISSING, partialFile.length().coerceAtMost(spec.bytes))
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableState.value = QuranModelState(ModelPhase.ERROR, error = "verification")
            }
        }
    }

    suspend fun download() = mutex.withLock {
        withContext(Dispatchers.IO) {
            cancelled.set(false)
            try {
                spec.validate()
                if (!folder.isDirectory && !folder.mkdirs()) throw IOException("storage")
                if (modelFile.isFile && modelFile.length() == spec.bytes && sha256(modelFile) == spec.sha256) {
                    mutableState.value = QuranModelState(ModelPhase.READY, spec.bytes)
                    return@withContext
                }
                if (partialFile.length() > spec.bytes) partialFile.delete()
                var offset = partialFile.length()
                if (offset == spec.bytes) {
                    finishDownloaded()
                    return@withContext
                }
                if (folder.usableSpace < spec.bytes - offset + 32L * 1024 * 1024) throw IOException("space")
                mutableState.value = QuranModelState(ModelPhase.DOWNLOADING, offset)
                val remote = (URL(spec.url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 15_000
                    instanceFollowRedirects = false
                    setRequestProperty("Accept-Encoding", "identity")
                    if (offset > 0) setRequestProperty("Range", "bytes=$offset-")
                }
                connection.set(remote)
                checkActive()
                val code = remote.responseCode
                if (code == HttpURLConnection.HTTP_OK) {
                    offset = 0
                    if (folder.usableSpace + partialFile.length() < spec.bytes + 32L * 1024 * 1024) throw IOException("space")
                }
                else if (code == HttpURLConnection.HTTP_PARTIAL) {
                    val range = remote.getHeaderField("Content-Range").orEmpty()
                    require(range == "bytes $offset-${spec.bytes - 1}/${spec.bytes}") { "range" }
                } else throw IOException("http_$code")
                val declaredLength = remote.contentLengthLong
                if (declaredLength > 0 && declaredLength != spec.bytes - offset) throw IOException("size")
                var total = offset
                remote.inputStream.use { input ->
                    FileOutputStream(partialFile, offset > 0).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            checkActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            if (total + count > spec.bytes) throw IOException("size")
                            output.write(buffer, 0, count)
                            total += count
                            mutableState.value = QuranModelState(ModelPhase.DOWNLOADING, total)
                        }
                        output.fd.sync()
                    }
                }
                if (total != spec.bytes) throw IOException("incomplete")
                finishDownloaded()
            } catch (error: Exception) {
                if (error is CancellationException || cancelled.get()) {
                    mutableState.value = QuranModelState(ModelPhase.PAUSED, partialFile.length().coerceAtMost(spec.bytes))
                    if (error is CancellationException) throw error
                } else mutableState.value = QuranModelState(ModelPhase.ERROR, partialFile.length().coerceAtMost(spec.bytes), error.message?.take(80))
            } finally {
                connection.getAndSet(null)?.disconnect()
            }
        }
    }

    fun cancel() { cancelled.set(true); connection.getAndSet(null)?.disconnect() }

    private suspend fun finishDownloaded() {
        checkActive()
        mutableState.value = QuranModelState(ModelPhase.VERIFYING, partialFile.length())
        if (sha256(partialFile) != spec.sha256) {
            partialFile.delete()
            throw IOException("checksum")
        }
        checkActive()
        if (modelFile.exists() && !modelFile.delete()) throw IOException("storage")
        if (!partialFile.renameTo(modelFile)) throw IOException("storage")
        mutableState.value = QuranModelState(ModelPhase.READY, spec.bytes)
    }

    private suspend fun checkActive() {
        currentCoroutineContext().ensureActive()
        if (cancelled.get()) throw CancellationException("Model download paused")
    }

    private suspend fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
