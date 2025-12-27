package io.kusius.letterbox.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.DrawableResource

interface NavigationRoute {
     fun IconContent(): DrawableResource
}
