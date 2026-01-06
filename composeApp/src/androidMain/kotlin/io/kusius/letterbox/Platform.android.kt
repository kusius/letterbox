package io.kusius.letterbox

import android.os.Build
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.mp.KoinPlatform.getKoin

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"

    override fun setupLogging() {
        Napier.base(DebugAntilog())
        Napier.base(getKoin().get())
    }
}

actual fun getPlatform(): Platform = AndroidPlatform()
