package io.kusius.letterbox.ui.common.cardstack.lazy

import androidx.compose.foundation.lazy.LazyScopeMarker
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@Stable
@LazyScopeMarker
interface LazyCardStackItemScope

class LazyCardStackItemScopeImpl : LazyCardStackItemScope

@LazyScopeMarker
interface LazyCardStackScope {
    fun items(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        placeholder: (@Composable LazyCardStackItemScope.(index: Int) -> Unit)? = null,
        itemContent: @Composable LazyCardStackItemScope.(index: Int) -> Unit,
    ) {
        error("The method is not implemented")
    }
}

inline fun <T> LazyCardStackScope.items(
    items: List<T>,
    noinline key: ((item: T) -> Any)? = null,
    noinline placeholder: (@Composable LazyCardStackItemScope.(item: T) -> Unit)? = null,
    crossinline itemContent: @Composable LazyCardStackItemScope.(item: T) -> Unit,
) = items(
    count = items.size,
    key = if (key != null) { index: Int -> key(items[index]) } else null,
    placeholder =
        if (placeholder != null) {
            { index: Int -> placeholder(items[index]) }
        } else {
            null
        },
) {
    itemContent(items[it])
}

inline fun <T> LazyCardStackScope.itemsIndexed(
    items: List<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    noinline placeholder: (@Composable LazyCardStackItemScope.(index: Int, item: T) -> Unit)? = null,
    crossinline itemContent: @Composable LazyCardStackItemScope.(index: Int, item: T) -> Unit,
) = items(
    count = items.size,
    key = if (key != null) { index: Int -> key(index, items[index]) } else null,
    placeholder =
        if (placeholder != null) {
            { index: Int -> placeholder(index, items[index]) }
        } else {
            null
        },
) {
    itemContent(it, items[it])
}

@Composable
fun rememberLazyCardStackState(
    initialIndex: Int = 0,
    visibleCards: Int = 2,
    stackDepth: Int = 4,
): LazyCardStackState =
    remember {
        LazyCardStackState(initialIndex, visibleCards, stackDepth)
    }

@Composable
fun rememberLazyCardStackItemProviderLambda(
    state: LazyCardStackState,
    content: LazyCardStackScope.() -> Unit,
): () -> LazyLayoutItemProvider {
    val latestContent = rememberUpdatedState(content)
    return remember(state) {
        val scope = LazyCardStackItemScopeImpl()
        val intervalContentState =
            derivedStateOf(referentialEqualityPolicy()) {
                LazyCardStackIntervalContent(latestContent.value)
            }
        val itemProviderState =
            derivedStateOf(referentialEqualityPolicy()) {
                val intervalContent = intervalContentState.value

                // Get the first item's key to detect content changes
                val firstKey =
                    if (intervalContent.itemCount > 0) {
                        val interval = intervalContent.intervals[0]
                        interval.value.key?.invoke(0)
                    } else {
                        null
                    }

                // Automatically update data size and first key to reset index when data changes
                state.updateDataSize(intervalContent.itemCount, firstKey)

                val map = LazyCardStackKeyIndexMap(state.nearestRange, intervalContent)

                LazyCardStackItemProviderImpl(
                    state = state,
                    intervalContent = intervalContent,
                    itemScope = scope,
                    keyIndexMap = map,
                )
            }

        itemProviderState::value
    }
}
