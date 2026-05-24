package com.misw4203.vinilos.presentation.viewmodel

import app.cash.turbine.test
import com.misw4203.vinilos.MainDispatcherRule
import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.repository.BandRepository
import com.misw4203.vinilos.domain.usecase.GetBandsUseCase
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
class BandListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeBandRepository : BandRepository {
        var nextResult: Result<List<BandSummary>> = Result.success(emptyList())
        var callCount = 0
        override suspend fun getBands(): List<BandSummary> {
            callCount++
            return nextResult.getOrThrow()
        }
        override suspend fun getBandDetail(id: Int): Band = error("not used")
        override suspend fun addMusicianToBand(bandId: Int, musicianId: Int) = error("not used")
        override suspend fun addAlbumToBand(bandId: Int, albumId: Long) {}
        override suspend fun addPrizeToBand(bandId: Int, prizeId: Int, premiationDate: String) {}
    }

    private fun buildVm(repo: FakeBandRepository) = BandListViewModel(GetBandsUseCase(repo))

    private fun sampleBands() = listOf(
        BandSummary(1, "Queen", "img"),
        BandSummary(2, "Aerosmith", "img"),
    )

    @Test
    fun `emits Loading then Success when bands returned`() = runTest {
        val repo = FakeBandRepository().apply { nextResult = Result.success(sampleBands()) }
        val vm = buildVm(repo)

        vm.uiState.test {
            assertEquals(BandListUiState.Loading, awaitItem())
            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state is BandListUiState.Success)
            assertEquals(2, (state as BandListUiState.Success).bands.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Empty when list empty`() = runTest {
        val repo = FakeBandRepository().apply { nextResult = Result.success(emptyList()) }
        val vm = buildVm(repo)

        vm.uiState.test {
            assertEquals(BandListUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(BandListUiState.Empty, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits network Error on IOException`() = runTest {
        val repo = FakeBandRepository().apply { nextResult = Result.failure(IOException("x")) }
        val vm = buildVm(repo)

        vm.uiState.test {
            assertEquals(BandListUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(BandListUiState.Error(isNetworkError = true), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits server Error on HttpException`() = runTest {
        val http = HttpException(Response.error<Any>(500, "".toResponseBody("text/plain".toMediaType())))
        val repo = FakeBandRepository().apply { nextResult = Result.failure(http) }
        val vm = buildVm(repo)

        vm.uiState.test {
            assertEquals(BandListUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(BandListUiState.Error(isNetworkError = false), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry re-invokes repository`() = runTest {
        val repo = FakeBandRepository().apply { nextResult = Result.failure(IOException("x")) }
        val vm = buildVm(repo)

        vm.uiState.test {
            assertEquals(BandListUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(BandListUiState.Error(isNetworkError = true), awaitItem())

            repo.nextResult = Result.success(sampleBands())
            vm.retry()

            assertEquals(BandListUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertTrue(awaitItem() is BandListUiState.Success)
            assertEquals(2, repo.callCount)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
