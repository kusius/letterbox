package io.kusius.letterbox.ui.common.cardstack.lazy

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasurePolicy
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kusius.letterbox.ui.common.cardstack.lazy.LazyCardStack.SCALE_REDUCTION_FACTOR
import io.kusius.letterbox.ui.common.swipeablecard.SwipeableCard
import io.kusius.letterbox.ui.theme.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

private object LazyCardStack {
    const val X_POSITION = 5
    const val Y_POSITION = 30
    const val SCALE_REDUCTION_FACTOR = 0.03f
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberLazyCardStackMeasurePolicy(
    itemProviderLambda: () -> LazyLayoutItemProvider,
    state: LazyCardStackState,
) = remember(state) {
    LazyLayoutMeasurePolicy { containerConstraints ->
        val itemProvider = itemProviderLambda()
        val itemCount = itemProvider.itemCount
        val last = minOf(state.nearestRange.last, itemCount - 1)
        val visibleIndices = state.index..last

        val indicesWithPlaceables =
            visibleIndices.associateWith { index ->
                compose(index).map {
                    it.measure(containerConstraints)
                }
            }

        val maxHeight = indicesWithPlaceables.values.flatMap { it }.maxOfOrNull { it.height } ?: 0
        layout(width = containerConstraints.maxWidth, height = maxHeight) {
            indicesWithPlaceables.forEach { (index, placeables) ->
                placeables.forEach { placeable ->
                    placeable.placeRelativeWithLayer(
                        x = LazyCardStack.X_POSITION,
                        y = -(LazyCardStack.Y_POSITION * index * 0.5f).toInt(),
                        zIndex = -index.toFloat(),
                    ) {
                        scaleX = 1f - index * SCALE_REDUCTION_FACTOR
                    }
                }
            }
        }
    }
}

@Composable
fun LazyCardStack(
    modifier: Modifier = Modifier,
    state: LazyCardStackState = rememberLazyCardStackState(),
    content: LazyCardStackScope.() -> Unit,
) {
    val itemProviderLambda = rememberLazyCardStackItemProviderLambda(state, content)
    val measurePolicy =
        rememberLazyCardStackMeasurePolicy(
            itemProviderLambda = itemProviderLambda,
            state = state,
        )
    LazyLayout(
        modifier = modifier,
        itemProvider = itemProviderLambda,
        measurePolicy = measurePolicy,
    )
}

@Composable
@Preview(showBackground = true)
fun LazyCardStackPreview() {
    val data = remember { mutableStateListOf("1", "2", "3") }
    val state = rememberLazyCardStackState()

    AppTheme {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyCardStack(
                state = state,
                modifier = Modifier.size(200.dp),
            ) {
                items(
                    items = data,
                    key = { item -> item },
                ) { item ->
                    SwipeableCard(
                        item = item,
                        key = item,
                        header = {
                            Row {
                                Text("Header $item")
                            }
                        },
                        onSwiped = { item, direction ->
                            state.moveNext()
//                            data.add("${data.size + 1}")
                        },
                        onAboutToSwipe = { _, _ -> },
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(16.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Text(item)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun LazyListPreview() {
    val items = mutableStateListOf<String>("1", "2", "3")
    AppTheme {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items.size) { index ->
                ElevatedCard(
                    modifier =
                        Modifier
                            .height(100.dp)
                            .fillMaxWidth()
                            .clickable {
                                items.add("${items.size + 1 }")
                            },
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(text = items[index])
                    }
                }
            }
        }
    }
}
