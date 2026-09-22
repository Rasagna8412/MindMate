package com.mindmate.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mindmate_settings")

class UserPreferencesStore(private val context: Context) {

    companion object {
        val KEY_PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")
        val KEY_PERSONALIZATION_ENABLED = booleanPreferencesKey("personalization_enabled")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_DEMO_MODE_ACTIVE = booleanPreferencesKey("demo_mode_active")
    }

    val preferredLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_PREFERRED_LANGUAGE] ?: "Auto"
    }

    val isPersonalizationEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_PERSONALIZATION_ENABLED] ?: true
    }

    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val isDemoModeActive: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEMO_MODE_ACTIVE] ?: false
    }

    suspend fun setPreferredLanguage(language: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PREFERRED_LANGUAGE] = language
        }
    }

    suspend fun setPersonalizationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PERSONALIZATION_ENABLED] = enabled
        }
    }

    suspend fun setCompletedOnboarding(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setDemoModeActive(active: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEMO_MODE_ACTIVE] = active
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
