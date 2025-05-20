package com.jvn.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Direct4meApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Direct4meApp() {
    val teal = Color(0xFF008C9E)
    val lightGray = Color(0xFFF5F5F5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightGray),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = { Text("Direct4Me Box Opener") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = teal,
                titleContentColor = Color.White
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = { /* Tau še prajdejo nuot stvarce */ },
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape),
                colors = ButtonDefaults.buttonColors(containerColor = teal)
            ) {
                Text(
                    text = "OPEN",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}