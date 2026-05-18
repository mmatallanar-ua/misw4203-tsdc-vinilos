package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.misw4203.vinilos.MainDispatcherRule
import com.misw4203.vinilos.domain.model.Musician
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.repository.MusicianRepository
import com.misw4203.vinilos.domain.usecase.GetMusicianDetailUseCase
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
class MusicianDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeMusicianRepository : MusicianRepository {
        var detailResult: Result<Musician> = Result.success(sampleMusician())
        override suspend fun getMusicians(): List<MusicianSummary> = emptyList()
        override suspend fun getMusicianDetail(id: Int): Musician = detailResult.getOrThrow()
        override suspend fun addAlbumToMusician(musicianId: Int, albumId: Int) = Unit
        override suspend fun addPrizeToMusician(musicianId: Int, prizeId: Int, premiationDate: String) = Unit
    }

    private fun buildViewModel(repo: FakeMusicianRepository, musicianId: Int = 2) =
        MusicianDetailViewModel(
            GetMusicianDetailUseCase(repo),
            SavedStateHandle(mapOf(Destinations.ArtistDetailArg to musicianId)),
        )

    @Test
    fun `starts in Loading then emits Success with musician data`() = runTest {
        val repo = FakeMusicianRepository()
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(MusicianDetailUiState.Loading, awaitItem())
            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state is MusicianDetailUiState.Success)
            assertEquals("Rubén Blades", (state as MusicianDetailUiState.Success).musician.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits NotFound on 404 HttpException`() = runTest {
        val repo = FakeMusicianRepository().apply {
            detailResult = Result.failure(
                HttpException(Response.error<Any>(404, "".toResponseBody("text/plain".toMediaType())))
            )
        }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(MusicianDetailUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(MusicianDetailUiState.NotFound, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits network Error on IOException`() = runTest {
        val repo = FakeMusicianRepository().apply {
            detailResult = Result.failure(IOException("offline"))
        }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(MusicianDetailUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(MusicianDetailUiState.Error(isNetworkError = true), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits server Error on non-404 HttpException`() = runTest {
        val repo = FakeMusicianRepository().apply {
            detailResult = Result.failure(
                HttpException(Response.error<Any>(500, "".toResponseBody("text/plain".toMediaType())))
            )
        }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(MusicianDetailUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(MusicianDetailUiState.Error(isNetworkError = false), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry recovers from Error to Success`() = runTest {
        val repo = FakeMusicianRepository().apply {
            detailResult = Result.failure(IOException("offline"))
        }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(MusicianDetailUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(MusicianDetailUiState.Error(isNetworkError = true), awaitItem())

            repo.detailResult = Result.success(sampleMusician())
            viewModel.retry()

            assertEquals(MusicianDetailUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertTrue(awaitItem() is MusicianDetailUiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh reloads the musician`() = runTest {
        val repo = FakeMusicianRepository().apply {
            detailResult = Result.failure(IOException("offline"))
        }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(MusicianDetailUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(MusicianDetailUiState.Error(isNetworkError = true), awaitItem())

            repo.detailResult = Result.success(sampleMusician())
            viewModel.refresh()

            assertEquals(MusicianDetailUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertTrue(awaitItem() is MusicianDetailUiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // NOTE: The "different id cancels previous job" test is not applicable under the
    // SavedStateHandle pattern. A different musician id means a new navigation entry,
    // which creates a new ViewModel instance — there is no in-place reload with a
    // different id.
}

private fun sampleMusician() = Musician(
    id = 2,
    name = "Rubén Blades",
    image = "img",
    description = "desc",
    birthDate = "1948-07-16",
    albums = emptyList(),
    prizes = emptyList(),
)
