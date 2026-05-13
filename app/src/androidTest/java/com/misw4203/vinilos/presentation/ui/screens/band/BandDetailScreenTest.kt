package com.misw4203.vinilos.presentation.ui.screens.band

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.misw4203.vinilos.R
import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.repository.BandRepository
import com.misw4203.vinilos.domain.usecase.GetBandDetailUseCase
import com.misw4203.vinilos.presentation.viewmodel.BandDetailViewModel
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class BandDetailScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private class FakeRepo(val result: Result<Band>) : BandRepository {
        override suspend fun getBands(): List<BandSummary> = error("not used")
        override suspend fun getBandDetail(id: Int): Band = result.getOrThrow()
        override suspend fun addMusicianToBand(bandId: Int, musicianId: Int) = error("not used")
    }

    private fun buildVm(repo: BandRepository) = BandDetailViewModel(GetBandDetailUseCase(repo))

    @Test
    fun successWithEmptyMembersShowsEmptyMembersStateCta() {
        val band = Band(1, "Queen", "", "", "", emptyList(), emptyList())
        val vm = buildVm(FakeRepo(Result.success(band)))

        composeTestRule.setContent {
            MaterialTheme {
                BandDetailScreen(
                    bandId = 1, onBack = {}, onMusicianClick = {}, onAddMusicians = {},
                    viewModel = vm,
                )
            }
        }
        val cta = composeTestRule.activity.getString(R.string.add_first_member_cta)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasText(cta)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(cta).assertIsDisplayed()
    }

    @Test
    fun successWithMembersShowsListAndAddButton() {
        val band = Band(
            1, "Queen", "", "", "",
            members = listOf(MusicianSummary(10, "Freddie", "", "")),
            albums = emptyList(),
        )
        val vm = buildVm(FakeRepo(Result.success(band)))

        composeTestRule.setContent {
            MaterialTheme {
                BandDetailScreen(
                    bandId = 1, onBack = {}, onMusicianClick = {}, onAddMusicians = {},
                    viewModel = vm,
                )
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasText("Freddie")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Freddie").assertIsDisplayed()
        composeTestRule.onNodeWithTag("band_detail_add_musicians").assertIsDisplayed()
    }

    @Test
    fun emptyCtaInvokesOnAddMusicians() {
        var clicked = false
        val band = Band(1, "Queen", "", "", "", emptyList(), emptyList())
        val vm = buildVm(FakeRepo(Result.success(band)))

        composeTestRule.setContent {
            MaterialTheme {
                BandDetailScreen(
                    bandId = 1, onBack = {}, onMusicianClick = {},
                    onAddMusicians = { clicked = true },
                    viewModel = vm,
                )
            }
        }
        val cta = composeTestRule.activity.getString(R.string.add_first_member_cta)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasText(cta)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(cta).performClick()
        assertEquals(true, clicked)
    }

    @Test
    fun errorStateShowsRetry() {
        val vm = buildVm(FakeRepo(Result.failure(java.io.IOException("offline"))))

        composeTestRule.setContent {
            MaterialTheme {
                BandDetailScreen(
                    bandId = 1, onBack = {}, onMusicianClick = {}, onAddMusicians = {},
                    viewModel = vm,
                )
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText(
                composeTestRule.activity.getString(com.misw4203.vinilos.R.string.action_retry)
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(com.misw4203.vinilos.R.string.action_retry)
        ).assertIsDisplayed()
    }

    @Test
    fun notFoundShowsMessage() {
        val http404 = HttpException(Response.error<Any>(404, "".toResponseBody("text/plain".toMediaType())))
        val vm = buildVm(FakeRepo(Result.failure(http404)))

        composeTestRule.setContent {
            MaterialTheme {
                BandDetailScreen(
                    bandId = 1, onBack = {}, onMusicianClick = {}, onAddMusicians = {},
                    viewModel = vm,
                )
            }
        }
        val title = composeTestRule.activity.getString(R.string.band_not_found_title)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasText(title)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }
}
