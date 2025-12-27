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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import androidx.navigation.toRoute
import androidx.window.core.layout.WindowSizeClass
import io.kusius.letterbox.di.appModule
import io.kusius.letterbox.ui.NavigationRoute
import io.kusius.letterbox.ui.feed.SummaryRoute
import io.kusius.letterbox.ui.feed.SummaryScreenRoot
import io.kusius.letterbox.ui.letterbox.LetterboxRoute
import io.kusius.letterbox.ui.letterbox.LetterboxScreenRoot
import io.kusius.letterbox.ui.mail.MailRoute
import io.kusius.letterbox.ui.mail.MailScreenRoot
import io.kusius.letterbox.ui.theme.AppTheme
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.KoinMultiplatformApplication
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

        AppTheme {
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(
                        routes = screenRoutes,
                        startDestination =
                            screenRoutes.indexOfFirst {
                                it is LetterboxRoute
                            },
                        onNavigate = { route ->
                            navController.navigate(
                                route = route,
                                navOptions =
                                    navOptions {
                                        popUpTo(route) { inclusive = true }
                                        launchSingleTop = true
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
    startDestination: Int,
    onNavigate: (route: NavigationRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedDestination by rememberSaveable(startDestination) {
        mutableIntStateOf(startDestination)
    }

    NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
        routes.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = index == selectedDestination,
                onClick = {
                    if (index != selectedDestination) {
                        onNavigate(item)
                        selectedDestination = index
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(item.IconContent()),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                },
            )
        }
    }
}
