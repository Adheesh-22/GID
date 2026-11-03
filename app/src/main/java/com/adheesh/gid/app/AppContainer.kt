package com.adheesh.gid.app

import com.adheesh.gid.feature.syllabus.data.SyllabusRepository
import com.adheesh.gid.feature.syllabus.data.SyllabusRepositoryImpl
import android.content.Context
import com.adheesh.gid.BuildConfig
import com.adheesh.gid.core.database.DefaultActivityTypes
import com.adheesh.gid.core.database.GIDDatabase
import com.adheesh.gid.core.database.GIDPreferences
import com.adheesh.gid.core.database.SampleDataSeeder
import com.adheesh.gid.core.notifications.Notifications
import com.adheesh.gid.core.scheduling.AlarmScheduler
import com.adheesh.gid.core.scheduling.AttendancePromptScheduler
import com.adheesh.gid.core.scheduling.ClassReminderScheduler
import com.adheesh.gid.core.scheduling.DailyMaintenanceWorker
import com.adheesh.gid.core.scheduling.EveningPromptScheduler
import com.adheesh.gid.core.scheduling.ExamReminderScheduler
import com.adheesh.gid.core.scheduling.TimerScheduler
import com.adheesh.gid.feature.alarm.data.ActivityTypeRepositoryImpl
import com.adheesh.gid.feature.alarm.data.AlarmRepositoryImpl
import com.adheesh.gid.feature.appearance.data.AppearanceStore
import com.adheesh.gid.feature.attendance.data.AttendanceRepository
import com.adheesh.gid.feature.attendance.data.AttendanceRepositoryImpl
import com.adheesh.gid.feature.attendance.domain.ClassSlot
import com.adheesh.gid.feature.auth.data.AuthRepository
import com.adheesh.gid.feature.auth.data.SupabaseAuthRepository
import com.adheesh.gid.feature.calendar.data.HolidayRepositoryImpl
import com.adheesh.gid.feature.capture.data.CaptureActions
import com.adheesh.gid.feature.capture.data.CaptureFileStore
import com.adheesh.gid.feature.capture.data.CaptureRepository
import com.adheesh.gid.feature.capture.data.CaptureRepositoryImpl
import com.adheesh.gid.feature.exams.data.ExamRepository
import com.adheesh.gid.feature.exams.data.ExamRepositoryImpl
import com.adheesh.gid.feature.sessions.data.SessionManager
import com.adheesh.gid.feature.sessions.data.SessionRepositoryImpl
import com.adheesh.gid.feature.settings.data.BackupManager
import com.adheesh.gid.feature.settings.data.ProfilePhotoStore
import com.adheesh.gid.feature.settings.data.ProfileRepository
import com.adheesh.gid.feature.settings.data.ProfileRepositoryImpl
import com.adheesh.gid.feature.settings.data.SupabaseProfileRemoteSource
import com.adheesh.gid.feature.subjects.data.SubjectRepository
import com.adheesh.gid.feature.subjects.data.SubjectRepositoryImpl
import com.adheesh.gid.feature.tasks.data.TaskRepositoryImpl
import com.adheesh.gid.feature.timetable.data.TimetableRepositoryImpl
import com.adheesh.gid.feature.widget.GIDWidget
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
    val appearanceStore = AppearanceStore(context, preferences)

    // ── Repositories ─────────────────────────────────────────────────────────
    val taskRepository         = TaskRepositoryImpl(database.taskDao())
    val subjectRepository: SubjectRepository = SubjectRepositoryImpl(database)
    val timetableRepository    = TimetableRepositoryImpl(database.timetableDao(), subjectRepository)
    val captureFiles           = CaptureFileStore(context)
    val examRepository: ExamRepository = ExamRepositoryImpl(database.examDao(), database.syllabusDao(), onChanged = { onExamsChanged() })
    val syllabusRepository: SyllabusRepository = SyllabusRepositoryImpl(database)
    val captureRepository: CaptureRepository = CaptureRepositoryImpl(database, captureFiles, examRepository)
    val captureActions         = CaptureActions(captureRepository, captureFiles, appScope)
    val backupManager          = BackupManager(context, database, preferences, captureFiles)
    val holidayRepository      = HolidayRepositoryImpl(database.holidayDao())
    val activityTypeRepository = ActivityTypeRepositoryImpl(database.activityTypeDao())
    val attendanceRepository: AttendanceRepository = AttendanceRepositoryImpl(
        dao         = database.attendanceDao(),
        subjectDao  = database.subjectDao(),
        subjects    = subjectRepository,
        timetable   = timetableRepository,
        preferences = preferences,
        onChanged   = { slot -> onAttendanceChanged(slot) },
    )
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
    val examReminderScheduler  = ExamReminderScheduler(context, examRepository, preferences)
    val attendancePromptScheduler =
        AttendancePromptScheduler(context, timetableRepository, holidayRepository, { attendanceRepository }, preferences)
    val sessionManager         = SessionManager(context, sessionRepository, activityTypeRepository, timerScheduler)

    // ── Debug seeding ────────────────────────────────────────────────────────
    val seeder = SampleDataSeeder(database)

    /** Set by the share dialog or evening notification: open Library (and maybe sort these). */
    val libraryRequest = MutableStateFlow<LibraryRequest?>(null)

    /** Set by an exam reminder: open that exam's page. */
    val examRequest = MutableStateFlow<String?>(null)

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
        runCatching { examReminderScheduler.replan() }
        runCatching { attendancePromptScheduler.replan() }
        runCatching { sessionManager.repairRunning() }
        // Runs once a day too, so the widget's countdown moves on after midnight.
        GIDWidget.refresh(context)
    }

    /** After any exam is added, edited, done or deleted. */
    private suspend fun onExamsChanged() {
        runCatching { examReminderScheduler.replan() }
    }

    /** After a class is marked in the app or from its notification (SPEC §2.12). */
    private suspend fun onAttendanceChanged(slot: ClassSlot?) {
        // Marked in the app while its 'Were you in …?' was still showing.
        if (slot != null) Notifications.cancelAttendancePrompt(context, slot)
        runCatching { attendancePromptScheduler.replan() }
    }

    /** Called once from GIDApplication.onCreate. */
    fun onAppStart() {
        Notifications.createChannels(context)
        DailyMaintenanceWorker.enqueue(context)
        // Any change to the Inbox count or upcoming exams, from a screen, the
        // share sheet or a receiver, redraws the home-screen widget.
        appScope.launch {
            combine(captureRepository.observeInboxCount(), examRepository.observeUpcoming()) { count, exams -> count to exams }
                .distinctUntilChanged()
                .drop(1)   // the first value is what the widget already shows
                .collect { GIDWidget.refresh(context) }
        }
        appScope.launch {
            if (preferences.isFirstLaunch.first()) {
                if (database.activityTypeDao().count() == 0) {
                    database.activityTypeDao().upsertAll(DefaultActivityTypes.all())
                }
                preferences.setFirstLaunchDone()
            }
            rescheduleEverything()
            // Captures saved before duplicate detection get their fingerprint once.
            if (!preferences.captureHashesFilled.first()) {
                runCatching { captureRepository.fillMissingHashes() }.onSuccess { preferences.setCaptureHashesFilled() }
            }
            // A profile saved offline is pushed on the next launch (SPEC §2.5).
            preferences.signedInUser.first()?.let { user -> profileRepository.syncPending(user.email) }
        }
    }
}
