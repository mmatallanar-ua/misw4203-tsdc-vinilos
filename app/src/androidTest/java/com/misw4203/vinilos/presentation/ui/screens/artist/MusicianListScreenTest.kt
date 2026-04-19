package com.misw4203.vinilos.presentation.ui.screens.artist

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.misw4203.vinilos.R
import com.misw4203.vinilos.domain.model.Musician
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.repository.MusicianRepository
import com.misw4203.vinilos.domain.usecase.GetMusiciansUseCase
import com.misw4203.vinilos.presentation.viewmodel.MusicianListViewModel
import org.junit.Rule
import org.junit.Test

class MusicianListScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private class FakeRepo(
        private val result: Result<List<MusicianSummary>>,
    ) : MusicianRepository {
        override suspend fun getMusicians(): List<MusicianSummary> = result.getOrThrow()
        override suspend fun getMusicianDetail(id: Int): Musician = error("unused")
    }

    private fun vm(result: Result<List<MusicianSummary>>) =
        MusicianListViewModel(GetMusiciansUseCase(FakeRepo(result)))

    private fun sampleMusicians() = listOf(
        MusicianSummary(100, "Rubén Blades Bellido de Luna", "url1", "1948-07-16T00:00:00.000Z"),
        MusicianSummary(101, "Tirone José González Orama", "url2", "1976-03-09T00:00:00.000Z"),
    )

    @Test
    fun rendersMusicianNamesOnSuccess() {
        val viewModel = vm(Result.success(sampleMusicians()))
        composeTestRule.setContent {
            MaterialTheme {
                MusicianListScreen(onMusicianClick = {}, viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText("Rubén Blades Bellido de Luna").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tirone José González Orama").assertIsDisplayed()
    }

    @Test
    fun rendersBirthDateOnSuccess() {
        val viewModel = vm(Result.success(sampleMusicians()))
        composeTestRule.setContent {
            MaterialTheme {
                MusicianListScreen(onMusicianClick = {}, viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText("1948-07-16").assertIsDisplayed()
        composeTestRule.onNodeWithText("1976-03-09").assertIsDisplayed()
    }

    @Test
    fun rendersEmptyStateOnEmptyList() {
        val viewModel = vm(Result.success(emptyList()))
        composeTestRule.setContent {
            MaterialTheme {
                MusicianListScreen(onMusicianClick = {}, viewModel = viewModel)
            }
        }

        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.state_empty_title)).assertIsDisplayed()
    }

    @Test
    fun rendersSearchBarOnSuccess() {
        val viewModel = vm(Result.success(sampleMusicians()))
        composeTestRule.setContent {
            MaterialTheme {
                MusicianListScreen(onMusicianClick = {}, viewModel = viewModel)
            }
        }

        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.search_placeholder_artists)).assertIsDisplayed()
    }
}
