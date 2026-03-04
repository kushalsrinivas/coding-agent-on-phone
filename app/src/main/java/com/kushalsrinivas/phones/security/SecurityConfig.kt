package com.kushalsrinivas.phones.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.securityStore: DataStore<Preferences> by preferencesDataStore(name = "security")

class SecurityConfig(private val context: Context) {

    companion object {
        private val ALLOWED_USER_IDS = stringSetPreferencesKey("allowed_telegram_user_ids")
        private val ALLOWED_DIRECTORIES = stringSetPreferencesKey("allowed_directories")
        private val RATE_LIMIT_PER_MINUTE = intPreferencesKey("rate_limit_per_minute")
    }

    val allowedUserIds: Flow<Set<String>> = context.securityStore.data
        .map { prefs -> prefs[ALLOWED_USER_IDS] ?: emptySet() }

    val allowedDirectories: Flow<Set<String>> = context.securityStore.data
        .map { prefs -> prefs[ALLOWED_DIRECTORIES] ?: setOf("projects/") }

    val rateLimitPerMinute: Flow<Int> = context.securityStore.data
        .map { prefs -> prefs[RATE_LIMIT_PER_MINUTE] ?: 30 }

    suspend fun setAllowedUserIds(ids: Set<String>) {
        context.securityStore.edit { prefs ->
            prefs[ALLOWED_USER_IDS] = ids
        }
    }

    suspend fun setAllowedDirectories(dirs: Set<String>) {
        context.securityStore.edit { prefs ->
            prefs[ALLOWED_DIRECTORIES] = dirs
        }
    }

    suspend fun setRateLimit(limit: Int) {
        context.securityStore.edit { prefs ->
            prefs[RATE_LIMIT_PER_MINUTE] = limit
        }
    }
}
