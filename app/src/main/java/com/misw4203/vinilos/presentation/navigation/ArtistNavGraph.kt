package com.misw4203.vinilos.presentation.navigation

import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.misw4203.vinilos.core.navigation.clearRefresh
import com.misw4203.vinilos.core.navigation.popWithRefresh
import com.misw4203.vinilos.core.navigation.rememberRefreshFlag
import com.misw4203.vinilos.presentation.ui.screens.artist.AddAlbumToMusicianScreen
import com.misw4203.vinilos.presentation.ui.screens.artist.AddPrizeToMusicianScreen
import com.misw4203.vinilos.presentation.ui.screens.artist.ArtistsHubScreen
import com.misw4203.vinilos.presentation.ui.screens.artist.MusicianDetailScreen
import com.misw4203.vinilos.presentation.ui.screens.band.AddAlbumToBandScreen
import com.misw4203.vinilos.presentation.ui.screens.band.AddMusiciansToBandScreen
import com.misw4203.vinilos.presentation.ui.screens.band.AddPrizeToBandScreen
import com.misw4203.vinilos.presentation.ui.screens.band.BandDetailScreen

/** Artists tab graph: musician + band screens (they share the Artists tab). */
fun NavGraphBuilder.artistGraph(navController: NavController) {
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
        val refreshFlag by backStackEntry.rememberRefreshFlag(Destinations.RefreshMusicianDetailKey)
        MusicianDetailScreen(
            onBack = { navController.navigateUp() },
            onAddAlbum = { navController.navigate(Destinations.addAlbumToMusician(id)) },
            onAddPrize = { navController.navigate(Destinations.addPrizeToMusician(id)) },
            refreshKey = refreshFlag,
            onRefreshHandled = { backStackEntry.clearRefresh(Destinations.RefreshMusicianDetailKey) },
        )
    }
    composable(
        route = Destinations.AddAlbumToMusician,
        arguments = listOf(navArgument(Destinations.AddAlbumMusicianArg) { type = NavType.IntType }),
    ) {
        AddAlbumToMusicianScreen(
            onBack = { navController.popWithRefresh(Destinations.RefreshMusicianDetailKey) },
        )
    }
    composable(
        route = Destinations.BandDetail,
        arguments = listOf(navArgument(Destinations.BandDetailArg) { type = NavType.IntType }),
    ) { entry ->
        val bandId = checkNotNull(entry.arguments?.getInt(Destinations.BandDetailArg)) {
            "Falta argumento ${Destinations.BandDetailArg} en ${Destinations.BandDetail}"
        }
        val refreshFlag by entry.rememberRefreshFlag(Destinations.RefreshBandDetailKey)
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
            onRefreshHandled = { entry.clearRefresh(Destinations.RefreshBandDetailKey) },
        )
    }
    composable(
        route = Destinations.AddMusiciansToBand,
        arguments = listOf(navArgument(Destinations.AddMusiciansBandArg) { type = NavType.IntType }),
    ) {
        AddMusiciansToBandScreen(
            onBack = { navController.popWithRefresh(Destinations.RefreshBandDetailKey) },
        )
    }
    composable(
        route = Destinations.AddAlbumToBand,
        arguments = listOf(navArgument(Destinations.AddAlbumBandArg) { type = NavType.IntType }),
    ) {
        AddAlbumToBandScreen(
            onBack = { navController.popWithRefresh(Destinations.RefreshBandDetailKey) },
        )
    }
    composable(
        route = Destinations.AddPrizeToBand,
        arguments = listOf(navArgument(Destinations.AddPrizeBandArg) { type = NavType.IntType }),
    ) {
        AddPrizeToBandScreen(
            onBack = { navController.popWithRefresh(Destinations.RefreshBandDetailKey) },
        )
    }
    composable(
        route = Destinations.AddPrizeToMusician,
        arguments = listOf(navArgument(Destinations.AddPrizeMusicianArg) { type = NavType.IntType }),
    ) {
        AddPrizeToMusicianScreen(
            onBack = { navController.popWithRefresh(Destinations.RefreshMusicianDetailKey) },
        )
    }
}
