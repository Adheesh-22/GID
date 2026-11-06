package com.adheesh.gid.app

import com.adheesh.gid.core.database.ExamKind
import com.adheesh.gid.feature.marks.ui.MarksDetailScreen
import com.adheesh.gid.feature.marks.ui.MarksScreen
import com.adheesh.gid.feature.marks.viewmodel.MarksDetailViewModel
import com.adheesh.gid.feature.marks.viewmodel.MarksViewModel
import com.adheesh.gid.feature.semesters.data.SemesterNames
import com.adheesh.gid.feature.semesters.ui.LocalSemesterChip
import com.adheesh.gid.feature.semesters.ui.SemesterChipState
import com.adheesh.gid.feature.semesters.ui.SemesterSheet
import android.app.Activity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.adheesh.gid.core.designsystem.LocalBackdropColors
import com.adheesh.gid.feature.appearance.ui.backdropColors
import com.adheesh.gid.feature.appearance.ui.rememberBackdropImage
import androidx.compose.runtime.CompositionLocalProvider
import com.adheesh.gid.feature.library.ui.LocalCategoryNames
import com.adheesh.gid.feature.settings.ui.HelpScreen
import com.adheesh.gid.feature.syllabus.domain.SyllabusProgress
import com.adheesh.gid.feature.syllabus.ui.SyllabusScreen
import com.adheesh.gid.feature.syllabus.viewmodel.SyllabusViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.adheesh.gid.BuildConfig
import com.adheesh.gid.R
import com.adheesh.gid.core.database.SignedInUser
import com.adheesh.gid.core.designsystem.GIDBackground
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDSurface
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.GIDTextField
import com.adheesh.gid.core.designsystem.Mustard
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.core.notifications.NotificationPermissionPrompt
import com.adheesh.gid.feature.alarm.ui.AlarmPopup
import com.adheesh.gid.feature.alarm.viewmodel.AlarmViewModel
import com.adheesh.gid.feature.appearance.domain.Appearance
import com.adheesh.gid.feature.appearance.ui.AppBackdrop
import com.adheesh.gid.feature.attendance.ui.AttendanceScreen
import com.adheesh.gid.feature.attendance.viewmodel.AttendanceViewModel
import com.adheesh.gid.feature.auth.ui.AuthScreen
import com.adheesh.gid.feature.auth.viewmodel.AuthViewModel
import com.adheesh.gid.feature.calendar.ui.CalendarScreen
import com.adheesh.gid.feature.calendar.viewmodel.CalendarViewModel
import com.adheesh.gid.feature.debug.ui.DebugScreen
import com.adheesh.gid.feature.exams.ui.ExamDetailScreen
import com.adheesh.gid.feature.exams.ui.ExamsScreen
import com.adheesh.gid.feature.exams.viewmodel.ExamDetailViewModel
import com.adheesh.gid.feature.exams.viewmodel.ExamsViewModel
import com.adheesh.gid.feature.history.ui.HistoryScreen
import com.adheesh.gid.feature.history.viewmodel.HistoryViewModel
import com.adheesh.gid.feature.home.ui.HomeScreen
import com.adheesh.gid.feature.home.viewmodel.HomeViewModel
import com.adheesh.gid.feature.library.ui.CaptureDetailScreen
import com.adheesh.gid.feature.library.ui.LibraryScreen
import com.adheesh.gid.feature.library.ui.SubjectDetailScreen
import com.adheesh.gid.feature.library.viewmodel.CaptureDetailViewModel
import com.adheesh.gid.feature.library.viewmodel.LibraryViewModel
import com.adheesh.gid.feature.library.viewmodel.SubjectDetailViewModel
import com.adheesh.gid.feature.settings.ui.SettingsScreen
import com.adheesh.gid.feature.settings.viewmodel.SettingsViewModel
import com.adheesh.gid.feature.tasks.data.TaskActions
import com.adheesh.gid.feature.tasks.ui.UpdateProgressDialog
import com.adheesh.gid.feature.tasks.viewmodel.ProgressViewModel
import com.adheesh.gid.feature.timetable.ui.TimetablePopup
import com.adheesh.gid.feature.timetable.viewmodel.TimetableViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val tabDestinations = setOf<NavKey>(Home, Calendar, Library, History, Settings)

/** Tabs that show the background photo; Settings and full-screen pages never do (SPEC §2.2). */
private val photoTabs = setOf<NavKey>(Home, Calendar, Library, History)

/** Wraps the cached account so "not loaded yet" differs from "signed out". */
private data class AccountState(val user: SignedInUser?)

// ── Spec §1 routing ──────────────────────────────────────────────────────────
// Launch: no session → Auth; saved session → Home, even offline.
// First sign-in → Settings if name or DOB is missing, else Home.
// Routing follows the signed-in account, so sign-in, sign-out and links from
// confirmation emails all land in the right place.
@Composable
fun GIDNavHost(container: AppContainer, openProgress: StateFlow<Boolean>, onProgressShown: () -> Unit) {
    val account by remember { container.authRepository.signedInUser.map { AccountState(it) } }
        .collectAsState(initial = null)

    val resolved = account
    if (resolved == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GIDCyan)
        }
        return
    }

    val backStack = rememberNavBackStack(if (resolved.user != null) Home else Auth)
    val scope = rememberCoroutineScope()

    LaunchedEffect(resolved.user?.id) {
        val user = resolved.user
        when {
            user == null && backStack.lastOrNull() != Auth -> {
                backStack.clear()
                backStack.add(Auth)
            }
            user != null && backStack.lastOrNull() == Auth -> {
                backStack.clear()
                backStack.add(Home)
                if (!container.profileRepository.isComplete()) backStack.add(Settings)
            }
        }
    }

    // A profile saved offline gets another chance whenever someone signs in.
    LaunchedEffect(resolved.user?.id) {
        resolved.user?.let { scope.launch { container.profileRepository.syncPending(it.email) } }
    }

    // Share dialog 'Sort now' and the evening notification's 'Sort inbox'.
    val libraryRequest by container.libraryRequest.collectAsState()
    LaunchedEffect(libraryRequest, resolved.user?.id) {
        if (libraryRequest != null && resolved.user != null) backStack.switchTab(Library)
    }

    // An exam reminder was tapped: open that exam.
    val examRequest by container.examRequest.collectAsState()
    LaunchedEffect(examRequest, resolved.user?.id) {
        val id = examRequest ?: return@LaunchedEffect
        if (resolved.user == null) return@LaunchedEffect
        backStack.switchTab(Home)
        backStack.add(ExamDetail(id))
        container.examRequest.value = null
    }

    val current by remember { derivedStateOf { backStack.lastOrNull() } }
    val showBottomBar = current in tabDestinations
    val incomplete by container.profileRepository.observeIncomplete().collectAsState(initial = false)
    val taskActions = remember { TaskActions(container.taskRepository) }
    val appearance by container.appearanceStore.appearance.collectAsState(initial = Appearance())
    val categoryNames by remember { container.categoryRepository.observeAll().map { list -> list.associate { it.id to it.name } } }
        .collectAsState(initial = emptyMap())
    // Home's popups; kept here so the background photo can hide while they are open.
    var alarmsOpen    by rememberSaveable { mutableStateOf(false) }
    var timetableOpen by rememberSaveable { mutableStateOf(false) }
    val showPhoto = current in photoTabs && !alarmsOpen && !timetableOpen

    // The semester in view, for the chip on each page (SPEC §2.14).
    val semesters by container.semesterRepository.semesters.collectAsState(initial = emptyList())
    val viewedSemester by container.semesterRepository.viewedId.collectAsState(initial = null)
    val runningSemester by container.semesterRepository.runningId.collectAsState(initial = null)
    var semesterSheetOpen by rememberSaveable { mutableStateOf(false) }
    val allSemestersLabel = stringResource(R.string.semester_all)
    val semesterChip = if (semesters.isEmpty()) null else SemesterChipState(
        label   = semesters.firstOrNull { it.id == viewedSemester }?.let { SemesterNames.short(it.name) } ?: allSemestersLabel,
        onClick = { semesterSheetOpen = true },
    )
    val backdrop = rememberBackdropImage(appearance.backgroundPath)
    val wallpaperColors = backdropColors(backdrop, appearance.backgroundDim, showPhoto)

    // The clock and battery icons follow the same rule as the text.
    val view = LocalView.current
    val darkStatusIcons = wallpaperColors.text.luminance() < 0.5f
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkStatusIcons
        }
    }

    val tabs = listOf(
        BottomNavItem(Home,     Icons.Rounded.Home,          stringResource(R.string.nav_home)),
        BottomNavItem(Calendar, Icons.Rounded.CalendarMonth, stringResource(R.string.nav_calendar)),
        BottomNavItem(Library,  Icons.Rounded.CollectionsBookmark, stringResource(R.string.nav_library)),
        BottomNavItem(History,  Icons.Rounded.BarChart,      stringResource(R.string.nav_history)),
        // The dot stays while name or DOB is missing (SPEC §1).
        BottomNavItem(Settings, Icons.Rounded.Settings,      stringResource(R.string.nav_settings), showBadge = incomplete),
    )

    Box(Modifier.fillMaxSize().background(GIDBackground)) {
        CompositionLocalProvider(LocalCategoryNames provides categoryNames, LocalBackdropColors provides wallpaperColors, LocalSemesterChip provides semesterChip) {
            AppBackdrop(backdrop, appearance.backgroundDim, visible = showPhoto)
            Scaffold(
                containerColor = Color.Transparent,
                floatingActionButton = {
                    if (BuildConfig.DEBUG && current == Home) {
                        SmallFloatingActionButton(
                            onClick        = { backStack.add(Debug) },
                            containerColor = Mustard,
                            contentColor   = OnCard,
                        ) { Icon(Icons.Rounded.BugReport, contentDescription = stringResource(R.string.cd_debug)) }
                    }
                },
                bottomBar = {
                    if (showBottomBar) {
                        GIDBottomBar(
                            items          = tabs,
                            currentDest    = current ?: Home,
                            onItemSelected = { dest -> backStack.switchTab(dest as NavKey) },
                        )
                    }
                },
            ) { innerPadding ->
                NavDisplay(
                    backStack        = backStack,
                    modifier         = Modifier.padding(innerPadding),
                    // Back on a tab returns to Home; back on Home leaves the app.
                    onBack           = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
                    entryDecorators  = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    // Tabs slide sideways, pages slide up and down (SPEC §1).
                    transitionSpec              = { gidPushTransition() },
                    popTransitionSpec           = { gidPopTransition() },
                    predictivePopTransitionSpec = { gidPopTransition() },
                    entryProvider    = entryProvider {
                        entry<Auth> {
                            val vm = viewModel { AuthViewModel(container.authRepository) }
                            AuthScreen(viewModel = vm)
                        }

                        entry<Home>(metadata = tabMetadata(0)) {
                            val vm = viewModel {
                                HomeViewModel(
                                    profiles  = container.profileRepository,
                                    photos    = container.photoStore,
                                    tasks     = container.taskRepository,
                                    holidays  = container.holidayRepository,
                                    timetable = container.timetableRepository,
                                    alarms    = container.alarmRepository,
                                    actions   = taskActions,
                                    looks     = container.appearanceStore,
                                    attendance = container.attendanceRepository,
                                )
                            }
                            val unsorted by container.captureRepository.observeInboxCount().collectAsState(initial = 0)
                            val upcomingExams by remember { container.examRepository.observeUpcoming() }.collectAsState(initial = emptyList())
                            val nextExam = upcomingExams.firstOrNull { !ExamKind.of(it.kind).isSubmitted }
                            val nextAssessment = upcomingExams.firstOrNull { ExamKind.of(it.kind).isSubmitted }
                            val nextAssessmentSubject by remember(nextAssessment?.subjectId) {
                                nextAssessment?.let { container.subjectRepository.observe(it.subjectId) } ?: flowOf(null)
                            }.collectAsState(initial = null)
                            val nextExamSubject by remember(nextExam?.subjectId) {
                                nextExam?.let { container.subjectRepository.observe(it.subjectId) } ?: flowOf(null)
                            }.collectAsState(initial = null)
                            val nextExamSyllabus by remember(nextExam?.id, nextExam?.subjectId) {
                                nextExam?.let { e -> container.syllabusRepository.observeExamSyllabus(e.id, e.subjectId).map { it.progress } }
                                    ?: flowOf(SyllabusProgress.EMPTY)
                            }.collectAsState(initial = SyllabusProgress.EMPTY)

                            HomeScreen(
                                viewModel       = vm,
                                onOpenSettings  = { backStack.switchTab(Settings) },
                                onOpenCalendar  = { date ->
                                    container.calendarSelection.value = date
                                    backStack.switchTab(Calendar)
                                },
                                onOpenAlarms    = { alarmsOpen = true },
                                onOpenTimetable = { timetableOpen = true },
                                unsortedCount   = unsorted,
                                onOpenLibrary   = { backStack.switchTab(Library) },
                                captureActions  = container.captureActions,
                                nextExam        = nextExam,
                                nextExamSubject = nextExamSubject,
                                nextExamSyllabus = nextExamSyllabus,
                                nextAssessment  = nextAssessment,
                                nextAssessmentSubject = nextAssessmentSubject,
                                onOpenAssessments = { backStack.add(Assessments) },
                                onOpenMarks     = { backStack.add(Marks) },
                                onOpenExams     = { backStack.add(Exams) },
                                onOpenAttendance = { backStack.add(Attendance) },
                            )
                            NotificationPermissionPrompt(container.preferences)

                            if (alarmsOpen) {
                                val alarmVm = viewModel {
                                    AlarmViewModel(
                                        alarms            = container.alarmRepository,
                                        types             = container.activityTypeRepository,
                                        scheduler         = container.alarmScheduler,
                                        sessions          = container.sessionManager,
                                        sessionRepository = container.sessionRepository,
                                    )
                                }
                                AlarmPopup(alarmVm, onDismiss = { alarmsOpen = false })
                            }
                            if (timetableOpen) {
                                val timetableVm = viewModel {
                                    TimetableViewModel(
                                        container.timetableRepository,
                                        container.classReminderScheduler,
                                        container.attendancePromptScheduler,
                                        container.preferences,
                                        container.vtopImporter,
                                    )
                                }
                                TimetablePopup(timetableVm, onDismiss = { timetableOpen = false })
                            }
                        }

                        entry<Calendar>(metadata = tabMetadata(1)) {
                            val vm = viewModel {
                                CalendarViewModel(
                                    tasks       = container.taskRepository,
                                    holidays    = container.holidayRepository,
                                    actions     = taskActions,
                                    exams       = container.examRepository,
                                    subjects    = container.subjectRepository,
                                    initialDate = container.calendarSelection.value,
                                )
                            }
                            CalendarScreen(
                                viewModel         = vm,
                                externalSelection = container.calendarSelection,
                                onSelectionUsed   = { container.calendarSelection.value = null },
                                onOpenExam        = { backStack.add(ExamDetail(it)) },
                            )
                        }

                        entry<Library>(metadata = tabMetadata(2)) {
                            val vm = viewModel { LibraryViewModel(container.captureRepository, container.subjectRepository, container.categoryRepository) }
                            LibraryScreen(
                                viewModel      = vm,
                                captureActions = container.captureActions,
                                onOpenSubject  = { backStack.add(SubjectDetail(it)) },
                                onOpenCapture  = { backStack.add(CaptureDetail(it)) },
                                onOpenExams    = { backStack.add(Exams) },
                                onOpenAssessments = { backStack.add(Assessments) },
                                onOpenMarks    = { backStack.add(Marks) },
                                requestedSort  = libraryRequest?.sortCaptureIds.orEmpty(),
                                onRequestUsed  = { container.libraryRequest.value = null },
                            )
                        }

                        entry<SubjectDetail>(metadata = pageMetadata) { key ->
                            val vm = viewModel(key = key.subjectId) {
                                SubjectDetailViewModel(
                                    key.subjectId, container.captureRepository, container.subjectRepository,
                                    container.examRepository, container.attendanceRepository, container.syllabusRepository,
                                    container.categoryRepository, container.semesterRepository,
                                )
                            }
                            Page {
                                SubjectDetailScreen(
                                    viewModel     = vm,
                                    onBack        = { backStack.removeAt(backStack.lastIndex) },
                                    onOpenCapture = { backStack.add(CaptureDetail(it)) },
                                    onOpenExam    = { backStack.add(ExamDetail(it)) },
                                    onOpenAttendance = { backStack.add(Attendance) },
                                    onOpenSyllabus = { backStack.add(Syllabus(key.subjectId)) },
                                )
                            }
                        }

                        entry<CaptureDetail>(metadata = pageMetadata) { key ->
                            val vm = viewModel(key = key.captureId) {
                                CaptureDetailViewModel(key.captureId, container.captureRepository, container.subjectRepository, container.categoryRepository)
                            }
                            Page { CaptureDetailScreen(viewModel = vm, onBack = { backStack.removeAt(backStack.lastIndex) }) }
                        }

                        entry<Exams>(metadata = pageMetadata) {
                            val vm = viewModel { ExamsViewModel(container.examRepository, container.subjectRepository, container.syllabusRepository, assessments = false, container.vtopImporter) }
                            Page {
                                ExamsScreen(
                                    viewModel  = vm,
                                    onBack     = { backStack.removeAt(backStack.lastIndex) },
                                    onOpenExam = { backStack.add(ExamDetail(it)) },
                                )
                            }
                        }

                        entry<ExamDetail>(metadata = pageMetadata) { key ->
                            val vm = viewModel(key = key.examId) {
                                ExamDetailViewModel(
                                    key.examId, container.examRepository, container.subjectRepository,
                                    container.captureRepository, container.syllabusRepository,
                                )
                            }
                            Page {
                                ExamDetailScreen(
                                    viewModel     = vm,
                                    onBack        = { backStack.removeAt(backStack.lastIndex) },
                                    onOpenCapture = { backStack.add(CaptureDetail(it)) },
                                    onOpenSyllabus = { backStack.add(Syllabus(it)) },
                                )
                            }
                        }

                        
                        entry<Assessments>(metadata = pageMetadata) {
                            val vm = viewModel(key = "assessments") {
                                ExamsViewModel(container.examRepository, container.subjectRepository, container.syllabusRepository, assessments = true, container.vtopImporter)
                            }
                            Page {
                                ExamsScreen(
                                    viewModel  = vm,
                                    onBack     = { backStack.removeAt(backStack.lastIndex) },
                                    onOpenExam = { backStack.add(ExamDetail(it)) },
                                )
                            }
                        }

                        entry<Marks>(metadata = pageMetadata) {
                            val vm = viewModel { MarksViewModel(container.marksRepository, container.vtopImporter) }
                            Page {
                                MarksScreen(
                                    viewModel     = vm,
                                    onBack        = { backStack.removeAt(backStack.lastIndex) },
                                    onOpenSubject = { backStack.add(MarksDetail(it)) },
                                )
                            }
                        }

                        entry<MarksDetail>(metadata = pageMetadata) { key ->
                            val vm = viewModel(key = "marks-${key.subjectId}") { MarksDetailViewModel(key.subjectId, container.marksRepository) }
                            Page { MarksDetailScreen(viewModel = vm, onBack = { backStack.removeAt(backStack.lastIndex) }) }
                        }

                        entry<Help>(metadata = pageMetadata) {
                            Page { HelpScreen(onBack = { backStack.removeAt(backStack.lastIndex) }) }
                        }

                        entry<Syllabus>(metadata = pageMetadata) { key ->
                            val vm = viewModel(key = "syllabus-${key.subjectId}") {
                                SyllabusViewModel(key.subjectId, container.syllabusRepository, container.subjectRepository)
                            }
                            Page { SyllabusScreen(viewModel = vm, onBack = { backStack.removeAt(backStack.lastIndex) }) }
                        }

                        entry<Attendance>(metadata = pageMetadata) {
                            val vm = viewModel { AttendanceViewModel(container.attendanceRepository) }
                            Page { AttendanceScreen(viewModel = vm, onBack = { backStack.removeAt(backStack.lastIndex) }) }
                        }

                        entry<History>(metadata = tabMetadata(3)) {
                            val vm = viewModel {
                                HistoryViewModel(container.taskRepository, container.sessionRepository, container.activityTypeRepository)
                            }
                            HistoryScreen(vm)
                        }

                        entry<Settings>(metadata = tabMetadata(4)) {
                            val vm = viewModel {
                                SettingsViewModel(
                                    profiles    = container.profileRepository,
                                    auth        = container.authRepository,
                                    photos      = container.photoStore,
                                    preferences = container.preferences,
                                    evening     = container.eveningPromptScheduler,
                                    backup      = container.backupManager,
                                    captureFiles = container.captureFiles,
                                )
                            }
                            // Opaque, so the background photo never shows behind Settings.
                            Page { SettingsScreen(viewModel = vm, onGoHome = { backStack.switchTab(Home) }, onOpenHelp = { backStack.add(Help) }) }
                        }

                        entry<Debug>(metadata = pageMetadata) {
                            if (BuildConfig.DEBUG) Page { DebugScreen(onBack = { backStack.removeAt(backStack.lastIndex) }) }
                        }
                    },
                )
            }
        }
    }

    // ── App-wide dialogs ─────────────────────────────────────────────────────
    if (semesterSheetOpen) {
        SemesterSheet(
            semesters = semesters,
            viewedId  = viewedSemester,
            runningId = runningSemester,
            onView    = { id ->
                scope.launch { container.semesterRepository.view(id) }
                semesterSheetOpen = false
            },
            onRunning = { id -> scope.launch { container.setRunningSemester(id) } },
            onAdd     = { name -> scope.launch { container.semesterRepository.view(container.semesterRepository.ensure(name).id) } },
            onDismiss = { semesterSheetOpen = false },
        )
    }

    val progressRequested by openProgress.collectAsState()
    if (progressRequested && resolved.user != null) {
        val vm = viewModel(key = "progress") { ProgressViewModel(container.taskRepository, taskActions) }
        UpdateProgressDialog(vm, onDismiss = onProgressShown)
    }

    val recovery by container.authRepository.passwordRecoveryPending.collectAsState()
    if (recovery) NewPasswordDialog(container)
}

/** A full-screen page: opaque, so nothing behind it shows through while it slides. */
@Composable
private fun Page(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(GIDBackground)) { content() }
}

/** Home is always the base, so Back from any tab goes Home and Back from Home exits. */
private fun NavBackStack<NavKey>.switchTab(dest: NavKey) {
    if (lastOrNull() == dest) return
    clear()
    add(Home)
    if (dest != Home) add(dest)
}

/** Shown after a password-reset link opens the app. */
@Composable
private fun NewPasswordDialog(container: AppContainer) {
    var password by rememberSaveable { mutableStateOf("") }
    var error    by rememberSaveable { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { container.authRepository.dismissPasswordRecovery() },
        containerColor   = GIDSurface,
        shape            = MaterialTheme.shapes.medium,
        title            = { Text(stringResource(R.string.auth_new_password_title), color = GIDText) },
        text             = {
            Column {
                GIDTextField(
                    value                = password,
                    onValueChange        = { password = it; error = null },
                    label                = stringResource(R.string.field_new_password),
                    error                = error?.let { stringResource(it) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
            }
        },
        confirmButton    = {
            TextButton(onClick = {
                if (password.length < 6) { error = R.string.auth_error_password_short; return@TextButton }
                scope.launch {
                    val failure = container.authRepository.updatePassword(password)
                    if (failure != null) error = R.string.auth_error_unknown
                }
            }) { Text(stringResource(R.string.action_save), color = GIDCyan) }
        },
        dismissButton    = {
            TextButton(onClick = { container.authRepository.dismissPasswordRecovery() }) {
                Text(stringResource(R.string.action_cancel), color = GIDMutedText)
            }
        },
    )
}
