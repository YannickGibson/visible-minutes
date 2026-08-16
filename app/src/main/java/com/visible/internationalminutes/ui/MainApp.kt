package com.visible.internationalminutes.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.visible.internationalminutes.ui.screens.CallHistoryScreen
import com.visible.internationalminutes.ui.screens.CountriesDirectoryScreen
import com.visible.internationalminutes.ui.screens.DashboardScreen
import com.visible.internationalminutes.ui.screens.SettingsScreen
import com.visible.internationalminutes.ui.theme.VisibleBlue

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object CallHistory : Screen("call_history", "Call Log", Icons.Default.Call)
    object Countries : Screen("countries", "Destinations", Icons.Default.Public)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun MainApp(
    viewModel: MainViewModel,
    uiState: MainUiState,
    onRequestPermissions: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    val navigationItems = listOf(
        Screen.Dashboard,
        Screen.CallHistory,
        Screen.Countries,
        Screen.Settings
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                navigationItems.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VisibleBlue,
                            selectedTextColor = VisibleBlue,
                            indicatorColor = VisibleBlue.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    uiState = uiState,
                    onRefresh = { viewModel.refreshData() },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onRequestPermissions = onRequestPermissions,
                    onSimClick = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.CallHistory.route) {
                CallHistoryScreen(
                    uiState = uiState,
                    onRefresh = { viewModel.refreshData() }
                )
            }

            composable(Screen.Countries.route) {
                CountriesDirectoryScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    uiState = uiState,
                    onBillingDayChange = { viewModel.setBillingDay(it) },
                    onTimezoneChange = { viewModel.setBillingTimezone(it) },
                    onSimSelectionChange = { viewModel.setSelectedSim(it) },
                    onPersistentNotificationToggle = { viewModel.setPersistentNotification(it) },
                    onDemoDataToggle = { viewModel.setDemoDataEnabled(it) }
                )
            }
        }
    }
}
