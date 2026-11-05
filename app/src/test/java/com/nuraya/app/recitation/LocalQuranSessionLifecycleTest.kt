package com.nuraya.app.recitation

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class LocalQuranSessionLifecycleTest {
    @Test fun missingPermissionCannotStartModelLoadingOrMicrophone() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(context).denyPermissions(Manifest.permission.RECORD_AUDIO)
        assertEquals(PackageManager.PERMISSION_DENIED,
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO))
        val states = mutableListOf<LocalQuranRecitationState>()
        var completed = 0
        val session = LocalQuranRecitationSession(context, onState = states::add, onFinal = { completed++ })
        try {
            session.start(File(context.filesDir, "missing-model.bin"))
            assertEquals("permission", states.last().speech.status)
            assertTrue(states.none { it.speech.listening || it.speech.waiting })
            assertEquals(0, completed)
        } finally { session.destroy() }
    }

    @Test fun destroyedSessionCannotReopenTheMicrophoneOrEmitAnotherResult() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(context).grantPermissions(Manifest.permission.RECORD_AUDIO)
        val states = mutableListOf<LocalQuranRecitationState>()
        var completed = 0
        val session = LocalQuranRecitationSession(context, onState = states::add, onFinal = { completed++ })
        session.destroy()
        val before = states.size
        session.start(File(context.filesDir, "missing-model.bin"))
        session.stopListening()
        assertEquals(before, states.size)
        assertEquals(0, completed)
    }
}
