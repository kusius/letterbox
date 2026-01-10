package io.kusius.letterbox.ui.common

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import letterbox.composeapp.generated.resources.Res
import letterbox.composeapp.generated.resources.fetching_mails
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoadingWithAnimation(
    progress: Float = 0f,
    modifier: Modifier = Modifier,
) {
    var animation by remember { mutableStateOf("") }
    val windowInfo = LocalWindowInfo.current
    val lottieSize = 0.5f * windowInfo.containerSize.width
    val isDarkTheme = isSystemInDarkTheme()
    LaunchedEffect(Unit) {
        animation =
            if (isDarkTheme) {
                Res.readBytes("files/loading.json").decodeToString()
            } else {
                Res.readBytes("files/loading-light.json").decodeToString()
            }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier,
    ) {
        if (animation.isNotEmpty()) {
            LottieLoader(
                json = animation,
                modifier = Modifier.size(lottieSize.dp),
            )
        }

        Loading(
            progress = progress,
            text = stringResource(Res.string.fetching_mails),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun Loading(
    progress: Float,
    text: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier,
    ) {
        text?.let {
            Text(text = text)
        }

        LinearProgressIndicator(
            progress = progress,
            modifier =
                Modifier
                    .fillMaxWidth(0.6f),
        )
    }
}
