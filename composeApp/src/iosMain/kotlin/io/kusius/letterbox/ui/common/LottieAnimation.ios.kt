package io.kusius.letterbox.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.infiniteretry.snizzors.SnizzorsUIView
import platform.UIKit.UIView

interface LottieUiKitViewProvider {
    fun jsonLottieView(json: String): UIView
}

lateinit var lottieUiKitViewProvider: LottieUiKitViewProvider

@Composable
actual fun LottieLoader(
    json: String,
    modifier: Modifier,
) {
    SnizzorsUIView(
        factory = { lottieUiKitViewProvider.jsonLottieView(json = json) },
        modifier = modifier,
    )
}
