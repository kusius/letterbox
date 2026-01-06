package io.kusius.letterbox.analytics

import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CrashlyticsAntiLog :
    Antilog(),
    KoinComponent {
    private val crashlytics: CrashlyticsDelegate by inject()

    override fun performLog(
        priority: LogLevel,
        tag: String?,
        throwable: Throwable?,
        message: String?,
    ) {
        if (priority < LogLevel.ERROR) return

        tag?.let {
            crashlytics.setCustomKey("tag", it)
        }

        message?.let {
            crashlytics.log(it)
        }

        throwable?.let {
            crashlytics.recordException(it)
        }
    }
}
