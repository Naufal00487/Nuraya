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
@Composable
fun ScreenColors(screen: String, dark: Boolean, content: @Composable () -> Unit) {
    val printed = screen == "mushaf"
    val qibla = screen == "qibla"
    val colors = if (printed) MushafColors
        else if (qibla) MaterialTheme.colorScheme.copy(background = Color(0xFF2C4066), surface = Color(0xFF2C4066), onSurface = Color.White, onSurfaceVariant = Color(0xFFE3ECFE), primary = Color(0xFFFFE6A9))
        else MaterialTheme.colorScheme
    val background = if (qibla) Brush.verticalGradient(listOf(Color(0xFF2C4066), Color(0xFF111C30)))
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
                isAppearanceLightStatusBars = screen != "home" && !qibla && (!dark || printed)
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
