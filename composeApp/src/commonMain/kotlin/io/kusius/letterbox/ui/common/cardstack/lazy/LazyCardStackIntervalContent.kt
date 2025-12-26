package io.kusius.letterbox.ui.common.cardstack.lazy

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutIntervalContent
import androidx.compose.foundation.lazy.layout.MutableIntervalList
import androidx.compose.foundation.lazy.layout.getDefaultLazyLayoutKey
import androidx.compose.runtime.Composable

@OptIn(ExperimentalFoundationApi::class)
class LazyCardStackIntervalContent(
    content: LazyCardStackScope.() -> Unit,
) : LazyLayoutIntervalContent<LazyCardStackInterval>(),
    LazyCardStackScope {
    override val intervals: MutableIntervalList<LazyCardStackInterval> = MutableIntervalList()

    init {
        apply(content)
    }

    override fun items(
        count: Int,
        key: ((Int) -> Any)?,
        placeholder: (@Composable LazyCardStackItemScope.(index: Int) -> Unit)?,
        itemContent: @Composable (LazyCardStackItemScope.(index: Int) -> Unit),
    ) {
        intervals.addInterval(
            count,
            LazyCardStackInterval(key = key, type = { null }, placeholder = placeholder, item = itemContent),
        )
    }
}

class LazyCardStackInterval(
    override val key: ((index: Int) -> Any)?,
    override val type: ((index: Int) -> Any?),
    val placeholder: (@Composable LazyCardStackItemScope.(index: Int) -> Unit)?,
    val item: @Composable LazyCardStackItemScope.(index: Int) -> Unit,
) : LazyLayoutIntervalContent.Interval

class LazyCardStackKeyIndexMap(
    nearestRange: IntRange,
    intervalContent: LazyLayoutIntervalContent<*>,
) {
    private val map: Map<Any, Int>
    private val keys: Array<Any?>
    private val keysStartIndex: Int

    init {
        // Traverses the interval [list] in order to create a mapping from the key to the index for
        // all the indexes in the passed [range].
        val list = intervalContent.intervals
        val first = nearestRange.first
        require(first >= 0) { "negative nearestRange.first" }
        val last = minOf(nearestRange.last, list.size - 1)
        if (last < first) {
            map = emptyMap()
            keys = emptyArray()
            keysStartIndex = 0
        } else {
            val size = last - first + 1
            keys = arrayOfNulls<Any?>(size)
            keysStartIndex = first
            map =
                mutableMapOf<Any, Int>().also { map ->
                    list.forEach(fromIndex = first, toIndex = last) {
                        val keyFactory = it.value.key
                        val start = maxOf(first, it.startIndex)
                        val end = minOf(last, it.startIndex + it.size - 1)
                        for (i in start..end) {
                            val key =
                                keyFactory?.invoke(i - it.startIndex) ?: -1
//                                getDefaultLazyLayoutKey(i)
                            map[key] = i
                            keys[i - keysStartIndex] = key
                        }
                    }
                }
        }
    }

    fun getIndex(key: Any): Int = map.getOrElse(key) { -1 }

    fun getKey(index: Int) = keys.getOrElse(index - keysStartIndex) { null }
}
