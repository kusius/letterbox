package io.kusius.letterbox.ui.common

import KottieAnimation
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kottieComposition.KottieCompositionSpec
import kottieComposition.animateKottieCompositionAsState
import kottieComposition.rememberKottieComposition

@Composable
actual fun LottieLoader(
    json: String,
    modifier: Modifier,
) {
    val composition =
        rememberKottieComposition(
            spec = KottieCompositionSpec.JsonString(json), // Or KottieCompositionSpec.Url || KottieCompositionSpec.JsonString
        )

    val animationState by animateKottieCompositionAsState(
        composition = composition,
        iterations = Int.MAX_VALUE,
    )

    KottieAnimation(
        composition = composition,
        progress = { animationState.progress },
        modifier = modifier,
    )
}
