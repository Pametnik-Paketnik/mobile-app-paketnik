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
import com.jvn.myapplication.ui.screens.BoxesListScreen
import com.jvn.myapplication.ui.screens.BoxDetailScreen
import com.jvn.myapplication.ui.screens.HomeScreen
import com.jvn.myapplication.ui.screens.UserSettingsScreen
import com.jvn.myapplication.data.repository.AuthRepository
import com.jvn.myapplication.data.model.BoxData
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(
    onLogout: () -> Unit = {}
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    
    // Get user type to determine which home screen to show
    val userType by authRepository.getUserType().collectAsState(initial = null)
    
    // State for box detail navigation
    var selectedBox by remember { mutableStateOf<BoxData?>(null) }
    var showBoxDetail by remember { mutableStateOf(false) }
    
    // Airbnb-style color palette
    val lightGray = Color(0xFFF7F7F7)
    
    // Get current destination for bottom navigation
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // All user types see the same navigation items now
    val filteredBottomNavItems = bottomNavItems

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = lightGray,
        bottomBar = {
            BottomNavigationBar(
                items = filteredBottomNavItems,
                currentDestination = currentRoute,
                onItemClick = { route ->
                    // If BoxDetailScreen is shown, close it when navigating to another tab
                    if (showBoxDetail) {
                        showBoxDetail = false
                        selectedBox = null
                    }
                    
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
                    // Show different home screens based on user type
                    when (userType) {
                        "USER" -> BoxesListScreen(
                            onBoxClick = { box ->
                                selectedBox = box
                                showBoxDetail = true
                            }
                        )
                        "HOST" -> HomeScreen() // Keep existing QR scanner for hosts
                        else -> HomeScreen() // Default fallback
                    }
                }
                
                composable(BottomNavItem.BoxesHistory.route) {
                    BoxesHistoryScreen()
                }
                
                composable(BottomNavItem.UserSettings.route) {
                    UserSettingsScreen(onLogout = onLogout)
                }
            }
            
            // Box Detail Screen overlay
            if (showBoxDetail && selectedBox != null) {
                BoxDetailScreen(
                    box = selectedBox!!,
                    onBackClick = {
                        showBoxDetail = false
                        selectedBox = null
                    }
                )
            }
        }
    }
} 