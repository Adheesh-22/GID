package com.adheesh.gid.feature.settings.ui

import android.content.Intent
import androidx.core.net.toUri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.adheesh.gid.R
import com.adheesh.gid.core.designsystem.Error
import com.adheesh.gid.core.designsystem.GIDCard
import com.adheesh.gid.core.designsystem.GIDCoral
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDDatePickerDialog
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDSurface
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.GIDTextField
import com.adheesh.gid.core.designsystem.GIDTimePickerDialog
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.core.designsystem.ProfileAvatar
import com.adheesh.gid.core.designsystem.ScreenTitle
import com.adheesh.gid.core.designsystem.SectionTitle
import com.adheesh.gid.core.util.formatMedium
import com.adheesh.gid.core.util.formatMinutesOfDay
import com.adheesh.gid.feature.settings.viewmodel.SettingsViewModel
import java.time.LocalDate

// ── Settings (SPEC §2.5) ─────────────────────────────────────────────────────
// Wireframe order: photo + 'Edit', name, DOB with age, email and phone,
// institution, then 'Go to home', 'Contact us', 'Sign out', Reliability.
@Composable
fun SettingsScreen(
    viewModel  : SettingsViewModel,
    onGoHome   : () -> Unit,
    modifier   : Modifier = Modifier,
) {
    val ui         by viewModel.ui.collectAsState()
    val photoPath  by viewModel.photoPath.collectAsState()
    val incomplete by viewModel.incomplete.collectAsState()
    val evening    by viewModel.eveningPromptMinutes.collectAsState()
    val context    = LocalContext.current
    val contactEmail   = stringResource(R.string.contact_email)
    val contactSubject = stringResource(R.string.contact_subject)

    var showDobPicker     by rememberSaveable { mutableStateOf(false) }
    var showEveningPicker by rememberSaveable { mutableStateOf(false) }
    var confirmSignOut    by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    // The system photo picker needs no storage permission.
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.setPhoto(uri)
    }

    val message = ui.message?.let { stringResource(it) }
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.messageShown()
        }
    }

    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScreenTitle(stringResource(R.string.settings_title))
            if (incomplete) {
                Text(stringResource(R.string.settings_setup_hint), style = MaterialTheme.typography.bodyMedium, color = GIDMutedText)
            }

            // ── Photo with 'Edit' under it ───────────────────────────────────
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                ProfileAvatar(
                    photoPath = photoPath,
                    name      = ui.name,
                    size      = 104.dp,
                    onClick   = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                )
                TextButton(onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Text(stringResource(R.string.settings_edit_photo), color = GIDCyan)
                }
            }

            GIDTextField(
                value           = ui.name,
                onValueChange   = viewModel::setName,
                label           = stringResource(R.string.field_name_required),
                error           = ui.nameError?.let { stringResource(it) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )

            // ── DOB (date picker) with computed age ──────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.weight(1f)) {
                    GIDTextField(
                        value         = ui.dobEpochDay?.let { LocalDate.ofEpochDay(it).formatMedium() }.orEmpty(),
                        onValueChange = {},
                        label         = stringResource(R.string.field_dob_required),
                        readOnly      = true,
                        error         = ui.dobError?.let { stringResource(it) },
                        trailingIcon  = { Icon(Icons.Rounded.Cake, contentDescription = null, tint = GIDMutedText) },
                    )
                    // A transparent layer on top so a tap anywhere opens the picker.
                    Box(Modifier.matchParentSize().clickable { showDobPicker = true })
                }
                GIDTextField(
                    value         = ui.age?.toString().orEmpty(),
                    onValueChange = {},
                    label         = stringResource(R.string.field_age),
                    readOnly      = true,
                    enabled       = false,
                    modifier      = Modifier.width(96.dp),
                )
            }

            GIDTextField(
                value         = ui.email,
                onValueChange = {},
                label         = stringResource(R.string.field_email),
                readOnly      = true,
                enabled       = false,
            )
            GIDTextField(
                value           = ui.phone,
                onValueChange   = viewModel::setPhone,
                label           = stringResource(R.string.field_phone),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            )
            GIDTextField(
                value           = ui.institution,
                onValueChange   = viewModel::setInstitution,
                label           = stringResource(R.string.field_institution),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            )

            Button(
                onClick  = viewModel::save,
                enabled  = !ui.saving && ui.loaded,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape    = MaterialTheme.shapes.large,
                colors   = ButtonDefaults.buttonColors(containerColor = GIDCyan, contentColor = OnCard),
            ) {
                Text(stringResource(if (ui.saving) R.string.settings_saving else R.string.action_save), style = MaterialTheme.typography.labelLarge)
            }

            // ── Buttons ──────────────────────────────────────────────────────
            PillButton(stringResource(R.string.settings_go_home), Icons.Rounded.Home, onClick = onGoHome)
            PillButton(stringResource(R.string.settings_contact), Icons.Rounded.Mail) {
                val intent = Intent(Intent.ACTION_SENDTO, "mailto:$contactEmail".toUri())
                    .putExtra(Intent.EXTRA_SUBJECT, contactSubject)
                runCatching { context.startActivity(intent) }
            }
            PillButton(stringResource(R.string.settings_sign_out), Icons.AutoMirrored.Rounded.Logout, tint = GIDCoral) {
                confirmSignOut = true
            }

            // ── Evening check-in time (SPEC §3: 21:00, configurable) ─────────
            GIDCard(Modifier.fillMaxWidth(), onClick = { showEveningPicker = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, tint = GIDCyan)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        SectionTitle(stringResource(R.string.settings_evening_title))
                        Text(stringResource(R.string.settings_evening_text), style = MaterialTheme.typography.bodySmall, color = GIDMutedText)
                    }
                    Text(formatMinutesOfDay(evening), style = MaterialTheme.typography.titleMedium, color = GIDText)
                }
            }

            ReliabilitySection()
            Spacer(Modifier.height(96.dp))   // room above the floating bottom bar
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
    }

    if (showDobPicker) {
        GIDDatePickerDialog(
            initial     = ui.dobEpochDay?.let(LocalDate::ofEpochDay) ?: LocalDate.now().minusYears(18),
            allowFuture = false,
            onDismiss   = { showDobPicker = false },
            onPicked    = { viewModel.setDob(it.toEpochDay()) },
        )
    }
    if (showEveningPicker) {
        GIDTimePickerDialog(
            initialMinutes = evening,
            onDismiss      = { showEveningPicker = false },
            onPicked       = viewModel::setEveningPrompt,
        )
    }
    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            containerColor   = GIDSurface,
            shape            = MaterialTheme.shapes.medium,
            title            = { Text(stringResource(R.string.settings_sign_out_title), color = GIDText) },
            text             = { Text(stringResource(R.string.settings_sign_out_text), color = GIDMutedText) },
            confirmButton    = {
                TextButton(onClick = { confirmSignOut = false; viewModel.signOut() }) {
                    Text(stringResource(R.string.settings_sign_out), color = Error)
                }
            },
            dismissButton    = {
                TextButton(onClick = { confirmSignOut = false }) { Text(stringResource(R.string.action_cancel), color = GIDMutedText) }
            },
        )
    }
}

@Composable
private fun PillButton(
    text    : String,
    icon    : androidx.compose.ui.graphics.vector.ImageVector,
    tint    : androidx.compose.ui.graphics.Color = GIDText,
    onClick : () -> Unit,
) {
    OutlinedButton(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape    = MaterialTheme.shapes.large,
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = tint)
    }
}
