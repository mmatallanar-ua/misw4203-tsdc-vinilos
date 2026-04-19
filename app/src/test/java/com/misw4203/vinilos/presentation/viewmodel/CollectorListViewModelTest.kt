package com.misw4203.vinilos.presentation.viewmodel

import app.cash.turbine.test
import com.misw4203.vinilos.MainDispatcherRule
import com.misw4203.vinilos.domain.model.Collector
import com.misw4203.vinilos.domain.model.CollectorSummary
import com.misw4203.vinilos.domain.repository.CollectorRepository
import com.misw4203.vinilos.domain.usecase.GetCollectorsUseCase
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
class CollectorListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeCollectorRepository : CollectorRepository {
        var nextResult: Result<List<CollectorSummary>> = Result.success(emptyList())
        var callCount = 0
        override suspend fun getCollectors(): List<CollectorSummary> {
            callCount++
            return nextResult.getOrThrow()
        }
        override suspend fun getCollectorById(id: Int): Collector = error("not used")
    }

    private fun buildViewModel(repo: FakeCollectorRepository) =
        CollectorListViewModel(GetCollectorsUseCase(repo))

    private fun sample() = listOf(
        CollectorSummary(100, "Manolo", "m@a.co", "3500"),
        CollectorSummary(101, "Jaime", "j@a.co", "3012"),
    )

    @Test
    fun `starts in Loading then emits Success when repository returns collectors`() = runTest {
        val repo = FakeCollectorRepository().apply { nextResult = Result.success(sample()) }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(CollectorListUiState.Loading, awaitItem())
            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state is CollectorListUiState.Success)
            assertEquals(2, (state as CollectorListUiState.Success).collectors.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Empty when repository returns empty list`() = runTest {
        val repo = FakeCollectorRepository().apply { nextResult = Result.success(emptyList()) }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(CollectorListUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(CollectorListUiState.Empty, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits network Error when repository throws IOException`() = runTest {
        val repo = FakeCollectorRepository().apply {
            nextResult = Result.failure(IOException("offline"))
        }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(CollectorListUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(CollectorListUiState.Error(isNetworkError = true), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits server Error when repository throws HttpException`() = runTest {
        val httpError = HttpException(
            Response.error<Any>(500, "".toResponseBody("text/plain".toMediaType()))
        )
        val repo = FakeCollectorRepository().apply { nextResult = Result.failure(httpError) }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(CollectorListUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(CollectorListUiState.Error(isNetworkError = false), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry re-invokes repository and recovers to Success`() = runTest {
        val repo = FakeCollectorRepository().apply {
            nextResult = Result.failure(IOException("offline"))
        }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(CollectorListUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(CollectorListUiState.Error(isNetworkError = true), awaitItem())

            repo.nextResult = Result.success(sample())
            viewModel.retry()

            assertEquals(CollectorListUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertTrue(awaitItem() is CollectorListUiState.Success)
            assertEquals(2, repo.callCount)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
