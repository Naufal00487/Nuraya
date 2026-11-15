package com.nuraya.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.getValue
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuraya.app.storage.UserSettings
import com.nuraya.app.ui.NurayaApp
import com.nuraya.app.ui.NurayaTheme

class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        com.nuraya.app.prayer.AdhanScheduler.reconcile(this)
        com.nuraya.app.reminders.QuranReminderScheduler.schedule(this)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent); setIntent(intent)
        com.nuraya.app.reminders.ReminderNavigation.accept(intent)
    }

    override fun onStop() {
        if (!isChangingConfigurations) (application as NurayaApplication).playback.stopReading()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(savedInstanceState==null)com.nuraya.app.reminders.ReminderNavigation.accept(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        if (android.os.Build.VERSION.SDK_INT >= 31) splashScreen.setOnExitAnimationListener { it.remove() }
        val app = application as NurayaApplication
        setContent {
            val settings by app.settings.settings.collectAsStateWithLifecycle(UserSettings())
            NurayaTheme(settings.darkMode) {
                Surface(Modifier.fillMaxSize(), color=MaterialTheme.colorScheme.background) { NurayaApp(app, settings) }
            }
        }
    }
}
