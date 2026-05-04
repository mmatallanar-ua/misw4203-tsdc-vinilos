package com.misw4203.vinilos.presentation.viewmodel

import app.cash.turbine.test
import com.misw4203.vinilos.MainDispatcherRule
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.AlbumDetail
import com.misw4203.vinilos.domain.model.CreateAlbumInput
import com.misw4203.vinilos.domain.repository.AlbumRepository
import com.misw4203.vinilos.domain.usecase.CreateAlbumUseCase
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
class CreateAlbumViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeAlbumRepository : AlbumRepository {
        var createResult: Result<Album> = Result.success(sampleAlbum())
        override suspend fun getAlbums(): List<Album> = emptyList()
        override suspend fun getAlbumById(id: Long): AlbumDetail = error("not used")
        override suspend fun addTrack(albumId: Long, request: com.misw4203.vinilos.data.remote.dto.CreateTrackRequest) =
            error("not used")
        override suspend fun addComment(albumId: Long, description: String, rating: Int, collectorId: Int): com.misw4203.vinilos.domain.model.Comment =
            error("not used")
        override suspend fun createAlbum(input: CreateAlbumInput): Album = createResult.getOrThrow()
    }

    private fun buildViewModel(repo: FakeAlbumRepository): CreateAlbumViewModel =
        CreateAlbumViewModel(CreateAlbumUseCase(repo))

    private fun sampleInput() = CreateAlbumInput(
        name = "Test Album",
        cover = "https://example.com/cover.jpg",
        releaseDate = "2024-01-15",
        description = "A test album description",
        genre = "Rock",
        recordLabel = "Sony Music",
    )

    @Test
    fun `starts in Idle state`() = runTest {
        val viewModel = buildViewModel(FakeAlbumRepository())
        assertEquals(CreateAlbumUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `submit transitions through Submitting then emits Success`() = runTest {
        val repo = FakeAlbumRepository()
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(CreateAlbumUiState.Idle, awaitItem())
            viewModel.submit(sampleInput())
            assertEquals(CreateAlbumUiState.Submitting, awaitItem())
            advanceUntilIdle()
            assertEquals(CreateAlbumUiState.Success, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `submit emits Error with network message on IOException`() = runTest {
        val repo = FakeAlbumRepository().apply {
            createResult = Result.failure(IOException("no connection"))
        }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(CreateAlbumUiState.Idle, awaitItem())
            viewModel.submit(sampleInput())
            assertEquals(CreateAlbumUiState.Submitting, awaitItem())
            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state is CreateAlbumUiState.Error)
            assertTrue((state as CreateAlbumUiState.Error).message.contains("conexión"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `submit emits Error with server code on HttpException`() = runTest {
        val httpError = HttpException(
            Response.error<Any>(422, "".toResponseBody("text/plain".toMediaType()))
        )
        val repo = FakeAlbumRepository().apply { createResult = Result.failure(httpError) }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(CreateAlbumUiState.Idle, awaitItem())
            viewModel.submit(sampleInput())
            assertEquals(CreateAlbumUiState.Submitting, awaitItem())
            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state is CreateAlbumUiState.Error)
            assertTrue((state as CreateAlbumUiState.Error).message.contains("422"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `submit emits generic Error on unexpected exception`() = runTest {
        val repo = FakeAlbumRepository().apply {
            createResult = Result.failure(RuntimeException("unexpected"))
        }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(CreateAlbumUiState.Idle, awaitItem())
            viewModel.submit(sampleInput())
            assertEquals(CreateAlbumUiState.Submitting, awaitItem())
            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state is CreateAlbumUiState.Error)
            assertTrue((state as CreateAlbumUiState.Error).message.isNotBlank())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `resetState returns to Idle after Success`() = runTest {
        val repo = FakeAlbumRepository()
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(CreateAlbumUiState.Idle, awaitItem())
            viewModel.submit(sampleInput())
            assertEquals(CreateAlbumUiState.Submitting, awaitItem())
            advanceUntilIdle()
            assertEquals(CreateAlbumUiState.Success, awaitItem())

            viewModel.resetState()
            assertEquals(CreateAlbumUiState.Idle, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `resetState returns to Idle after Error`() = runTest {
        val repo = FakeAlbumRepository().apply {
            createResult = Result.failure(IOException("offline"))
        }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(CreateAlbumUiState.Idle, awaitItem())
            viewModel.submit(sampleInput())
            assertEquals(CreateAlbumUiState.Submitting, awaitItem())
            advanceUntilIdle()
            assertTrue(awaitItem() is CreateAlbumUiState.Error)

            viewModel.resetState()
            assertEquals(CreateAlbumUiState.Idle, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private fun sampleAlbum() = Album(
    id = 99L,
    name = "Test Album",
    coverUrl = "https://example.com/cover.jpg",
    artistName = "",
    releaseYear = "2024",
    genre = "Rock",
)
