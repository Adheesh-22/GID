package com.adheesh.gid.feature.auth.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.adheesh.gid.BuildConfig
import com.adheesh.gid.R
import com.adheesh.gid.core.designsystem.Error
import com.adheesh.gid.core.designsystem.GIDBackground
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDPeriwinkle
import com.adheesh.gid.core.designsystem.GIDSurfaceHigh
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.GIDTextField
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.feature.auth.data.AuthErrorKind
import com.adheesh.gid.feature.auth.viewmodel.AuthMode
import com.adheesh.gid.feature.auth.viewmodel.AuthUiState
import com.adheesh.gid.feature.auth.viewmodel.AuthViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

// ── AuthScreen (SPEC §2.1) ───────────────────────────────────────────────────
// Curved header with logo mark, title, email, password with show/hide, wide
// pill button, Google social row, sign-up toggle and forgot password.
@Composable
fun AuthScreen(viewModel: AuthViewModel, modifier: Modifier = Modifier) {
    val ui by viewModel.ui.collectAsState()
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    Box(modifier.fillMaxSize().background(GIDBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AuthHeader(isSignUp = ui.mode == AuthMode.SIGN_UP)
            Spacer(Modifier.height(24.dp))
            AuthForm(
                ui        = ui,
                onEmail   = viewModel::setEmail,
                onPass    = viewModel::setPassword,
                onConfirm = viewModel::setConfirmPassword,
                onSubmit  = viewModel::submit,
                onForgot  = viewModel::forgotPassword,
                onToggle  = viewModel::toggleMode,
                onGoogle  = {
                    if (!viewModel.isGoogleConfigured) {
                        viewModel.showError(AuthErrorKind.GOOGLE_NOT_CONFIGURED)
                    } else {
                        scope.launch {
                            requestGoogleIdToken(
                                context  = context,
                                onToken  = viewModel::signInWithGoogle,
                                onFailed = viewModel::showError,
                            )
                        }
                    }
                },
            )
            Spacer(Modifier.height(32.dp))
        }

        if (ui.loading) {
            Box(
                modifier         = Modifier.fillMaxSize().background(GIDBackground.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = GIDCyan) }
        }
    }
}

/** Header whose bottom edge curves downward in the middle. */
private val CurvedBottom = GenericShape { size, _ ->
    val curve = size.height * 0.18f
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height - curve)
    quadraticTo(size.width / 2f, size.height + curve, 0f, size.height - curve)
    close()
}

@Composable
private fun AuthHeader(isSignUp: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(CurvedBottom)
            .background(GIDPeriwinkle),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.statusBarsPadding(),
        ) {
            Box(
                modifier         = Modifier.size(72.dp).clip(CircleShape).background(GIDBackground),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.auth_logo_letter), style = MaterialTheme.typography.displayMedium, color = GIDCyan)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text  = stringResource(if (isSignUp) R.string.auth_title_sign_up else R.string.auth_title_sign_in),
                style = MaterialTheme.typography.titleLarge,
                color = OnCard,
            )
        }
    }
}

@Composable
private fun AuthForm(
    ui        : AuthUiState,
    onEmail   : (String) -> Unit,
    onPass    : (String) -> Unit,
    onConfirm : (String) -> Unit,
    onSubmit  : () -> Unit,
    onForgot  : () -> Unit,
    onToggle  : () -> Unit,
    onGoogle  : () -> Unit,
) {
    val focus       = LocalFocusManager.current
    var showPass    by rememberSaveable { mutableStateOf(false) }
    var showConfirm by rememberSaveable { mutableStateOf(false) }
    val isSignUp    = ui.mode == AuthMode.SIGN_UP

    Column(
        modifier            = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        GIDTextField(
            value           = ui.email,
            onValueChange   = onEmail,
            label           = stringResource(R.string.field_email),
            error           = ui.emailError?.let { stringResource(it) },
            keyboardOptions = KeyboardOptions(
                keyboardType   = KeyboardType.Email,
                imeAction      = ImeAction.Next,
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
            ),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
            leadingIcon     = { Icon(Icons.Rounded.Email, contentDescription = null, tint = GIDMutedText) },
        )

        GIDTextField(
            value                = ui.password,
            onValueChange        = onPass,
            label                = stringResource(R.string.field_password),
            error                = ui.passwordError?.let { stringResource(it) },
            visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions      = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction    = if (isSignUp) ImeAction.Next else ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focus.moveFocus(FocusDirection.Down) },
                onDone = { focus.clearFocus(); onSubmit() },
            ),
            leadingIcon  = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = GIDMutedText) },
            trailingIcon = { VisibilityToggle(showPass) { showPass = !showPass } },
        )

        AnimatedVisibility(visible = isSignUp) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                GIDTextField(
                    value                = ui.confirmPassword,
                    onValueChange        = onConfirm,
                    label                = stringResource(R.string.field_confirm_password),
                    error                = ui.confirmError?.let { stringResource(it) },
                    visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions      = KeyboardActions(onDone = { focus.clearFocus(); onSubmit() }),
                    leadingIcon          = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = GIDMutedText) },
                    trailingIcon         = { VisibilityToggle(showConfirm) { showConfirm = !showConfirm } },
                )
                Text(stringResource(R.string.auth_privacy_line), style = MaterialTheme.typography.bodySmall, color = GIDMutedText)
            }
        }

        ui.error?.let { Banner(stringResource(it), Icons.Rounded.ErrorOutline, Error) }
        ui.info?.let { Banner(stringResource(it), Icons.Rounded.Info, GIDCyan) }

        Button(
            onClick  = { focus.clearFocus(); onSubmit() },
            enabled  = !ui.loading,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = MaterialTheme.shapes.large,
            colors   = ButtonDefaults.buttonColors(containerColor = GIDCyan, contentColor = OnCard),
        ) {
            Text(
                stringResource(if (isSignUp) R.string.auth_sign_up else R.string.auth_sign_in),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            )
        }

        // ── Social row (Google only) ─────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(Modifier.weight(1f), color = GIDSurfaceHigh)
            Text(
                stringResource(R.string.auth_or_continue),
                color    = GIDMutedText,
                style    = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            HorizontalDivider(Modifier.weight(1f), color = GIDSurfaceHigh)
        }
        OutlinedButton(
            onClick  = onGoogle,
            enabled  = !ui.loading,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = MaterialTheme.shapes.large,
            border   = BorderStroke(1.dp, GIDSurfaceHigh),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = GIDText),
        ) {
            Icon(Icons.Rounded.AccountCircle, contentDescription = null, tint = GIDCyan, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.auth_google), style = MaterialTheme.typography.labelLarge)
        }

        TextButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = buildAnnotatedString {
                    append(stringResource(if (isSignUp) R.string.auth_have_account else R.string.auth_no_account))
                    append(" ")
                    withStyle(SpanStyle(color = GIDCyan, fontWeight = FontWeight.Bold)) {
                        append(stringResource(if (isSignUp) R.string.auth_sign_in_link else R.string.auth_sign_up_link))
                    }
                },
                style     = MaterialTheme.typography.bodyMedium,
                color     = GIDMutedText,
                textAlign = TextAlign.Center,
            )
        }

        if (!isSignUp) {
            TextButton(onClick = onForgot, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.auth_forgot), color = GIDMutedText, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun VisibilityToggle(visible: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            imageVector        = if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
            contentDescription = stringResource(if (visible) R.string.cd_hide_password else R.string.cd_show_password),
            tint               = GIDMutedText,
        )
    }
}

@Composable
private fun Banner(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Surface(shape = MaterialTheme.shapes.small, color = color.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier              = Modifier.padding(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, contentDescription = null, tint = color)
            Text(text, style = MaterialTheme.typography.bodySmall, color = color)
        }
    }
}

// ── Google Sign-In via Credential Manager (Phase 2 Step D) ───────────────────
// Credential Manager shows Google's account picker and returns an ID token.
// The nonce is hashed for Google and sent raw to Supabase, which checks that
// they match so a stolen token can't be replayed.
private suspend fun requestGoogleIdToken(
    context  : Context,
    onToken  : (idToken: String, rawNonce: String) -> Unit,
    onFailed : (AuthErrorKind) -> Unit,
) {
    val rawNonce    = UUID.randomUUID().toString()
    val hashedNonce = MessageDigest.getInstance("SHA-256")
        .digest(rawNonce.toByteArray())
        .joinToString("") { "%02x".format(it) }
    try {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setNonce(hashedNonce)
            .build()
        val request    = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result     = CredentialManager.create(context).getCredential(context, request)
        val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
        onToken(credential.idToken, rawNonce)
    } catch (_: GetCredentialCancellationException) {
        // The user closed the picker: nothing to show.
    } catch (_: NoCredentialException) {
        onFailed(AuthErrorKind.GOOGLE_NOT_CONFIGURED)
    } catch (_: GetCredentialException) {
        onFailed(AuthErrorKind.UNKNOWN)
    } catch (_: Exception) {
        onFailed(AuthErrorKind.UNKNOWN)
    }
}
