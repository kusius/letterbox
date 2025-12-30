package io.kusius.letterbox.ui.common.swipeablecard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import io.github.aakira.napier.Napier
import io.kusius.letterbox.ui.theme.AppTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt

@Composable
fun <T> SwipeableCard(
    item: T,
    key: Any?,
    draggable: Boolean = true,
    scrollable: Boolean = true,
    headerDraggable: Boolean = draggable,
    onSwiped: (T, CardSwipeDirection) -> Unit,
    onAboutToSwipe: (T, CardSwipeDirection?) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable (T) -> Unit = {},
    content: @Composable (T) -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier.background(Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        val cardWidth = constraints.maxWidth.toFloat()
        val cardHeight = constraints.maxHeight.toFloat()
        val windowInfo = LocalWindowInfo.current.containerSize
        val scope = rememberCoroutineScope()

        val swipeThresholdOffset =
            Offset(
                x = cardWidth / 2f,
                y = if (draggable) cardHeight / 2f else cardHeight / 100f,
            )

        val offset =
            remember(key) {
                Animatable(Offset(0f, 0f), Offset.VectorConverter)
            }

        suspend fun onSwipedInternal(
            item: T,
            direction: CardSwipeDirection,
        ) {
            onSwiped(item, direction)
            when (direction) {
                CardSwipeDirection.Right -> {
                    offset.animateTo(
                        targetValue = Offset(cardWidth, offset.value.y),
                        animationSpec = tween(300),
                    )
                    offset.snapTo(Offset.Zero)
                }

                CardSwipeDirection.Left -> {
                    offset.animateTo(
                        targetValue = Offset(-cardWidth, offset.value.y),
                        animationSpec = tween(300),
                    )
                    offset.snapTo(Offset.Zero)
                }

                CardSwipeDirection.Down -> {
                    offset.animateTo(Offset.Zero, tween(300))
                }
            }
        }

        val nestedScrollConnection =
            remember {
                object : NestedScrollConnection {
                    override suspend fun onPostFling(
                        consumed: Velocity,
                        available: Velocity,
                    ): Velocity {
                        if (offset.value.y < swipeThresholdOffset.y) {
                            // Resets the card offset when scroll gesture finishes without
                            // reaching the threshold
                            scope.launch {
                                offset.animateTo(Offset.Zero)
                            }
                        }
                        return super.onPostFling(consumed, available)
                    }

                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource,
                    ): Offset {
                        Napier.d("pre available: $available")
                        return Offset.Zero
                    }

                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource,
                    ): Offset {
                        // Called after the child has consumed what it can
                        // 'available' is the unconsumed scroll delta
                        var consumedOffset = Offset.Zero
//                        Napier.d("post available: $available, consumed: $consumed")

                        if (available.y != 0f) {
                            // Child couldn't consume the scroll (reached boundary)
                            val unconsumedScroll = available.y

                            if (unconsumedScroll > 0) {
                                val newOffset = offset.value + available

                                scope.launch {
                                    offset.snapTo(newOffset)
                                    val direction = getSwipeDirection(offset.value, swipeThresholdOffset)
                                    if (direction != null) {
                                        consumedOffset += available
                                        onSwipedInternal(item, direction)
                                    }
                                }
//                                Napier.d("Reached top - scrolling up further")
                            } else {
                                Napier.d("Reached bottom - scrolling down further")
                            }
                        }

                        // Return Offset.Zero if you don't want to consume it in parent
                        // Or return 'available' to consume it
                        return consumedOffset
                    }
                }
            }

        val scrollableSwipeModifier =
            if (scrollable) {
                Modifier
            } else {
                Modifier
                    .scrollableSwipe(
                        swipeThresholdOffset = swipeThresholdOffset,
                        key = key,
                        onOffsetChange = { newOffset ->
                            Napier.d("Offset change: $newOffset")
                            offset.snapTo(newOffset / 200f)
                        },
                        onAboutToSwipe = { direction ->
                            onAboutToSwipe(item, direction)
                        },
                        onSwiped = { direction ->
                            onSwipedInternal(direction = direction, item = item)
                        },
                    )
            }

        val dragModifier =
            Modifier
                .swipeable(
                    key = key,
                    onRest = {
                        offset.animateTo(Offset.Zero)
                    },
                    swipeThresholdOffset = swipeThresholdOffset,
                    onOffsetChange = { newOffset -> offset.snapTo(newOffset) },
                    onAboutToSwipe = { direction -> onAboutToSwipe(item, direction) },
                    onSwiped = { direction ->
                        onSwipedInternal(direction = direction, item = item)
                    },
                )

        ElevatedCard(
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(16.dp),
            modifier =
                Modifier
                    .offset { IntOffset(offset.value.x.roundToInt(), offset.value.y.roundToInt()) }
                    .then(if (draggable) dragModifier else Modifier)
                    .then(scrollableSwipeModifier),
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier =
                        Modifier
                            .wrapContentSize()
                            .then(if (headerDraggable) dragModifier else Modifier)
                            .then(scrollableSwipeModifier),
                ) {
                    header(item)
                }

                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .nestedScroll(nestedScrollConnection),
                ) {
                    content(item)
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewSwipeableCard() {
    AppTheme {
        SwipeableCard(
            item = "Hello",
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp),
            onSwiped = { item, direction ->
                when (direction) {
                    CardSwipeDirection.Left -> {
                        println("Archive ✉️")
                    }

                    CardSwipeDirection.Right -> {
                        println("Read ✅")
                    }

                    CardSwipeDirection.Down -> {
                        println("Dismiss 🚫")
                    }
                }
            },
            onAboutToSwipe = { _, _ -> },
            key = { 0 },
            header = { item ->
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().background(AppTheme.colorScheme().secondaryContainer),
                ) {
                    Text("$item header")
                }
            },
        ) { item ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier.fillMaxSize(),
            ) {
                Text(item)
            }
        }
    }
}
