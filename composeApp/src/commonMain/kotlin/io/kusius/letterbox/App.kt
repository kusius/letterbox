@file:OptIn(KoinExperimentalAPI::class)

package io.kusius.letterbox

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import androidx.navigation.toRoute
import androidx.window.core.layout.WindowSizeClass
import io.kusius.letterbox.di.appModule
import io.kusius.letterbox.ui.feed.SummaryRoute
import io.kusius.letterbox.ui.feed.SummaryScreenRoot
import io.kusius.letterbox.ui.letterbox.LetterboxRoute
import io.kusius.letterbox.ui.letterbox.LetterboxScreenRoot
import io.kusius.letterbox.ui.mail.MailRoute
import io.kusius.letterbox.ui.mail.MailScreenRoot
import io.kusius.letterbox.ui.theme.AppTheme
import org.koin.compose.KoinMultiplatformApplication
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.koinConfiguration

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
            Scaffold { innerPadding ->
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
