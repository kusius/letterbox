package io.kusius.letterbox.ui.common.swipeablecard

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

@Composable
internal fun Modifier.swipeable(
    key: Any?,
    swipeThresholdOffset: Offset,
    onRest: suspend () -> Unit = {},
    onOffsetChange: suspend (Offset) -> Unit = {},
    onSwiped: suspend (CardSwipeDirection) -> Unit = {},
    onAboutToSwipe: suspend (CardSwipeDirection?) -> Unit = {},
): Modifier {
    val scope = rememberCoroutineScope()
    val aboutToSwipeSet = remember { mutableStateSetOf<CardSwipeDirection?>() }
    var offset =
        remember(key) {
            Offset.Zero
        }

    return this then
        pointerInput(key) {
            detectDragGestures(
                onDragEnd = {
                    scope.launch {
                        when (val direction = getSwipeDirection(offset, swipeThresholdOffset)) {
                            null -> {
                                offset = Offset.Zero
                                onRest()
                            }

                            else -> {
                                onSwiped(direction)
                                aboutToSwipeSet.clear()
                                offset = Offset.Zero
                            }
                        }
                    }
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    scope.launch {
                        offset += dragAmount
                        onOffsetChange(offset)

                        val direction = getSwipeDirection(offset, swipeThresholdOffset)

                        if (!aboutToSwipeSet.contains(direction)) {
                            aboutToSwipeSet.clear()
                            aboutToSwipeSet.add(direction)
                            onAboutToSwipe(direction)
                        }
                    }
                },
            )
        }
}

@Composable
fun Modifier.scrollableSwipe(
    swipeThresholdOffset: Offset,
    key: Any?,
    onOffsetChange: suspend (Offset) -> Unit = {},
    onAboutToSwipe: suspend (CardSwipeDirection?) -> Unit = {},
    onSwiped: suspend (CardSwipeDirection) -> Unit = {},
): Modifier {
    val scope = rememberCoroutineScope()
    val sensitivity = 100
    var offset = remember { Offset.Zero }
    val aboutToSwipeSet = remember { mutableStateSetOf<CardSwipeDirection?>() }

    return this
        .detectScrollGestures(
            key = key,
            onGesture = { gesture ->
                when (gesture) {
                    is ScrollGesture.Changed -> {
                        val scrollDelta = gesture.offset
                        if (scrollDelta.x != 0f) {
                            scope.launch {
                                val dragAmount = Offset(scrollDelta.x * sensitivity, 0f)
                                offset += dragAmount
                                onOffsetChange(offset)

                                val direction = getSwipeDirection(offset, swipeThresholdOffset)
                                if (!aboutToSwipeSet.contains(direction)) {
                                    aboutToSwipeSet.clear()
                                    aboutToSwipeSet.add(direction)
                                    direction?.let { onAboutToSwipe(it) }
                                }
                            }
                        }
                    }

                    is ScrollGesture.Ended -> {
                        scope.launch {
                            val direction = getSwipeDirection(offset, swipeThresholdOffset)
                            direction?.let {
                                onSwiped(it)
                                aboutToSwipeSet.clear()
                                offset = Offset.Zero
                            }
                        }
                    }

                    else -> {}
                }
            },
        )
}

internal fun getSwipeDirection(
    offset: Offset,
    threshold: Offset,
): CardSwipeDirection? {
    val direction =
        when {
            offset.x > threshold.x -> {
                CardSwipeDirection.Right
            }

            offset.x < -threshold.x -> {
                CardSwipeDirection.Left
            }

            offset.y > threshold.y -> {
                CardSwipeDirection.Down
            }

            else -> {
                null
            }
        }

    return direction
}
