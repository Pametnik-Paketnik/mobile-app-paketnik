package com.jvn.myapplication.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jvn.myapplication.ui.screens.BoxesHistoryScreen
import com.jvn.myapplication.ui.screens.HomeScreen
import com.jvn.myapplication.ui.screens.UserSettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(
    onLogout: () -> Unit = {}
) {
    val navController = rememberNavController()
    
    // Airbnb-style color palette
    val lightGray = Color(0xFFF7F7F7)
    
    // Get current destination for bottom navigation
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = lightGray,
        bottomBar = {
            BottomNavigationBar(
                items = bottomNavItems,
                currentDestination = currentRoute,
                onItemClick = { route ->
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            // Pop up to the start destination to avoid building up a large stack
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0) // Remove default insets
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NavHost(
                navController = navController,
                startDestination = BottomNavItem.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(BottomNavItem.Home.route) {
                    HomeScreen()
                }
                
                composable(BottomNavItem.BoxesHistory.route) {
                    BoxesHistoryScreen()
                }
                
                composable(BottomNavItem.UserSettings.route) {
                    UserSettingsScreen(onLogout = onLogout)
                }
            }
        }
    }
} 