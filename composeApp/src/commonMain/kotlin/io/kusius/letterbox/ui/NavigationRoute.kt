package io.kusius.letterbox.ui

import org.jetbrains.compose.resources.DrawableResource

interface Route

interface NavigationRoute : Route {
    val drawable: DrawableResource
}
