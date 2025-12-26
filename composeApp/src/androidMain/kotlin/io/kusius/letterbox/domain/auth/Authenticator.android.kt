package io.kusius.letterbox.domain.auth

import android.content.Context
import androidx.activity.ComponentActivity
import com.google.android.gms.common.api.Scope

actual fun getAuthenticator(): Authenticator = AndroidAuthenticator.getInstance()

class AndroidAuthenticator private constructor(
    context: ComponentActivity,
) : Authenticator {
    private val authWithGoogleUseCase = AuthorizeWithGoogleUseCase(context)

    override suspend fun authenticate(forceAuthorization: Boolean): AuthResult {
        val result =
            authWithGoogleUseCase(
                Scope("https://www.googleapis.com/auth/gmail.readonly"),
                Scope("https://www.googleapis.com/auth/gmail.modify"),
                forceAuthorization = forceAuthorization,
            )
        return result
    }

    override suspend fun revoke() {
        authWithGoogleUseCase.clearIdentityState()
    }

    companion object {
        private var instance: AndroidAuthenticator? = null

        fun initialize(context: Context) {
            require(context is ComponentActivity) {
                "Context must be a ComponentActivity"
            }

            if (instance == null) {
                instance = AndroidAuthenticator(context)
            }
        }

        fun getInstance(): AndroidAuthenticator {
            require(instance != null) {
                "Call initialize (context: Context) before fetching the instance"
            }

            return instance!!
        }
    }
}
