package com.misw4203.vinilos.presentation.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.misw4203.vinilos.R
import com.misw4203.vinilos.core.navigation.clearRefresh
import com.misw4203.vinilos.core.navigation.collectRefresh
import com.misw4203.vinilos.core.navigation.popWithRefresh
import com.misw4203.vinilos.core.navigation.rememberRefreshFlag
import com.misw4203.vinilos.presentation.ui.screens.album.AddCommentScreen
import com.misw4203.vinilos.presentation.ui.screens.album.AddPerformerToAlbumScreen
import com.misw4203.vinilos.presentation.ui.screens.album.AddTrackScreen
import com.misw4203.vinilos.presentation.ui.screens.album.AlbumDetailScreen
import com.misw4203.vinilos.presentation.ui.screens.album.AlbumListScreen
import com.misw4203.vinilos.presentation.ui.screens.album.CreateAlbumScreen
import com.misw4203.vinilos.presentation.viewmodel.AlbumDetailViewModel

/** Album tab graph: list, create, detail, and the add-performer/track/comment flows. */
fun NavGraphBuilder.albumGraph(navController: NavController, snackbarHostState: SnackbarHostState) {
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
            entry.collectRefresh(Destinations.TrackAddedKey) {
                viewModel.retry()
                snackbarHostState.showSnackbar(trackAddedMessage)
            }
        }
        // Comment addition: refresh only (HU09 pattern)
        val refreshFlag by entry.rememberRefreshFlag(Destinations.RefreshAlbumDetailKey)
        AlbumDetailScreen(
            albumId = albumId,
            onBack = { navController.popBackStack() },
            onAddTrack = { navController.navigate(Destinations.addTrack(albumId)) },
            onAddComment = { navController.navigate(Destinations.addComment(albumId)) },
            onAddPerformer = {
                navController.navigate(Destinations.addPerformerToAlbum(albumId))
            },
            refreshKey = refreshFlag,
            onRefreshHandled = { entry.clearRefresh(Destinations.RefreshAlbumDetailKey) },
            viewModel = viewModel,
        )
    }
    composable(
        route = Destinations.AddPerformerToAlbum,
        arguments = listOf(navArgument(Destinations.AddPerformerAlbumArg) { type = NavType.LongType }),
    ) {
        AddPerformerToAlbumScreen(
            onBack = { navController.popWithRefresh(Destinations.RefreshAlbumDetailKey) },
        )
    }
    composable(
        route = Destinations.AddTrack,
        arguments = listOf(navArgument(Destinations.AddTrackAlbumArg) { type = NavType.LongType }),
    ) {
        AddTrackScreen(
            onBack = { navController.popBackStack() },
            onSuccess = { navController.popWithRefresh(Destinations.TrackAddedKey) },
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
            onSuccess = { navController.popWithRefresh(Destinations.RefreshAlbumDetailKey) },
        )
    }
}
