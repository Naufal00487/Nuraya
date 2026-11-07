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

val Emerald = Color(0xFF2C4066)
val Gold = Color(0xFFBEA153)
private val LightColors = lightColorScheme(
    primary = Emerald, onPrimary = Color.White,
    primaryContainer = Color(0xFFE2EBFA), onPrimaryContainer = Color(0xFF25395B),
    secondary = Color(0xFF74602C), secondaryContainer = Color(0xFFF2EBCD), onSecondaryContainer = Color(0xFF56451C),
    background = Color(0xFFF6F8FC), surface = Color(0xFFF6F8FC),
    surfaceContainer = Color(0xFFEDF1F8), surfaceContainerLow = Color(0xFFEEF3FB),
    surfaceContainerHigh = Color(0xFFE4EBF6), surfaceVariant = Color(0xFFE4EBF6),
    onSurface = Color(0xFF213352), onSurfaceVariant = Color(0xFF52617A),
    outline = Color(0xFF94A4C0), outlineVariant = Color(0xFFD7E0EF)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFBACDF4), onPrimary = Color(0xFF122440),
    primaryContainer = Color(0xFF2C4066), onPrimaryContainer = Color(0xFFE3ECFE),
    secondary = Color(0xFFDEC77F), secondaryContainer = Color(0xFF51482C),
    background = Color(0xFF111C30), surface = Color(0xFF111C30),
    surfaceContainer = Color(0xFF1B2A43), surfaceContainerLow = Color(0xFF17253B),
    surfaceContainerHigh = Color(0xFF293A55), surfaceVariant = Color(0xFF293A55),
    onSurface = Color(0xFFE5ECF8), onSurfaceVariant = Color(0xFFB9C5DC),
    outlineVariant = Color(0xFF384B69)
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
