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
    val softGray = Color(0xFFF8F9FA)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = softGray,
        bottomBar = {
            BottomNavigationBar(navController = navController)
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