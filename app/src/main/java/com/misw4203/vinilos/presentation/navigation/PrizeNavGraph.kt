package com.misw4203.vinilos.presentation.navigation

import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.misw4203.vinilos.core.navigation.clearRefresh
import com.misw4203.vinilos.core.navigation.popWithRefresh
import com.misw4203.vinilos.core.navigation.rememberRefreshFlag
import com.misw4203.vinilos.presentation.ui.screens.prize.CreatePrizeScreen
import com.misw4203.vinilos.presentation.ui.screens.prize.PrizeListScreen

/** Prizes tab graph: prize list and create-prize flow. */
fun NavGraphBuilder.prizeGraph(navController: NavController) {
    composable(Destinations.Prizes) { entry ->
        val refreshFlag by entry.rememberRefreshFlag(Destinations.RefreshPrizesKey)
        PrizeListScreen(
            onCreatePrize = { navController.navigate(Destinations.CreatePrize) },
            refreshKey = refreshFlag,
            onRefreshHandled = { entry.clearRefresh(Destinations.RefreshPrizesKey) },
        )
    }
    composable(Destinations.CreatePrize) {
        CreatePrizeScreen(
            onBack = { navController.popBackStack() },
            onSuccess = { navController.popWithRefresh(Destinations.RefreshPrizesKey) },
        )
    }
}
