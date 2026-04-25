package com.misw4203.vinilos.presentation.ui.screens.artist

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.misw4203.vinilos.R
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.Musician
import com.misw4203.vinilos.domain.model.MusicianPrize
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.repository.MusicianRepository
import com.misw4203.vinilos.domain.usecase.GetMusicianDetailUseCase
import com.misw4203.vinilos.domain.usecase.GetMusiciansUseCase
import com.misw4203.vinilos.presentation.viewmodel.MusicianDetailViewModel
import com.misw4203.vinilos.presentation.viewmodel.MusicianListViewModel
import org.junit.Rule
import org.junit.Test

class ArtistNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private class FakeRepo(
        private val musicians: List<MusicianSummary>,
        private val detail: Musician,
    ) : MusicianRepository {
        override suspend fun getMusicians(): List<MusicianSummary> = musicians
        override suspend fun getMusicianDetail(id: Int): Musician = detail
    }

    private fun sampleMusicians() = listOf(
        MusicianSummary(1, "Rubén Blades", "", "1948-07-16T00:00:00.000Z"),
        MusicianSummary(2, "Willie Colón", "", "1950-04-28T00:00:00.000Z"),
    )

    private fun sampleDetail() = Musician(
        id = 1,
        name = "Rubén Blades",
        image = "",
        description = "Cantante panameño de salsa y activista político.",
        birthDate = "1948-07-16T00:00:00.000Z",
        albums = listOf(Album(1L, "Siembra", "", "Rubén Blades", "1978", "Salsa")),
        prizes = listOf(
            MusicianPrize(1, "Grammy Latino", "Recording Academy", "Premio a la excelencia musical.", "2000-01-01T00:00:00.000Z"),
        ),
    )

    private fun launchNavGraph() {
        val repo = FakeRepo(sampleMusicians(), sampleDetail())
        val listVm = MusicianListViewModel(GetMusiciansUseCase(repo))
        val detailVm = MusicianDetailViewModel(GetMusicianDetailUseCase(repo))

        composeTestRule.setContent {
            MaterialTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "artists") {
                    composable("artists") {
                        MusicianListScreen(
                            onMusicianClick = { id -> navController.navigate("artist/$id") },
                            viewModel = listVm,
                        )
                    }
                    composable(
                        route = "artist/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType }),
                    ) { entry ->
                        val id = entry.arguments?.getInt("id") ?: 0
                        MusicianDetailScreen(
                            musicianId = id,
                            onBack = { navController.navigateUp() },
                            viewModel = detailVm,
                        )
                    }
                }
            }
        }
    }

    // --- Listado ---

    @Test
    fun artistListShowsBothMusicianNames() {
        launchNavGraph()
        composeTestRule.onNodeWithText("Rubén Blades").assertIsDisplayed()
        composeTestRule.onNodeWithText("Willie Colón").assertIsDisplayed()
    }

    // --- Navegación lista → detalle ---

    @Test
    fun tapOnArtistOpensDetailScreen() {
        launchNavGraph()
        composeTestRule.onNodeWithText("Rubén Blades").performClick()
        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.artist_detail_title)).assertIsDisplayed()
    }

    @Test
    fun tapOnArtistShowsCorrectDetailContent() {
        launchNavGraph()
        composeTestRule.onNodeWithText("Rubén Blades").performClick()
        composeTestRule.onNodeWithText("Cantante panameño de salsa y activista político.").assertIsDisplayed()
    }

    // --- Navegación detalle → lista (back) ---

    @Test
    fun backButtonOnDetailReturnsToList() {
        launchNavGraph()
        composeTestRule.onNodeWithText("Rubén Blades").performClick()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.artist_detail_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(
            composeTestRule.activity.getString(R.string.action_back)
        ).performClick()
        composeTestRule.onNodeWithText("Willie Colón").assertIsDisplayed()
    }
}
