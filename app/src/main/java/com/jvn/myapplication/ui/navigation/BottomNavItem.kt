package com.jvn.myapplication.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem(
        route = "home",
        title = "Home",
        icon = Icons.Default.Home
    )
    
    object BoxesHistory : BottomNavItem(
        route = "boxes_history",
        title = "History",
        icon = Icons.Default.Info
    )
    
    object Map : BottomNavItem(
        route = "map",
        title = "Map",
        icon = Icons.Default.Place
    )
    
    object UserSettings : BottomNavItem(
        route = "user_settings",
        title = "Settings",
        icon = Icons.Default.Settings
    )
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.BoxesHistory,
    BottomNavItem.Map,
    BottomNavItem.UserSettings
) 