package com.wbtracker.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.wbtracker.app.ui.screen.addproduct.AddProductScreen
import com.wbtracker.app.ui.screen.analytics.AnalyticsScreen
import com.wbtracker.app.ui.screen.detail.ProductDetailScreen
import com.wbtracker.app.ui.screen.favorites.FavoritesScreen
import com.wbtracker.app.ui.screen.home.HomeScreen
import com.wbtracker.app.ui.screen.profile.ProfileScreen
import com.wbtracker.app.ui.screen.splash.SplashScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Favorites : Screen("favorites")
    object Analytics : Screen("analytics")
    object Profile : Screen("profile")
    object Dashboard : Screen("dashboard")
    object ProductDetail : Screen("product/{articleId}") {
        fun createRoute(articleId: Long) = "product/$articleId"
    }
    object AddProduct : Screen("add_product?url={url}") {
        fun createRoute(url: String = "") = "add_product?url=$url"
    }
}

@Composable
fun WbTrackerNavGraph(
    navController: NavHostController,
    onOpenAddTargetSheet: () -> Unit = {},
    sharedUrl: String? = null
) {
    val startDestination = if (!sharedUrl.isNullOrEmpty()) {
        Screen.AddProduct.createRoute(sharedUrl)
    } else {
        Screen.Splash.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToProduct = { articleId ->
                    navController.navigate(Screen.ProductDetail.createRoute(articleId))
                },
                onOpenAddSheet = onOpenAddTargetSheet
            )
        }

        composable(Screen.Dashboard.route) {
            HomeScreen(
                onNavigateToProduct = { articleId ->
                    navController.navigate(Screen.ProductDetail.createRoute(articleId))
                },
                onOpenAddSheet = onOpenAddTargetSheet
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onNavigateToProduct = { articleId ->
                    navController.navigate(Screen.ProductDetail.createRoute(articleId))
                }
            )
        }

        composable(Screen.Analytics.route) {
            AnalyticsScreen()
        }

        composable(Screen.Profile.route) {
            ProfileScreen()
        }

        composable(
            route = Screen.ProductDetail.route,
            arguments = listOf(navArgument("articleId") { type = NavType.LongType })
        ) {
            ProductDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AddProduct.route,
            arguments = listOf(navArgument("url") {
                type = NavType.StringType
                defaultValue = ""
            })
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            AddProductScreen(
                initialUrl = url,
                onNavigateBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0)
                        }
                    }
                }
            )
        }
    }
}
