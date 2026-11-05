package com.nuraya.app.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.nurayaPreferences by preferencesDataStore(name = "nuraya_preferences")

data class UserSettings(
    val language: String = "id",
    val translation: String = "id",
    val arabicSize: Float = 34f,
    val darkMode: Boolean = false,
    val tajwidColors: Boolean = true,
    val followRecitation: Boolean = true,
    val readerQariId: String = "ar.alafasy",
)

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.nurayaPreferences
    private val languageKey = stringPreferencesKey("language")
    private val translationKey = stringPreferencesKey("translation")
    private val arabicSizeKey = floatPreferencesKey("arabic_size")
    private val darkModeKey = booleanPreferencesKey("dark_mode")
    private val tajwidColorsKey = booleanPreferencesKey("tajwid_colors")
    private val followRecitationKey = booleanPreferencesKey("follow_recitation")
    private val readerQariKey = stringPreferencesKey("reader_qari")

    val settings: Flow<UserSettings> = store.data.catch {
        if (it is IOException) emit(emptyPreferences()) else throw it
    }.map { prefs ->
        UserSettings(
            language = prefs[languageKey]?.takeIf { it in languages } ?: "id",
            translation = prefs[translationKey]?.takeIf { it in translations } ?: "id",
            arabicSize = (prefs[arabicSizeKey] ?: 34f).takeIf { it.isFinite() }?.coerceIn(22f, 48f) ?: 34f,
            darkMode = prefs[darkModeKey] ?: false,
            tajwidColors = prefs[tajwidColorsKey] ?: true,
            followRecitation = prefs[followRecitationKey] ?: true,
            readerQariId = prefs[readerQariKey] ?: "ar.alafasy",
        )
    }

    suspend fun setLanguage(language: String) {
        require(language in languages)
        store.edit { it[languageKey] = language }
    }

    suspend fun setTranslation(translation: String) {
        require(translation in translations)
        store.edit { it[translationKey] = translation }
    }

    suspend fun setArabicSize(size: Float) {
        require(size.isFinite())
        store.edit { it[arabicSizeKey] = size.coerceIn(22f, 48f) }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        store.edit { it[darkModeKey] = enabled }
    }

    suspend fun setTajwidColors(enabled: Boolean) { store.edit { it[tajwidColorsKey] = enabled } }
    suspend fun setFollowRecitation(enabled: Boolean) { store.edit { it[followRecitationKey] = enabled } }
    suspend fun setReaderQari(id: String) {
        require(id.matches(Regex("[A-Za-z0-9._-]{1,100}")))
        store.edit { it[readerQariKey] = id }
    }

    companion object {
        private val languages = setOf("id", "en")
        private val translations = setOf("id", "en", "none")
    }
}
