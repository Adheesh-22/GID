package com.adheesh.gid.feature.debug.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adheesh.gid.GIDApplication
import com.adheesh.gid.core.designsystem.Coral
import com.adheesh.gid.core.designsystem.HolidayGreen
import com.adheesh.gid.core.designsystem.Mustard
import kotlinx.coroutines.launch

// ── Debug Screen ──────────────────────────────────────────────────────────────
// Only reachable in BuildConfig.DEBUG builds (enforced in GIDNavHost).
// Provides two actions: seed realistic sample data, or nuke everything.
@Composable
fun DebugScreen(
    onBack    : () -> Unit,
    modifier  : Modifier = Modifier,
) {
    val context   = LocalContext.current
    val container = (context.applicationContext as GIDApplication).container
    val scope     = rememberCoroutineScope()
    var status    by remember { mutableStateOf("") }
    var loading   by remember { mutableStateOf(false) }

    Column(
        modifier              = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement   = Arrangement.spacedBy(16.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))

        Icon(
            imageVector        = Icons.Rounded.BugReport,
            contentDescription = null,
            tint               = Mustard,
            modifier           = Modifier.size(48.dp),
        )

        Text(
            text      = "DEBUG",
            style     = MaterialTheme.typography.displayLarge,
            color     = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Text(
            text  = "This screen is only visible in debug builds.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))

        // Seed sample data ────────────────────────────────────────────────────
        Button(
            onClick = {
                loading = true
                status  = ""
                scope.launch {
                    try {
                        container.seeder.seed()
                        status = "✓ Sample data seeded successfully"
                    } catch (e: Exception) {
                        status = "✗ Error: ${e.message}"
                    } finally {
                        loading = false
                    }
                }
            },
            enabled  = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape    = MaterialTheme.shapes.large,
            colors   = ButtonDefaults.buttonColors(
                containerColor = HolidayGreen,
                contentColor   = MaterialTheme.colorScheme.background,
            ),
        ) {
            Text("Seed sample data", style = MaterialTheme.typography.labelLarge)
        }

        // Clear all data ───────────────────────────────────────────────────────
        OutlinedButton(
            onClick = {
                loading = true
                status  = ""
                scope.launch {
                    try {
                        container.seeder.clearAll()
                        status = "✓ All data cleared"
                    } catch (e: Exception) {
                        status = "✗ Error: ${e.message}"
                    } finally {
                        loading = false
                    }
                }
            },
            enabled  = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape    = MaterialTheme.shapes.large,
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = Coral),
            border   = BorderStroke(1.dp, Coral),
        ) {
            Text("Clear all data", style = MaterialTheme.typography.labelLarge)
        }

        // Status text ──────────────────────────────────────────────────────────
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
        }
        if (status.isNotBlank()) {
            Text(
                text  = status,
                style = MaterialTheme.typography.bodyMedium,
                color = if (status.startsWith("✓")) HolidayGreen else Coral,
            )
        }

        Spacer(Modifier.weight(1f))

        // Back button
        TextButton(onClick = onBack) {
            Text("← Back to app", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
