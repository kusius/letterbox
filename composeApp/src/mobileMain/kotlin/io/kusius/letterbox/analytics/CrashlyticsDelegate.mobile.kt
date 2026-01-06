package io.kusius.letterbox.analytics

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.crashlytics.crashlytics

internal actual fun getPlatformCrashlyticsDelegate(): CrashlyticsDelegate =
    object : CrashlyticsDelegate {
        private val crashlytics = Firebase.crashlytics

        override fun recordException(error: Throwable) {
            crashlytics.recordException(error)
        }

        override fun log(message: String) {
            crashlytics.log(message)
        }

        override fun setCustomKey(
            key: String,
            value: String,
        ) {
            crashlytics.setCustomKey(key, value)
        }
    }
