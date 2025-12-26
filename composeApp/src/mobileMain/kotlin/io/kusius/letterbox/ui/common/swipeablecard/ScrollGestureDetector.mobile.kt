package io.kusius.letterbox.ui.common.swipeablecard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun Modifier.detectScrollGestures(
    key: Any?,
    onGesture: (ScrollGesture) -> Unit,
): Modifier = this // no-op on mobile
