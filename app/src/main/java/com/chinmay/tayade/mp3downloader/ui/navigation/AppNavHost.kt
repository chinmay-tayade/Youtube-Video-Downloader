package com.chinmay.tayade.mp3downloader.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chinmay.tayade.mp3downloader.ui.screens.DownloadsScreen
import com.chinmay.tayade.mp3downloader.ui.screens.FormatsScreen
import com.chinmay.tayade.mp3downloader.ui.screens.HomeScreen
import com.chinmay.tayade.mp3downloader.ui.screens.SettingsScreen

@Composable
fun AppNavHost(
    startUrl: String?,
    openDownloadsOnLaunch: Boolean,
    onDownloadsShortcutConsumed: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Destinations.HOME) {

        composable(Destinations.HOME) {
            HomeScreen(
                initialUrl = startUrl,
                onFetch = { url -> navController.navigate(Destinations.formats(url)) },
                onOpenDownloads = { navController.navigate(Destinations.DOWNLOADS) },
                onOpenSettings = { navController.navigate(Destinations.SETTINGS) },
            )
        }

        composable(
            route = Destinations.FORMATS,
            arguments = listOf(navArgument(Destinations.FORMATS_ARG_URL) { type = NavType.StringType }),
        ) { entry ->
            val encoded = entry.arguments?.getString(Destinations.FORMATS_ARG_URL).orEmpty()
            FormatsScreen(
                url = Destinations.decode(encoded),
                onBack = { navController.popBackStack() },
                onEnqueued = {
                    navController.navigate(Destinations.DOWNLOADS) {
                        popUpTo(Destinations.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Destinations.DOWNLOADS) {
            DownloadsScreen(onBack = { navController.popBackStack() })
        }

        composable(Destinations.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }

    LaunchedEffect(openDownloadsOnLaunch) {
        if (openDownloadsOnLaunch) {
            navController.navigate(Destinations.DOWNLOADS) { launchSingleTop = true }
            onDownloadsShortcutConsumed()
        }
    }
}
