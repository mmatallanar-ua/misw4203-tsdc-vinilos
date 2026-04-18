package com.misw4203.vinilos.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.misw4203.vinilos.presentation.ui.screens.album.AlbumDetailScreen
import com.misw4203.vinilos.presentation.ui.screens.album.AlbumListScreen

@Composable
fun VinilosNavHost() {
    val navController = rememberNavController()

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
    }
}
