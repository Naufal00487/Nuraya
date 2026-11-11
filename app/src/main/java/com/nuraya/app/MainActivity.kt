package com.nuraya.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuraya.app.storage.UserSettings
import com.nuraya.app.ui.NurayaApp
import com.nuraya.app.ui.NurayaTheme

class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        com.nuraya.app.prayer.AdhanScheduler.reconcile(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= 31) splashScreen.setOnExitAnimationListener { it.remove() }
        val app = application as NurayaApplication
        setContent {
            val settings by app.settings.settings.collectAsStateWithLifecycle(UserSettings())
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !settings.darkMode
                    isAppearanceLightNavigationBars = !settings.darkMode
                }
            }
            NurayaTheme(settings.darkMode) {
                Surface(Modifier.fillMaxSize(), color=MaterialTheme.colorScheme.background) { NurayaApp(app, settings) }
            }
        }
    }
}
