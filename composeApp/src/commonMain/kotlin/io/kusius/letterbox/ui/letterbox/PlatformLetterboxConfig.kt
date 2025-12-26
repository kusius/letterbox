package io.kusius.letterbox.ui.letterbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

expect class PlatformLetterboxConfig() {
    fun visibleCards(): Int

    fun isDraggableCards(): Boolean

    fun isExpandable(): Boolean
}

@Composable
fun rememberPlatformConfig(): PlatformLetterboxConfig = remember { PlatformLetterboxConfig() }
