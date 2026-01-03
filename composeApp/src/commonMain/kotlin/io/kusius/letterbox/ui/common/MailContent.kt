package io.kusius.letterbox.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import com.multiplatform.webview.request.RequestInterceptor
import com.multiplatform.webview.request.WebRequest
import com.multiplatform.webview.request.WebRequestInterceptResult
import com.multiplatform.webview.web.NativeWebView
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import io.kusius.letterbox.model.Mail
import io.kusius.letterbox.model.MailPart
import io.kusius.letterbox.model.MailPartBody
import io.kusius.letterbox.model.MimeTypes

@Composable
fun MailItem(mail: Mail) {
    MailContent(
        content = mail.mailPart,
        key = mail.summary.id,
    )
}

@Composable
private fun MailContent(
    content: MailPart,
    key: Any?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (content.mimeType) {
            MimeTypes.Multipart.ALTERNATIVE -> {
                val html = content.parts.find { it.mimeType == MimeTypes.Text.HTML }
                html?.let {
                    it.body?.let { body ->
                        HtmlMailContent(
                            body,
                            key = key,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            MimeTypes.Text.HTML -> {
                content.body?.let { body ->
                    HtmlMailContent(
                        body,
                        key,
                    )
                }
            }

            MimeTypes.Text.PLAIN -> {
                content.body?.let { body ->
                    body.data?.let { text ->
                        Text(text = text)
                    }
                }
            }

            MimeTypes.Multipart.MIXED -> {
                content.parts.forEach { part ->
                    MailContent(
                        content = part,
                        key = key,
                    )
                }
            }

            MimeTypes.Image.PNG -> {
                content.body?.let {
                    MailAttachment(
                        it,
                    )
                }
            }

            else -> {
                Text("Could not display mime type ${content.mimeType}")
            }
        }
    }
}

@Composable
private fun MailAttachment(
    body: MailPartBody,
    modifier: Modifier = Modifier,
) {
    if (body.data != null) {
        Text(text = "Received attachment with PNG data ${body.size} ", modifier = modifier)
    }
}

@Composable
private fun HtmlMailContent(
    body: MailPartBody,
    key: Any?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        body.data?.let {
            val html =
                it
                    .injectViewportMeta()
            HtmlMail(html, key)
        }
    }
}

private fun String.injectViewportMeta(): String {
    val metaTag = """<meta name="viewport" content="width=device-width, initial-scale=1">"""
    return if (this.contains("<head>")) {
        this.replace("<head>", "<head>$metaTag")
    } else {
        "$metaTag$this"
    }
}

@Composable
internal fun HtmlMail(
    html: String,
    key: Any?,
    modifier: Modifier = Modifier,
) {
    key(key) {
        val state =
            rememberWebViewStateWithHTMLData(
                data = html,
                mimeType = MimeTypes.Text.HTML.mimeType,
            ).apply {
                webSettings.iOSWebSettings.apply {
                    // Prevents showing transparent background when overscrolling.
                    opaque = true
                }
                webSettings.desktopWebSettings.apply {
                    disablePopupWindows = true
                }
            }

        val uriHandler = LocalUriHandler.current
        val navigator =
            rememberWebViewNavigator(
                requestInterceptor =
                    object : RequestInterceptor {
                        override fun onInterceptUrlRequest(
                            request: WebRequest,
                            navigator: WebViewNavigator,
                        ): WebRequestInterceptResult {
                            // Open email urls in external browser.
                            if (request.url.contains("http")) {
                                uriHandler.openUri(request.url)
                                return WebRequestInterceptResult.Reject
                            }

                            return WebRequestInterceptResult.Allow
                        }
                    },
            )

        val bgColor = MaterialTheme.colorScheme.background
        WebView(
            state = state,
            navigator = navigator,
            captureBackPresses = false,
            onCreated = { webView: NativeWebView ->
                configureNativeWebview(webView, bgColor)
            },
            modifier =
                modifier
                    .fillMaxSize()
                    .clipToBounds(),
        )
    }
}

expect fun configureNativeWebview(
    webView: NativeWebView,
    backgroundColor: Color = Color.Unspecified,
)
