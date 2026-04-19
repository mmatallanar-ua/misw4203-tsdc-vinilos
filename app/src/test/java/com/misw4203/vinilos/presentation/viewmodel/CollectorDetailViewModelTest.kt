package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.misw4203.vinilos.MainDispatcherRule
import com.misw4203.vinilos.domain.model.Collector
import com.misw4203.vinilos.domain.model.CollectorSummary
import com.misw4203.vinilos.domain.repository.CollectorRepository
import com.misw4203.vinilos.domain.usecase.GetCollectorDetailUseCase
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
class CollectorDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeCollectorRepository : CollectorRepository {
        var detailResult: Result<Collector> = Result.success(sample())
        override suspend fun getCollectors(): List<CollectorSummary> = emptyList()
        override suspend fun getCollectorById(id: Int): Collector = detailResult.getOrThrow()
    }

    private fun buildViewModel(
        repo: FakeCollectorRepository,
        collectorId: Int = 100,
    ): CollectorDetailViewModel {
        val handle = SavedStateHandle(mapOf(Destinations.CollectorDetailArg to collectorId))
        return CollectorDetailViewModel(GetCollectorDetailUseCase(repo), handle)
    }

    @Test
    fun `starts in Loading then emits Success with collector`() = runTest {
        val repo = FakeCollectorRepository()
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(CollectorDetailUiState.Loading, awaitItem())
            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state is CollectorDetailUiState.Success)
            assertEquals("Manolo", (state as CollectorDetailUiState.Success).collector.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits NotFound when repository throws 404 HttpException`() = runTest {
        val httpError = HttpException(
            Response.error<Any>(404, "".toResponseBody("text/plain".toMediaType()))
        )
        val repo = FakeCollectorRepository().apply { detailResult = Result.failure(httpError) }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(CollectorDetailUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(CollectorDetailUiState.NotFound, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits server Error on non-404 HttpException`() = runTest {
        val httpError = HttpException(
            Response.error<Any>(500, "".toResponseBody("text/plain".toMediaType()))
        )
        val repo = FakeCollectorRepository().apply { detailResult = Result.failure(httpError) }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(CollectorDetailUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(CollectorDetailUiState.Error(isNetworkError = false), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits network Error on IOException`() = runTest {
        val repo = FakeCollectorRepository().apply {
            detailResult = Result.failure(IOException("offline"))
        }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(CollectorDetailUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(CollectorDetailUiState.Error(isNetworkError = true), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry recovers from Error to Success`() = runTest {
        val repo = FakeCollectorRepository().apply {
            detailResult = Result.failure(IOException("offline"))
        }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(CollectorDetailUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(CollectorDetailUiState.Error(isNetworkError = true), awaitItem())

            repo.detailResult = Result.success(sample())
            viewModel.retry()

            assertEquals(CollectorDetailUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertTrue(awaitItem() is CollectorDetailUiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private fun sample() = Collector(
    id = 100,
    name = "Manolo",
    telephone = "3500",
    email = "m@a.co",
    comments = emptyList(),
    favoritePerformers = emptyList(),
    collectorAlbums = emptyList(),
)
