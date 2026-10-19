package com.adheesh.gid.feature.auth.data

import io.github.jan.supabase.auth.user.UserInfo

/** Sealed result type for all auth operations. */
sealed interface AuthResult {
    data class Success(val user: UserInfo) : AuthResult
    data class Error(val kind: AuthErrorKind, val raw: String = "") : AuthResult
}

enum class AuthErrorKind {
    WRONG_PASSWORD,
    EMAIL_NOT_CONFIRMED,
    ACCOUNT_EXISTS,
    NO_INTERNET,
    UNKNOWN,
}

/** All auth operations the app needs. Implemented by [SupabaseAuthRepository]. */
interface AuthRepository {

    /** Current signed-in user, or null if no session. */
    suspend fun currentUser(): UserInfo?

    /** True if there is a valid (possibly offline-cached) session. */
    suspend fun hasSession(): Boolean

    /** Email + password sign-in. */
    suspend fun signIn(email: String, password: String): AuthResult

    /** Email + password sign-up. */
    suspend fun signUp(email: String, password: String): AuthResult

    /** Send a Supabase password-reset email. Returns true on success. */
    suspend fun forgotPassword(email: String): Boolean

    /** Sign out and clear the local session. */
    suspend fun signOut()

    /**
     * Sign in with a Google ID token obtained from Credential Manager.
     * Used in Step D (Google Sign-In).
     */
    suspend fun signInWithGoogleIdToken(idToken: String): AuthResult
}
