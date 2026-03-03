package com.bettermingle.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bettermingle_settings")

data class AppSettings(
    val isPremium: Boolean = false,
    val premiumUntil: Long? = null,
    val userName: String = "",
    val userEmail: String = "",
    val userAvatarUrl: String = "",
    val notificationsEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false
)

class SettingsManager(private val context: Context) {

    private object Keys {
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val PREMIUM_UNTIL = longPreferencesKey("premium_until")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_AVATAR_URL = stringPreferencesKey("user_avatar_url")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            isPremium = prefs[Keys.IS_PREMIUM] ?: false,
            premiumUntil = prefs[Keys.PREMIUM_UNTIL],
            userName = prefs[Keys.USER_NAME] ?: "",
            userEmail = prefs[Keys.USER_EMAIL] ?: "",
            userAvatarUrl = prefs[Keys.USER_AVATAR_URL] ?: "",
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false
        )
    }

    suspend fun updatePremiumStatus(isPremium: Boolean, premiumUntil: Long?) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_PREMIUM] = isPremium
            if (premiumUntil != null) {
                prefs[Keys.PREMIUM_UNTIL] = premiumUntil
            }
        }
    }

    suspend fun updateUserInfo(name: String, email: String, avatarUrl: String = "") {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = name
            prefs[Keys.USER_EMAIL] = email
            prefs[Keys.USER_AVATAR_URL] = avatarUrl
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = true
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
