package com.adheesh.gid.app

import android.content.Context
import com.adheesh.gid.BuildConfig
import com.adheesh.gid.core.database.DefaultActivityTypes
import com.adheesh.gid.core.database.GIDDatabase
import com.adheesh.gid.core.database.GIDPreferences
import com.adheesh.gid.core.database.SampleDataSeeder
import com.adheesh.gid.core.notifications.Notifications
import com.adheesh.gid.core.scheduling.AlarmScheduler
import com.adheesh.gid.core.scheduling.ClassReminderScheduler
import com.adheesh.gid.core.scheduling.DailyMaintenanceWorker
import com.adheesh.gid.core.scheduling.EveningPromptScheduler
import com.adheesh.gid.core.scheduling.TimerScheduler
import com.adheesh.gid.feature.alarm.data.ActivityTypeRepositoryImpl
import com.adheesh.gid.feature.alarm.data.AlarmRepositoryImpl
import com.adheesh.gid.feature.auth.data.AuthRepository
import com.adheesh.gid.feature.auth.data.SupabaseAuthRepository
import com.adheesh.gid.feature.calendar.data.HolidayRepositoryImpl
import com.adheesh.gid.feature.sessions.data.SessionManager
import com.adheesh.gid.feature.sessions.data.SessionRepositoryImpl
import com.adheesh.gid.feature.settings.data.ProfilePhotoStore
import com.adheesh.gid.feature.settings.data.ProfileRepository
import com.adheesh.gid.feature.settings.data.ProfileRepositoryImpl
import com.adheesh.gid.feature.settings.data.SupabaseProfileRemoteSource
import com.adheesh.gid.feature.tasks.data.TaskRepositoryImpl
import com.adheesh.gid.feature.timetable.data.TimetableRepositoryImpl
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

// ── AppContainer – manual DI (no Hilt in V1, RULES §3) ───────────────────────
// Builds every repository and scheduler once. Screens get what they need from
// here through their ViewModel factories; receivers reach it through
// GIDApplication.
class AppContainer(private val context: Context) {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Supabase client ──────────────────────────────────────────────────────
    // Keys come from local.properties via BuildConfig and are never committed.
    // Without keys the client is null and auth shows "not configured".
    val supabase: SupabaseClient? =
        if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_ANON_KEY.isBlank()) null
        else runCatching {
            createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
            ) {
                install(Auth) {
                    // Email-confirmation and password-reset links open the app here.
                    scheme = "com.adheesh.gid"
                    host   = "callback"
                }
                install(Postgrest)   // profile row only (RULES §6)
            }
        }.getOrNull()

    // ── Local storage ────────────────────────────────────────────────────────
    val database: GIDDatabase = GIDDatabase.create(
        context                   = context,
        allowDestructiveMigration = BuildConfig.DEBUG,
    )
    val preferences = GIDPreferences(context)
    val photoStore  = ProfilePhotoStore(context, preferences)

    // ── Repositories ─────────────────────────────────────────────────────────
    val taskRepository         = TaskRepositoryImpl(database.taskDao())
    val timetableRepository    = TimetableRepositoryImpl(database.timetableDao())
    val holidayRepository      = HolidayRepositoryImpl(database.holidayDao())
    val activityTypeRepository = ActivityTypeRepositoryImpl(database.activityTypeDao())
    val alarmRepository        = AlarmRepositoryImpl(database.alarmDao())
    val sessionRepository      = SessionRepositoryImpl(database.activitySessionDao())
    val profileRepository: ProfileRepository =
        ProfileRepositoryImpl(database.profileDao(), SupabaseProfileRemoteSource(supabase))

    val authRepository: AuthRepository = SupabaseAuthRepository(
        client         = supabase,
        preferences    = preferences,
        googleClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
        // Pull the cloud profile before routing decides Settings vs Home.
        onSignedIn     = { user -> profileRepository.pullFromRemote(user.id) },
        appScope       = appScope,
    )

    // ── Scheduling ───────────────────────────────────────────────────────────
    val alarmScheduler         = AlarmScheduler(context, alarmRepository)
    val timerScheduler         = TimerScheduler(context)
    val classReminderScheduler = ClassReminderScheduler(context, timetableRepository, holidayRepository, preferences)
    val eveningPromptScheduler = EveningPromptScheduler(context, preferences)
    val sessionManager         = SessionManager(context, sessionRepository, activityTypeRepository, timerScheduler)

    // ── Debug seeding ────────────────────────────────────────────────────────
    val seeder = SampleDataSeeder(database)

    /** Date tapped on Home's calendar strip; the Calendar tab opens on it. */
    val calendarSelection = MutableStateFlow<LocalDate?>(null)

    /**
     * Rebuilds every alarm Android may have forgotten (SPEC §7): after boot,
     * clock or time-zone changes, app updates, and once a day.
     */
    suspend fun rescheduleEverything() {
        runCatching { alarmRepository.observeAll().first().forEach { alarmScheduler.schedule(it) } }
        runCatching { classReminderScheduler.replan() }
        runCatching { eveningPromptScheduler.schedule() }
        runCatching { sessionManager.repairRunning() }
    }

    /** Called once from GIDApplication.onCreate. */
    fun onAppStart() {
        Notifications.createChannels(context)
        DailyMaintenanceWorker.enqueue(context)
        appScope.launch {
            if (preferences.isFirstLaunch.first()) {
                if (database.activityTypeDao().count() == 0) {
                    database.activityTypeDao().upsertAll(DefaultActivityTypes.all())
                }
                preferences.setFirstLaunchDone()
            }
            rescheduleEverything()
            // A profile saved offline is pushed on the next launch (SPEC §2.5).
            preferences.signedInUser.first()?.let { user -> profileRepository.syncPending(user.email) }
        }
    }
}
