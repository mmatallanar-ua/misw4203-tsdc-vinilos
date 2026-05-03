package com.misw4203.vinilos.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.misw4203.vinilos.R
import com.misw4203.vinilos.presentation.viewmodel.AlbumDetailViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.misw4203.vinilos.presentation.ui.components.VinilosBottomNav
import com.misw4203.vinilos.presentation.ui.components.VinilosDestination
import com.misw4203.vinilos.presentation.ui.screens.album.AddCommentScreen
import com.misw4203.vinilos.presentation.ui.screens.album.AddTrackScreen
import com.misw4203.vinilos.presentation.ui.screens.album.AlbumDetailScreen
import com.misw4203.vinilos.presentation.ui.screens.album.AlbumListScreen
import com.misw4203.vinilos.presentation.ui.screens.artist.MusicianDetailScreen
import com.misw4203.vinilos.presentation.ui.screens.artist.MusicianListScreen
import com.misw4203.vinilos.presentation.ui.screens.collector.CollectorDetailScreen
import com.misw4203.vinilos.presentation.ui.screens.collector.CollectorListScreen

@Composable
fun VinilosNavHost() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val selectedDestination = when {
        currentRoute == Destinations.ArtistList || currentRoute?.startsWith("artist/") == true -> VinilosDestination.Artists
        currentRoute == Destinations.Collectors || currentRoute?.startsWith("collector/") == true -> VinilosDestination.Collectors
        else -> VinilosDestination.Albums
    }

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
                    val viewModel = hiltViewModel<AlbumDetailViewModel>()
                    val trackAddedMessage = stringResource(R.string.add_track_success)
                    // Track addition: show snackbar + refresh
                    LaunchedEffect(entry) {
                        entry.savedStateHandle.getStateFlow("track_added", false).collect { added ->
                            if (added) {
                                entry.savedStateHandle["track_added"] = false
                                viewModel.retry()
                                snackbarHostState.showSnackbar(trackAddedMessage)
                            }
                        }
                    }
                    // Comment addition: refresh only (HU09 pattern)
                    val refreshFlag by entry.savedStateHandle
                        .getStateFlow(Destinations.RefreshAlbumDetailKey, false)
                        .collectAsStateWithLifecycle()
                    AlbumDetailScreen(
                        albumId = albumId,
                        onBack = { navController.popBackStack() },
                        onAddTrack = { navController.navigate(Destinations.addTrack(albumId)) },
                        onAddComment = { navController.navigate(Destinations.addComment(albumId)) },
                        refreshKey = refreshFlag,
                        onRefreshHandled = {
                            entry.savedStateHandle[Destinations.RefreshAlbumDetailKey] = false
                        },
                        viewModel = viewModel,
                    )
                }
                composable(
                    route = Destinations.AddTrack,
                    arguments = listOf(navArgument(Destinations.AddTrackAlbumArg) { type = NavType.LongType }),
                ) {
                    AddTrackScreen(
                        onBack = { navController.popBackStack() },
                        onSuccess = {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("track_added", true)
                            navController.popBackStack()
                        },
                    )
                }
                composable(
                    route = Destinations.AddComment,
                    arguments = listOf(
                        navArgument(Destinations.AddCommentAlbumArg) { type = NavType.LongType },
                        navArgument(Destinations.AddCommentCollectorArg) { type = NavType.IntType },
                    ),
                ) {
                    AddCommentScreen(
                        onBack = { navController.popBackStack() },
                        onSuccess = {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(Destinations.RefreshAlbumDetailKey, true)
                            navController.popBackStack()
                        },
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
                    CollectorListScreen(
                        onCollectorClick = { id -> navController.navigate(Destinations.collectorDetail(id)) },
                    )
                }
                composable(
                    route = Destinations.CollectorDetail,
                    arguments = listOf(navArgument(Destinations.CollectorDetailArg) { type = NavType.IntType }),
                ) { entry ->
                    val collectorId = entry.arguments?.getInt(Destinations.CollectorDetailArg) ?: return@composable
                    CollectorDetailScreen(
                        collectorId = collectorId,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
