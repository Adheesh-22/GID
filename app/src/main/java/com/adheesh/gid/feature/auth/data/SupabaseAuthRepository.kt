package com.adheesh.gid.feature.auth.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.user.UserInfo
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.UnknownHostException

// ── SupabaseAuthRepository ────────────────────────────────────────────────────
// All network calls run on Dispatchers.IO.
// Exceptions are caught and mapped to typed AuthErrorKind so the UI never
// needs to inspect raw exception messages.
class SupabaseAuthRepository(
    private val client: SupabaseClient,
) : AuthRepository {

    override suspend fun currentUser(): UserInfo? = withContext(Dispatchers.IO) {
        runCatching { client.auth.currentUserOrNull() }.getOrNull()
    }

    override suspend fun hasSession(): Boolean = withContext(Dispatchers.IO) {
        // currentSessionOrNull() returns the locally-cached session even offline.
        runCatching { client.auth.currentSessionOrNull() != null }.getOrDefault(false)
    }

    override suspend fun signIn(email: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            runCatching {
                client.auth.signInWith(Email) {
                    this.email    = email
                    this.password = password
                }
                AuthResult.Success(client.auth.currentUserOrNull()!!)
            }.getOrElse { e -> mapError(e) }
        }

    override suspend fun signUp(email: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            runCatching {
                client.auth.signUpWith(Email) {
                    this.email    = email
                    this.password = password
                }
                // After sign-up with email confirmation enabled Supabase returns
                // a user but no session. We still treat it as success so the UI
                // can show the "check your email" message.
                val user = client.auth.currentUserOrNull()
                    ?: return@runCatching AuthResult.Error(AuthErrorKind.EMAIL_NOT_CONFIRMED,
                        "Please confirm your email before signing in.")
                AuthResult.Success(user)
            }.getOrElse { e -> mapError(e) }
        }

    override suspend fun forgotPassword(email: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                client.auth.resetPasswordForEmail(email)
                true
            }.getOrDefault(false)
        }

    override suspend fun signOut() = withContext(Dispatchers.IO) {
        runCatching { client.auth.signOut() }
        Unit
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): AuthResult =
        withContext(Dispatchers.IO) {
            runCatching {
                client.auth.signInWith(IDToken) {
                    this.idToken  = idToken
                    this.provider = Google
                }
                AuthResult.Success(client.auth.currentUserOrNull()!!)
            }.getOrElse { e -> mapError(e) }
        }

    // ── Error mapping ─────────────────────────────────────────────────────────
    private fun mapError(e: Throwable): AuthResult.Error {
        val msg = e.message?.lowercase() ?: ""
        val kind = when {
            e is UnknownHostException
            || e is IOException && "failed to connect" in msg
            || e is HttpRequestTimeoutException -> AuthErrorKind.NO_INTERNET

            e is RestException && ("invalid login credentials" in msg
                    || "invalid_credentials" in msg
                    || "wrong password" in msg
                    || "email not confirmed" !in msg && "invalid" in msg)
                                    -> AuthErrorKind.WRONG_PASSWORD

            e is RestException && ("email not confirmed" in msg
                    || "email_not_confirmed" in msg) -> AuthErrorKind.EMAIL_NOT_CONFIRMED

            e is RestException && ("user already registered" in msg
                    || "already been registered" in msg
                    || "email address is already" in msg) -> AuthErrorKind.ACCOUNT_EXISTS

            else -> AuthErrorKind.UNKNOWN
        }
        return AuthResult.Error(kind, e.message ?: "")
    }
}
