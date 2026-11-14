package com.example.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.uiPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "fit_journey_ui_prefs"
)

enum class AppLanguage(val code: String) {
    ARABIC("ar"),
    ENGLISH("en");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            if (code == "en") ENGLISH else ARABIC
    }
}

enum class ThemePreference(val code: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromCode(code: String?): ThemePreference = when (code) {
            "light" -> LIGHT
            "dark" -> DARK
            else -> SYSTEM
        }
    }
}

data class UiPreferences(
    val language: AppLanguage = AppLanguage.ARABIC,
    val theme: ThemePreference = ThemePreference.SYSTEM
)

/**
 * Stores strictly non-sensitive UI preferences (language and theme).
 * Sensitive health/PAR-Q data is never cached in local storage.
 */
class PreferencesRepository(private val context: Context) {
    private val languageKey = stringPreferencesKey("preferred_language")
    private val themeKey = stringPreferencesKey("theme_mode")

    val uiPreferences: Flow<UiPreferences> = context.uiPrefsDataStore.data.map { prefs ->
        UiPreferences(
            language = AppLanguage.fromCode(prefs[languageKey]),
            theme = ThemePreference.fromCode(prefs[themeKey])
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.uiPrefsDataStore.edit { prefs ->
            prefs[languageKey] = language.code
        }
    }

    suspend fun setTheme(theme: ThemePreference) {
        context.uiPrefsDataStore.edit { prefs ->
            prefs[themeKey] = theme.code
        }
    }
}
