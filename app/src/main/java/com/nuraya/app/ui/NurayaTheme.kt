package com.nuraya.app.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

val Emerald = Color(0xFF168C7C)
val Gold = Color(0xFFB49A58)
private val LightColors = lightColorScheme(
    primary = Emerald, onPrimary = Color.White,
    primaryContainer = Color(0xFFDDEAE1), onPrimaryContainer = Color(0xFF23463A),
    secondary = Color(0xFF74602C), secondaryContainer = Color(0xFFF2EBCD), onSecondaryContainer = Color(0xFF56451C),
    background = Color(0xFFF5F6F5), surface = Color(0xFFF5F6F5),
    surfaceContainer = Color(0xFFE9EEEC), surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFE5EBE8), surfaceVariant = Color(0xFFE5EBE8),
    onSurface = Color(0xFF192420), onSurfaceVariant = Color(0xFF62706A),
    outline = Color(0xFF89978F), outlineVariant = Color(0xFFDCE2DE)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF7BD7C3), onPrimary = Color(0xFF19372B),
    primaryContainer = Color(0xFF168C7C), onPrimaryContainer = Color(0xFFE5F2E6),
    secondary = Color(0xFFDEC77F), secondaryContainer = Color(0xFF51482C),
    background = Color(0xFF121516), surface = Color(0xFF121516),
    surfaceContainer = Color(0xFF292F32), surfaceContainerLow = Color(0xFF242A2D),
    surfaceContainerHigh = Color(0xFF30383B), surfaceVariant = Color(0xFF30383B),
    onSurface = Color(0xFFF1F3F2), onSurfaceVariant = Color(0xFFB4BFC0),
    outlineVariant = Color(0xFF3B4447)
)

@Composable
fun NurayaTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 56.dp) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography(
            displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 34.sp, lineHeight = 42.sp),
            headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp),
            headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 34.sp),
            headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
            titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp),
            titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
            titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp),
            bodyLarge = TextStyle(fontSize = 19.sp, lineHeight = 29.sp),
            bodyMedium = TextStyle(fontSize = 18.sp, lineHeight = 27.sp),
            bodySmall = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
            labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
            labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp),
            labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp)
        ), content = content)
    }
}

fun label(language: String, id: String, en: String) = if (language == "en") en else id
