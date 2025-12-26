package io.kusius.letterbox.domain.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual fun getAuthenticator(): Authenticator = IosAuthenticator.getInstance()

class IosAuthenticator private constructor() : Authenticator {
    override suspend fun authenticate(forceAuthorization: Boolean): AuthResult =
        withContext(Dispatchers.Main) {
            try {
                val token = requestGoogleSignIn()
                return@withContext if (token != null && token.authToken != null) {
                    AuthResult.Granted(accessToken = token.authToken, token.refreshToken)
                } else if (token != null && token.serverAuthCode != null) {
                    AuthResult.Login(token.serverAuthCode)
                } else {
                    AuthResult.Error(Throwable("No token returned"))
                }
            } catch (e: Throwable) {
                AuthResult.Error(e)
            }
        }

    override suspend fun revoke() {
        // Ask native code to sign out
        requestGoogleSignOut()
    }

    companion object {
        private var instance: IosAuthenticator? = null

        fun getInstance(): IosAuthenticator {
            if (instance == null) {
                instance = IosAuthenticator()
            }

            return instance!!
        }
    }
}
