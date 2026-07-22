package com.example.database

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_prefs")

class DataStoreManager(private val context: Context) {
    
    companion object {
        val FAMILY_ID = stringPreferencesKey("family_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_PIN = stringPreferencesKey("user_pin")
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
    }

    val familyIdFlow: Flow<String?> = context.dataStore.data.map { it[FAMILY_ID] }
    val userNameFlow: Flow<String?> = context.dataStore.data.map { it[USER_NAME] }
    val userPinFlow: Flow<String?> = context.dataStore.data.map { it[USER_PIN] }
    val isDarkThemeFlow: Flow<Boolean> = context.dataStore.data.map { it[IS_DARK_THEME] ?: true }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_DARK_THEME] = isDark
        }
    }


    suspend fun saveAuthData(familyId: String, userName: String, pin: String) {
        context.dataStore.edit { prefs ->
            prefs[FAMILY_ID] = familyId
            prefs[USER_NAME] = userName
            prefs[USER_PIN] = pin
        }
    }


    suspend fun clearAuthData() {
        context.dataStore.edit { prefs ->
            prefs.remove(FAMILY_ID)
            prefs.remove(USER_NAME)
            prefs.remove(USER_PIN)
        }
    }
}
