package com.misw4203.vinilos.presentation.ui.screens.album

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.misw4203.vinilos.R
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.AlbumDetail
import com.misw4203.vinilos.domain.repository.AlbumRepository
import com.misw4203.vinilos.domain.usecase.GetAlbumsUseCase
import com.misw4203.vinilos.presentation.viewmodel.AlbumListViewModel
import org.junit.Rule
import org.junit.Test

class AlbumListScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private class FakeRepo(
        private val result: Result<List<Album>>,
    ) : AlbumRepository {
        override suspend fun getAlbums(): List<Album> = result.getOrThrow()
        override suspend fun getAlbumById(id: Long): AlbumDetail = error("unused")
    }

    private fun vm(result: Result<List<Album>>) =
        AlbumListViewModel(GetAlbumsUseCase(FakeRepo(result)))

    @Test
    fun rendersAlbumsOnSuccess() {
        val viewModel = vm(
            Result.success(
                listOf(
                    Album(1L, "Buscando América", "", "Rubén Blades", "1984", "Salsa"),
                    Album(2L, "A Night at the Opera", "", "Queen", "1975", "Rock"),
                )
            )
        )
        composeTestRule.setContent {
            MaterialTheme {
                AlbumListScreen(onAlbumClick = {}, viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText("Buscando América").assertIsDisplayed()
        composeTestRule.onNodeWithText("A Night at the Opera").assertIsDisplayed()
    }

    @Test
    fun rendersEmptyStateOnEmptyList() {
        val viewModel = vm(Result.success(emptyList()))
        composeTestRule.setContent {
            MaterialTheme {
                AlbumListScreen(onAlbumClick = {}, viewModel = viewModel)
            }
        }
        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.state_empty_title)).assertIsDisplayed()
    }
}
