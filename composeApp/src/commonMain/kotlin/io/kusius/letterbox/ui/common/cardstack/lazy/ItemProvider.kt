package io.kusius.letterbox.ui.common.cardstack.lazy

import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutPinnableItem
import androidx.compose.runtime.Composable

interface LazyCardStackItemProvider : LazyLayoutItemProvider {
    val keyIndexMap: LazyCardStackKeyIndexMap
    val itemScope: LazyCardStackItemScopeImpl
}

class LazyCardStackItemProviderImpl(
    private val state: LazyCardStackState,
    private val intervalContent: LazyCardStackIntervalContent,
    override val itemScope: LazyCardStackItemScopeImpl,
    override val keyIndexMap: LazyCardStackKeyIndexMap,
) : LazyCardStackItemProvider {
    override val itemCount: Int
        get() = intervalContent.itemCount

    @Composable
    override fun Item(
        index: Int,
        key: Any,
    ) {
        LazyLayoutPinnableItem(key, index, state.pinnedItems) {
            intervalContent.withInterval(index) { localIndex, content ->
                if (index - state.index < state.visibleCards || content.placeholder == null) {
                    content.item(itemScope, localIndex)
                } else {
                    content.placeholder.invoke(itemScope, localIndex)
                }
            }
        }
    }

    override fun getKey(index: Int): Any = keyIndexMap.getKey(index) ?: intervalContent.getKey(index)

    override fun getIndex(key: Any): Int = keyIndexMap.getIndex(key)
}
