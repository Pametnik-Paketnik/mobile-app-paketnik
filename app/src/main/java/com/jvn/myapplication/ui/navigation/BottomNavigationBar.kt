package com.jvn.myapplication.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BottomNavigationBar(
    items: List<BottomNavItem>,
    currentDestination: String?,
    onItemClick: (String) -> Unit
) {
    // Airbnb-style color palette
    val airbnbRed = Color(0xFFFF5A5F)
    val cardWhite = Color(0xFFFFFFFF)
    val textLight = Color(0xFF767676)

    Card(
        modifier = Modifier
            .height(100.dp)
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        colors = CardDefaults.cardColors(containerColor = cardWhite),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentDestination == item.route
                
                // Fixed-width container to prevent movement
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp), // Fixed height
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp) // Fixed size container
                            .clip(CircleShape)
                            .clickable { onItemClick(item.route) },
                        contentAlignment = Alignment.Center
                    ) {
                        // Background circle - always same size
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    if (isSelected) airbnbRed.copy(alpha = 0.15f) else Color.Transparent,
                                    CircleShape
                                )
                        )
                        
                        // Icon - always centered and same size
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = animateColorAsState(
                                targetValue = if (isSelected) airbnbRed else textLight,
                                animationSpec = tween(300),
                                label = "iconColor"
                            ).value,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
} 