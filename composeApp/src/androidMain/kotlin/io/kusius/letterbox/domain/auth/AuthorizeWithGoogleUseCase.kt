package io.kusius.letterbox.domain.auth

import android.app.Activity.RESULT_OK
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import io.github.aakira.napier.Napier
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation

private val log = KotlinLogging.logger {}

class AuthorizeWithGoogleUseCase(
    private val context: ComponentActivity,
) {
    private lateinit var continuation: Continuation<AuthResult>
    private val intentLauncher =
        context.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->

            if (result.resultCode == RESULT_OK) {
                val authCode = getAuthCode(result.data)
                val accessToken = getAccessToken(result.data)

                if (accessToken != null) {
                    continueWithResult(AuthResult.Granted(accessToken, null))
                } else if (authCode != null) {
                    continueWithResult(AuthResult.Login(authCode))
                } else {
                    continueWithError("No authorization code or access token")
                }
            } else {
                continueWithError("Authorization was cancelled or denied.")
            }
        }

    private fun getAccessToken(intent: Intent?): String? {
        return try {
            val authorizationResult =
                Identity
                    .getAuthorizationClient(context)
                    .getAuthorizationResultFromIntent(intent)

            val accessToken = authorizationResult.accessToken
            if (accessToken == null) {
                log.error { "Token was null " }
            }
            return accessToken
        } catch (e: Throwable) {
            log.error { "Encountered error $e" }
            null
        }
    }

    private fun getAuthCode(intent: Intent?): String? =
        try {
            // extract the result
            val authorizationResult =
                Identity
                    .getAuthorizationClient(context)
                    .getAuthorizationResultFromIntent(intent)
            val authorizationCode = authorizationResult.serverAuthCode
            if (authorizationCode == null) {
                log.error { "No authorization code " }
            }

            authorizationCode
        } catch (e: ApiException) {
            Napier.e("Could not get auth result from intent", e)
            null
        }

    suspend operator fun invoke(
        vararg scopes: Scope,
        forceAuthorization: Boolean = false,
    ) = invoke(scopes.asList(), forceAuthorization = forceAuthorization)

    // Returns the authorization code
    suspend operator fun invoke(
        scopes: List<Scope>,
        forceAuthorization: Boolean = false,
    ): AuthResult =
        suspendCancellableCoroutine { cont ->
            continuation = cont

            val authorizationRequest =
                AuthorizationRequest
                    .Builder()
                    .setRequestedScopes(scopes)
                    .build()

            Identity
                .getAuthorizationClient(context)
                .authorize(authorizationRequest)
                .addOnSuccessListener {
                    val pendingIntent = it.pendingIntent
                    if (pendingIntent == null) {
                        // Check if there's a token
                        if (it.accessToken != null) {
                            continueWithResult(AuthResult.Granted(it.accessToken!!,  null))
                        } else {
                            clearIdentityState()
                            continueWithError("intent was null")
                            return@addOnSuccessListener
                        }
                    } else if (it.hasResolution() || forceAuthorization) {
                        // coroutine continued by the intentLauncher
                        intentLauncher.launch(
                            IntentSenderRequest.Builder(pendingIntent.intentSender).build(),
                        )
                    }
                }.addOnFailureListener {
                    log.error { "Auth request failed with $it" }
                    continueWithException(it)
                }
        }

    private fun continueWithResult(authResult: AuthResult) {
        continuation.resumeWith(Result.success(authResult))
    }

    private fun continueWithError(error: String) {
        continueWithResult(AuthResult.Error(Exception(error)))
    }

    private fun continueWithException(exception: Throwable) {
        continueWithResult(AuthResult.Error(exception))
    }

    fun clearIdentityState() {
        Identity.getSignInClient(context).signOut()
    }
}
