@file:OptIn(KoinExperimentalAPI::class)

package io.kusius.letterbox

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import androidx.navigation.toRoute
import androidx.window.core.layout.WindowSizeClass
import io.kusius.letterbox.di.appModule
import io.kusius.letterbox.ui.NavigationRoute
import io.kusius.letterbox.ui.Route
import io.kusius.letterbox.ui.RouteRegistry
import io.kusius.letterbox.ui.feed.SummaryRoute
import io.kusius.letterbox.ui.feed.SummaryScreenRoot
import io.kusius.letterbox.ui.letterbox.LetterboxRoute
import io.kusius.letterbox.ui.letterbox.LetterboxScreenRoot
import io.kusius.letterbox.ui.mail.MailRoute
import io.kusius.letterbox.ui.mail.MailScreenRoot
import io.kusius.letterbox.ui.theme.AppTheme
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.KoinMultiplatformApplication
import org.koin.compose.koinInject
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.koinConfiguration

private val screenRoutes =
    listOf<NavigationRoute>(
        LetterboxRoute,
        SummaryRoute,
    )

@Composable
fun App(windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass) {
    KoinMultiplatformApplication(
        config =
            koinConfiguration {
                modules(appModule())
            },
    ) {
        val navController = rememberNavController()
        val routeRegistry: RouteRegistry = koinInject<RouteRegistry>()

        AppTheme {
            val currentBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute: Route? = routeRegistry.deserialize(currentBackStackEntry)

            Scaffold(
                bottomBar = {
                    BottomNavigationBar(
                        routes = screenRoutes,
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(
                                route = route,
                                navOptions =
                                    navOptions {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    },
                            )
                        },
                    )
                },
            ) { innerPadding ->
                val modifier = Modifier.padding(innerPadding)

                NavHost(navController = navController, startDestination = LetterboxRoute) {
                    composable<LetterboxRoute> {
                        LetterboxScreenRoot(modifier = modifier, windowSizeClass = windowSizeClass)
                    }
                    composable<SummaryRoute> {
                        SummaryScreenRoot(
                            modifier = modifier,
                            onNavigateToMail = {
                                navController.navigate(
                                    route = MailRoute(it),
                                    navOptions =
                                        navOptions {
                                            launchSingleTop = true
                                        },
                                )
                            },
                        )
                    }
                    composable<MailRoute> { backStackEntry ->
                        val route: MailRoute = backStackEntry.toRoute()
                        MailScreenRoot(
                            mailId = route.mailId,
                            modifier = modifier,
                            onBack = {},
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    routes: List<NavigationRoute>,
    currentRoute: Route?,
    onNavigate: (route: NavigationRoute) -> Unit,
) {
    if (currentRoute !is NavigationRoute) return

    NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
        routes.forEach { item ->
            val isSelected = currentRoute::class == item::class
//                currentRoute?.contains(item::class.qualifiedName ?: "") == true
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    onNavigate(item)
                },
                icon = {
                    Icon(
                        painter = painterResource(item.drawable),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                },
            )
        }
    }
}
