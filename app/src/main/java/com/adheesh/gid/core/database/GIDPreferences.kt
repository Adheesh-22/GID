package com.adheesh.gid.core.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// ── GID DataStore Preferences ─────────────────────────────────────────────────
// SPEC §6 DataStore (not Room):
//   eveningPromptTime      – minutes from midnight when the 21:00 prompt fires
//   defaultReminderMinutes – how many minutes before class a reminder fires
//   profilePhotoPath       – absolute path to the locally stored profile photo
//
// Using property delegate extension on Context to get a single instance.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "gid_prefs"
)

class GIDPreferences(context: Context) {

    private val dataStore = context.applicationContext.dataStore

    // ── Keys ─────────────────────────────────────────────────────────────────
    companion object {
        val EVENING_PROMPT_TIME       = intPreferencesKey("evening_prompt_time")
        val DEFAULT_REMINDER_MINUTES  = intPreferencesKey("default_reminder_minutes")
        val PROFILE_PHOTO_PATH        = stringPreferencesKey("profile_photo_path")
        val IS_FIRST_LAUNCH           = booleanPreferencesKey("is_first_launch")
        val LAST_SEEN_VERSION         = intPreferencesKey("last_seen_version")

        // Defaults
        const val DEFAULT_EVENING_PROMPT_MINUTES = 21 * 60   // 21:00
        const val DEFAULT_REMINDER_MINUTES_VALUE = 10
    }

    // ── Flows for observation ─────────────────────────────────────────────────

    val eveningPromptTime: Flow<Int> = dataStore.data.map { prefs ->
        prefs[EVENING_PROMPT_TIME] ?: DEFAULT_EVENING_PROMPT_MINUTES
    }

    val defaultReminderMinutes: Flow<Int> = dataStore.data.map { prefs ->
        prefs[DEFAULT_REMINDER_MINUTES] ?: DEFAULT_REMINDER_MINUTES_VALUE
    }

    val profilePhotoPath: Flow<String?> = dataStore.data.map { prefs ->
        prefs[PROFILE_PHOTO_PATH]
    }

    val isFirstLaunch: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[IS_FIRST_LAUNCH] ?: true
    }

    // ── Write helpers ─────────────────────────────────────────────────────────

    suspend fun setEveningPromptTime(minutesFromMidnight: Int) {
        dataStore.edit { it[EVENING_PROMPT_TIME] = minutesFromMidnight }
    }

    suspend fun setDefaultReminderMinutes(minutes: Int) {
        dataStore.edit { it[DEFAULT_REMINDER_MINUTES] = minutes }
    }

    suspend fun setProfilePhotoPath(path: String?) {
        dataStore.edit {
            if (path == null) it.remove(PROFILE_PHOTO_PATH)
            else it[PROFILE_PHOTO_PATH] = path
        }
    }

    suspend fun setFirstLaunchDone() {
        dataStore.edit { it[IS_FIRST_LAUNCH] = false }
    }
}
