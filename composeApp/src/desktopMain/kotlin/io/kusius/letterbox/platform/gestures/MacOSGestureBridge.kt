package io.kusius.letterbox.platform.gestures

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

interface MacOSGestureBridge : Library {
    fun interface GestureCallback : Callback {
        fun invoke(
            phase: Int,
            deltaX: Double,
            deltaY: Double,
        )
    }

    fun installGestureListener(
        window: Pointer?,
        callback: GestureCallback,
    )

    companion object Companion {
        val INSTANCE: MacOSGestureBridge? by lazy {
            Native
                .load<MacOSGestureBridge?>("GestureBridge", MacOSGestureBridge::class.java)
        }

        private val nativeReferences = mutableListOf<GestureCallback>()

        fun installWithStrongReference(
            window: Pointer?,
            callback: GestureCallback,
        ) {
            nativeReferences.add(callback) // Prevent GC
            INSTANCE?.installGestureListener(window, callback)
        }
    }
}
