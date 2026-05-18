package com.misw4203.vinilos.presentation.viewmodel

import app.cash.turbine.test
import com.misw4203.vinilos.MainDispatcherRule
import com.misw4203.vinilos.domain.model.Prize
import com.misw4203.vinilos.domain.repository.PrizeRepository
import com.misw4203.vinilos.domain.usecase.CreatePrizeUseCase
import com.misw4203.vinilos.domain.usecase.GetPrizesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PrizesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakePrizeRepository(
        var prizesResult: Result<List<Prize>> = Result.success(emptyList()),
        var createResult: Result<Prize> = Result.success(samplePrize()),
    ) : PrizeRepository {
        var getPrizesCallCount = 0
        override suspend fun getPrizes(): List<Prize> {
            getPrizesCallCount++
            return prizesResult.getOrThrow()
        }
        override suspend fun createPrize(name: String, description: String, organization: String) =
            createResult.getOrThrow()
    }

    private fun buildViewModel(repo: FakePrizeRepository) =
        PrizesViewModel(GetPrizesUseCase(repo))

    @Test
    fun `starts in Loading then emits Success when prizes load`() = runTest {
        val prizes = listOf(samplePrize())
        val repo = FakePrizeRepository(prizesResult = Result.success(prizes))
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(PrizesUiState.Loading, awaitItem())
            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state is PrizesUiState.Success)
            assertEquals(1, (state as PrizesUiState.Success).prizes.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Empty when repository returns empty list`() = runTest {
        val repo = FakePrizeRepository(prizesResult = Result.success(emptyList()))
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(PrizesUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(PrizesUiState.Empty, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits network Error when repository throws IOException`() = runTest {
        val repo = FakePrizeRepository(prizesResult = Result.failure(IOException()))
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(PrizesUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(PrizesUiState.Error(isNetworkError = true), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry reloads prizes and recovers to Success`() = runTest {
        val repo = FakePrizeRepository(prizesResult = Result.failure(IOException()))
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(PrizesUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(PrizesUiState.Error(isNetworkError = true), awaitItem())

            repo.prizesResult = Result.success(listOf(samplePrize()))
            viewModel.retry()

            assertEquals(PrizesUiState.Loading, awaitItem())
            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state is PrizesUiState.Success)
            assertEquals(2, repo.getPrizesCallCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    companion object {
        fun samplePrize() = Prize(id = 1, name = "Grammy", description = "A music award", organization = "NARAS")
    }
}
