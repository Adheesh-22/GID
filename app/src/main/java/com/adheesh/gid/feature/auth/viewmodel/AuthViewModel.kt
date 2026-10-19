package com.adheesh.gid.feature.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adheesh.gid.feature.auth.data.AuthErrorKind
import com.adheesh.gid.feature.auth.data.AuthRepository
import com.adheesh.gid.feature.auth.data.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── UI state ──────────────────────────────────────────────────────────────────
enum class AuthMode { SIGN_IN, SIGN_UP }

data class AuthUiState(
    val mode            : AuthMode = AuthMode.SIGN_IN,
    val email           : String   = "",
    val password        : String   = "",
    val confirmPassword : String   = "",
    val loading         : Boolean  = false,
    val errorMessage    : String?  = null,
    // Inline field-level errors
    val emailError      : String?  = null,
    val passwordError   : String?  = null,
    val confirmError    : String?  = null,
    // One-shot events (consumed by the UI)
    val navigateToHome          : Boolean = false,
    val navigateToSettings      : Boolean = false,
    val showResetEmailSent      : Boolean = false,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────
class AuthViewModel(
    private val repo          : AuthRepository,
    private val isProfileComplete: suspend () -> Boolean,
) : ViewModel() {

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui.asStateFlow()

    // ── Field updates ─────────────────────────────────────────────────────────
    fun setEmail(v: String)           = _ui.update { it.copy(email = v, emailError = null, errorMessage = null) }
    fun setPassword(v: String)        = _ui.update { it.copy(password = v, passwordError = null, errorMessage = null) }
    fun setConfirmPassword(v: String) = _ui.update { it.copy(confirmPassword = v, confirmError = null) }

    fun toggleMode() = _ui.update {
        it.copy(
            mode             = if (it.mode == AuthMode.SIGN_IN) AuthMode.SIGN_UP else AuthMode.SIGN_IN,
            errorMessage     = null,
            emailError       = null,
            passwordError    = null,
            confirmError     = null,
        )
    }

    // ── Primary action ────────────────────────────────────────────────────────
    fun submit() {
        val s = _ui.value
        if (!validate(s)) return

        _ui.update { it.copy(loading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = when (s.mode) {
                AuthMode.SIGN_IN -> repo.signIn(s.email.trim(), s.password)
                AuthMode.SIGN_UP -> repo.signUp(s.email.trim(), s.password)
            }
            handleResult(result)
        }
    }

    // ── Forgot password ───────────────────────────────────────────────────────
    fun forgotPassword() {
        val email = _ui.value.email.trim()
        if (email.isBlank()) {
            _ui.update { it.copy(emailError = "Enter your email first") }
            return
        }
        _ui.update { it.copy(loading = true, errorMessage = null) }
        viewModelScope.launch {
            val sent = repo.forgotPassword(email)
            _ui.update {
                it.copy(
                    loading             = false,
                    showResetEmailSent  = sent,
                    errorMessage        = if (!sent) "Could not send reset email. Check your connection." else null,
                )
            }
        }
    }

    // ── Google Sign-In ────────────────────────────────────────────────────────
    fun signInWithGoogle(idToken: String) {
        _ui.update { it.copy(loading = true, errorMessage = null) }
        viewModelScope.launch {
            handleResult(repo.signInWithGoogleIdToken(idToken))
        }
    }

    // ── Consume one-shot events ───────────────────────────────────────────────
    fun onNavigationConsumed() = _ui.update {
        it.copy(navigateToHome = false, navigateToSettings = false)
    }
    fun onResetEmailSentConsumed() = _ui.update { it.copy(showResetEmailSent = false) }

    // ── Private helpers ───────────────────────────────────────────────────────
    private suspend fun handleResult(result: AuthResult) {
        when (result) {
            is AuthResult.Success -> {
                val complete = isProfileComplete()
                _ui.update {
                    it.copy(
                        loading            = false,
                        navigateToHome     = complete,
                        navigateToSettings = !complete,
                    )
                }
            }
            is AuthResult.Error -> {
                val msg = when (result.kind) {
                    AuthErrorKind.WRONG_PASSWORD       -> "Wrong password. Please try again."
                    AuthErrorKind.EMAIL_NOT_CONFIRMED  -> "Please confirm your email before signing in. Check your inbox."
                    AuthErrorKind.ACCOUNT_EXISTS       -> "An account with this email already exists. Sign in instead."
                    AuthErrorKind.NO_INTERNET          -> "No internet connection. Please check your network."
                    AuthErrorKind.UNKNOWN              -> "Something went wrong. Please try again."
                }
                _ui.update { it.copy(loading = false, errorMessage = msg) }
            }
        }
    }

    private fun validate(s: AuthUiState): Boolean {
        var ok = true
        if (s.email.isBlank() || !s.email.contains("@")) {
            _ui.update { it.copy(emailError = "Enter a valid email address") }
            ok = false
        }
        if (s.password.length < 6) {
            _ui.update { it.copy(passwordError = "Password must be at least 6 characters") }
            ok = false
        }
        if (s.mode == AuthMode.SIGN_UP && s.password != s.confirmPassword) {
            _ui.update { it.copy(confirmError = "Passwords do not match") }
            ok = false
        }
        return ok
    }
}
