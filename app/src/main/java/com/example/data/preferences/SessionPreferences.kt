package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "immich_vault_prefs")

data class SessionCredentials(
    val serverUrl: String = "",
    val accessToken: String = "",
    val apiKey: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val isAdmin: Boolean = false,
) {
    val isAuthenticated: Boolean
        get() = serverUrl.isNotBlank() && (accessToken.isNotBlank() || apiKey.isNotBlank())
}

class SessionPreferences(private val context: Context) {

    private object Keys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val API_KEY = stringPreferencesKey("api_key")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val IS_ADMIN = booleanPreferencesKey("is_admin")
    }

    val sessionFlow: Flow<SessionCredentials> = context.dataStore.data.map { prefs ->
        SessionCredentials(
            serverUrl = prefs[Keys.SERVER_URL].orEmpty(),
            accessToken = prefs[Keys.ACCESS_TOKEN].orEmpty(),
            apiKey = prefs[Keys.API_KEY].orEmpty(),
            userId = prefs[Keys.USER_ID].orEmpty(),
            userName = prefs[Keys.USER_NAME].orEmpty(),
            userEmail = prefs[Keys.USER_EMAIL].orEmpty(),
            isAdmin = prefs[Keys.IS_ADMIN] ?: false,
        )
    }

    suspend fun saveSession(credentials: SessionCredentials) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SERVER_URL] = credentials.serverUrl
            prefs[Keys.ACCESS_TOKEN] = credentials.accessToken
            prefs[Keys.API_KEY] = credentials.apiKey
            prefs[Keys.USER_ID] = credentials.userId
            prefs[Keys.USER_NAME] = credentials.userName
            prefs[Keys.USER_EMAIL] = credentials.userEmail
            prefs[Keys.IS_ADMIN] = credentials.isAdmin
        }
    }

    suspend fun clearSession(keepServerUrl: Boolean = true) {
        context.dataStore.edit { prefs ->
            val savedUrl = prefs[Keys.SERVER_URL].orEmpty()
            prefs.clear()
            if (keepServerUrl && savedUrl.isNotBlank()) {
                prefs[Keys.SERVER_URL] = savedUrl
            }
        }
    }
}
