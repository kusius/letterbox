package io.kusius.letterbox.domain.auth

sealed interface AuthResult {
    class Granted(
        val accessToken: String,
        val refreshToken: String?,
    ) : AuthResult

    class Login(
        val code: String,
    ) : AuthResult

    class Error(
        val e: Throwable,
    ) : AuthResult
}

interface Authenticator {
    suspend fun authenticate(forceAuthorization: Boolean = false): AuthResult

    suspend fun revoke()
}

expect fun getAuthenticator(): Authenticator
