package io.github.tatooinoyo.star.badge.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.tatooinoyo.star.badge.ui.about.AboutScreen
import io.github.tatooinoyo.star.badge.ui.home.HomeScreen
import io.github.tatooinoyo.star.badge.ui.home.HomeViewModel
import io.github.tatooinoyo.star.badge.ui.settings.SettingsScreen

// 定义导航路线
object NavRoutes {
    const val Home = "home"
    const val Settings = "settings"
    const val About = "about"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    homeViewModel: HomeViewModel? = null,
    nfcPayload: String? = null,
    onNfcDataConsumed: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.Home
    ) {
        composable(NavRoutes.Home) {
            HomeScreen(
                homeViewModel = homeViewModel,
                nfcPayload = nfcPayload,
                onNfcDataConsumed = onNfcDataConsumed,
                onNavigateToSettings = { navController.navigate(NavRoutes.Settings) },
                onNavigateToAbout = { navController.navigate(NavRoutes.About) }
            )
        }
        composable(NavRoutes.Settings) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.About) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}