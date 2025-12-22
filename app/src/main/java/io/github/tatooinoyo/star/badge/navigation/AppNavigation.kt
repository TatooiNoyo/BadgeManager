package io.github.tatooinoyo.star.badge.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.tatooinoyo.star.badge.ui.about.AboutScreen
import io.github.tatooinoyo.star.badge.ui.home.BadgeManagerScreen
import io.github.tatooinoyo.star.badge.ui.settings.SettingsScreen

// 定义导航路线
object NavRoutes {
    const val Home = "home"
    const val Settings = "settings"
    const val About = "about"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.Home
    ) {
        composable(NavRoutes.Home) {
            BadgeManagerScreen(
                navController = navController
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