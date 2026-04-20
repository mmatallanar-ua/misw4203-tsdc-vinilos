package com.misw4203.vinilos.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.misw4203.vinilos.presentation.ui.components.VinilosBottomNav
import com.misw4203.vinilos.presentation.ui.components.VinilosDestination
import com.misw4203.vinilos.presentation.ui.screens.album.AlbumDetailScreen
import com.misw4203.vinilos.presentation.ui.screens.album.AlbumListScreen
import com.misw4203.vinilos.presentation.ui.screens.artist.MusicianDetailScreen
import com.misw4203.vinilos.presentation.ui.screens.artist.MusicianListScreen

@Composable
fun VinilosNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val selectedDestination = when {
        currentRoute == Destinations.ArtistList || currentRoute?.startsWith("artist/") == true -> VinilosDestination.Artists
        currentRoute == Destinations.Collectors -> VinilosDestination.Collectors
        else -> VinilosDestination.Albums
    }

    Scaffold(
        bottomBar = {
            VinilosBottomNav(
                selected = selectedDestination,
                onSelect = { dest ->
                    val route = when (dest) {
                        VinilosDestination.Albums -> Destinations.AlbumList
                        VinilosDestination.Artists -> Destinations.ArtistList
                        VinilosDestination.Collectors -> Destinations.Collectors
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
                composable(Destinations.AlbumList) {
                    AlbumListScreen(
                        onAlbumClick = { albumId ->
                            navController.navigate(Destinations.albumDetail(albumId))
                        },
                    )
                }
                composable(
                    route = Destinations.AlbumDetail,
                    arguments = listOf(navArgument(Destinations.AlbumDetailArg) { type = NavType.LongType }),
                ) { entry ->
                    val albumId = entry.arguments?.getLong(Destinations.AlbumDetailArg) ?: 0L
                    AlbumDetailScreen(
                        albumId = albumId,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Destinations.ArtistList) {
                    MusicianListScreen(
                        onMusicianClick = { id -> navController.navigate("artist/$id") },
                    )
                }
                composable(
                    route = "artist/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.IntType }),
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getInt("id") ?: return@composable
                    MusicianDetailScreen(
                        musicianId = id,
                        onBack = { navController.navigateUp() },
                    )
                }
                composable(Destinations.Collectors) {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
