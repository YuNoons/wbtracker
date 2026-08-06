package com.wbtracker.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.wbtracker.app.ui.screen.addproduct.AddProductScreen
import com.wbtracker.app.ui.screen.dashboard.DashboardScreen
import com.wbtracker.app.ui.screen.detail.ProductDetailScreen
// import com.wbtracker.app.ui.screen.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object ProductDetail : Screen("product/{articleId}") {
        fun createRoute(articleId: Long) = "product/$articleId"
    }
    object AddProduct : Screen("add_product?url={url}") {
        fun createRoute(url: String = "") = "add_product?url=$url"
    }
    object Settings : Screen("settings")
}

@Composable
fun WbTrackerNavGraph(
    navController: NavHostController,
    sharedUrl: String? = null
) {
    val startDestination = if (!sharedUrl.isNullOrEmpty()) {
        Screen.AddProduct.createRoute(sharedUrl)
    } else {
        Screen.Dashboard.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToAddProduct = { navController.navigate(Screen.AddProduct.route) },
                onNavigateToProduct = { articleId ->
                    navController.navigate(Screen.ProductDetail.createRoute(articleId))
                },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
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
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(0)
                        }
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            // SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
