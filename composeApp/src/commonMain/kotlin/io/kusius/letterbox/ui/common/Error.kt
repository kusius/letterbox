package io.kusius.letterbox.ui.common

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
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
import io.kusius.letterbox.ui.theme.AppTheme
import letterbox.composeapp.generated.resources.Res
import letterbox.composeapp.generated.resources.oh_no
import org.jetbrains.compose.resources.stringResource

@Composable
fun Error(modifier: Modifier = Modifier) {
    var animation by remember { mutableStateOf("") }
    val windowInfo = LocalWindowInfo.current
    val lottieSize = 0.5f * windowInfo.containerSize.width
    val isDarkTheme = isSystemInDarkTheme()
    LaunchedEffect(Unit) {
        animation = Res.readBytes("files/sad.json").decodeToString()
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

        Text(
            text = stringResource(Res.string.oh_no),
            maxLines = 3,
            style = AppTheme.typography().titleMedium,
            color = AppTheme.colorScheme().error,
        )
    }
}
