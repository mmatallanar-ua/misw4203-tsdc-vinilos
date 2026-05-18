package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.misw4203.vinilos.MainDispatcherRule
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.AlbumDetail
import com.misw4203.vinilos.domain.model.Comment
import com.misw4203.vinilos.domain.model.Performer
import com.misw4203.vinilos.domain.model.Track
import com.misw4203.vinilos.domain.repository.AlbumRepository
import com.misw4203.vinilos.domain.usecase.GetAlbumDetailUseCase
import com.misw4203.vinilos.domain.usecase.RemoveCommentUseCase
import com.misw4203.vinilos.domain.usecase.RemoveTrackUseCase
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
        var removeError: Throwable? = null
        val removedTracks = mutableListOf<Pair<Long, Long>>()
        val removedComments = mutableListOf<Pair<Long, Long>>()
        override suspend fun getAlbums(): List<Album> = emptyList()
        override suspend fun getAlbumById(id: Long): AlbumDetail = detailResult.getOrThrow()
        override suspend fun addTrack(albumId: Long, request: com.misw4203.vinilos.data.remote.dto.CreateTrackRequest) =
            com.misw4203.vinilos.domain.model.Track(1L, request.name, request.duration)
        override suspend fun createAlbum(input: com.misw4203.vinilos.domain.model.CreateAlbumInput): Album = error("not used")
        override suspend fun addComment(
            albumId: Long,
            description: String,
            rating: Int,
            collectorId: Int,
        ): Comment = Comment(0L, description, rating)

        override suspend fun removeTrack(albumId: Long, trackId: Long) {
            removeError?.let { throw it }
            removedTracks += albumId to trackId
        }

        override suspend fun removeComment(albumId: Long, commentId: Long) {
            removeError?.let { throw it }
            removedComments += albumId to commentId
        }
    }

    private fun buildViewModel(
        repo: FakeAlbumRepository,
        albumId: Long = 1L,
    ): AlbumDetailViewModel {
        val handle = SavedStateHandle(mapOf(Destinations.AlbumDetailArg to albumId))
        return AlbumDetailViewModel(
            GetAlbumDetailUseCase(repo),
            RemoveTrackUseCase(repo),
            RemoveCommentUseCase(repo),
            handle,
        )
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

    @Test
    fun `removeTrack calls repository and removes optimistically`() = runTest {
        val repo = FakeAlbumRepository()
        val viewModel = buildViewModel(repo)
        advanceUntilIdle()
        val track = (viewModel.uiState.value as AlbumDetailUiState.Success).album.tracks[0]

        viewModel.events.test {
            viewModel.removeTrack(track)
            advanceUntilIdle()
            assertEquals(AlbumDetailEvent.Removed("Decisiones"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(listOf(1L to 1L), repo.removedTracks)
        val state = viewModel.uiState.value as AlbumDetailUiState.Success
        assertEquals(1, state.album.tracks.size)
        assertTrue(state.album.tracks.none { it.id == 1L })
    }

    @Test
    fun `removeComment calls repository and removes optimistically`() = runTest {
        val repo = FakeAlbumRepository()
        val viewModel = buildViewModel(repo)
        advanceUntilIdle()
        val comment = (viewModel.uiState.value as AlbumDetailUiState.Success).album.comments[0]

        viewModel.removeComment(comment)
        advanceUntilIdle()

        assertEquals(listOf(1L to 1L), repo.removedComments)
        val state = viewModel.uiState.value as AlbumDetailUiState.Success
        assertTrue(state.album.comments.isEmpty())
    }

    @Test
    fun `failed track removal restores state and emits network failure`() = runTest {
        val repo = FakeAlbumRepository().apply { removeError = IOException("offline") }
        val viewModel = buildViewModel(repo)
        advanceUntilIdle()
        val track = (viewModel.uiState.value as AlbumDetailUiState.Success).album.tracks[0]

        viewModel.events.test {
            viewModel.removeTrack(track)
            advanceUntilIdle()
            assertEquals(
                AlbumDetailEvent.RemoveFailed(isNetworkError = true),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }

        val state = viewModel.uiState.value as AlbumDetailUiState.Success
        assertEquals(2, state.album.tracks.size)
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
    performers = listOf(
        Performer(id = 1L, name = "Rubén Blades", imageUrl = ""),
    ),
    comments = listOf(
        Comment(id = 1L, description = "The most relevant album of Ruben Blades", rating = 5),
    ),
)
