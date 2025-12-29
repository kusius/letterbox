package io.kusius.letterbox.ui

import androidx.navigation.NavBackStackEntry
import androidx.navigation.toRoute
import io.kusius.letterbox.ui.feed.SummaryRoute
import io.kusius.letterbox.ui.letterbox.LetterboxRoute
import io.kusius.letterbox.ui.mail.MailRoute
import kotlin.reflect.KClass

class RouteRegistry {
    private data class RouteInfo<T : Route>(
        val kClass: KClass<T>,
        val deserializer: (NavBackStackEntry) -> T,
    )

    private val routes = mutableListOf<RouteInfo<*>>()

    init {
        register<LetterboxRoute>()
        register<SummaryRoute>()
        register<MailRoute>()
    }

    private inline fun <reified T : Route> register() {
        routes.add(
            RouteInfo(
                kClass = T::class,
                deserializer = { it.toRoute<T>() },
            ),
        )
    }

    fun deserialize(entry: NavBackStackEntry?): Route? {
        entry ?: return null

        val destinationRoute = entry.destination.route ?: return null

        // Try to find exact match based on the route pattern
        return routes.firstNotNullOfOrNull { routeInfo ->
            val className =
                routeInfo.kClass.qualifiedName
                    ?: return@firstNotNullOfOrNull null

            // Check if destination matches this route class
            if (destinationRoute.contains(className) ||
                destinationRoute.startsWith(routeInfo.kClass.simpleName ?: "")
            ) {
                runCatching { routeInfo.deserializer(entry) }.getOrElse {
                    throw IllegalStateException(
                        "Could not deserialize $className. \n" +
                            "1. Check that your route is @Serializable\n" +
                            "2. Check that you have registered your route in this class's init block",
                    )
                }
            } else {
                null
            }
        }
    }
}
