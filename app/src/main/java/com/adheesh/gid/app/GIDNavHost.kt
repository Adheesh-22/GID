package com.adheesh.gid.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.adheesh.gid.BuildConfig
import com.adheesh.gid.GIDApplication
import com.adheesh.gid.core.designsystem.GIDBackground
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.Mustard
import com.adheesh.gid.feature.auth.ui.AuthScreen
import com.adheesh.gid.feature.auth.viewmodel.AuthViewModel
import com.adheesh.gid.feature.calendar.ui.CalendarScreen
import com.adheesh.gid.feature.debug.ui.DebugScreen
import com.adheesh.gid.feature.history.ui.HistoryScreen
import com.adheesh.gid.feature.home.ui.HomeScreen
import com.adheesh.gid.feature.settings.ui.SettingsScreen

// Destinations that show the bottom navigation bar
private val bottomBarDestinations = setOf(Home, Calendar, History, Settings)

// ── Spec §1 routing ───────────────────────────────────────────────────────────
// Launch: no session → Auth. Session exists → Home (even offline).
// First sign-in/up → Settings if name or DOB missing, else Home.
// Back from Home must not return to Auth.
@Composable
fun GIDNavHost() {
    val context   = LocalContext.current
    val container = (context.applicationContext as GIDApplication).container

    // ── Start-destination resolution ──────────────────────────────────────────
    // Show a blank screen while we check for a cached session so there's no
    // flicker of the Auth screen before we know the user is already signed in.
    var startResolved by remember { mutableStateOf(false) }
    var startDest     by remember { mutableStateOf<NavKey>(Auth) }

    LaunchedEffect(Unit) {
        startDest     = if (container.authRepository.hasSession()) Home else Auth
        startResolved = true
    }

    if (!startResolved) {
        // Splash-like blank while resolving
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GIDCyan)
        }
        return
    }

    // ── Back-stack ────────────────────────────────────────────────────────────
    val backStack = rememberNavBackStack(startDest)
    val currentDest by remember { derivedStateOf { backStack.lastOrNull() } }
    val showBottomBar = currentDest in bottomBarDestinations

    // ── Build AuthViewModel once ──────────────────────────────────────────────
    val authViewModel = remember {
        AuthViewModel(
            repo                 = container.authRepository,
            isProfileComplete    = { container.isProfileComplete() },
        )
    }

    val bottomBarItems = remember {
        listOf(
            BottomNavItem(Home,     Icons.Rounded.Home,          "Home"),
            BottomNavItem(Calendar, Icons.Rounded.CalendarMonth, "Calendar"),
            BottomNavItem(History,  Icons.Rounded.BarChart,      "History"),
            BottomNavItem(Settings, Icons.Rounded.Settings,      "Settings", showBadge = true),
        )
    }

    Scaffold(
        floatingActionButton = {
            if (BuildConfig.DEBUG && showBottomBar) {
                SmallFloatingActionButton(
                    onClick        = { backStack.add(Debug) },
                    containerColor = Mustard,
                    contentColor   = MaterialTheme.colorScheme.background,
                ) {
                    Icon(Icons.Rounded.BugReport, contentDescription = "Debug")
                }
            }
        },
        bottomBar = {
            if (showBottomBar) {
                GIDBottomBar(
                    items          = bottomBarItems,
                    currentDest    = currentDest ?: Home,
                    onItemSelected = { dest ->
                        backStack.removeIf { it in bottomBarDestinations }
                        backStack.add(dest)
                    },
                )
            }
        },
        containerColor = GIDBackground,
    ) { innerPadding ->

        NavDisplay(
            backStack     = backStack,
            modifier      = Modifier.padding(innerPadding),
            onBack        = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {

                // ── Auth ──────────────────────────────────────────────────────
                entry<Auth> {
                    AuthScreen(
                        viewModel         = authViewModel,
                        onNavigateHome    = {
                            backStack.clear()
                            backStack.add(Home)
                        },
                        onNavigateSettings = {
                            // First-sign-in: go to Settings (setup mode).
                            // Clear Auth from back-stack so Back goes Home.
                            backStack.clear()
                            backStack.add(Home)
                            backStack.add(Settings)
                        },
                    )
                }

                // ── Home ──────────────────────────────────────────────────────
                entry<Home>     { HomeScreen() }

                // ── Calendar ─────────────────────────────────────────────────
                entry<Calendar> { CalendarScreen() }

                // ── History ──────────────────────────────────────────────────
                entry<History>  { HistoryScreen() }

                // ── Settings ─────────────────────────────────────────────────
                entry<Settings> { SettingsScreen() }

                // ── Debug (debug builds only) ─────────────────────────────────
                entry<Debug> {
                    if (BuildConfig.DEBUG) {
                        DebugScreen(onBack = { backStack.removeLastOrNull() })
                    }
                }
            },
        )
    }
}
