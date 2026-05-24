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
import com.misw4203.vinilos.presentation.ui.screens.collector.AddAlbumToCollectorScreen
import com.misw4203.vinilos.presentation.ui.screens.collector.AddFavoritePerformerScreen
import com.misw4203.vinilos.presentation.ui.screens.collector.CollectorDetailScreen
import com.misw4203.vinilos.presentation.ui.screens.collector.CollectorListScreen

/** Collectors tab graph: list, detail, and the add-album/favorite-performer flows. */
fun NavGraphBuilder.collectorGraph(navController: NavController) {
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
        val refreshFlag by entry.rememberRefreshFlag(Destinations.RefreshCollectorDetailKey)
        CollectorDetailScreen(
            collectorId = collectorId,
            onBack = { navController.popBackStack() },
            onAddFavoritePerformer = {
                navController.navigate(Destinations.addFavoritePerformer(collectorId))
            },
            onAddAlbum = { navController.navigate(Destinations.addAlbumToCollector(collectorId)) },
            refreshKey = refreshFlag,
            onRefreshHandled = { entry.clearRefresh(Destinations.RefreshCollectorDetailKey) },
        )
    }
    composable(
        route = Destinations.AddAlbumToCollector,
        arguments = listOf(navArgument(Destinations.AddAlbumCollectorArg) { type = NavType.IntType }),
    ) {
        AddAlbumToCollectorScreen(
            onBack = { navController.popWithRefresh(Destinations.RefreshCollectorDetailKey) },
        )
    }
    composable(
        route = Destinations.AddFavoritePerformer,
        arguments = listOf(
            navArgument(Destinations.AddFavoritePerformerCollectorArg) { type = NavType.IntType },
        ),
    ) {
        AddFavoritePerformerScreen(
            onBack = { navController.popWithRefresh(Destinations.RefreshCollectorDetailKey) },
        )
    }
}
