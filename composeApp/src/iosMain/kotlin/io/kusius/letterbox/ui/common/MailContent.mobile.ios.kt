package io.kusius.letterbox.ui.common

import androidx.compose.ui.graphics.Color
import com.multiplatform.webview.util.toUIColor
import com.multiplatform.webview.web.NativeWebView

actual fun configureNativeWebview(
    webView: NativeWebView,
    backgroundColor: Color,
) {
    webView.underPageBackgroundColor = backgroundColor.toUIColor()
}
