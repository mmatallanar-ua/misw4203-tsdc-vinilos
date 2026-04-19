package com.misw4203.vinilos.presentation.viewmodel

import app.cash.turbine.test
import com.misw4203.vinilos.MainDispatcherRule
import com.misw4203.vinilos.domain.model.Musician
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.repository.MusicianRepository
import com.misw4203.vinilos.domain.usecase.GetMusiciansUseCase
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
class MusicianListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeMusicianRepository : MusicianRepository {
        var nextResult: Result<List<MusicianSummary>> = Result.success(emptyList())
        var callCount = 0
        override suspend fun getMusicians(): List<MusicianSummary> {
            callCount++
            return nextResult.getOrThrow()
        }
        override suspend fun getMusicianDetail(id: Int): Musician = error("not used")
    }

    private fun buildViewModel(repo: FakeMusicianRepository) =
        MusicianListViewModel(GetMusiciansUseCase(repo))

    private fun sampleMusicians() = listOf(
        MusicianSummary(100, "Rubén Blades", "url1"),
        MusicianSummary(101, "Queen", "url2"),
    )

    @Test
    fun `starts in Loading then emits Success when repository returns musicians`() = runTest {
        val repo = FakeMusicianRepository().apply {
            nextResult = Result.success(sampleMusicians())
        }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(MusicianListUiState.Loading, awaitItem())
            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state is MusicianListUiState.Success)
            assertEquals(2, (state as MusicianListUiState.Success).musicians.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Empty when repository returns empty list`() = runTest {
        val repo = FakeMusicianRepository().apply { nextResult = Result.success(emptyList()) }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(MusicianListUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(MusicianListUiState.Empty, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits network Error when repository throws IOException`() = runTest {
        val repo = FakeMusicianRepository().apply {
            nextResult = Result.failure(IOException("offline"))
        }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(MusicianListUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(MusicianListUiState.Error(isNetworkError = true), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits server Error when repository throws HttpException`() = runTest {
        val httpError = HttpException(
            Response.error<Any>(500, "".toResponseBody("text/plain".toMediaType()))
        )
        val repo = FakeMusicianRepository().apply { nextResult = Result.failure(httpError) }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(MusicianListUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(MusicianListUiState.Error(isNetworkError = false), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry re-invokes repository and recovers to Success`() = runTest {
        val repo = FakeMusicianRepository().apply {
            nextResult = Result.failure(IOException("offline"))
        }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(MusicianListUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(MusicianListUiState.Error(isNetworkError = true), awaitItem())

            repo.nextResult = Result.success(sampleMusicians())
            viewModel.retry()

            assertEquals(MusicianListUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertTrue(awaitItem() is MusicianListUiState.Success)
            assertEquals(2, repo.callCount)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
