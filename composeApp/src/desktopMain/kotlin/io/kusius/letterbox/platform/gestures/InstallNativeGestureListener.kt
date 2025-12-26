package io.kusius.letterbox.platform.gestures

import androidx.compose.ui.awt.ComposeWindow
import com.sun.jna.Pointer
import io.kusius.letterbox.DesktopOS
import io.kusius.letterbox.platform.gestures.converters.MacOSGestureConverter
import io.kusius.letterbox.ui.common.swipeablecard.ScrollGesture

fun ComposeWindow.installNativeGestureListener(callback: (ScrollGesture) -> Unit) {
    val windowPtr = Pointer(windowHandle)

    when (DesktopOS.current()) {
        DesktopOS.WINDOWS -> {
            TODO()
        }

        DesktopOS.MACOS -> {
            val converter = MacOSGestureConverter()

            val nativeCallback =
                MacOSGestureBridge.GestureCallback { phase, sx, sy ->
                    val gesture = converter(phase, sx, sy)
                    callback(gesture)
                }
            MacOSGestureBridge.installWithStrongReference(windowPtr, nativeCallback)
        }

        DesktopOS.LINUX -> {
            TODO()
        }

        DesktopOS.UNKNOWN -> {
            IllegalStateException("No gesture converter for this platform!")
        }
    }
}
