package com.misw4203.vinilos.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.misw4203.vinilos.presentation.ui.components.VinilosBottomNav
import com.misw4203.vinilos.presentation.ui.components.VinilosDestination

@Composable
fun VinilosNavHost() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val selectedDestination = selectedTab(currentRoute)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            VinilosBottomNav(
                selected = selectedDestination,
                onSelect = { dest ->
                    val route = when (dest) {
                        VinilosDestination.Albums -> Destinations.AlbumList
                        VinilosDestination.Artists -> Destinations.ArtistList
                        VinilosDestination.Collectors -> Destinations.Collectors
                        VinilosDestination.Prizes -> Destinations.Prizes
                    }
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = Destinations.AlbumList,
            ) {
                albumGraph(navController, snackbarHostState)
                artistGraph(navController)
                collectorGraph(navController)
                prizeGraph(navController)
            }
        }
    }
}
