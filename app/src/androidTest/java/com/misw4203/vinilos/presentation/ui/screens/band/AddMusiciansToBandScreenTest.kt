package com.misw4203.vinilos.presentation.ui.screens.band

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import com.misw4203.vinilos.R
import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.repository.BandRepository
import com.misw4203.vinilos.domain.repository.MusicianRepository
import com.misw4203.vinilos.domain.usecase.AddMusicianToBandUseCase
import com.misw4203.vinilos.domain.usecase.GetBandDetailUseCase
import com.misw4203.vinilos.domain.usecase.GetMusiciansUseCase
import com.misw4203.vinilos.presentation.navigation.Destinations
import com.misw4203.vinilos.presentation.viewmodel.AddMusiciansToBandViewModel
import org.junit.Rule
import org.junit.Test

class AddMusiciansToBandScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private class FakeMusicianRepo(val list: List<MusicianSummary>) : MusicianRepository {
        override suspend fun getMusicians() = list
        override suspend fun getMusicianDetail(id: Int) = error("not used")
    }

    private class FakeBandRepo(val band: Band) : BandRepository {
        var added = mutableListOf<Pair<Int, Int>>()
        override suspend fun getBands(): List<BandSummary> = error("not used")
        override suspend fun getBandDetail(id: Int): Band = band
        override suspend fun addMusicianToBand(bandId: Int, musicianId: Int) {
            added += bandId to musicianId
        }
    }

    private fun buildVm(
        catalog: List<MusicianSummary>,
        currentMembers: List<MusicianSummary>,
        bandId: Int = 1,
    ): AddMusiciansToBandViewModel {
        val band = Band(bandId, "Queen", "", "", "", currentMembers, emptyList())
        val bandRepo = FakeBandRepo(band)
        return AddMusiciansToBandViewModel(
            getMusicians = GetMusiciansUseCase(FakeMusicianRepo(catalog)),
            getBandDetail = GetBandDetailUseCase(bandRepo),
            addMusicianToBand = AddMusicianToBandUseCase(bandRepo),
            savedStateHandle = SavedStateHandle(mapOf(Destinations.AddMusiciansBandArg to bandId)),
        )
    }

    @Test
    fun rendersAvailableExcludingExistingMembers() {
        val catalog = listOf(
            MusicianSummary(10, "Freddie Mercury", "", ""),
            MusicianSummary(11, "Brian May", "", ""),
        )
        val members = listOf(MusicianSummary(10, "Freddie Mercury", "", ""))
        val vm = buildVm(catalog, members)

        composeTestRule.setContent {
            MaterialTheme {
                AddMusiciansToBandScreen(onBack = {}, viewModel = vm)
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithTag("available_musician_11")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("available_musician_11").assertIsDisplayed()
        // Freddie is already a member, so should NOT be in available list
        assert(
            composeTestRule
                .onAllNodesWithTag("available_musician_10")
                .fetchSemanticsNodes().isEmpty()
        )
    }

    @Test
    fun searchFiltersList() {
        val catalog = listOf(
            MusicianSummary(10, "Freddie Mercury", "", ""),
            MusicianSummary(11, "Brian May", "", ""),
        )
        val vm = buildVm(catalog, emptyList())

        composeTestRule.setContent {
            MaterialTheme {
                AddMusiciansToBandScreen(onBack = {}, viewModel = vm)
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithTag("available_musician_10")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("add_musicians_search").performTextInput("brian")
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithTag("available_musician_10")
                .fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNodeWithTag("available_musician_11").assertIsDisplayed()
    }

    @Test
    fun addingMusicianRemovesFromAvailableList() {
        val catalog = listOf(MusicianSummary(10, "Freddie Mercury", "", ""))
        val vm = buildVm(catalog, emptyList())

        composeTestRule.setContent {
            MaterialTheme {
                AddMusiciansToBandScreen(onBack = {}, viewModel = vm)
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithTag("available_musician_10")
                .fetchSemanticsNodes().isNotEmpty()
        }
        val cd = composeTestRule.activity.getString(
            R.string.cd_add_musician_to_band, "Freddie Mercury"
        )
        composeTestRule.onNodeWithContentDescription(cd).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithTag("available_musician_10")
                .fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNodeWithTag("current_member_10").assertIsDisplayed()
    }

    @Test
    fun loadErrorShowsErrorStateWithRetry() {
        val musicianRepo = FakeMusicianRepo(emptyList())
        val bandRepo = object : BandRepository {
            override suspend fun getBands(): List<BandSummary> = error("not used")
            override suspend fun getBandDetail(id: Int): Band = throw java.io.IOException("offline")
            override suspend fun addMusicianToBand(bandId: Int, musicianId: Int) = Unit
        }
        val vm = AddMusiciansToBandViewModel(
            getMusicians = GetMusiciansUseCase(musicianRepo),
            getBandDetail = GetBandDetailUseCase(bandRepo),
            addMusicianToBand = AddMusicianToBandUseCase(bandRepo),
            savedStateHandle = SavedStateHandle(mapOf(Destinations.AddMusiciansBandArg to 1)),
        )

        composeTestRule.setContent {
            MaterialTheme {
                AddMusiciansToBandScreen(onBack = {}, viewModel = vm)
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText(
                composeTestRule.activity.getString(R.string.action_retry)
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.action_retry)
        ).assertIsDisplayed()
    }
}
