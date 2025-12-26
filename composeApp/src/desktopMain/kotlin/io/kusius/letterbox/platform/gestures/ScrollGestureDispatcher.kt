package io.kusius.letterbox.platform.gestures

import androidx.compose.ui.awt.ComposeWindow
import io.kusius.letterbox.ui.common.swipeablecard.ScrollGesture

fun interface ScrollGestureListener {
    fun invoke(gesture: ScrollGesture)
}

class ScrollGestureDispatcher private constructor(
    window: ComposeWindow,
) {
    private val listeners =
        mutableMapOf<Any, ScrollGestureListener>()

    init {
        window.installNativeGestureListener { gesture ->
            listeners.values.forEach { it.invoke(gesture) }
        }
    }

    fun register(
        key: Any,
        listener: ScrollGestureListener,
    ) {
        listeners[key] = listener
    }

    fun unregister(key: Any) {
        listeners.remove(key)
    }

    companion object {
        private var instance: ScrollGestureDispatcher? = null

        fun initialize(window: ComposeWindow) {
            if (instance == null) {
                instance = ScrollGestureDispatcher(window)
            }
        }

        fun getInstance(): ScrollGestureDispatcher =
            if (instance == null) {
                throw IllegalStateException("You must initialize the dispatcher")
            } else {
                instance!!
            }
    }
}
