package io.kusius.letterbox

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"

    override fun debugBuild() {}
}

actual fun getPlatform(): Platform = AndroidPlatform()
