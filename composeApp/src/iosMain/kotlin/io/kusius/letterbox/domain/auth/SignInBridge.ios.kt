package io.kusius.letterbox.domain.auth

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSDictionary
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.darwin.NSObjectProtocol
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.native.concurrent.ThreadLocal

private const val KMM_GOOGLE_SIGNIN_NOTIFICATION = "KMMGoogleSignInStart"
private const val KMM_GOOGLE_SIGNIN_REVOKE_NOTIFICATION = "KMMGoogleSignInRevoke"
private const val KMM_GOOGLE_SIGNIN_COMPLETED_NOTIFICATION = "KMMGoogleSignInCompleted"

class Tokens(
    val authToken: String?,
    val refreshToken: String?,
    val serverAuthCode: String?,
)

@ThreadLocal
private var pendingContinuation: CancellableContinuation<Tokens?>? = null

suspend fun requestGoogleSignIn(): Tokens? =
    suspendCancellableCoroutine { cont ->
        // Only a single active sign-in flow is supported at a time for simplicity
        if (pendingContinuation != null) {
            cont.resumeWithException(IllegalStateException("A sign-in flow is already in progress"))
            return@suspendCancellableCoroutine
        }

        pendingContinuation = cont

        // Register an observer that will be called when Swift posts the completion notification
        var observer: NSObjectProtocol? = null
        observer =
            NSNotificationCenter.defaultCenter.addObserverForName(
                KMM_GOOGLE_SIGNIN_COMPLETED_NOTIFICATION,
                null,
                null,
            ) { notification: NSNotification? ->
                val userInfo = notification?.userInfo as? NSDictionary
                val token = userInfo?.objectForKey("token") as? String
                val refreshToken = userInfo?.objectForKey("refreshToken") as? String
                val serverAuthCode = userInfo?.objectForKey("serverAuthCode") as? String
                val error = userInfo?.objectForKey("error") as? String

                Napier.d("Sign-in completion notification received, object is null=${userInfo == null}")
                // Remove observer
                if (observer != null) {
                    NSNotificationCenter.defaultCenter.removeObserver(observer!!)
                }

                pendingContinuation = null

                val tokens =
                    Tokens(
                        authToken = token,
                        refreshToken = refreshToken,
                        serverAuthCode = serverAuthCode,
                    )

                if (tokens.authToken != null || tokens.serverAuthCode != null) {
                    cont.resume(tokens)
                } else {
                    cont.resumeWithException(Exception("Sign-in failed or cancelled. Error $error"))
                }
            }

        // Post a notification that Swift side observes to start the Google Sign-In flow
        Napier.d("Scheduling sign-in start notification on main queue: $KMM_GOOGLE_SIGNIN_NOTIFICATION")
        NSOperationQueue.mainQueue.addOperationWithBlock {
            Napier.d("Posting sign-in start notification now on main thread")
            NSNotificationCenter.defaultCenter.postNotificationName(KMM_GOOGLE_SIGNIN_NOTIFICATION, null)
        }

        cont.invokeOnCancellation {
            // Clear continuation and remove observer if coroutine is cancelled
            if (pendingContinuation === cont) {
                pendingContinuation = null
                NSNotificationCenter.defaultCenter.removeObserver(observer)
            }
        }
    }

// Trigger native revoke/sign-out on Swift side by posting a notification
fun requestGoogleSignOut() {
    Napier.d("Scheduling sign-out notification on main queue: $KMM_GOOGLE_SIGNIN_REVOKE_NOTIFICATION")
    NSOperationQueue.mainQueue.addOperationWithBlock {
        Napier.d("Posting sign-out notification now on main thread")
        NSNotificationCenter.defaultCenter.postNotificationName(KMM_GOOGLE_SIGNIN_REVOKE_NOTIFICATION, null)
    }
}
