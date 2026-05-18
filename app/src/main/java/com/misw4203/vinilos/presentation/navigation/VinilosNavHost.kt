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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import com.misw4203.vinilos.presentation.ui.screens.album.AddPerformerToAlbumScreen
import com.misw4203.vinilos.presentation.ui.screens.album.AddTrackScreen
import com.misw4203.vinilos.presentation.ui.screens.album.AlbumDetailScreen
import com.misw4203.vinilos.presentation.ui.screens.album.AlbumListScreen
import com.misw4203.vinilos.presentation.ui.screens.album.CreateAlbumScreen
import com.misw4203.vinilos.presentation.ui.screens.artist.AddAlbumToMusicianScreen
import com.misw4203.vinilos.presentation.ui.screens.artist.AddPrizeToMusicianScreen
import com.misw4203.vinilos.presentation.ui.screens.artist.ArtistsHubScreen
import com.misw4203.vinilos.presentation.ui.screens.artist.MusicianDetailScreen
import com.misw4203.vinilos.presentation.ui.screens.band.AddAlbumToBandScreen
import com.misw4203.vinilos.presentation.ui.screens.band.AddMusiciansToBandScreen
import com.misw4203.vinilos.presentation.ui.screens.band.AddPrizeToBandScreen
import com.misw4203.vinilos.presentation.ui.screens.band.BandDetailScreen
import com.misw4203.vinilos.presentation.ui.screens.collector.AddAlbumToCollectorScreen
import com.misw4203.vinilos.presentation.ui.screens.collector.AddFavoritePerformerScreen
import com.misw4203.vinilos.presentation.ui.screens.collector.CollectorDetailScreen
import com.misw4203.vinilos.presentation.ui.screens.collector.CollectorListScreen
import com.misw4203.vinilos.presentation.ui.screens.prize.CreatePrizeScreen
import com.misw4203.vinilos.presentation.ui.screens.prize.PrizeListScreen

@Composable
fun VinilosNavHost() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val selectedDestination = when {
        currentRoute == Destinations.ArtistList ||
            currentRoute?.startsWith(Destinations.ArtistRoutePrefix) == true ||
            currentRoute?.startsWith(Destinations.BandRoutePrefix) == true ||
            currentRoute?.startsWith(Destinations.MusicianRoutePrefix) == true -> VinilosDestination.Artists
        currentRoute == Destinations.Collectors ||
            currentRoute?.startsWith(Destinations.CollectorRoutePrefix) == true -> VinilosDestination.Collectors
        currentRoute == Destinations.Prizes || currentRoute == Destinations.CreatePrize -> VinilosDestination.Prizes
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
                composable(Destinations.AlbumList) {
                    AlbumListScreen(
                        onAlbumClick = { albumId ->
                            navController.navigate(Destinations.albumDetail(albumId))
                        },
                        onCreateAlbum = {
                            navController.navigate(Destinations.CreateAlbum)
                        },
                    )
                }
                composable(Destinations.CreateAlbum) {
                    CreateAlbumScreen(
                        onBack = { navController.popBackStack() },
                        onAlbumCreated = { navController.popBackStack() },
                    )
                }
                composable(
                    route = Destinations.AlbumDetail,
                    arguments = listOf(navArgument(Destinations.AlbumDetailArg) { type = NavType.LongType }),
                ) { entry ->
                    val albumId = checkNotNull(entry.arguments?.getLong(Destinations.AlbumDetailArg)) {
                        "Falta argumento ${Destinations.AlbumDetailArg} en ${Destinations.AlbumDetail}"
                    }
                    val viewModel = hiltViewModel<AlbumDetailViewModel>()
                    val trackAddedMessage = stringResource(R.string.add_track_success)
                    // Track addition: show snackbar + refresh
                    LaunchedEffect(entry) {
                        entry.savedStateHandle.getStateFlow(Destinations.TrackAddedKey, false).collect { added ->
                            if (added) {
                                entry.savedStateHandle[Destinations.TrackAddedKey] = false
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
                        onAddPerformer = {
                            navController.navigate(Destinations.addPerformerToAlbum(albumId))
                        },
                        refreshKey = refreshFlag,
                        onRefreshHandled = {
                            entry.savedStateHandle[Destinations.RefreshAlbumDetailKey] = false
                        },
                        viewModel = viewModel,
                    )
                }
                composable(
                    route = Destinations.AddPerformerToAlbum,
                    arguments = listOf(navArgument(Destinations.AddPerformerAlbumArg) { type = NavType.LongType }),
                ) {
                    AddPerformerToAlbumScreen(
                        onBack = {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(Destinations.RefreshAlbumDetailKey, true)
                            navController.popBackStack()
                        },
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
                                ?.set(Destinations.TrackAddedKey, true)
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
                    ArtistsHubScreen(
                        onMusicianClick = { id -> navController.navigate(Destinations.artistDetail(id)) },
                        onBandClick = { id -> navController.navigate(Destinations.bandDetail(id)) },
                    )
                }
                composable(
                    route = Destinations.ArtistDetail,
                    arguments = listOf(navArgument(Destinations.ArtistDetailArg) { type = NavType.IntType }),
                ) { backStackEntry ->
                    val id = checkNotNull(backStackEntry.arguments?.getInt(Destinations.ArtistDetailArg)) {
                        "Falta argumento ${Destinations.ArtistDetailArg} en ${Destinations.ArtistDetail}"
                    }
                    val refreshFlag by backStackEntry.savedStateHandle
                        .getStateFlow(Destinations.RefreshMusicianDetailKey, false)
                        .collectAsStateWithLifecycle()
                    MusicianDetailScreen(
                        onBack = { navController.navigateUp() },
                        onAddAlbum = { navController.navigate(Destinations.addAlbumToMusician(id)) },
                        onAddPrize = { navController.navigate(Destinations.addPrizeToMusician(id)) },
                        refreshKey = refreshFlag,
                        onRefreshHandled = {
                            backStackEntry.savedStateHandle[Destinations.RefreshMusicianDetailKey] = false
                        },
                    )
                }
                composable(
                    route = Destinations.AddAlbumToMusician,
                    arguments = listOf(navArgument(Destinations.AddAlbumMusicianArg) { type = NavType.IntType }),
                ) {
                    AddAlbumToMusicianScreen(
                        onBack = {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(Destinations.RefreshMusicianDetailKey, true)
                            navController.popBackStack()
                        },
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
                    val collectorId = checkNotNull(entry.arguments?.getInt(Destinations.CollectorDetailArg)) {
                        "Falta argumento ${Destinations.CollectorDetailArg} en ${Destinations.CollectorDetail}"
                    }
                    val refreshFlag by entry.savedStateHandle
                        .getStateFlow(Destinations.RefreshCollectorDetailKey, false)
                        .collectAsStateWithLifecycle()
                    CollectorDetailScreen(
                        collectorId = collectorId,
                        onBack = { navController.popBackStack() },
                        onAddFavoritePerformer = {
                            navController.navigate(Destinations.addFavoritePerformer(collectorId))
                        },
                        onAddAlbum = { navController.navigate(Destinations.addAlbumToCollector(collectorId)) },
                        refreshKey = refreshFlag,
                        onRefreshHandled = {
                            entry.savedStateHandle[Destinations.RefreshCollectorDetailKey] = false
                        },
                    )
                }
                composable(
                    route = Destinations.AddAlbumToCollector,
                    arguments = listOf(navArgument(Destinations.AddAlbumCollectorArg) { type = NavType.IntType }),
                ) {
                    AddAlbumToCollectorScreen(
                        onBack = {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(Destinations.RefreshCollectorDetailKey, true)
                            navController.popBackStack()
                        },
                    )
                }
                composable(
                    route = Destinations.AddFavoritePerformer,
                    arguments = listOf(
                        navArgument(Destinations.AddFavoritePerformerCollectorArg) { type = NavType.IntType },
                    ),
                ) {
                    AddFavoritePerformerScreen(
                        onBack = {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(Destinations.RefreshCollectorDetailKey, true)
                            navController.popBackStack()
                        },
                    )
                }
                composable(
                    route = Destinations.BandDetail,
                    arguments = listOf(navArgument(Destinations.BandDetailArg) { type = NavType.IntType }),
                ) { entry ->
                    val bandId = checkNotNull(entry.arguments?.getInt(Destinations.BandDetailArg)) {
                        "Falta argumento ${Destinations.BandDetailArg} en ${Destinations.BandDetail}"
                    }
                    val refreshFlag by entry.savedStateHandle
                        .getStateFlow(Destinations.RefreshBandDetailKey, false)
                        .collectAsStateWithLifecycle()
                    BandDetailScreen(
                        bandId = bandId,
                        onBack = { navController.popBackStack() },
                        onMusicianClick = { id -> navController.navigate(Destinations.artistDetail(id)) },
                        onAddMusicians = {
                            navController.navigate(Destinations.addMusiciansToBand(bandId))
                        },
                        onAddAlbum = {
                            navController.navigate(Destinations.addAlbumToBand(bandId))
                        },
                        onAwardPrize = {
                            navController.navigate(Destinations.addPrizeToBand(bandId))
                        },
                        refreshKey = refreshFlag,
                        onRefreshHandled = {
                            entry.savedStateHandle[Destinations.RefreshBandDetailKey] = false
                        },
                    )
                }
                composable(
                    route = Destinations.AddMusiciansToBand,
                    arguments = listOf(navArgument(Destinations.AddMusiciansBandArg) { type = NavType.IntType }),
                ) {
                    AddMusiciansToBandScreen(
                        onBack = {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(Destinations.RefreshBandDetailKey, true)
                            navController.popBackStack()
                        },
                    )
                }
                composable(
                    route = Destinations.AddAlbumToBand,
                    arguments = listOf(navArgument(Destinations.AddAlbumBandArg) { type = NavType.IntType }),
                ) {
                    AddAlbumToBandScreen(
                        onBack = {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(Destinations.RefreshBandDetailKey, true)
                            navController.popBackStack()
                        },
                    )
                }
                composable(
                    route = Destinations.AddPrizeToBand,
                    arguments = listOf(navArgument(Destinations.AddPrizeBandArg) { type = NavType.IntType }),
                ) {
                    AddPrizeToBandScreen(
                        onBack = {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(Destinations.RefreshBandDetailKey, true)
                            navController.popBackStack()
                        },
                    )
                }
                composable(
                    route = Destinations.AddPrizeToMusician,
                    arguments = listOf(navArgument(Destinations.AddPrizeMusicianArg) { type = NavType.IntType }),
                ) {
                    AddPrizeToMusicianScreen(
                        onBack = {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(Destinations.RefreshMusicianDetailKey, true)
                            navController.popBackStack()
                        },
                    )
                }
                composable(Destinations.Prizes) { entry ->
                    val refreshFlag by entry.savedStateHandle
                        .getStateFlow(Destinations.RefreshPrizesKey, false)
                        .collectAsStateWithLifecycle()
                    PrizeListScreen(
                        onCreatePrize = { navController.navigate(Destinations.CreatePrize) },
                        refreshKey = refreshFlag,
                        onRefreshHandled = {
                            entry.savedStateHandle[Destinations.RefreshPrizesKey] = false
                        },
                    )
                }
                composable(Destinations.CreatePrize) {
                    CreatePrizeScreen(
                        onBack = { navController.popBackStack() },
                        onSuccess = {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(Destinations.RefreshPrizesKey, true)
                            navController.popBackStack()
                        },
                    )
                }
            }
        }
    }
}
