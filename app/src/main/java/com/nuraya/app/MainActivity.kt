package com.nuraya.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuraya.app.storage.UserSettings
import com.nuraya.app.ui.NurayaApp
import com.nuraya.app.ui.NurayaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as NurayaApplication
        setContent {
            val settings by app.settings.settings.collectAsStateWithLifecycle(UserSettings())
            NurayaTheme(settings.darkMode) { NurayaApp(app, settings) }
        }
    }
}
