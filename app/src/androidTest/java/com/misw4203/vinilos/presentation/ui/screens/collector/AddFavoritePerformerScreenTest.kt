package com.misw4203.vinilos.presentation.ui.screens.collector

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import com.misw4203.vinilos.R
import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.model.CollectorDetail
import com.misw4203.vinilos.domain.model.CollectorSummary
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.model.Performer
import com.misw4203.vinilos.domain.repository.BandRepository
import com.misw4203.vinilos.domain.repository.CollectorRepository
import com.misw4203.vinilos.domain.repository.MusicianRepository
import com.misw4203.vinilos.domain.usecase.AddFavoriteBandUseCase
import com.misw4203.vinilos.domain.usecase.AddFavoriteMusicianUseCase
import com.misw4203.vinilos.domain.usecase.GetBandsUseCase
import com.misw4203.vinilos.domain.usecase.GetCollectorDetailUseCase
import com.misw4203.vinilos.domain.usecase.GetMusiciansUseCase
import com.misw4203.vinilos.presentation.navigation.Destinations
import com.misw4203.vinilos.presentation.viewmodel.AddFavoritePerformerViewModel
import org.junit.Rule
import org.junit.Test

class AddFavoritePerformerScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private class FakeMusicianRepo(val list: List<MusicianSummary>) : MusicianRepository {
        override suspend fun getMusicians() = list
        override suspend fun getMusicianDetail(id: Int) = error("not used")
        override suspend fun addAlbumToMusician(musicianId: Int, albumId: Long) = error("not used")
        override suspend fun addPrizeToMusician(musicianId: Int, prizeId: Int, premiationDate: String) = error("not used")
    }

    private class FakeBandRepo(val list: List<BandSummary>) : BandRepository {
        override suspend fun getBands(): List<BandSummary> = list
        override suspend fun getBandDetail(id: Int): Band = error("not used")
        override suspend fun addMusicianToBand(bandId: Int, musicianId: Int) = error("not used")
        override suspend fun addAlbumToBand(bandId: Int, albumId: Long) {}
        override suspend fun addPrizeToBand(bandId: Int, prizeId: Int, premiationDate: String) {}
    }

    private class FakeCollectorRepo(val detail: CollectorDetail) : CollectorRepository {
        val addedMusicians = mutableListOf<Pair<Int, Int>>()
        val addedBands = mutableListOf<Pair<Int, Int>>()
        override suspend fun getCollectors(): List<CollectorSummary> = error("not used")
        override suspend fun getCollectorDetail(id: Int): CollectorDetail = detail
        override suspend fun addAlbumToCollector(
            collectorId: Int,
            albumId: Long,
            price: Double,
            status: String
        ) = error("not used")

        override suspend fun addFavoriteMusician(collectorId: Int, musicianId: Int) {
            addedMusicians += collectorId to musicianId
        }
        override suspend fun addFavoriteBand(collectorId: Int, bandId: Int) {
            addedBands += collectorId to bandId
        }
        override suspend fun removeFavoriteMusician(collectorId: Int, musicianId: Int) {}
        override suspend fun removeFavoriteBand(collectorId: Int, bandId: Int) {}
        override suspend fun removeAlbumFromCollector(collectorId: Int, albumId: Long) {}
    }

    private fun buildVm(
        musicianCatalog: List<MusicianSummary>,
        bandCatalog: List<BandSummary>,
        currentFavorites: List<Performer>,
        collectorId: Int = 100,
    ): AddFavoritePerformerViewModel {
        val detail = CollectorDetail(
            id = collectorId,
            name = "Manolo Bellon",
            telephone = "3001234567",
            email = "manolo@test.com",
            description = "",
            collectorAlbums = emptyList(),
            favoritePerformers = currentFavorites,
            comments = emptyList(),
        )
        val musicianRepo = FakeMusicianRepo(musicianCatalog)
        val bandRepo = FakeBandRepo(bandCatalog)
        val collectorRepo = FakeCollectorRepo(detail)
        return AddFavoritePerformerViewModel(
            getMusicians = GetMusiciansUseCase(musicianRepo),
            getBands = GetBandsUseCase(bandRepo),
            getCollectorDetail = GetCollectorDetailUseCase(collectorRepo),
            addFavoriteMusician = AddFavoriteMusicianUseCase(collectorRepo),
            addFavoriteBand = AddFavoriteBandUseCase(collectorRepo),
            savedStateHandle = SavedStateHandle(
                mapOf(Destinations.AddFavoritePerformerCollectorArg to collectorId),
            ),
        )
    }

    @Test
    fun musiciansTabRendersAvailableExcludingFavorites() {
        val musicians = listOf(
            MusicianSummary(10, "Rubén Blades", "", ""),
            MusicianSummary(11, "Shakira", "", ""),
        )
        val favorites = listOf(Performer(10L, "Rubén Blades", ""))
        val vm = buildVm(musicians, emptyList(), favorites)

        composeTestRule.setContent {
            MaterialTheme { AddFavoritePerformerScreen(onBack = {}, viewModel = vm) }
        }

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithTag("available_favorite_performer_11")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("available_favorite_performer_11").assertIsDisplayed()
        assert(
            composeTestRule
                .onAllNodesWithTag("available_favorite_performer_10")
                .fetchSemanticsNodes().isEmpty(),
        )
        composeTestRule.onNodeWithTag("current_favorite_performer_10").assertIsDisplayed()
    }

    @Test
    fun bandsTabShowsAvailableBandsWhenSelected() {
        val bands = listOf(
            BandSummary(20, "Queen", ""),
            BandSummary(21, "The Beatles", ""),
        )
        val vm = buildVm(emptyList(), bands, emptyList())

        composeTestRule.setContent {
            MaterialTheme { AddFavoritePerformerScreen(onBack = {}, viewModel = vm) }
        }

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithTag("add_favorite_performer_tab_bands")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("add_favorite_performer_tab_bands").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithTag("available_favorite_band_20")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("available_favorite_band_20").assertIsDisplayed()
        composeTestRule.onNodeWithTag("available_favorite_band_21").assertIsDisplayed()
    }

    @Test
    fun searchFiltersMusiciansTab() {
        val musicians = listOf(
            MusicianSummary(10, "Rubén Blades", "", ""),
            MusicianSummary(11, "Shakira", "", ""),
        )
        val vm = buildVm(musicians, emptyList(), emptyList())

        composeTestRule.setContent {
            MaterialTheme { AddFavoritePerformerScreen(onBack = {}, viewModel = vm) }
        }
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithTag("available_favorite_performer_10")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("add_favorite_performer_search").performTextInput("shakira")
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithTag("available_favorite_performer_10")
                .fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNodeWithTag("available_favorite_performer_11").assertIsDisplayed()
    }

    @Test
    fun tappingAddBandMovesItToCurrentList() {
        val bands = listOf(BandSummary(20, "Queen", ""))
        val vm = buildVm(emptyList(), bands, emptyList())

        composeTestRule.setContent {
            MaterialTheme { AddFavoritePerformerScreen(onBack = {}, viewModel = vm) }
        }
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithTag("add_favorite_performer_tab_bands")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("add_favorite_performer_tab_bands").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithTag("available_favorite_band_20")
                .fetchSemanticsNodes().isNotEmpty()
        }
        // Click directo sobre el "+" via contentDescription (clicar el Row no llega
        // al boton porque el centro del Row cae sobre el texto, no sobre el icono).
        val addCd = composeTestRule.activity.getString(R.string.cd_add_band_to_favorites, "Queen")
        composeTestRule.onNodeWithContentDescription(addCd).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithTag("available_favorite_band_20")
                .fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNodeWithTag("current_favorite_band_20").assertIsDisplayed()
    }
}
