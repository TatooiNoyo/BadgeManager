package io.github.tatooinoyo.star.badge.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.tatooinoyo.star.badge.data.BadgeRepository
import io.github.tatooinoyo.star.badge.ui.about.AboutScreen
import io.github.tatooinoyo.star.badge.ui.helpus.HelpUsScreen
import io.github.tatooinoyo.star.badge.ui.home.HomeScreen
import io.github.tatooinoyo.star.badge.ui.home.HomeViewModel
import io.github.tatooinoyo.star.badge.ui.settings.SettingsScreen

// 定义导航路线
object NavRoutes {
    const val Home = "home"
    const val Settings = "settings"
    const val About = "about"
    const val HelpUs = "helpUs"
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
        composable(
            route = NavRoutes.Home,
            exitTransition = {
                slideOutHorizontally(
                    animationSpec = tween(240, easing = FastOutSlowInEasing),
                    targetOffsetX = { width -> -width / 5 },
                ) + fadeOut(animationSpec = tween(160))
            },
            popEnterTransition = {
                slideInHorizontally(
                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                    initialOffsetX = { width -> -width / 5 },
                ) + fadeIn(animationSpec = tween(200, delayMillis = 30))
            },
        ) {
            HomeScreen(
                homeViewModel = homeViewModel,
                nfcPayload = nfcPayload,
                onNfcDataConsumed = onNfcDataConsumed,
                onNavigateToSettings = { navController.navigate(NavRoutes.Settings) },
                onNavigateToAbout = { navController.navigate(NavRoutes.About) },
                onNavigateToUnrecordedBadges = { navController.navigate(NavRoutes.HelpUs) }
            )
        }
        composable(
            route = NavRoutes.Settings,
            enterTransition = { secondaryPageEnterTransition() },
            popExitTransition = { secondaryPageExitTransition() },
        ) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = NavRoutes.About,
            enterTransition = { secondaryPageEnterTransition() },
            popExitTransition = { secondaryPageExitTransition() },
        ) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = NavRoutes.HelpUs,
            enterTransition = { secondaryPageEnterTransition() },
            popExitTransition = { secondaryPageExitTransition() },
        ) {
            // 使用 Repository 全量列表，避免受首页标签筛选影响
            val badges by BadgeRepository.badges.collectAsState()
            HelpUsScreen(
                badges = badges,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

private fun secondaryPageEnterTransition() =
    slideInHorizontally(
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        initialOffsetX = { width -> width },
    ) + fadeIn(animationSpec = tween(180, delayMillis = 40))

private fun secondaryPageExitTransition() =
    slideOutHorizontally(
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        targetOffsetX = { width -> width },
    ) + fadeOut(animationSpec = tween(180))
