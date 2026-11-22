package com.example.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "mcq_learning_prefs")

class AppPreferences(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val langKey = stringPreferencesKey("app_language")
    private val downloadedSetsKey = stringSetPreferencesKey("downloaded_question_sets")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[themeKey] ?: ThemeMode.SYSTEM.name
        ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.SYSTEM
    }

    val language: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        AppLanguage.fromCode(prefs[langKey])
    }

    val downloadedSetIds: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[downloadedSetsKey] ?: emptySet()
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeKey] = mode.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[langKey] = language.code }
    }

    suspend fun markSetDownloaded(setId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[downloadedSetsKey] ?: emptySet()
            prefs[downloadedSetsKey] = current + setId
        }
    }

    suspend fun removeDownloadedSet(setId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[downloadedSetsKey] ?: emptySet()
            prefs[downloadedSetsKey] = current - setId
        }
    }
}
