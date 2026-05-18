package com.misw4203.vinilos.presentation.viewmodel

import app.cash.turbine.test
import com.misw4203.vinilos.MainDispatcherRule
import com.misw4203.vinilos.domain.model.Prize
import com.misw4203.vinilos.domain.repository.PrizeRepository
import com.misw4203.vinilos.domain.usecase.CreatePrizeUseCase
import com.misw4203.vinilos.domain.usecase.GetPrizesUseCase
import com.misw4203.vinilos.presentation.common.DomainFailure
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CreatePrizeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakePrizeRepository(
        var prizesResult: Result<List<Prize>> = Result.success(emptyList()),
        var createResult: Result<Prize> = Result.success(
            Prize(99, "Grammy", "A music award", "NARAS")
        ),
    ) : PrizeRepository {
        var createCallCount = 0
        var createCalled: Boolean = false
        override suspend fun getPrizes() = prizesResult.getOrThrow()
        override suspend fun createPrize(name: String, description: String, organization: String): Prize {
            createCallCount++
            createCalled = true
            return createResult.getOrThrow()
        }
    }

    private fun buildViewModel(repo: FakePrizeRepository) =
        CreatePrizeViewModel(GetPrizesUseCase(repo), CreatePrizeUseCase(repo))

    private fun httpError(code: Int = 400) = HttpException(
        Response.error<Any>(code, "".toResponseBody("text/plain".toMediaType()))
    )

    // ── Prizes list loading ────────────────────────────────────────────────

    @Test
    fun `starts in LoadingPrizes then emits Ready with prizes`() = runTest {
        val prizes = listOf(Prize(1, "Grammy", "Desc", "NARAS"))
        val repo = FakePrizeRepository(prizesResult = Result.success(prizes))
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(CreatePrizeUiState.LoadingPrizes, awaitItem())
            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state is CreatePrizeUiState.Ready)
            assertEquals(1, (state as CreatePrizeUiState.Ready).existingPrizes.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when prizes load fails gracefully falls back to Ready with empty list`() = runTest {
        val repo = FakePrizeRepository(prizesResult = Result.failure(IOException()))
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(CreatePrizeUiState.LoadingPrizes, awaitItem())
            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state is CreatePrizeUiState.Ready)
            assertEquals(0, (state as CreatePrizeUiState.Ready).existingPrizes.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Successful submission ──────────────────────────────────────────────

    @Test
    fun `submit with valid fields emits Submitting then Ready and Submitted event`() = runTest {
        val repo = FakePrizeRepository()
        val viewModel = buildViewModel(repo)

        // Subscribe to events BEFORE submitting so the collector is active when tryEmit fires
        viewModel.events.test {
            // Let the initial prize load finish
            advanceUntilIdle()

            viewModel.uiState.test {
                // Drain initial LoadingPrizes → Ready
                advanceUntilIdle()
                cancelAndIgnoreRemainingEvents()
            }

            viewModel.submit("Grammy", "Top award", "NARAS")
            advanceUntilIdle()

            // createCalled verifies the API was actually invoked (no tautology)
            assertTrue(repo.createCalled)

            // uiState ended up in Ready (not Error, not Submitting)
            assertTrue(viewModel.uiState.value is CreatePrizeUiState.Ready)

            // The Submitted event was emitted
            val event = awaitItem()
            assertEquals(CreatePrizeEvent.Submitted, event)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Required field validation (CA04) ──────────────────────────────────

    @Test
    fun `submit with blank name emits ValidationFailed BLANK_FIELDS without calling API`() = runTest {
        val repo = FakePrizeRepository()
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(CreatePrizeUiState.LoadingPrizes, awaitItem())
            advanceUntilIdle()
            awaitItem() // Ready
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.events.test {
            viewModel.submit("", "Desc", "Org")
            val event = awaitItem()
            assertTrue(event is CreatePrizeEvent.ValidationFailed)
            assertEquals(PrizeValidation.BLANK_FIELDS, (event as CreatePrizeEvent.ValidationFailed).reason)
            assertFalse(repo.createCalled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `submit with blank organization emits ValidationFailed BLANK_FIELDS without calling API`() = runTest {
        val repo = FakePrizeRepository()
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(CreatePrizeUiState.LoadingPrizes, awaitItem())
            advanceUntilIdle()
            awaitItem() // Ready
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.events.test {
            viewModel.submit("Grammy", "Desc", "")
            val event = awaitItem()
            assertTrue(event is CreatePrizeEvent.ValidationFailed)
            assertEquals(PrizeValidation.BLANK_FIELDS, (event as CreatePrizeEvent.ValidationFailed).reason)
            assertFalse(repo.createCalled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `submit with blank description emits ValidationFailed BLANK_FIELDS without calling API`() = runTest {
        val repo = FakePrizeRepository()
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(CreatePrizeUiState.LoadingPrizes, awaitItem())
            advanceUntilIdle()
            awaitItem() // Ready
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.events.test {
            viewModel.submit("Grammy", "", "NARAS")
            val event = awaitItem()
            assertTrue(event is CreatePrizeEvent.ValidationFailed)
            assertEquals(PrizeValidation.BLANK_FIELDS, (event as CreatePrizeEvent.ValidationFailed).reason)
            assertFalse(repo.createCalled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Duplicate detection (CA06) ─────────────────────────────────────────

    @Test
    fun `submit with duplicate name and organization emits ValidationFailed DUPLICATE without calling API`() = runTest {
        val existing = listOf(Prize(1, "Grammy", "A music award", "NARAS"))
        val repo = FakePrizeRepository(prizesResult = Result.success(existing))
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(CreatePrizeUiState.LoadingPrizes, awaitItem())
            advanceUntilIdle()
            awaitItem() // Ready with existing prizes
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.events.test {
            viewModel.submit("Grammy", "Any description", "NARAS")
            val event = awaitItem()
            assertTrue(event is CreatePrizeEvent.ValidationFailed)
            assertEquals(PrizeValidation.DUPLICATE, (event as CreatePrizeEvent.ValidationFailed).reason)
            assertFalse(repo.createCalled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `duplicate check is case-insensitive`() = runTest {
        val existing = listOf(Prize(1, "grammy", "A music award", "naras"))
        val repo = FakePrizeRepository(prizesResult = Result.success(existing))
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(CreatePrizeUiState.LoadingPrizes, awaitItem())
            advanceUntilIdle()
            awaitItem() // Ready
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.events.test {
            viewModel.submit("GRAMMY", "Any description", "NARAS")
            val event = awaitItem()
            assertTrue(event is CreatePrizeEvent.ValidationFailed)
            assertEquals(PrizeValidation.DUPLICATE, (event as CreatePrizeEvent.ValidationFailed).reason)
            assertFalse(repo.createCalled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Network and server errors (CA07) ──────────────────────────────────

    @Test
    fun `submit with IOException emits Error with NETWORK category`() = runTest {
        val repo = FakePrizeRepository(createResult = Result.failure(IOException()))
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(CreatePrizeUiState.LoadingPrizes, awaitItem())
            advanceUntilIdle()
            awaitItem() // Ready

            viewModel.submit("Grammy", "Desc", "NARAS")
            assertEquals(CreatePrizeUiState.Submitting, awaitItem())
            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state is CreatePrizeUiState.Error)
            assertEquals(DomainFailure.NETWORK, (state as CreatePrizeUiState.Error).category)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `submit with HttpException emits Error with SERVER category`() = runTest {
        val repo = FakePrizeRepository(createResult = Result.failure(httpError(400)))
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(CreatePrizeUiState.LoadingPrizes, awaitItem())
            advanceUntilIdle()
            awaitItem() // Ready

            viewModel.submit("Grammy", "Desc", "NARAS")
            assertEquals(CreatePrizeUiState.Submitting, awaitItem())
            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state is CreatePrizeUiState.Error)
            assertEquals(DomainFailure.SERVER, (state as CreatePrizeUiState.Error).category)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error state is set before resetState call allowing form data preservation`() = runTest {
        val repo = FakePrizeRepository(createResult = Result.failure(IOException()))
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(CreatePrizeUiState.LoadingPrizes, awaitItem())
            advanceUntilIdle()
            awaitItem() // Ready

            viewModel.submit("Grammy", "Desc", "NARAS")
            assertEquals(CreatePrizeUiState.Submitting, awaitItem())
            advanceUntilIdle()
            val errorState = awaitItem()
            assertTrue(errorState is CreatePrizeUiState.Error)
            assertEquals(DomainFailure.NETWORK, (errorState as CreatePrizeUiState.Error).category)

            // resetState reloads prizes without clearing form (form lives in the UI)
            viewModel.resetState()
            assertEquals(CreatePrizeUiState.LoadingPrizes, awaitItem())
            advanceUntilIdle()
            val recovered = awaitItem()
            assertTrue(recovered is CreatePrizeUiState.Ready)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
