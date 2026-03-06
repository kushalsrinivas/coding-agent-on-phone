package com.kushalsrinivas.phones.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kushalsrinivas.phones.ui.screens.*
import com.kushalsrinivas.phones.viewmodel.DashboardViewModel
import com.kushalsrinivas.phones.viewmodel.SettingsViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    data object Agent : Screen("agent", "Agent", Icons.Default.Code)
    data object Bot : Screen("bot", "Bot", Icons.Default.SmartToy)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

private val screens = listOf(Screen.Dashboard, Screen.Agent, Screen.Bot, Screen.Settings)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val dashboardViewModel: DashboardViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    val botToken by settingsViewModel.botToken.collectAsState()
    val anthropicApiKey by settingsViewModel.anthropicApiKey.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    settingsBotToken = botToken,
                    settingsAnthropicApiKey = anthropicApiKey,
                )
            }
            composable(Screen.Agent.route) {
                AgentTerminalScreen(viewModel = dashboardViewModel)
            }
            composable(Screen.Bot.route) {
                BotTerminalScreen(viewModel = dashboardViewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    dashboardViewModel = dashboardViewModel,
                    settingsViewModel = settingsViewModel,
                )
            }
        }
    }
}
