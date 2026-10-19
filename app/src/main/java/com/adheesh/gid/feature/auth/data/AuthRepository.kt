package com.adheesh.gid.feature.auth.data

import android.content.Intent
import com.adheesh.gid.core.database.SignedInUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Result of an auth operation. */
sealed interface AuthResult {
    data class Success(val user: SignedInUser) : AuthResult
    /** Sign-up worked but Supabase wants the email confirmed before sign-in. */
    data object ConfirmEmailSent : AuthResult
    data class Error(val kind: AuthErrorKind, val raw: String = "") : AuthResult
}

enum class AuthErrorKind {
    WRONG_PASSWORD,
    EMAIL_NOT_CONFIRMED,
    ACCOUNT_EXISTS,
    WEAK_PASSWORD,
    NO_INTERNET,
    RATE_LIMITED,
    NOT_CONFIGURED,
    GOOGLE_NOT_CONFIGURED,
    UNKNOWN,
}

/** All auth operations the app needs. Implemented by [SupabaseAuthRepository]. */
interface AuthRepository {

    /**
     * The signed-in account cached on the device, or null when signed out.
     * This is what routing uses, so a saved session opens Home even offline.
     */
    val signedInUser: Flow<SignedInUser?>

    /** True after the user opened a password-reset link; the UI asks for a new password. */
    val passwordRecoveryPending: StateFlow<Boolean>

    /** True when Supabase keys were provided in local.properties. */
    val isConfigured: Boolean

    /** True when the Google web client id was provided in local.properties. */
    val isGoogleConfigured: Boolean

    suspend fun signIn(email: String, password: String): AuthResult

    suspend fun signUp(email: String, password: String): AuthResult

    /** Sends the Supabase password-reset email. */
    suspend fun forgotPassword(email: String): AuthResult?

    /** Sets a new password after a reset link. Returns null on success. */
    suspend fun updatePassword(newPassword: String): AuthErrorKind?

    fun dismissPasswordRecovery()

    /** Sign out and clear the local session. */
    suspend fun signOut()

    /** Sign in with a Google ID token obtained from Credential Manager. */
    suspend fun signInWithGoogleIdToken(idToken: String, rawNonce: String?): AuthResult

    /** Handles email-confirmation and password-reset links that open the app. */
    fun handleDeepLink(intent: Intent)
}
