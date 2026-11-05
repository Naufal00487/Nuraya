package com.nuraya.app.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.nuraya.app.data.QuranRepository
import com.nuraya.app.storage.DownloadLocks
import com.nuraya.app.storage.DownloadRepository
import com.nuraya.app.storage.NurayaDatabase
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Only trusted catalog identifiers enter the worker; arbitrary URLs and filesystem paths do not. */
class AudioDownloadWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    private val dao = NurayaDatabase.get(context).downloadDao()
    private var revision: Long = -1L
    private var downloadId = ""
    private var title = "Nuraya"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val qariId = inputData.getString("qari") ?: return@withContext Result.failure()
        val surahId = inputData.getInt("surah", 0)
        if (!DownloadSafety.validQariId(qariId) || surahId !in 1..114) return@withContext Result.failure()
        downloadId = DownloadRepository.downloadId(qariId, surahId)
        val row = dao.get(downloadId) ?: return@withContext Result.failure()
        revision = row.updatedAt
        title = row.title
        if (row.status == "cancelled" || row.status == "complete") return@withContext Result.success()
        if (runAttemptCount == 0 && inputData.getLong("revision", -1L) != revision) return@withContext Result.failure()

        val output = DownloadRepository.audioFile(applicationContext, qariId, surahId)
        val temp = File(output.parentFile, "${output.name}.$id.part")
        var connection: HttpsURLConnection? = null
        var progress = 0
        try {
            currentCoroutineContext().ensureActive()
            val catalog = QuranRepository(applicationContext)
            val qari = catalog.qaris.firstOrNull { it.id == qariId }
                ?: throw PermanentDownloadException("Qari tidak tersedia / Reciter unavailable")
            if (catalog.surahs.none { it.id == surahId }) throw PermanentDownloadException("Surah tidak tersedia / Surah unavailable")
            val url = URI(qari.audioUrl(surahId))
            if (!DownloadSafety.safeSource(url)) throw PermanentDownloadException("Alamat audio tidak aman / Unsafe audio address")
            if (output.parentFile?.isDirectory != true && output.parentFile?.mkdirs() != true) {
                throw PermanentDownloadException("Penyimpanan tidak tersedia / Storage unavailable")
            }
            if (output.parentFile!!.usableSpace < DownloadSafety.SPACE_RESERVE_BYTES) {
                throw PermanentDownloadException("Penyimpanan penuh / Storage full")
            }
            setForeground(foreground(0))
            if (!update("downloading", 0)) return@withContext Result.failure()
            connection = openConnection(url)
            val length = connection.contentLengthLong
            if (length > DownloadSafety.MAX_AUDIO_BYTES) throw PermanentDownloadException("Berkas terlalu besar / File too large")
            if (length > 0 && length + DownloadSafety.SPACE_RESERVE_BYTES > output.parentFile!!.usableSpace) {
                throw PermanentDownloadException("Penyimpanan penuh / Storage full")
            }
            val contentType = connection.contentType.orEmpty().lowercase()
            if (contentType.startsWith("text/") || contentType.contains("json") || contentType.contains("html")) {
                throw PermanentDownloadException("Sumber tidak mengirim audio / Source did not return audio")
            }
            var downloaded = 0L
            var nextDiskCheck = 0L
            var lastUpdate = 0L
            connection.inputStream.use { input ->
                temp.outputStream().buffered().use { outputStream ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        downloaded += count
                        if (downloaded > DownloadSafety.MAX_AUDIO_BYTES) throw PermanentDownloadException("Berkas terlalu besar / File too large")
                        if (downloaded >= nextDiskCheck) {
                            if (temp.parentFile!!.usableSpace < DownloadSafety.SPACE_RESERVE_BYTES) {
                                throw PermanentDownloadException("Penyimpanan penuh / Storage full")
                            }
                            nextDiskCheck = downloaded + 2 * 1024 * 1024
                        }
                        outputStream.write(buffer, 0, count)
                        progress = DownloadSafety.progress(downloaded, length)
                        val now = System.currentTimeMillis()
                        if (now - lastUpdate >= 750L) {
                            if (!update("downloading", progress)) return@withContext Result.failure()
                            setProgress(workDataOf("progress" to progress, "bytes" to downloaded, "total" to length))
                            setForeground(foreground(progress, length <= 0))
                            lastUpdate = now
                        }
                    }
                }
            }
            if (length > 0 && downloaded != length) throw IOException("Incomplete response")
            validateAudio(temp)
            currentCoroutineContext().ensureActive()
            val saved = DownloadLocks.forId(downloadId).withLock {
                currentCoroutineContext().ensureActive()
                val current = dao.get(downloadId)
                if (current == null || current.updatedAt != revision || current.status == "cancelled") return@withLock false
                Files.move(temp.toPath(), output.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                revision = maxOf(System.currentTimeMillis(), revision + 1L)
                dao.updateState(downloadId, "complete", 100, output.absolutePath, "", revision)
                true
            }
            if (saved) Result.success(workDataOf("downloadId" to downloadId)) else Result.failure()
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) { update("cancelled", progress) }
            throw cancelled
        } catch (error: PermanentDownloadException) {
            update("failed", progress, error.message.orEmpty())
            Result.failure()
        } catch (error: IOException) {
            Log.w("NurayaDownload", "Audio download connection failed (attempt ${runAttemptCount + 1})", error)
            if (runAttemptCount < 2) {
                update("queued", 0, "Koneksi terputus; mencoba kembali / Connection lost; retrying")
                Result.retry()
            } else {
                update("failed", progress, "Audio gagal diunduh. Periksa koneksi, lalu coba lagi / Download failed. Check your connection and retry")
                Result.failure()
            }
        } catch (error: Exception) {
            update("failed", progress, "Unduhan tidak dapat dilanjutkan / Download could not continue")
            Result.failure()
        } finally {
            connection?.disconnect()
            if (temp.exists()) temp.delete()
        }
    }

    private suspend fun update(status: String, progress: Int, error: String = ""): Boolean =
        DownloadLocks.forId(downloadId).withLock {
            val current = dao.get(downloadId) ?: return@withLock false
            if (current.updatedAt != revision) return@withLock false
            revision = maxOf(System.currentTimeMillis(), revision + 1L)
            dao.updateState(downloadId, status, progress, "", error, revision)
            true
        }

    private fun openConnection(initial: URI): HttpsURLConnection {
        var current = initial
        repeat(5) {
            if (!DownloadSafety.allowedRedirect(initial, current)) throw PermanentDownloadException("Pengalihan sumber ditolak / Source redirect rejected")
            val connection = URL(current.toASCIIString()).openConnection() as HttpsURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("User-Agent", "Nuraya/1.0 Android")
            connection.setRequestProperty("Accept", "audio/mpeg,audio/*;q=0.9,*/*;q=0.5")
            val code = connection.responseCode
            if (code in 200..299) return connection
            val redirect = connection.getHeaderField("Location")
            connection.disconnect()
            if (code in listOf(301, 302, 303, 307, 308) && !redirect.isNullOrBlank()) {
                current = current.resolve(redirect)
            } else if (code == 408 || code == 429 || code in 500..599) {
                throw IOException("Audio service unavailable ($code)")
            } else {
                throw PermanentDownloadException("Audio tidak tersedia (HTTP $code) / Audio unavailable")
            }
        }
        throw PermanentDownloadException("Terlalu banyak pengalihan / Too many redirects")
    }

    private fun validateAudio(file: File) {
        if (file.length() < DownloadSafety.MIN_AUDIO_BYTES) throw PermanentDownloadException("Berkas audio tidak lengkap / Incomplete audio file")
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            if (duration <= 0L) throw PermanentDownloadException("Berkas audio tidak dapat dibaca / Audio file cannot be read")
        } catch (error: PermanentDownloadException) {
            throw error
        } catch (error: Exception) {
            throw PermanentDownloadException("Berkas audio tidak valid / Invalid audio file")
        } finally {
            retriever.release()
        }
    }

    private fun foreground(progress: Int, indeterminate: Boolean = false): ForegroundInfo {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Unduhan / Downloads", NotificationManager.IMPORTANCE_LOW))
        val notification = Notification.Builder(applicationContext, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText("Menyimpan audio / Saving audio")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, indeterminate)
            .addAction(Notification.Action.Builder(null, "Batal / Cancel", WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)).build())
            .build()
        val notificationId = 20_000 + (downloadId.hashCode() and 0x0fffffff)
        return if (Build.VERSION.SDK_INT >= 29) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private class PermanentDownloadException(message: String) : IOException(message)

    companion object {
        private const val CHANNEL = "nuraya-downloads"
    }
}

/** Pure rules shared with local tests; network trust is never disabled. */
internal object DownloadSafety {
    const val MIN_AUDIO_BYTES = 1024L
    const val MAX_AUDIO_BYTES = 1024L * 1024L * 1024L
    const val SPACE_RESERVE_BYTES = 32L * 1024L * 1024L

    fun validQariId(id: String): Boolean = id.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,79}"))

    fun safeSource(uri: URI): Boolean =
        uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank() &&
            uri.userInfo == null && (uri.port == -1 || uri.port == 443) && uri.fragment == null

    fun allowedRedirect(source: URI, destination: URI): Boolean =
        safeSource(source) && safeSource(destination) && source.host.equals(destination.host, ignoreCase = true)

    fun progress(downloaded: Long, expected: Long): Int =
        if (expected <= 0L) 0 else ((downloaded.toDouble() / expected.toDouble()) * 100).toInt().coerceIn(0, 99)
}
