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

val Emerald = Color(0xFF164F43)
val Gold = Color(0xFFAC8748)
private val LightColors = lightColorScheme(
    primary = Emerald, onPrimary = Color.White,
    primaryContainer = Color(0xFFDDEDE5), onPrimaryContainer = Color(0xFF163F35),
    secondary = Color(0xFF77571F), secondaryContainer = Color(0xFFF2E8D4), onSecondaryContainer = Color(0xFF59421D),
    background = Color(0xFFFAF9F5), surface = Color(0xFFFAF9F5),
    surfaceContainer = Color(0xFFF0F1E9), surfaceContainerLow = Color(0xFFF4F4EE),
    surfaceContainerHigh = Color(0xFFE9EEE6), surfaceVariant = Color(0xFFE9EEE6),
    onSurface = Color(0xFF203B32), onSurfaceVariant = Color(0xFF4F6056),
    outline = Color(0xFF94A397), outlineVariant = Color(0xFFDFE5DC)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFA2D6BC), onPrimary = Color(0xFF103A2E),
    primaryContainer = Color(0xFF234F40), onPrimaryContainer = Color(0xFFD0ECD9),
    secondary = Color(0xFFDFC28E), secondaryContainer = Color(0xFF51432B),
    background = Color(0xFF101D18), surface = Color(0xFF101D18),
    surfaceContainer = Color(0xFF1A2B23), surfaceContainerLow = Color(0xFF17271F),
    surfaceContainerHigh = Color(0xFF27382E), surfaceVariant = Color(0xFF27382E),
    onSurface = Color(0xFFE0E8DE), onSurfaceVariant = Color(0xFFB2BFB3),
    outlineVariant = Color(0xFF34473B)
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
