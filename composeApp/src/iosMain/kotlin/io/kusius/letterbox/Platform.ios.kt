package io.kusius.letterbox

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.kusius.letterbox.analytics.CrashlyticsAntiLog
import io.kusius.letterbox.analytics.CrashlyticsDelegate
import org.koin.mp.KoinPlatform.getKoin
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

    override fun setupLogging() {
        Napier.base(DebugAntilog())
        Napier.base(getKoin().get())
    }
}

actual fun getPlatform(): Platform = IOSPlatform()
