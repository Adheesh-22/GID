package com.adheesh.gid.core.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

// ── GID DataStore Preferences ─────────────────────────────────────────────────
// SPEC §6 DataStore (not Room):
//   eveningPromptTime      – minutes from midnight when the 21:00 prompt fires
//   defaultReminderMinutes – how many minutes before class a reminder fires
//   profilePhotoPath       – absolute path to the locally stored profile photo
//   homeName / Font / Color, backgroundPath / Dim – Home and app look (SPEC §2.2)
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
        // Signed-in account, cached locally so the app opens Home offline even
        // after the Supabase access token has expired (SPEC §1).
        val SIGNED_IN_USER_ID         = stringPreferencesKey("signed_in_user_id")
        val SIGNED_IN_EMAIL           = stringPreferencesKey("signed_in_email")
        // How many class-reminder alarms are currently scheduled, so a re-plan
        // can cancel exactly the old ones before scheduling new ones.
        val SCHEDULED_REMINDER_COUNT  = intPreferencesKey("scheduled_reminder_count")
        val SCHEDULED_EXAM_REMINDER_COUNT = intPreferencesKey("scheduled_exam_reminder_count")
        val NOTIFICATION_RATIONALE_SHOWN = booleanPreferencesKey("notification_rationale_shown")
        // Appearance: local only, never synced or backed up.
        val HOME_NAME                 = stringPreferencesKey("home_name")
        val HOME_NAME_FONT            = stringPreferencesKey("home_name_font")
        val HOME_NAME_COLOR           = stringPreferencesKey("home_name_color")
        val BACKGROUND_PATH           = stringPreferencesKey("background_path")
        val BACKGROUND_DIM            = floatPreferencesKey("background_dim")
        val ATTENDANCE_TARGET         = intPreferencesKey("attendance_target")
        // The semester on screen, and the one whose classes are running (SPEC §2.14).
        val VIEWED_SEMESTER           = stringPreferencesKey("viewed_semester")
        val RUNNING_SEMESTER          = stringPreferencesKey("running_semester")
        val SCHEDULED_ATTENDANCE_PROMPT_COUNT = intPreferencesKey("scheduled_attendance_prompt_count")
        val CAPTURE_HASHES_FILLED     = booleanPreferencesKey("capture_hashes_filled")

        // Defaults
        const val DEFAULT_EVENING_PROMPT_MINUTES = 21 * 60   // 21:00
        const val DEFAULT_REMINDER_MINUTES_VALUE = 10
        const val DEFAULT_BACKGROUND_DIM         = 0.45f
        const val DEFAULT_ATTENDANCE_TARGET      = 75
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

    val signedInUser: Flow<SignedInUser?> = dataStore.data.map { prefs ->
        val id = prefs[SIGNED_IN_USER_ID] ?: return@map null
        SignedInUser(id = id, email = prefs[SIGNED_IN_EMAIL].orEmpty())
    }

    val scheduledReminderCount: Flow<Int> = dataStore.data.map { it[SCHEDULED_REMINDER_COUNT] ?: 0 }

    val scheduledExamReminderCount: Flow<Int> = dataStore.data.map { it[SCHEDULED_EXAM_REMINDER_COUNT] ?: 0 }

    val notificationRationaleShown: Flow<Boolean> = dataStore.data.map {
        it[NOTIFICATION_RATIONALE_SHOWN] ?: false
    }

    val homeName: Flow<String?> = dataStore.data.map { it[HOME_NAME] }
    val homeNameFont: Flow<String?> = dataStore.data.map { it[HOME_NAME_FONT] }
    val homeNameColor: Flow<String?> = dataStore.data.map { it[HOME_NAME_COLOR] }
    val backgroundPath: Flow<String?> = dataStore.data.map { it[BACKGROUND_PATH] }
    val backgroundDim: Flow<Float> = dataStore.data.map { it[BACKGROUND_DIM] ?: DEFAULT_BACKGROUND_DIM }

    /** Null = every semester. */
    val viewedSemesterId: Flow<String?> = dataStore.data.map { it[VIEWED_SEMESTER] }.distinctUntilChanged()
    /** Null = not chosen yet, so every class counts. */
    val runningSemesterId: Flow<String?> = dataStore.data.map { it[RUNNING_SEMESTER] }.distinctUntilChanged()

    val attendanceTarget: Flow<Int> = dataStore.data.map { it[ATTENDANCE_TARGET] ?: DEFAULT_ATTENDANCE_TARGET }
    val scheduledAttendancePromptCount: Flow<Int> = dataStore.data.map { it[SCHEDULED_ATTENDANCE_PROMPT_COUNT] ?: 0 }
    val captureHashesFilled: Flow<Boolean> = dataStore.data.map { it[CAPTURE_HASHES_FILLED] ?: false }

    // ── Write helpers ─────────────────────────────────────────────────────────

    suspend fun setSignedInUser(user: SignedInUser?) {
        dataStore.edit {
            if (user == null) {
                it.remove(SIGNED_IN_USER_ID)
                it.remove(SIGNED_IN_EMAIL)
            } else {
                it[SIGNED_IN_USER_ID] = user.id
                it[SIGNED_IN_EMAIL]   = user.email
            }
        }
    }

    suspend fun setScheduledReminderCount(count: Int) {
        dataStore.edit { it[SCHEDULED_REMINDER_COUNT] = count }
    }

    suspend fun setScheduledExamReminderCount(count: Int) {
        dataStore.edit { it[SCHEDULED_EXAM_REMINDER_COUNT] = count }
    }

    suspend fun setNotificationRationaleShown() {
        dataStore.edit { it[NOTIFICATION_RATIONALE_SHOWN] = true }
    }

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

    /** A blank [name] goes back to the profile's first name. */
    suspend fun setHomeName(name: String?) {
        dataStore.edit {
            if (name.isNullOrBlank()) it.remove(HOME_NAME) else it[HOME_NAME] = name.trim()
        }
    }

    suspend fun setHomeNameFont(font: String) {
        dataStore.edit { it[HOME_NAME_FONT] = font }
    }

    suspend fun setHomeNameColor(color: String) {
        dataStore.edit { it[HOME_NAME_COLOR] = color }
    }

    suspend fun setBackgroundPath(path: String?) {
        dataStore.edit {
            if (path == null) it.remove(BACKGROUND_PATH) else it[BACKGROUND_PATH] = path
        }
    }

    suspend fun setBackgroundDim(dim: Float) {
        dataStore.edit { it[BACKGROUND_DIM] = dim }
    }

    suspend fun setViewedSemester(id: String?) {
        dataStore.edit { if (id == null) it.remove(VIEWED_SEMESTER) else it[VIEWED_SEMESTER] = id }
    }

    suspend fun setRunningSemester(id: String?) {
        dataStore.edit { if (id == null) it.remove(RUNNING_SEMESTER) else it[RUNNING_SEMESTER] = id }
    }

    suspend fun setAttendanceTarget(percent: Int) {
        dataStore.edit { it[ATTENDANCE_TARGET] = percent }
    }

    suspend fun setScheduledAttendancePromptCount(count: Int) {
        dataStore.edit { it[SCHEDULED_ATTENDANCE_PROMPT_COUNT] = count }
    }

    suspend fun setCaptureHashesFilled() {
        dataStore.edit { it[CAPTURE_HASHES_FILLED] = true }
    }

    suspend fun setFirstLaunchDone() {
        dataStore.edit { it[IS_FIRST_LAUNCH] = false }
    }
}

/** The account that is signed in on this phone (id = Supabase auth user id). */
data class SignedInUser(val id: String, val email: String)
