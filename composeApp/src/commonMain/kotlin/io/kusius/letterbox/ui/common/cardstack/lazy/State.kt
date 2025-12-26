package io.kusius.letterbox.ui.common.cardstack.lazy

import androidx.compose.foundation.lazy.layout.LazyLayoutPinnedItemList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy

class LazyCardStackState(
    initialIndex: Int = 0,
    val visibleCards: Int = 2,
    val stackDepth: Int = 4,
) {
    var index by mutableIntStateOf(initialIndex)
        private set

    var nearestRange: IntRange by mutableStateOf(
        initialIndex..<initialIndex + stackDepth,
        structuralEqualityPolicy(),
    )
        private set

    private var lastDataSize: Int = 0
    private var lastFirstKey: Any? = null

    fun moveNext() {
        index++
        nearestRange = index..<index + stackDepth
    }

    fun updateDataSize(
        newSize: Int,
        firstKey: Any? = null,
    ) {
        if (newSize != lastDataSize || firstKey != lastFirstKey) {
            index = 0
            lastDataSize = newSize
            lastFirstKey = firstKey
            val maxIndex = minOf(stackDepth - 1, newSize - 1)
            nearestRange = 0..maxIndex
        }
    }

    internal val pinnedItems = LazyLayoutPinnedItemList()
}
