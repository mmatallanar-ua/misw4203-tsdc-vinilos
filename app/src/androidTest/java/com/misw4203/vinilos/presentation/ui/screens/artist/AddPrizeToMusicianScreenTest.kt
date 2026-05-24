package com.misw4203.vinilos.presentation.ui.screens.artist

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import com.misw4203.vinilos.R
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.Musician
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.model.Prize
import com.misw4203.vinilos.domain.repository.MusicianRepository
import com.misw4203.vinilos.domain.repository.PrizeRepository
import com.misw4203.vinilos.domain.usecase.AddPrizeToMusicianUseCase
import com.misw4203.vinilos.domain.usecase.GetMusicianDetailUseCase
import com.misw4203.vinilos.domain.usecase.GetPrizesUseCase
import com.misw4203.vinilos.presentation.navigation.Destinations
import com.misw4203.vinilos.presentation.viewmodel.AddPrizeToMusicianViewModel
import org.junit.Rule
import org.junit.Test

class AddPrizeToMusicianScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val musician = Musician(
        id = 1,
        name = "Gustavo Cerati",
        image = "",
        description = "",
        birthDate = "1959-08-11T00:00:00.000Z",
        albums = listOf(Album(1L, "Soda Stereo", "", "", "1984", "")),
        prizes = emptyList(),
    )

    private val prizes = listOf(
        Prize(10, "Grammy Latino", "Descripción 1", "Academia Latina"),
        Prize(11, "Premio Konex", "Descripción 2", "Fundación Konex"),
    )

    private fun buildVm(
        musician: Musician = this.musician,
        prizes: List<Prize> = this.prizes,
        addPrize: suspend (Int, Int, String) -> Unit = { _, _, _ -> },
    ): AddPrizeToMusicianViewModel {
        val musicianRepo = object : MusicianRepository {
            override suspend fun getMusicians(): List<MusicianSummary> = error("not used")
            override suspend fun getMusicianDetail(id: Int): Musician = musician
            override suspend fun addAlbumToMusician(musicianId: Int, albumId: Long) = Unit
            override suspend fun addPrizeToMusician(mId: Int, prizeId: Int, date: String) =
                addPrize(mId, prizeId, date)
        }
        val prizeRepo = object : PrizeRepository {
            override suspend fun getPrizes(): List<Prize> = prizes
            override suspend fun createPrize(name: String, description: String, organization: String): Prize =
                error("not used")
        }
        return AddPrizeToMusicianViewModel(
            getMusicianDetail = GetMusicianDetailUseCase(musicianRepo),
            getPrizes = GetPrizesUseCase(prizeRepo),
            addPrizeToMusician = AddPrizeToMusicianUseCase(musicianRepo),
            savedStateHandle = SavedStateHandle(mapOf(Destinations.AddPrizeMusicianArg to 1)),
        )
    }

    // CA01 — prize list and add button visible after loading
    @Test
    fun rendersBackButtonAndPrizeListAfterLoading() {
        val vm = buildVm()

        composeTestRule.setContent { MaterialTheme { AddPrizeToMusicianScreen(onBack = {}, viewModel = vm) } }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("available_prize_10").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("add_prize_musician_back").assertIsDisplayed()
        composeTestRule.onNodeWithTag("available_prize_10").assertIsDisplayed()
        composeTestRule.onNodeWithTag("available_prize_11").assertIsDisplayed()
    }

    // CA08 — search filters prizes
    @Test
    fun searchFiltersPrizesByName() {
        val vm = buildVm()

        composeTestRule.setContent { MaterialTheme { AddPrizeToMusicianScreen(onBack = {}, viewModel = vm) } }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("available_prize_10").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("add_prize_musician_search").performTextInput("konex")
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("available_prize_10").fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNodeWithTag("available_prize_11").assertIsDisplayed()
    }

    // CA05 — submit button disabled when no prize selected
    @Test
    fun submitButtonDisabledWhenNoPrizeSelected() {
        val vm = buildVm()

        composeTestRule.setContent { MaterialTheme { AddPrizeToMusicianScreen(onBack = {}, viewModel = vm) } }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("add_prize_musician_submit").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("add_prize_musician_submit").assertIsNotEnabled()
    }

    // CA05 — submit enabled when prize selected and valid date entered
    @Test
    fun submitEnabledWhenPrizeSelectedAndValidDate() {
        val vm = buildVm()

        composeTestRule.setContent { MaterialTheme { AddPrizeToMusicianScreen(onBack = {}, viewModel = vm) } }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("available_prize_10").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("available_prize_10").performClick()
        vm.onDateSelected(1577836800000L) // 2020-01-01 UTC
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("add_prize_musician_submit").assertIsEnabled()
    }

    // CA06 — future date shows validation error
    @Test
    fun futureDateShowsValidationError() {
        val vm = buildVm()

        composeTestRule.setContent { MaterialTheme { AddPrizeToMusicianScreen(onBack = {}, viewModel = vm) } }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("available_prize_10").fetchSemanticsNodes().isNotEmpty()
        }
        vm.onDateSelected(32503680000000L) // year 2999
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.add_prize_musician_date_error_future)
        ).assertIsDisplayed()
    }

    // CA04 — successful add shows snackbar
    @Test
    fun successfulAddShowsSnackbar() {
        val vm = buildVm()

        composeTestRule.setContent { MaterialTheme { AddPrizeToMusicianScreen(onBack = {}, viewModel = vm) } }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("available_prize_10").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("available_prize_10").performClick()
        vm.onDateSelected(1577836800000L) // 2020-01-01 UTC
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("add_prize_musician_submit").performClick()

        val successText = composeTestRule.activity.getString(
            R.string.add_prize_musician_success, "Grammy Latino"
        )
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(successText).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(successText).assertIsDisplayed()
    }

    // CA09 — network error shows retry
    @Test
    fun networkErrorShowsRetryOption() {
        val musicianRepo = object : MusicianRepository {
            override suspend fun getMusicians(): List<MusicianSummary> = error("not used")
            override suspend fun getMusicianDetail(id: Int): Musician = throw java.io.IOException("offline")
            override suspend fun addAlbumToMusician(musicianId: Int, albumId: Long) = Unit
            override suspend fun addPrizeToMusician(mId: Int, prizeId: Int, date: String) = Unit
        }
        val prizeRepo = object : PrizeRepository {
            override suspend fun getPrizes(): List<Prize> = prizes
            override suspend fun createPrize(name: String, description: String, organization: String): Prize =
                error("not used")
        }
        val vm = AddPrizeToMusicianViewModel(
            getMusicianDetail = GetMusicianDetailUseCase(musicianRepo),
            getPrizes = GetPrizesUseCase(prizeRepo),
            addPrizeToMusician = AddPrizeToMusicianUseCase(musicianRepo),
            savedStateHandle = SavedStateHandle(mapOf(Destinations.AddPrizeMusicianArg to 1)),
        )

        composeTestRule.setContent { MaterialTheme { AddPrizeToMusicianScreen(onBack = {}, viewModel = vm) } }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(
                composeTestRule.activity.getString(R.string.action_retry)
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.action_retry)
        ).assertIsDisplayed()
    }

    // CA12 — empty prizes state shown on artist detail
    @Test
    fun emptyPrizesStateShownOnArtistDetail() {
        val vm = buildVm()

        composeTestRule.setContent { MaterialTheme { AddPrizeToMusicianScreen(onBack = {}, viewModel = vm) } }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("add_prize_musician_submit").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.add_prize_musician_empty_prizes)
        ).assertIsDisplayed()
    }
}
