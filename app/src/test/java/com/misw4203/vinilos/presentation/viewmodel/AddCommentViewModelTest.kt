package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.misw4203.vinilos.MainDispatcherRule
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.AlbumDetail
import com.misw4203.vinilos.domain.model.Comment
import com.misw4203.vinilos.domain.repository.AlbumRepository
import com.misw4203.vinilos.domain.usecase.AddCommentUseCase
import com.misw4203.vinilos.presentation.navigation.Destinations
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AddCommentViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeAlbumRepository : AlbumRepository {
        var addCommentResult: Result<Comment> = Result.success(
            Comment(id = 1L, description = "ok", rating = 5),
        )
        var lastArgs: AddCommentArgs? = null
        var callCount = 0

        override suspend fun getAlbums(): List<Album> = emptyList()
        override suspend fun getAlbumById(id: Long): AlbumDetail = error("not used")
        override suspend fun addTrack(albumId: Long, request: com.misw4203.vinilos.data.remote.dto.CreateTrackRequest) =
            error("not used")
        override suspend fun addComment(
            albumId: Long,
            description: String,
            rating: Int,
            collectorId: Int,
        ): Comment {
            callCount++
            lastArgs = AddCommentArgs(albumId, description, rating, collectorId)
            return addCommentResult.getOrThrow()
        }
    }

    private data class AddCommentArgs(
        val albumId: Long,
        val description: String,
        val rating: Int,
        val collectorId: Int,
    )

    private fun buildViewModel(
        repo: FakeAlbumRepository,
        albumId: Long = 42L,
        collectorId: Int = 100,
    ): AddCommentViewModel {
        val handle = SavedStateHandle(
            mapOf(
                Destinations.AddCommentAlbumArg to albumId,
                Destinations.AddCommentCollectorArg to collectorId,
            ),
        )
        return AddCommentViewModel(AddCommentUseCase(repo), handle)
    }

    @Test
    fun `submit with valid form posts and emits Success`() = runTest {
        val repo = FakeAlbumRepository().apply {
            addCommentResult = Result.success(
                Comment(id = 9L, description = "Excelente", rating = 5),
            )
        }
        val viewModel = buildViewModel(repo)
        viewModel.onDescriptionChange("Excelente")
        viewModel.onRatingChange(5)

        viewModel.uiState.test {
            assertEquals(AddCommentUiState.Idle, awaitItem())
            viewModel.submit()
            assertEquals(AddCommentUiState.Loading, awaitItem())
            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state is AddCommentUiState.Success)
            assertEquals(9L, (state as AddCommentUiState.Success).comment.id)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, repo.callCount)
        assertEquals(
            AddCommentArgs(albumId = 42L, description = "Excelente", rating = 5, collectorId = 100),
            repo.lastArgs,
        )
    }

    @Test
    fun `submit with empty description sets descriptionError and does not call repo`() = runTest {
        val repo = FakeAlbumRepository()
        val viewModel = buildViewModel(repo)
        viewModel.onRatingChange(4)
        // description left blank

        viewModel.submit()
        advanceUntilIdle()

        val form = viewModel.form.value
        assertEquals(FormError.EmptyDescription, form.descriptionError)
        assertEquals(null, form.ratingError)
        assertEquals(AddCommentUiState.Idle, viewModel.uiState.value)
        assertEquals(0, repo.callCount)
    }

    @Test
    fun `submit with rating zero sets ratingError and does not call repo`() = runTest {
        val repo = FakeAlbumRepository()
        val viewModel = buildViewModel(repo)
        viewModel.onDescriptionChange("Buen álbum")
        // rating left at 0

        viewModel.submit()
        advanceUntilIdle()

        val form = viewModel.form.value
        assertEquals(FormError.InvalidRating, form.ratingError)
        assertEquals(null, form.descriptionError)
        assertEquals(AddCommentUiState.Idle, viewModel.uiState.value)
        assertEquals(0, repo.callCount)
    }

    @Test
    fun `submit emits network Error when repository throws IOException`() = runTest {
        val repo = FakeAlbumRepository().apply {
            addCommentResult = Result.failure(IOException("offline"))
        }
        val viewModel = buildViewModel(repo)
        viewModel.onDescriptionChange("Buen álbum")
        viewModel.onRatingChange(3)

        viewModel.uiState.test {
            assertEquals(AddCommentUiState.Idle, awaitItem())
            viewModel.submit()
            assertEquals(AddCommentUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(AddCommentUiState.Error(isNetworkError = true), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `submit emits server Error when repository throws HttpException`() = runTest {
        val repo = FakeAlbumRepository().apply {
            addCommentResult = Result.failure(
                HttpException(Response.error<Any>(400, "".toResponseBody("text/plain".toMediaType())))
            )
        }
        val viewModel = buildViewModel(repo)
        viewModel.onDescriptionChange("Buen álbum")
        viewModel.onRatingChange(3)

        viewModel.uiState.test {
            assertEquals(AddCommentUiState.Idle, awaitItem())
            viewModel.submit()
            assertEquals(AddCommentUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(AddCommentUiState.Error(isNetworkError = false), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onRatingChange clamps values into the 0 to 5 range`() = runTest {
        val repo = FakeAlbumRepository()
        val viewModel = buildViewModel(repo)

        viewModel.onRatingChange(99)
        assertEquals(5, viewModel.form.value.rating)
        viewModel.onRatingChange(-3)
        assertEquals(0, viewModel.form.value.rating)
        viewModel.onRatingChange(3)
        assertEquals(3, viewModel.form.value.rating)
    }

    @Test
    fun `onDescriptionChange and onRatingChange clear their respective errors`() = runTest {
        val repo = FakeAlbumRepository()
        val viewModel = buildViewModel(repo)
        viewModel.submit() // both errors triggered
        advanceUntilIdle()

        assertNotNull(viewModel.form.value.descriptionError)
        assertNotNull(viewModel.form.value.ratingError)

        viewModel.onDescriptionChange("now filled")
        assertEquals(null, viewModel.form.value.descriptionError)
        assertNotNull(viewModel.form.value.ratingError)

        viewModel.onRatingChange(4)
        assertEquals(null, viewModel.form.value.ratingError)
    }

    @Test
    fun `resetError moves Error state back to Idle`() = runTest {
        val repo = FakeAlbumRepository().apply {
            addCommentResult = Result.failure(IOException("offline"))
        }
        val viewModel = buildViewModel(repo)
        viewModel.onDescriptionChange("Buen álbum")
        viewModel.onRatingChange(3)
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(AddCommentUiState.Error(isNetworkError = true), viewModel.uiState.value)

        viewModel.resetError()
        assertEquals(AddCommentUiState.Idle, viewModel.uiState.value)
    }
}
