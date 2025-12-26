package io.kusius.letterbox.ui.common

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.multiplatform.webview.web.NativeWebView

actual fun configureNativeWebview(
    webView: NativeWebView,
    backgroundColor: Color,
) {
    webView.settings.apply {
        // Allow mobile rendering by NOT using wide viewport mode
        loadWithOverviewMode = false
        useWideViewPort = false
        // Enable zoom controls for accessibility but start zoomed to fit
        builtInZoomControls = true
        displayZoomControls = false
        defaultTextEncodingName = "utf-8"
    }
}
