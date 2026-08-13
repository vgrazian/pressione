package com.pressione.iperteso.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pressione.iperteso.domain.model.AuthSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "iperteso_session")

/**
 * Session manager — stores auth session in DataStore.
 * 8-hour TTL, matching the web app.
 */
class SessionManager(private val context: Context) {

    companion object {
        private val KEY_USERNAME = stringPreferencesKey("session_username")
        private val KEY_ROLE = stringPreferencesKey("session_role")
        private val KEY_EMAIL = stringPreferencesKey("session_email")
        private val KEY_LOGIN_TIMESTAMP = longPreferencesKey("session_login_ts")
    }

    val session: Flow<AuthSession?> = context.dataStore.data.map { prefs ->
        val username = prefs[KEY_USERNAME] ?: return@map null
        val role = prefs[KEY_ROLE] ?: return@map null
        val email = prefs[KEY_EMAIL] ?: return@map null
        val loginTs = prefs[KEY_LOGIN_TIMESTAMP] ?: return@map null

        val session = AuthSession(username, role, email, loginTs)
        if (session.isExpired) null else session
    }

    suspend fun saveSession(session: AuthSession) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USERNAME] = session.username
            prefs[KEY_ROLE] = session.role
            prefs[KEY_EMAIL] = session.email
            prefs[KEY_LOGIN_TIMESTAMP] = session.loginTimestamp
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}
