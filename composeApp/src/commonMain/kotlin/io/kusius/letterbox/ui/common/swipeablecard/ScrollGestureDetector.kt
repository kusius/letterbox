package io.kusius.letterbox.ui.common.swipeablecard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

sealed interface ScrollGesture {
    object None : ScrollGesture

    object Started : ScrollGesture

    object Ended : ScrollGesture

    class Changed(
        val offset: Offset,
    ) : ScrollGesture
}

/**
 * Platform-specific scroll gesture detection.
 * On desktop/macOS, this uses native scroll phase detection for precise gesture tracking.
 */

@Composable
expect fun Modifier.detectScrollGestures(
    key: Any?,
    onGesture: (ScrollGesture) -> Unit,
): Modifier
