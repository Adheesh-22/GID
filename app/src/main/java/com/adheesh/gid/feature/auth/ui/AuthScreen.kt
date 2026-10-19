package com.adheesh.gid.feature.auth.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.adheesh.gid.BuildConfig
import com.adheesh.gid.core.designsystem.*
import com.adheesh.gid.feature.auth.viewmodel.AuthMode
import com.adheesh.gid.feature.auth.viewmodel.AuthUiState
import com.adheesh.gid.feature.auth.viewmodel.AuthViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

// ── AuthScreen ────────────────────────────────────────────────────────────────
// Spec 2.1: curved header + logo; email; password with show/hide; wide pill
// sign-in button; Google social row; toggle sign-up; forgot password.
// Error messages are inline per the spec.
@Composable
fun AuthScreen(
    viewModel        : AuthViewModel,
    onNavigateHome   : () -> Unit,
    onNavigateSettings: () -> Unit,
    modifier         : Modifier = Modifier,
) {
    val ui    by viewModel.ui.collectAsState()
    val ctx   = LocalContext.current
    val scope = rememberCoroutineScope()

    // Consume one-shot navigation events
    LaunchedEffect(ui.navigateToHome) {
        if (ui.navigateToHome) { viewModel.onNavigationConsumed(); onNavigateHome() }
    }
    LaunchedEffect(ui.navigateToSettings) {
        if (ui.navigateToSettings) { viewModel.onNavigationConsumed(); onNavigateSettings() }
    }

    // "Reset email sent" snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(ui.showResetEmailSent) {
        if (ui.showResetEmailSent) {
            snackbarHostState.showSnackbar("Password reset email sent! Check your inbox.")
            viewModel.onResetEmailSentConsumed()
        }
    }

    Scaffold(
        snackbarHost    = { SnackbarHost(snackbarHostState) },
        containerColor  = GIDBackground,
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // ── Curved header ─────────────────────────────────────────────
                AuthHeader(isSignUp = ui.mode == AuthMode.SIGN_UP)

                Spacer(Modifier.height(32.dp))

                // ── Form ──────────────────────────────────────────────────────
                AuthForm(
                    ui        = ui,
                    onEmail   = viewModel::setEmail,
                    onPass    = viewModel::setPassword,
                    onConfirm = viewModel::setConfirmPassword,
                    onSubmit  = viewModel::submit,
                    onForgot  = viewModel::forgotPassword,
                    onToggle  = viewModel::toggleMode,
                    onGoogle  = {
                        scope.launch {
                            launchGoogleSignIn(ctx) { token ->
                                viewModel.signInWithGoogle(token)
                            }
                        }
                    },
                )

                Spacer(Modifier.height(32.dp))
            }

            // Full-screen loading overlay
            if (ui.loading) {
                Box(
                    modifier         = Modifier
                        .fillMaxSize()
                        .background(GIDBackground.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = GIDCyan)
                }
            }
        }
    }
}

// ── Curved header ─────────────────────────────────────────────────────────────
@Composable
private fun AuthHeader(isSignUp: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Periwinkle, GIDSurface),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Logo mark – circle with initial letter
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(GIDBackground),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = "G",
                    style = MaterialTheme.typography.displayMedium.copy(
                        color      = GIDCyan,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text  = if (isSignUp) "Create your GID account" else "Sign in to GID",
                style = MaterialTheme.typography.titleLarge.copy(
                    color      = GIDText,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

// ── Form ──────────────────────────────────────────────────────────────────────
@Composable
private fun AuthForm(
    ui       : AuthUiState,
    onEmail  : (String) -> Unit,
    onPass   : (String) -> Unit,
    onConfirm: (String) -> Unit,
    onSubmit : () -> Unit,
    onForgot : () -> Unit,
    onToggle : () -> Unit,
    onGoogle : () -> Unit,
) {
    val focusMgr    = LocalFocusManager.current
    var showPass    by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    val isSignUp    = ui.mode == AuthMode.SIGN_UP

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        // Email field
        GIDTextField(
            value         = ui.email,
            onValueChange = onEmail,
            label         = "Email",
            error         = ui.emailError,
            keyboardOptions = KeyboardOptions(
                keyboardType    = KeyboardType.Email,
                imeAction       = ImeAction.Next,
                capitalization  = KeyboardCapitalization.None,
            ),
            keyboardActions = KeyboardActions(onNext = { focusMgr.moveFocus(FocusDirection.Down) }),
            leadingIcon     = { Icon(Icons.Rounded.Email, contentDescription = null, tint = GIDMutedText) },
        )

        // Password field
        GIDTextField(
            value         = ui.password,
            onValueChange = onPass,
            label         = "Password",
            error         = ui.passwordError,
            visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction    = if (isSignUp) ImeAction.Next else ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusMgr.moveFocus(FocusDirection.Down) },
                onDone = { focusMgr.clearFocus(); onSubmit() },
            ),
            leadingIcon  = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = GIDMutedText) },
            trailingIcon = {
                IconButton(onClick = { showPass = !showPass }) {
                    Icon(
                        imageVector        = if (showPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (showPass) "Hide password" else "Show password",
                        tint               = GIDMutedText,
                    )
                }
            },
        )

        // Confirm password (sign-up only)
        AnimatedVisibility(visible = isSignUp) {
            GIDTextField(
                value         = ui.confirmPassword,
                onValueChange = onConfirm,
                label         = "Confirm password",
                error         = ui.confirmError,
                visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction    = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { focusMgr.clearFocus(); onSubmit() }),
                leadingIcon  = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = GIDMutedText) },
                trailingIcon = {
                    IconButton(onClick = { showConfirm = !showConfirm }) {
                        Icon(
                            imageVector        = if (showConfirm) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (showConfirm) "Hide" else "Show",
                            tint               = GIDMutedText,
                        )
                    }
                },
            )
        }

        // Privacy notice (sign-up only)
        AnimatedVisibility(visible = isSignUp) {
            Text(
                text  = "GID stores your name, date of birth, email, phone and institution in the cloud. " +
                        "All productivity data (tasks, sleep, focus) stays on your device only.",
                style = MaterialTheme.typography.bodySmall,
                color = GIDMutedText,
            )
        }

        // Inline error banner
        AnimatedVisibility(visible = ui.errorMessage != null) {
            ui.errorMessage?.let { err ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = Error.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = Error)
                        Text(err, style = MaterialTheme.typography.bodySmall, color = Error)
                    }
                }
            }
        }

        // ── Primary button ────────────────────────────────────────────────────
        Button(
            onClick  = { focusMgr.clearFocus(); onSubmit() },
            enabled  = !ui.loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape    = MaterialTheme.shapes.extraLarge,
            colors   = ButtonDefaults.buttonColors(
                containerColor = GIDCyan,
                contentColor   = GIDBackground,
            ),
        ) {
            Text(
                text  = if (isSignUp) "Sign Up" else "Sign In",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            )
        }

        // ── Forgot password (sign-in only) ────────────────────────────────────
        AnimatedVisibility(visible = !isSignUp) {
            TextButton(
                onClick  = onForgot,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text  = "Forgot password?",
                    color = GIDMutedText,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        // ── Divider ───────────────────────────────────────────────────────────
        Row(
            verticalAlignment   = Alignment.CenterVertically,
            modifier            = Modifier.fillMaxWidth(),
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = GIDSurfaceHigh)
            Text(
                text     = "  or  ",
                color    = GIDMutedText,
                style    = MaterialTheme.typography.bodySmall,
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = GIDSurfaceHigh)
        }

        // ── Google button ─────────────────────────────────────────────────────
        OutlinedButton(
            onClick  = onGoogle,
            enabled  = !ui.loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape    = MaterialTheme.shapes.extraLarge,
            border   = BorderStroke(1.dp, GIDSurfaceHigh),
            colors   = OutlinedButtonDefaults.outlinedButtonColors(contentColor = GIDText),
        ) {
            Icon(
                imageVector        = Icons.Rounded.AccountCircle,
                contentDescription = "Google",
                tint               = GIDCyan,
                modifier           = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text  = "Continue with Google",
                style = MaterialTheme.typography.labelLarge,
            )
        }

        // ── Toggle sign-in / sign-up ──────────────────────────────────────────
        TextButton(
            onClick  = onToggle,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = buildAnnotatedString {
                    append(if (isSignUp) "Already have an account? " else "Don't have an account? ")
                    withStyle(SpanStyle(color = GIDCyan, fontWeight = FontWeight.Bold)) {
                        append(if (isSignUp) "Sign in" else "Sign up")
                    }
                },
                style     = MaterialTheme.typography.bodyMedium,
                color     = GIDMutedText,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── Reusable text field ───────────────────────────────────────────────────────
@Composable
private fun GIDTextField(
    value                : String,
    onValueChange        : (String) -> Unit,
    label                : String,
    error                : String?              = null,
    visualTransformation : VisualTransformation = VisualTransformation.None,
    keyboardOptions      : KeyboardOptions      = KeyboardOptions.Default,
    keyboardActions      : KeyboardActions      = KeyboardActions.Default,
    leadingIcon          : (@Composable () -> Unit)? = null,
    trailingIcon         : (@Composable () -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value                = value,
            onValueChange        = onValueChange,
            label                = { Text(label) },
            singleLine           = true,
            isError              = error != null,
            visualTransformation = visualTransformation,
            keyboardOptions      = keyboardOptions,
            keyboardActions      = keyboardActions,
            leadingIcon          = leadingIcon,
            trailingIcon         = trailingIcon,
            modifier             = Modifier.fillMaxWidth(),
            colors               = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = GIDCyan,
                unfocusedBorderColor = GIDSurfaceHigh,
                errorBorderColor     = Error,
                focusedLabelColor    = GIDCyan,
                unfocusedLabelColor  = GIDMutedText,
                cursorColor          = GIDCyan,
                focusedTextColor     = GIDText,
                unfocusedTextColor   = GIDText,
            ),
            shape = MaterialTheme.shapes.medium,
        )
        if (error != null) {
            Text(
                text  = error,
                color = Error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

// ── Google Sign-In via Credential Manager ─────────────────────────────────────
private suspend fun launchGoogleSignIn(
    context  : android.content.Context,
    onToken  : (String) -> Unit,
) {
    try {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)  // show all accounts
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        val result     = CredentialManager.create(context).getCredential(context, request)
        val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
        onToken(credential.idToken)
    } catch (_: GetCredentialException) {
        // User cancelled or no Google account configured – silently ignore
    }
}
