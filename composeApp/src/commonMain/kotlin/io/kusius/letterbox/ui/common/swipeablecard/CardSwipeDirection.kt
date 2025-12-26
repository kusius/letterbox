package io.kusius.letterbox.ui.common.swipeablecard

sealed interface CardSwipeDirection {
    object Left : CardSwipeDirection

    object Right : CardSwipeDirection
    object Down : CardSwipeDirection
}
