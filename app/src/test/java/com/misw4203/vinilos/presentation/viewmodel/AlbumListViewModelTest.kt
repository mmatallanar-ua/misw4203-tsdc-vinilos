package com.misw4203.vinilos.presentation.viewmodel

import app.cash.turbine.test
import com.misw4203.vinilos.MainDispatcherRule
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.repository.AlbumRepository
import com.misw4203.vinilos.domain.usecase.GetAlbumsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeAlbumRepository : AlbumRepository {
        var nextResult: Result<List<Album>> = Result.success(emptyList())
        var callCount = 0
        override suspend fun getAlbums(): List<Album> {
            callCount++
            return nextResult.getOrThrow()
        }
    }

    private fun sampleAlbums() = listOf(
        Album(
            id = 100,
            name = "Buscando América",
            coverUrl = "https://example.com/a.jpg",
            artistName = "Rubén Blades",
            releaseYear = "1984",
            genre = "Salsa",
        ),
        Album(
            id = 102,
            name = "A Night at the Opera",
            coverUrl = "https://example.com/b.jpg",
            artistName = "Queen",
            releaseYear = "1975",
            genre = "Rock",
        ),
    )

    private fun buildViewModel(repo: FakeAlbumRepository): AlbumListViewModel =
        AlbumListViewModel(GetAlbumsUseCase(repo))

    @Test
    fun `starts in Loading then emits Success when repository returns albums`() = runTest {
        val repo = FakeAlbumRepository().apply { nextResult = Result.success(sampleAlbums()) }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(AlbumListUiState.Loading, awaitItem())
            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state is AlbumListUiState.Success)
            assertEquals(2, (state as AlbumListUiState.Success).albums.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Empty when repository returns empty list`() = runTest {
        val repo = FakeAlbumRepository().apply { nextResult = Result.success(emptyList()) }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(AlbumListUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(AlbumListUiState.Empty, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits network Error when repository throws IOException`() = runTest {
        val repo = FakeAlbumRepository().apply {
            nextResult = Result.failure(IOException("no connection"))
        }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(AlbumListUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(AlbumListUiState.Error(isNetworkError = true), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits server Error when repository throws HttpException`() = runTest {
        val httpError = HttpException(
            Response.error<Any>(500, "".toResponseBody("text/plain".toMediaType()))
        )
        val repo = FakeAlbumRepository().apply { nextResult = Result.failure(httpError) }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(AlbumListUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(AlbumListUiState.Error(isNetworkError = false), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry re-invokes repository and recovers to Success`() = runTest {
        val repo = FakeAlbumRepository().apply {
            nextResult = Result.failure(IOException("offline"))
        }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(AlbumListUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(AlbumListUiState.Error(isNetworkError = true), awaitItem())

            repo.nextResult = Result.success(sampleAlbums())
            viewModel.retry()

            assertEquals(AlbumListUiState.Loading, awaitItem())
            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state is AlbumListUiState.Success)
            assertEquals(2, repo.callCount)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
