package com.wbtracker.app.ui.screen.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.wbtracker.app.ui.screen.home.HomeScreen

@Composable
fun DashboardScreen(
    onNavigateToAddProduct: () -> Unit,
    onNavigateToProduct: (Long) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    HomeScreen(
        onNavigateToProduct = onNavigateToProduct,
        onOpenAddSheet = onNavigateToAddProduct,
        modifier = modifier,
        viewModel = viewModel
    )
}
