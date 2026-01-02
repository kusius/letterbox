package io.kusius.letterbox.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun LottieLoader(
    json: String,
    modifier: Modifier = Modifier,
)
