package com.bettermingle.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bettermingle_settings")

enum class PremiumTier { FREE, PRO, BUSINESS }

data class AppSettings(
    val isPremium: Boolean = false,
    val premiumTier: PremiumTier = PremiumTier.FREE,
    val premiumUntil: Long? = null,
    val userName: String = "",
    val userEmail: String = "",
    val userAvatarUrl: String = "",
    val notificationsEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val themeMode: String = "system", // "system", "light", "dark"
    val appLanguage: String = "system" // "system", "cs", "en"
)

class SettingsManager(private val context: Context) {

    private object Keys {
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val PREMIUM_UNTIL = longPreferencesKey("premium_until")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_AVATAR_URL = stringPreferencesKey("user_avatar_url")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val PREMIUM_TIER = stringPreferencesKey("premium_tier")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val USER_PHONE = stringPreferencesKey("user_phone")
        val USER_DEPARTMENT = stringPreferencesKey("user_department")
        val USER_BIO = stringPreferencesKey("user_bio")
        val PROFILE_SETUP_COMPLETED = booleanPreferencesKey("profile_setup_completed")
        val YEAR_IN_REVIEW_DISMISSED_YEAR = intPreferencesKey("year_in_review_dismissed_year")
        val ACTIVITIES_JSON = stringPreferencesKey("user_activities_json")
        val UNREAD_ACTIVITY_COUNT = intPreferencesKey("unread_activity_count")
        val COACH_MARKS_SEEN = stringPreferencesKey("coach_marks_seen")
    }

    data class LocalActivityEntry(
        val id: String,
        val eventId: String,
        val eventName: String,
        val actorName: String,
        val actorId: String,
        val type: String,
        val description: String,
        val timestamp: Long
    )

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            isPremium = run {
                val tier = try { PremiumTier.valueOf((prefs[Keys.PREMIUM_TIER] ?: "FREE").uppercase()) } catch (_: Exception) { PremiumTier.FREE }
                val until = prefs[Keys.PREMIUM_UNTIL]
                tier != PremiumTier.FREE && (until == null || until > System.currentTimeMillis())
            },
            premiumTier = try { PremiumTier.valueOf((prefs[Keys.PREMIUM_TIER] ?: "FREE").uppercase()) } catch (_: Exception) { PremiumTier.FREE },
            premiumUntil = prefs[Keys.PREMIUM_UNTIL],
            userName = prefs[Keys.USER_NAME] ?: "",
            userEmail = prefs[Keys.USER_EMAIL] ?: "",
            userAvatarUrl = prefs[Keys.USER_AVATAR_URL] ?: "",
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            themeMode = prefs[Keys.THEME_MODE] ?: "system",
            appLanguage = prefs[Keys.APP_LANGUAGE] ?: "system"
        )
    }

    suspend fun updatePremiumStatus(isPremium: Boolean, premiumUntil: Long?, tier: PremiumTier? = null) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_PREMIUM] = isPremium
            if (premiumUntil != null) {
                prefs[Keys.PREMIUM_UNTIL] = premiumUntil
            }
            if (tier != null) {
                prefs[Keys.PREMIUM_TIER] = tier.name
            } else if (isPremium && prefs[Keys.PREMIUM_TIER] == PremiumTier.FREE.name) {
                prefs[Keys.PREMIUM_TIER] = PremiumTier.PRO.name
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

    suspend fun updateFullProfile(
        name: String,
        email: String,
        avatarUrl: String = "",
        phone: String = "",
        department: String = "",
        bio: String = "",
        profileSetupCompleted: Boolean = false
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = name
            prefs[Keys.USER_EMAIL] = email
            prefs[Keys.USER_AVATAR_URL] = avatarUrl
            prefs[Keys.USER_PHONE] = phone
            prefs[Keys.USER_DEPARTMENT] = department
            prefs[Keys.USER_BIO] = bio
            prefs[Keys.PROFILE_SETUP_COMPLETED] = profileSetupCompleted
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode
        }
    }

    suspend fun setAppLanguage(language: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_LANGUAGE] = language
        }
    }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = true
        }
    }

    suspend fun dismissYearInReview(year: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.YEAR_IN_REVIEW_DISMISSED_YEAR] = year
        }
    }

    fun getYearInReviewDismissedYear(): Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.YEAR_IN_REVIEW_DISMISSED_YEAR] ?: 0
    }

    suspend fun setDebugTier(tier: PremiumTier) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PREMIUM_TIER] = tier.name
            prefs[Keys.IS_PREMIUM] = tier != PremiumTier.FREE
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { prefs ->
            val keepOnboarding = prefs[Keys.ONBOARDING_COMPLETED] ?: false
            prefs.clear()
            if (keepOnboarding) {
                prefs[Keys.ONBOARDING_COMPLETED] = true
            }
        }
    }

    // --- Coach marks ---

    suspend fun markCoachMarkSeen(id: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.COACH_MARKS_SEEN] ?: ""
            val set = current.split(",").filter { it.isNotBlank() }.toMutableSet()
            set.add(id)
            prefs[Keys.COACH_MARKS_SEEN] = set.joinToString(",")
        }
    }

    fun isCoachMarkSeen(id: String): Flow<Boolean> = context.dataStore.data.map { prefs ->
        val current = prefs[Keys.COACH_MARKS_SEEN] ?: ""
        id in current.split(",")
    }

    suspend fun resetCoachMarks() {
        context.dataStore.edit { prefs ->
            prefs[Keys.COACH_MARKS_SEEN] = ""
        }
    }

    // --- Local activity storage (ShelfSelf pattern) ---

    suspend fun addUserActivity(entry: LocalActivityEntry, maxItems: Int = 200) {
        context.dataStore.edit { prefs ->
            val json = prefs[Keys.ACTIVITIES_JSON] ?: "[]"
            val arr = try { JSONArray(json) } catch (_: Exception) { JSONArray() }
            val obj = JSONObject().apply {
                put("id", entry.id)
                put("eventId", entry.eventId)
                put("eventName", entry.eventName)
                put("actorName", entry.actorName)
                put("actorId", entry.actorId)
                put("type", entry.type)
                put("description", entry.description)
                put("timestamp", entry.timestamp)
            }
            val newArr = JSONArray().apply { put(obj) }
            for (i in 0 until arr.length()) {
                if (i >= maxItems - 1) break
                newArr.put(arr.getJSONObject(i))
            }
            prefs[Keys.ACTIVITIES_JSON] = newArr.toString()
        }
    }

    fun getUserActivitiesFlow(): Flow<List<LocalActivityEntry>> = context.dataStore.data.map { prefs ->
        val json = prefs[Keys.ACTIVITIES_JSON] ?: "[]"
        val arr = try { JSONArray(json) } catch (_: Exception) { JSONArray() }
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                add(
                    LocalActivityEntry(
                        id = obj.optString("id", ""),
                        eventId = obj.optString("eventId", ""),
                        eventName = obj.optString("eventName", ""),
                        actorName = obj.optString("actorName", ""),
                        actorId = obj.optString("actorId", ""),
                        type = obj.optString("type", ""),
                        description = obj.optString("description", ""),
                        timestamp = obj.optLong("timestamp", 0L)
                    )
                )
            }
        }
    }

    suspend fun incrementUnreadActivityCount(by: Int = 1) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.UNREAD_ACTIVITY_COUNT] ?: 0
            prefs[Keys.UNREAD_ACTIVITY_COUNT] = current + by
        }
    }

    suspend fun clearUnreadActivityCount() {
        context.dataStore.edit { prefs ->
            prefs[Keys.UNREAD_ACTIVITY_COUNT] = 0
        }
    }

    fun getUnreadActivityCountFlow(): Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.UNREAD_ACTIVITY_COUNT] ?: 0
    }
}
