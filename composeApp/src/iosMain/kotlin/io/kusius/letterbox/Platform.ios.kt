package io.kusius.letterbox

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

    override fun debugBuild() {
        Napier.base(DebugAntilog())
    }
}

actual fun getPlatform(): Platform = IOSPlatform()
