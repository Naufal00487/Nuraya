package com.nuraya.app.storage

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.nuraya.app.data.Qari
import com.nuraya.app.data.Surah
import com.nuraya.app.media.AudioDownloadWorker
import com.nuraya.app.media.DownloadSafety
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** App-private downloads. No storage permission or shared-storage path is required. */
class DownloadRepository(context: Context, private val dao: DownloadDao) {
    private val context = context.applicationContext
    private val workManager = WorkManager.getInstance(this.context)
    val downloads: Flow<List<DownloadEntity>> = dao.observeAll()

    suspend fun enqueue(surah: Surah, qari: Qari): Unit = withContext(Dispatchers.IO) {
        require(surah.id in 1..114)
        val id = downloadId(qari.id, surah.id)
        DownloadLocks.forId(id).withLock {
            val existing = dao.get(id)
            if (existing?.status == "complete" && validLocalFile(existing) != null) return@withLock
            val active = workManager.getWorkInfosForUniqueWork(workName(id)).get()
                .any { !it.state.isFinished }
            if (active) return@withLock
            val revision = maxOf(System.currentTimeMillis(), (existing?.updatedAt ?: 0L) + 1L)
            dao.upsert(
                DownloadEntity(
                    id = id,
                    qariId = qari.id,
                    surahId = surah.id,
                    title = "${surah.name} · ${qari.name}",
                    status = "queued",
                    updatedAt = revision,
                ),
            )
            val request = OneTimeWorkRequestBuilder<AudioDownloadWorker>()
                .setInputData(workDataOf("qari" to qari.id, "surah" to surah.id, "revision" to revision))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
                .addTag("nuraya-audio")
                .build()
            try {
                workManager.enqueueUniqueWork(workName(id), ExistingWorkPolicy.KEEP, request).result.get()
            } catch (error: Exception) {
                dao.updateState(id, "failed", 0, "", "Tidak dapat menjadwalkan unduhan / Could not schedule download", revision + 1L)
                throw error
            }
        }
    }

    suspend fun cancel(id: String): Unit = withContext(Dispatchers.IO) {
        DownloadLocks.forId(id).withLock {
            workManager.cancelUniqueWork(workName(id)).result.get()
            val existing = dao.get(id) ?: return@withLock
            if (existing.status != "complete") {
                dao.updateState(id, "cancelled", existing.progress, "", "", nextRevision(existing))
            }
        }
    }

    /** The UI asks for confirmation before calling this destructive local operation. */
    suspend fun delete(id: String): Unit = withContext(Dispatchers.IO) {
        DownloadLocks.forId(id).withLock {
            workManager.cancelUniqueWork(workName(id)).result.get()
            val existing = dao.get(id) ?: return@withLock
            val expected = audioFile(context, existing.qariId, existing.surahId)
            if (expected.exists() && !expected.delete()) {
                throw java.io.IOException("Tidak dapat menghapus berkas / Could not delete file")
            }
            dao.delete(id)
        }
    }

    suspend fun localFile(qariId: String, surahId: Int): File? = withContext(Dispatchers.IO) {
        val id = downloadId(qariId, surahId)
        DownloadLocks.forId(id).withLock {
            val existing = dao.get(id) ?: return@withLock null
            if (existing.status != "complete") return@withLock null
            validLocalFile(existing).also { file ->
                if (file == null) {
                    dao.updateState(id, "failed", 0, "", "Berkas tidak tersedia; unduh kembali / File missing; download again", nextRevision(existing))
                }
            }
        }
    }

    /** Recover visible state after process death or a restored database without its media files. */
    suspend fun reconcile(): Unit = withContext(Dispatchers.IO) {
        val rows = downloads.first()
        for (row in rows) {
            DownloadLocks.forId(row.id).withLock {
                val latest = dao.get(row.id) ?: return@withLock
                when (latest.status) {
                    "complete" -> if (validLocalFile(latest) == null) {
                        dao.updateState(row.id, "failed", 0, "", "Berkas tidak tersedia; unduh kembali / File missing; download again", nextRevision(latest))
                    }
                    "queued", "downloading" -> {
                        val active = workManager.getWorkInfosForUniqueWork(workName(row.id)).get()
                            .any { !it.state.isFinished }
                        if (!active) {
                            dao.updateState(row.id, "failed", latest.progress, "", "Unduhan terhenti; coba lagi / Download interrupted; try again", nextRevision(latest))
                        }
                    }
                }
            }
        }
    }

    private fun validLocalFile(item: DownloadEntity): File? {
        if (item.filePath.isBlank()) return null
        val expected = audioFile(context, item.qariId, item.surahId)
        return expected.takeIf {
            it.canonicalPath == File(item.filePath).canonicalPath && it.isFile && it.length() >= DownloadSafety.MIN_AUDIO_BYTES
        }
    }

    private fun nextRevision(item: DownloadEntity): Long = maxOf(System.currentTimeMillis(), item.updatedAt + 1L)

    companion object {
        fun downloadId(qariId: String, surahId: Int): String {
            require(DownloadSafety.validQariId(qariId)) { "Invalid reciter identifier" }
            require(surahId in 1..114) { "Invalid surah" }
            return "$qariId:$surahId"
        }

        internal fun workName(id: String): String = "nuraya-download-$id"

        internal fun audioFile(context: Context, qariId: String, surahId: Int): File {
            downloadId(qariId, surahId)
            return File(File(context.filesDir, "audio"), "$qariId-$surahId.mp3")
        }
    }
}

/** Serializes file + row transitions; Room retains the persistent state across process restarts. */
internal object DownloadLocks {
    private val locks = ConcurrentHashMap<String, Mutex>()
    fun forId(id: String): Mutex = locks.getOrPut(id) { Mutex() }
}
