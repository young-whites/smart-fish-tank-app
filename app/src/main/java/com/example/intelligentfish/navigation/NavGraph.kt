package com.example.intelligentfish.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.intelligentfish.ui.control.ControlScreen
import com.example.intelligentfish.ui.dashboard.DashboardScreen
import com.example.intelligentfish.ui.settings.SettingsScreen
import com.example.intelligentfish.viewmodel.FishTankViewModel

sealed class Screen(val route: String, val label: String, val icon: String) {
    data object Dashboard : Screen("dashboard", "首页", "🏠")
    data object Control : Screen("control", "控制", "🎛")
    data object Settings : Screen("settings", "设置", "⚙️")
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Control,
    Screen.Settings
)

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: FishTankViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(viewModel)
        }
        composable(Screen.Control.route) {
            ControlScreen(viewModel)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(viewModel)
        }
    }
}
