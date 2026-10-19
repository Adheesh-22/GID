package com.adheesh.gid.app

import android.content.Context
import com.adheesh.gid.BuildConfig
import com.adheesh.gid.core.database.DefaultActivityTypes
import com.adheesh.gid.core.database.GIDDatabase
import com.adheesh.gid.core.database.GIDPreferences
import com.adheesh.gid.core.database.SampleDataSeeder
import com.adheesh.gid.feature.alarm.data.ActivityTypeRepositoryImpl
import com.adheesh.gid.feature.alarm.data.AlarmRepositoryImpl
import com.adheesh.gid.feature.auth.data.AuthRepository
import com.adheesh.gid.feature.auth.data.SupabaseAuthRepository
import com.adheesh.gid.feature.calendar.data.HolidayRepositoryImpl
import com.adheesh.gid.feature.sessions.data.SessionRepositoryImpl
import com.adheesh.gid.feature.settings.data.ProfileRepositoryImpl
import com.adheesh.gid.feature.tasks.data.TaskRepositoryImpl
import com.adheesh.gid.feature.timetable.data.TimetableRepositoryImpl
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// ── AppContainer – manual DI (no Hilt in V1, RULES §3) ───────────────────────
class AppContainer(context: Context) {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Supabase client ───────────────────────────────────────────────────────
    // Keys are injected from local.properties via BuildConfig at build time.
    // They are NEVER committed to source control.
    val supabase = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
    ) {
        install(Auth)           // session persistence handled by supabase-kt
        install(Postgrest)      // for profile upsert in Phase 3
    }

    // ── Auth repository ───────────────────────────────────────────────────────
    val authRepository: AuthRepository = SupabaseAuthRepository(supabase)

    // ── Database ──────────────────────────────────────────────────────────────
    val database: GIDDatabase = GIDDatabase.create(
        context                   = context,
        allowDestructiveMigration = BuildConfig.DEBUG,
    )

    // ── DataStore ─────────────────────────────────────────────────────────────
    val preferences = GIDPreferences(context)

    // ── Repositories ──────────────────────────────────────────────────────────
    val taskRepository         = TaskRepositoryImpl(database.taskDao())
    val timetableRepository    = TimetableRepositoryImpl(database.timetableDao())
    val holidayRepository      = HolidayRepositoryImpl(database.holidayDao())
    val activityTypeRepository = ActivityTypeRepositoryImpl(database.activityTypeDao())
    val alarmRepository        = AlarmRepositoryImpl(database.alarmDao())
    val sessionRepository      = SessionRepositoryImpl(database.activitySessionDao())
    val profileRepository      = ProfileRepositoryImpl(database.profileDao())

    // ── Sample data seeder (debug only) ──────────────────────────────────────
    val seeder = SampleDataSeeder(database)

    // ── Helper: is the local profile complete? ────────────────────────────────
    // Used by AuthViewModel to decide Home vs Settings after sign-in.
    suspend fun isProfileComplete(): Boolean {
        val profile = database.profileDao().getProfile()
        return profile?.name?.isNotBlank() == true && profile.dob != null
    }

    // ── First-launch initialisation ───────────────────────────────────────────
    init {
        appScope.launch {
            val isFirst = preferences.isFirstLaunch.first()
            if (isFirst) {
                database.activityTypeDao().upsertAll(DefaultActivityTypes.all())
                preferences.setFirstLaunchDone()
            }
        }
    }
}
