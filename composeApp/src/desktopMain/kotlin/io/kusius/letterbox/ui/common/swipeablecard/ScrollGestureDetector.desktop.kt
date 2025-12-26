package io.kusius.letterbox.ui.common.swipeablecard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.kusius.letterbox.platform.gestures.ScrollGestureDispatcher
import kotlinx.coroutines.launch

/**
 * Desktop implementation of scroll gesture detection.
 * Uses native macOS NSEvent scroll phase detection for perfect UX.
 */
@Composable
actual fun Modifier.detectScrollGestures(
    key: Any?,
    onGesture: (ScrollGesture) -> Unit,
): Modifier {
    val listenerKey = key ?: this
    val scope = rememberCoroutineScope()
    val dispatcher = ScrollGestureDispatcher.getInstance()

    DisposableEffect(key) {
        dispatcher.register(listenerKey) { gesture ->
            scope.launch {
                onGesture(gesture)
            }
        }

        onDispose {
            dispatcher.unregister(listenerKey)
        }
    }

    return this
}
