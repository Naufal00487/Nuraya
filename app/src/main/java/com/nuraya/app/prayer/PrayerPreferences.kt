package com.nuraya.app.prayer

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** Local settings shared by manual selection and explicit one-shot device location. */
class PrayerPreferenceStore(context: Context) {
    internal val preferences: SharedPreferences = context.applicationContext.getSharedPreferences("nuraya_prayer", Context.MODE_PRIVATE)
    fun read(): PrayerPreferences {
        val location = runCatching {
            if (!preferences.getBoolean("location_selected", false)) null else PrayerLocation(
                preferences.getString("name", "").orEmpty(),
                preferences.getString("latitude", "").orEmpty().toDouble(),
                preferences.getString("longitude", "").orEmpty().toDouble(),
                preferences.getString("zone", "").orEmpty(),
            )
        }.getOrNull()
        return PrayerPreferences(location,
            runCatching { preferences.getInt("hijri_adjustment", 0) }.getOrDefault(0).coerceIn(-2, 2),
            runCatching { preferences.getInt("minute_adjustment", 2) }.getOrDefault(2).coerceIn(-30, 30))
    }
    fun save(value: PrayerPreferences) {
        preferences.edit().apply {
            putBoolean("location_selected", value.location != null)
            value.location?.let {
                putString("name", it.name); putString("latitude", it.latitude.toString())
                putString("longitude", it.longitude.toString()); putString("zone", it.zoneId)
            } ?: run {
                remove("name"); remove("latitude"); remove("longitude"); remove("zone")
            }
            putInt("hijri_adjustment", value.hijriAdjustment)
            putInt("minute_adjustment", value.minuteAdjustment)
        }.apply()
    }
}

@Composable
internal fun rememberPrayerPreferences(): Pair<PrayerPreferences, (PrayerPreferences) -> Unit> {
    val context = LocalContext.current
    val store = remember(context.applicationContext) { PrayerPreferenceStore(context) }
    var value by remember(store) { mutableStateOf(store.read()) }
    DisposableEffect(store) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> value = store.read() }
        store.preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { store.preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return value to { next -> store.save(next); value = next }
}
