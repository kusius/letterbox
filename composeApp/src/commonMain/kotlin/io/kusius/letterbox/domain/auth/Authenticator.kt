package io.kusius.letterbox.domain.auth

internal object Scopes {
    val values: List<String> =
        listOf(
            "https://www.googleapis.com/auth/gmail.readonly",
            "https://www.googleapis.com/auth/gmail.modify",
        )

    const val VERSION = 2
}

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
