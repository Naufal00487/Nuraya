package com.nuraya.app

import android.app.Application
import android.util.Log
import com.nuraya.app.data.QuranRepository
import com.nuraya.app.media.PlaybackManager
import com.nuraya.app.storage.DownloadRepository
import com.nuraya.app.storage.LibraryRepository
import com.nuraya.app.storage.NurayaDatabase
import com.nuraya.app.storage.SettingsRepository
import com.nuraya.app.study.StudyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

class NurayaApplication : Application() {
    // Show the launch scene once per process, not again on a reader rotation.
    internal var launchArtworkShown = false
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val quran by lazy { QuranRepository(this) }
    val study by lazy { StudyRepository(this) }
    val database by lazy { NurayaDatabase.get(this) }
    val library by lazy { LibraryRepository(database.libraryDao()) }
    val settings by lazy { SettingsRepository(this) }
    val downloads by lazy { DownloadRepository(this, database.downloadDao()) }
    val playback by lazy { PlaybackManager(this, quran, library, downloads) }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            try {
                downloads.reconcile()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                // Recovery is best effort; an unavailable disk must not crash reading at startup.
                // Download actions still surface their own errors and validate local files again.
                Log.w("Nuraya", "Download state reconciliation could not complete", error)
            }
        }
    }
}
