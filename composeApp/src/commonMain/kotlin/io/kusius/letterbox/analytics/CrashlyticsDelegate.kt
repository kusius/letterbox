package io.kusius.letterbox.analytics

internal interface CrashlyticsDelegate {
    fun recordException(error: Throwable)

    fun log(message: String)

    fun setCustomKey(
        key: String,
        value: String,
    )
}

internal expect fun getPlatformCrashlyticsDelegate(): CrashlyticsDelegate
