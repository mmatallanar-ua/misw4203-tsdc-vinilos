package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.misw4203.vinilos.MainDispatcherRule
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.AlbumDetail
import com.misw4203.vinilos.domain.model.Track
import com.misw4203.vinilos.domain.repository.AlbumRepository
import com.misw4203.vinilos.domain.usecase.GetAlbumDetailUseCase
import com.misw4203.vinilos.presentation.navigation.Destinations
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
class AlbumDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeAlbumRepository : AlbumRepository {
        var detailResult: Result<AlbumDetail> = Result.success(sampleDetail())
        override suspend fun getAlbums(): List<Album> = emptyList()
        override suspend fun getAlbumById(id: Long): AlbumDetail = detailResult.getOrThrow()
    }

    private fun buildViewModel(
        repo: FakeAlbumRepository,
        albumId: Long = 1L,
    ): AlbumDetailViewModel {
        val handle = SavedStateHandle(mapOf(Destinations.AlbumDetailArg to albumId))
        return AlbumDetailViewModel(GetAlbumDetailUseCase(repo), handle)
    }

    @Test
    fun `starts in Loading then emits Success with album data`() = runTest {
        val repo = FakeAlbumRepository()
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(AlbumDetailUiState.Loading, awaitItem())
            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state is AlbumDetailUiState.Success)
            assertEquals("Buscando América", (state as AlbumDetailUiState.Success).album.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits NotFound when repository throws 404 HttpException`() = runTest {
        val httpError = HttpException(
            Response.error<Any>(404, "".toResponseBody("text/plain".toMediaType()))
        )
        val repo = FakeAlbumRepository().apply { detailResult = Result.failure(httpError) }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(AlbumDetailUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(AlbumDetailUiState.NotFound, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits server Error when repository throws non-404 HttpException`() = runTest {
        val httpError = HttpException(
            Response.error<Any>(500, "".toResponseBody("text/plain".toMediaType()))
        )
        val repo = FakeAlbumRepository().apply { detailResult = Result.failure(httpError) }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(AlbumDetailUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(AlbumDetailUiState.Error(isNetworkError = false), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits network Error when repository throws IOException`() = runTest {
        val repo = FakeAlbumRepository().apply {
            detailResult = Result.failure(IOException("no connection"))
        }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(AlbumDetailUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(AlbumDetailUiState.Error(isNetworkError = true), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry recovers from Error to Success`() = runTest {
        val repo = FakeAlbumRepository().apply {
            detailResult = Result.failure(IOException("offline"))
        }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(AlbumDetailUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(AlbumDetailUiState.Error(isNetworkError = true), awaitItem())

            repo.detailResult = Result.success(sampleDetail())
            viewModel.retry()

            assertEquals(AlbumDetailUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertTrue(awaitItem() is AlbumDetailUiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Success state exposes tracks from album`() = runTest {
        val repo = FakeAlbumRepository()
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            awaitItem()
            advanceUntilIdle()
            val state = awaitItem() as AlbumDetailUiState.Success
            assertEquals(2, state.album.tracks.size)
            assertEquals("Decisiones", state.album.tracks[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private fun sampleDetail() = AlbumDetail(
    id = 1L,
    name = "Buscando América",
    coverUrl = "https://example.com/cover.jpg",
    artistName = "Rubén Blades",
    releaseDate = "1984-01-01T00:00:00.000Z",
    genre = "Salsa",
    recordLabel = "Elektra",
    description = "Álbum conceptual de salsa política.",
    tracks = listOf(
        Track(id = 1L, name = "Decisiones", duration = "5:30"),
        Track(id = 2L, name = "Desapariciones", duration = "6:10"),
    ),
)
