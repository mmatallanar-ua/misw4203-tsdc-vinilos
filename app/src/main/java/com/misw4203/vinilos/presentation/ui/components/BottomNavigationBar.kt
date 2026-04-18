package com.misw4203.vinilos.presentation.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class BottomNavScreen(val route: String, val label: String) {
    object Albums : BottomNavScreen("albums", "Álbumes")
    object Artists : BottomNavScreen("artists", "Artistas")
    object Collectors : BottomNavScreen("collectors", "Coleccionistas")
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        BottomNavScreen.Albums,
        BottomNavScreen.Artists,
        BottomNavScreen.Collectors
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { screen ->
            NavigationBarItem(
                selected = currentRoute == screen.route || (screen == BottomNavScreen.Artists && currentRoute?.startsWith("artist/") == true),
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = when (screen) {
                            BottomNavScreen.Albums -> Icons.Filled.Album
                            BottomNavScreen.Artists -> Icons.Filled.Person
                            BottomNavScreen.Collectors -> Icons.Filled.Star
                        },
                        contentDescription = screen.label
                    )
                },
                label = {
                    Text(text = screen.label, fontWeight = FontWeight.Bold)
                }
            )
        }
    }
}
