package com.adheesh.gid.feature.auth.data

import android.content.Intent
import com.adheesh.gid.core.database.GIDPreferences
import com.adheesh.gid.core.database.SignedInUser
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.exception.AuthWeakPasswordException
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException

// ── SupabaseAuthRepository ────────────────────────────────────────────────────
// Talks to Supabase Auth and mirrors the signed-in account into DataStore.
// supabase-kt persists its own tokens, but those expire after an hour; the
// DataStore copy is what keeps the app signed in offline (SPEC §1).
// [client] is null when local.properties has no keys: every call then returns
// NOT_CONFIGURED instead of crashing.
class SupabaseAuthRepository(
    private val client          : SupabaseClient?,
    private val preferences     : GIDPreferences,
    private val googleClientId  : String,
    /** Called after every successful sign-in, before routing reads the profile. */
    private val onSignedIn      : suspend (SignedInUser) -> Unit,
    /** App-wide scope, used for work that must outlive the screen (deep links). */
    private val appScope        : CoroutineScope,
) : AuthRepository {

    override val signedInUser: Flow<SignedInUser?> = preferences.signedInUser

    private val _recovery = MutableStateFlow(false)
    override val passwordRecoveryPending: StateFlow<Boolean> = _recovery.asStateFlow()

    override val isConfigured: Boolean = client != null
    override val isGoogleConfigured: Boolean = googleClientId.isNotBlank()

    override suspend fun signIn(email: String, password: String): AuthResult = callOrUnknown { c ->
        c.auth.signInWith(Email) {
            this.email    = email
            this.password = password
        }
        finishSignIn(c.auth.currentUserOrNull())
    }

    override suspend fun signUp(email: String, password: String): AuthResult = callOrUnknown { c ->
        val user = c.auth.signUpWith(Email, redirectUrl = REDIRECT_URL) {
            this.email    = email
            this.password = password
        }
        val session = c.auth.currentSessionOrNull()
        when {
            // Supabase hides existing accounts when confirmation is on: it returns
            // a user with no identities instead of an error.
            user != null && user.identities?.isEmpty() == true ->
                AuthResult.Error(AuthErrorKind.ACCOUNT_EXISTS)
            // "Confirm email" is on: no session until the link is opened.
            session == null -> AuthResult.ConfirmEmailSent
            else            -> finishSignIn(c.auth.currentUserOrNull())
        }
    }

    override suspend fun forgotPassword(email: String): AuthResult? = call { c ->
        c.auth.resetPasswordForEmail(email, redirectUrl = REDIRECT_URL)
        null
    }

    override suspend fun updatePassword(newPassword: String): AuthErrorKind? {
        val result = call { c ->
            c.auth.updateUser { password = newPassword }
            _recovery.value = false
            null
        }
        return (result as? AuthResult.Error)?.kind
    }

    override fun dismissPasswordRecovery() {
        _recovery.value = false
    }

    override suspend fun signOut() = withContext(Dispatchers.IO) {
        // Clear the local copy first so the app leaves Home even when offline.
        preferences.setSignedInUser(null)
        client?.let { c -> runCatching { c.auth.signOut() } }
        Unit
    }

    override suspend fun signInWithGoogleIdToken(idToken: String, rawNonce: String?): AuthResult =
        callOrUnknown { c ->
            c.auth.signInWith(IDToken) {
                this.idToken  = idToken
                this.provider = Google
                this.nonce    = rawNonce
            }
            finishSignIn(c.auth.currentUserOrNull())
        }

    override fun handleDeepLink(intent: Intent) {
        val c = client ?: return
        val data = intent.data?.toString() ?: return
        if (!data.startsWith(REDIRECT_URL)) return
        val isRecovery = "type=recovery" in data
        c.handleDeeplinks(intent) { session ->
            val user = session.user ?: return@handleDeeplinks
            appScope.launch {
                runCatching { finishSignIn(user) }
                if (isRecovery) _recovery.value = true
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun finishSignIn(user: UserInfo?): AuthResult {
        user ?: return AuthResult.Error(AuthErrorKind.UNKNOWN)
        val signedIn = SignedInUser(id = user.id, email = user.email.orEmpty())
        onSignedIn(signedIn)
        preferences.setSignedInUser(signedIn)
        return AuthResult.Success(signedIn)
    }

    private suspend fun callOrUnknown(block: suspend (SupabaseClient) -> AuthResult): AuthResult =
        call(block) ?: AuthResult.Error(AuthErrorKind.UNKNOWN)

    /** Runs [block] on IO with a configured client and maps every failure. */
    private suspend fun <T : AuthResult?> call(block: suspend (SupabaseClient) -> T): AuthResult? =
        withContext(Dispatchers.IO) {
            val c = client ?: return@withContext AuthResult.Error(AuthErrorKind.NOT_CONFIGURED)
            try {
                block(c)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                mapError(e)
            }
        }

    // Supabase returns machine-readable error codes; fall back to the message
    // text for older servers that only send a description.
    private fun mapError(e: Throwable): AuthResult.Error {
        val msg = e.message?.lowercase().orEmpty()
        val kind = when {
            e is HttpRequestException || e is HttpRequestTimeoutException || e is IOException ->
                AuthErrorKind.NO_INTERNET
            e is AuthWeakPasswordException -> AuthErrorKind.WEAK_PASSWORD
            e is AuthRestException -> when (e.errorCode) {
                AuthErrorCode.InvalidCredentials     -> AuthErrorKind.WRONG_PASSWORD
                AuthErrorCode.EmailNotConfirmed      -> AuthErrorKind.EMAIL_NOT_CONFIRMED
                AuthErrorCode.UserAlreadyExists,
                AuthErrorCode.EmailExists            -> AuthErrorKind.ACCOUNT_EXISTS
                AuthErrorCode.WeakPassword           -> AuthErrorKind.WEAK_PASSWORD
                AuthErrorCode.OverRequestRateLimit,
                AuthErrorCode.OverEmailSendRateLimit -> AuthErrorKind.RATE_LIMITED
                else                                 -> fromMessage(msg)
            }
            else -> fromMessage(msg)
        }
        return AuthResult.Error(kind, e.message.orEmpty())
    }

    private fun fromMessage(msg: String): AuthErrorKind = when {
        "invalid login credentials" in msg      -> AuthErrorKind.WRONG_PASSWORD
        "email not confirmed" in msg            -> AuthErrorKind.EMAIL_NOT_CONFIRMED
        "already registered" in msg             -> AuthErrorKind.ACCOUNT_EXISTS
        "unable to resolve host" in msg ||
        "failed to connect" in msg              -> AuthErrorKind.NO_INTERNET
        else                                    -> AuthErrorKind.UNKNOWN
    }

    companion object {
        /** Must match the intent-filter in AndroidManifest.xml and Supabase's Redirect URLs. */
        const val REDIRECT_URL = "com.adheesh.gid://callback"
    }
}
