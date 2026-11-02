package com.adheesh.gid

import android.content.Intent
import android.os.Bundle
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.adheesh.gid.app.GIDNavHost
import com.adheesh.gid.app.LibraryRequest
import com.adheesh.gid.core.designsystem.GIDTheme
import com.adheesh.gid.core.notifications.Notifications
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    /** True when the evening notification asked for 'Update today's progress'. */
    private val openProgress = MutableStateFlow(false)

    private val container get() = (application as GIDApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge: the system draws behind the status and nav bars.
        // The app is dark-only, so ask for light status/nav bar icons.
        enableEdgeToEdge(
            statusBarStyle     = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        handleIntent(intent)

        setContent {
            GIDTheme {
                GIDNavHost(
                    container       = container,
                    openProgress    = openProgress,
                    onProgressShown = { openProgress.value = false },
                )
            }
        }
    }

    // The app is already open and a notification or email link brings it forward.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        container.authRepository.handleDeepLink(intent)
        when (intent.getStringExtra(Notifications.EXTRA_OPEN)) {
            Notifications.OPEN_PROGRESS -> openProgress.value = true
            Notifications.OPEN_LIBRARY  -> container.libraryRequest.value =
                LibraryRequest(intent.getStringArrayExtra(Notifications.EXTRA_CAPTURE_IDS)?.toList().orEmpty())
            else -> return
        }
        intent.removeExtra(Notifications.EXTRA_OPEN)   // don't reopen on rotation
    }
}
