package io.kusius.letterbox.platform.gestures.converters

import io.kusius.letterbox.ui.common.swipeablecard.ScrollGesture

fun interface GestureConverter {
    operator fun invoke(vararg args: Any): ScrollGesture
}
