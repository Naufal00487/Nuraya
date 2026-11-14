package com.nuraya.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Opaque parchment controls over the unchanged printed pages. */
val MushafColors = lightColorScheme(
    primary = Color(0xFF70562A), onPrimary = Color.White,
    primaryContainer = Color(0xFFF1E2BD), onPrimaryContainer = Color(0xFF473819),
    secondary = Color(0xFF557052), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5E9D2), onSecondaryContainer = Color(0xFF30442E),
    background = Color(0xFFFFFCF3), surface = Color(0xFFFFF8E8),
    surfaceContainer = Color(0xFFF6EDD9), surfaceContainerLow = Color(0xFFFBF3E3),
    surfaceContainerHigh = Color(0xFFF0E5CD), surfaceContainerHighest = Color(0xFFEADDBF),
    surfaceVariant = Color(0xFFF0E5CD), surfaceTint = Color.Transparent,
    onSurface = Color(0xFF3E3527), onSurfaceVariant = Color(0xFF6B604C),
    outline = Color(0xFF9B8968), outlineVariant = Color(0xFFDED0B2),
)
private val NightMushafColors = darkColorScheme(
    primary = Color(0xFFE5CC92), onPrimary = Color(0xFF382B16),
    primaryContainer = Color(0xFF534329), onPrimaryContainer = Color(0xFFF6E5BF),
    secondary = Color(0xFFBDCAA4), secondaryContainer = Color(0xFF39442F),
    background = Color(0xFF242119), surface = Color(0xFF242119),
    surfaceContainer = Color(0xFF332E24), surfaceContainerLow = Color(0xFF2A261E),
    surfaceContainerHigh = Color(0xFF40392C), surfaceVariant = Color(0xFF40392C),
    onSurface = Color(0xFFF3EAD7), onSurfaceVariant = Color(0xFFD1C4AA), outlineVariant = Color(0xFF5A4F3D),
)

@Composable
fun ScreenColors(screen: String, dark: Boolean, content: @Composable () -> Unit) {
    val printed = screen == "mushaf"
    val quran = screen in setOf("quran", "surahs", "reader", "mushaf")
    val qibla = screen == "qibla"
    val colors = if (quran) { if (dark && !printed) NightMushafColors else MushafColors }
        else if (qibla) MaterialTheme.colorScheme.copy(background = Color(0xFF247F78), surface = Color(0xFF247F78), onSurface = Color.White, onSurfaceVariant = Color(0xFFD8EEDE), primary = Color(0xFFFFE6A9))
        else MaterialTheme.colorScheme
    val background = if (qibla) Brush.verticalGradient(listOf(Color(0xFF247F78), Color(0xFF144B48)))
        else Brush.verticalGradient(listOf(colors.background, colors.background))
    val view = LocalView.current
    SideEffect {
        view.context.activity()?.window?.let { window ->
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= 29) {
                window.isNavigationBarContrastEnforced = false
                window.isStatusBarContrastEnforced = false
            }
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !qibla && (!dark || printed)
                isAppearanceLightNavigationBars = !qibla && (!dark || printed)
            }
        }
    }
    MaterialTheme(colorScheme = colors) {
        Box(Modifier.fillMaxSize().background(background)) { content() }
    }
}
private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}
