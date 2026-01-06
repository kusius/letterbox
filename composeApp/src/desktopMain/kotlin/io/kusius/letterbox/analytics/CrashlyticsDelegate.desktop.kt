package io.kusius.letterbox.analytics

import io.github.aakira.napier.Napier

internal actual fun getPlatformCrashlyticsDelegate(): CrashlyticsDelegate =
    object : CrashlyticsDelegate {
        override fun recordException(error: Throwable) {
            Napier.w("No error Crashlytics for desktop")
        }

        override fun log(message: String) {
            Napier.w("No error Crashlytics for desktop")
        }

        override fun setCustomKey(
            key: String,
            value: String,
        ) {
            Napier.w("No error Crashlytics for desktop")
        }
    }
