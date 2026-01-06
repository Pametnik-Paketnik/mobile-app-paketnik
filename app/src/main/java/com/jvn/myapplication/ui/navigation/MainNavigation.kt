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
import com.jvn.myapplication.ui.screens.CleanerScreen
import com.jvn.myapplication.ui.screens.HomeScreen
import com.jvn.myapplication.ui.screens.MapScreen
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
    
    val userType by authRepository.getUserType().collectAsState(initial = null)
    
    var selectedBox by remember { mutableStateOf<BoxData?>(null) }
    var showBoxDetail by remember { mutableStateOf(false) }
    
    val lightGray = Color(0xFFF7F7F7)
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val filteredBottomNavItems = when (userType) {
        "CLEANER" -> listOf(
            BottomNavItem.Home,
            BottomNavItem.UserSettings
        )
        else -> bottomNavItems
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = lightGray,
        bottomBar = {
            BottomNavigationBar(
                items = filteredBottomNavItems,
                currentDestination = currentRoute,
                onItemClick = { route ->
                    if (showBoxDetail) {
                        showBoxDetail = false
                        selectedBox = null
                    }
                    
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
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
                    when (userType) {
                        "USER" -> BoxesListScreen(
                            onBoxClick = { box ->
                                selectedBox = box
                                showBoxDetail = true
                            }
                        )
                        "HOST" -> HomeScreen()
                        "CLEANER" -> CleanerScreen()
                        else -> HomeScreen()
                    }
                }
                
                composable(BottomNavItem.BoxesHistory.route) {
                    if (userType == "CLEANER") {
                        LaunchedEffect(Unit) {
                            navController.navigate(BottomNavItem.Home.route) {
                                popUpTo(BottomNavItem.Home.route) { inclusive = true }
                            }
                        }
                    } else {
                        BoxesHistoryScreen()
                    }
                }
                
                composable(BottomNavItem.Map.route) {
                    MapScreen()
                }
                
                composable(BottomNavItem.UserSettings.route) {
                    UserSettingsScreen(onLogout = onLogout)
                }
            }
            
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