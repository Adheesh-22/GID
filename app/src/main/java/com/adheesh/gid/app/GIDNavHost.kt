package com.adheesh.gid.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CalendarMonth
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
import com.adheesh.gid.feature.auth.ui.AuthScreen
import com.adheesh.gid.feature.auth.viewmodel.AuthViewModel
import com.adheesh.gid.feature.calendar.ui.CalendarScreen
import com.adheesh.gid.feature.calendar.viewmodel.CalendarViewModel
import com.adheesh.gid.feature.debug.ui.DebugScreen
import com.adheesh.gid.feature.history.ui.HistoryScreen
import com.adheesh.gid.feature.history.viewmodel.HistoryViewModel
import com.adheesh.gid.feature.home.ui.HomeScreen
import com.adheesh.gid.feature.home.viewmodel.HomeViewModel
import com.adheesh.gid.feature.settings.ui.SettingsScreen
import com.adheesh.gid.feature.settings.viewmodel.SettingsViewModel
import com.adheesh.gid.feature.tasks.data.TaskActions
import com.adheesh.gid.feature.tasks.ui.UpdateProgressDialog
import com.adheesh.gid.feature.tasks.viewmodel.ProgressViewModel
import com.adheesh.gid.feature.timetable.ui.TimetablePopup
import com.adheesh.gid.feature.timetable.viewmodel.TimetableViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val tabDestinations = setOf<NavKey>(Home, Calendar, History, Settings)

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

    val current by remember { derivedStateOf { backStack.lastOrNull() } }
    val showBottomBar = current in tabDestinations
    val incomplete by container.profileRepository.observeIncomplete().collectAsState(initial = false)
    val taskActions = remember { TaskActions(container.taskRepository) }

    val tabs = listOf(
        BottomNavItem(Home,     Icons.Rounded.Home,          stringResource(R.string.nav_home)),
        BottomNavItem(Calendar, Icons.Rounded.CalendarMonth, stringResource(R.string.nav_calendar)),
        BottomNavItem(History,  Icons.Rounded.BarChart,      stringResource(R.string.nav_history)),
        // The dot stays while name or DOB is missing (SPEC §1).
        BottomNavItem(Settings, Icons.Rounded.Settings,      stringResource(R.string.nav_settings), showBadge = incomplete),
    )

    Scaffold(
        containerColor = GIDBackground,
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
            entryProvider    = entryProvider {
                entry<Auth> {
                    val vm = viewModel { AuthViewModel(container.authRepository) }
                    AuthScreen(viewModel = vm)
                }

                entry<Home> {
                    val vm = viewModel {
                        HomeViewModel(
                            profiles  = container.profileRepository,
                            photos    = container.photoStore,
                            tasks     = container.taskRepository,
                            holidays  = container.holidayRepository,
                            timetable = container.timetableRepository,
                            alarms    = container.alarmRepository,
                            types     = container.activityTypeRepository,
                            actions   = taskActions,
                        )
                    }
                    var alarmsOpen    by rememberSaveable { mutableStateOf(false) }
                    var timetableOpen by rememberSaveable { mutableStateOf(false) }

                    HomeScreen(
                        viewModel       = vm,
                        onOpenSettings  = { backStack.switchTab(Settings) },
                        onOpenCalendar  = { date ->
                            container.calendarSelection.value = date
                            backStack.switchTab(Calendar)
                        },
                        onOpenAlarms    = { alarmsOpen = true },
                        onOpenTimetable = { timetableOpen = true },
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
                            TimetableViewModel(container.timetableRepository, container.classReminderScheduler, container.preferences)
                        }
                        TimetablePopup(timetableVm, onDismiss = { timetableOpen = false })
                    }
                }

                entry<Calendar> {
                    val vm = viewModel {
                        CalendarViewModel(
                            tasks       = container.taskRepository,
                            holidays    = container.holidayRepository,
                            actions     = taskActions,
                            initialDate = container.calendarSelection.value,
                        )
                    }
                    CalendarScreen(
                        viewModel         = vm,
                        externalSelection = container.calendarSelection,
                        onSelectionUsed   = { container.calendarSelection.value = null },
                    )
                }

                entry<History> {
                    val vm = viewModel {
                        HistoryViewModel(container.taskRepository, container.sessionRepository, container.activityTypeRepository)
                    }
                    HistoryScreen(vm)
                }

                entry<Settings> {
                    val vm = viewModel {
                        SettingsViewModel(
                            profiles    = container.profileRepository,
                            auth        = container.authRepository,
                            photos      = container.photoStore,
                            preferences = container.preferences,
                            evening     = container.eveningPromptScheduler,
                        )
                    }
                    SettingsScreen(viewModel = vm, onGoHome = { backStack.switchTab(Home) })
                }

                entry<Debug> {
                    if (BuildConfig.DEBUG) DebugScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
                }
            },
        )
    }

    // ── App-wide dialogs ─────────────────────────────────────────────────────
    val progressRequested by openProgress.collectAsState()
    if (progressRequested && resolved.user != null) {
        val vm = viewModel(key = "progress") { ProgressViewModel(container.taskRepository, taskActions) }
        UpdateProgressDialog(vm, onDismiss = onProgressShown)
    }

    val recovery by container.authRepository.passwordRecoveryPending.collectAsState()
    if (recovery) NewPasswordDialog(container)
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
