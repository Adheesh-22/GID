package com.adheesh.gid.feature.auth.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adheesh.gid.R
import com.adheesh.gid.feature.auth.data.AuthErrorKind
import com.adheesh.gid.feature.auth.data.AuthRepository
import com.adheesh.gid.feature.auth.data.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode { SIGN_IN, SIGN_UP }

// Messages are string resource ids so all text lives in strings.xml.
data class AuthUiState(
    val mode            : AuthMode = AuthMode.SIGN_IN,
    val email           : String   = "",
    val password        : String   = "",
    val confirmPassword : String   = "",
    val loading         : Boolean  = false,
    @param:StringRes val error         : Int? = null,
    @param:StringRes val info          : Int? = null,
    @param:StringRes val emailError    : Int? = null,
    @param:StringRes val passwordError : Int? = null,
    @param:StringRes val confirmError  : Int? = null,
)

// Navigation after sign-in is not handled here: the nav host watches the
// signed-in account and routes to Settings or Home (SPEC §1).
class AuthViewModel(private val repo: AuthRepository) : ViewModel() {

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui.asStateFlow()

    val isGoogleConfigured: Boolean get() = repo.isGoogleConfigured

    fun setEmail(v: String)           = _ui.update { it.copy(email = v, emailError = null, error = null, info = null) }
    fun setPassword(v: String)        = _ui.update { it.copy(password = v, passwordError = null, error = null) }
    fun setConfirmPassword(v: String) = _ui.update { it.copy(confirmPassword = v, confirmError = null) }

    fun toggleMode() = _ui.update {
        it.copy(
            mode          = if (it.mode == AuthMode.SIGN_IN) AuthMode.SIGN_UP else AuthMode.SIGN_IN,
            error         = null,
            info          = null,
            emailError    = null,
            passwordError = null,
            confirmError  = null,
        )
    }

    fun submit() {
        val s = _ui.value
        if (!validate(s)) return
        _ui.update { it.copy(loading = true, error = null, info = null) }
        viewModelScope.launch {
            val result = when (s.mode) {
                AuthMode.SIGN_IN -> repo.signIn(s.email.trim(), s.password)
                AuthMode.SIGN_UP -> repo.signUp(s.email.trim(), s.password)
            }
            handle(result)
        }
    }

    fun forgotPassword() {
        val email = _ui.value.email.trim()
        if (!email.contains("@")) {
            _ui.update { it.copy(emailError = R.string.auth_error_email_first) }
            return
        }
        _ui.update { it.copy(loading = true, error = null, info = null) }
        viewModelScope.launch {
            when (val result = repo.forgotPassword(email)) {
                null                  -> _ui.update { it.copy(loading = false, info = R.string.auth_reset_sent) }
                is AuthResult.Error   -> _ui.update { it.copy(loading = false, error = messageFor(result.kind)) }
                else                  -> _ui.update { it.copy(loading = false) }
            }
        }
    }

    fun signInWithGoogle(idToken: String, rawNonce: String) {
        _ui.update { it.copy(loading = true, error = null, info = null) }
        viewModelScope.launch { handle(repo.signInWithGoogleIdToken(idToken, rawNonce)) }
    }

    fun showError(kind: AuthErrorKind) = _ui.update { it.copy(loading = false, error = messageFor(kind)) }

    private fun handle(result: AuthResult?) {
        when (result) {
            is AuthResult.Success     -> _ui.update { it.copy(loading = false) }
            AuthResult.ConfirmEmailSent -> _ui.update {
                it.copy(loading = false, mode = AuthMode.SIGN_IN, password = "", confirmPassword = "", info = R.string.auth_confirm_sent)
            }
            is AuthResult.Error       -> _ui.update { it.copy(loading = false, error = messageFor(result.kind)) }
            null                      -> _ui.update { it.copy(loading = false) }
        }
    }

    private fun validate(s: AuthUiState): Boolean {
        val emailError    = if (!s.email.trim().contains("@") || s.email.trim().length < 5) R.string.auth_error_email else null
        val passwordError = if (s.password.length < MIN_PASSWORD) R.string.auth_error_password_short else null
        val confirmError  = if (s.mode == AuthMode.SIGN_UP && s.password != s.confirmPassword) R.string.auth_error_mismatch else null
        _ui.update { it.copy(emailError = emailError, passwordError = passwordError, confirmError = confirmError) }
        return emailError == null && passwordError == null && confirmError == null
    }

    @StringRes
    private fun messageFor(kind: AuthErrorKind): Int = when (kind) {
        AuthErrorKind.WRONG_PASSWORD        -> R.string.auth_error_wrong_password
        AuthErrorKind.EMAIL_NOT_CONFIRMED   -> R.string.auth_error_not_confirmed
        AuthErrorKind.ACCOUNT_EXISTS        -> R.string.auth_error_exists
        AuthErrorKind.WEAK_PASSWORD         -> R.string.auth_error_weak
        AuthErrorKind.NO_INTERNET           -> R.string.auth_error_offline
        AuthErrorKind.RATE_LIMITED          -> R.string.auth_error_rate_limited
        AuthErrorKind.NOT_CONFIGURED        -> R.string.auth_error_not_configured
        AuthErrorKind.GOOGLE_NOT_CONFIGURED -> R.string.auth_error_google_not_configured
        AuthErrorKind.UNKNOWN               -> R.string.auth_error_unknown
    }

    private companion object {
        // Supabase's default minimum password length.
        const val MIN_PASSWORD = 6
    }
}
