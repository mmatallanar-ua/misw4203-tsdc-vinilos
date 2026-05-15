package com.misw4203.vinilos.presentation.viewmodel

import app.cash.turbine.test
import com.misw4203.vinilos.MainDispatcherRule
import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.repository.BandRepository
import com.misw4203.vinilos.domain.usecase.GetBandDetailUseCase
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
class BandDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeBandRepository : BandRepository {
        var nextResult: Result<Band> = Result.success(sampleBand(1))
        var callCount = 0
        override suspend fun getBands(): List<BandSummary> = error("not used")
        override suspend fun getBandDetail(id: Int): Band {
            callCount++
            return nextResult.getOrThrow()
        }
        override suspend fun addMusicianToBand(bandId: Int, musicianId: Int) = error("not used")
    }

    companion object {
        fun sampleBand(id: Int) = Band(
            id = id, name = "Queen", image = "img", description = "desc",
            creationDate = "1970-01-01", members = emptyList(), albums = emptyList(),
        )
    }

    private fun buildVm(repo: FakeBandRepository) = BandDetailViewModel(GetBandDetailUseCase(repo))

    @Test
    fun `loadBand emits Loading then Success`() = runTest {
        val repo = FakeBandRepository().apply { nextResult = Result.success(sampleBand(1)) }
        val vm = buildVm(repo)

        vm.uiState.test {
            assertEquals(BandDetailUiState.Loading, awaitItem())
            vm.loadBand(1)
            advanceUntilIdle()
            assertTrue(awaitItem() is BandDetailUiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadBand with 404 emits NotFound`() = runTest {
        val http404 = HttpException(Response.error<Any>(404, "".toResponseBody("text/plain".toMediaType())))
        val repo = FakeBandRepository().apply { nextResult = Result.failure(http404) }
        val vm = buildVm(repo)

        vm.uiState.test {
            assertEquals(BandDetailUiState.Loading, awaitItem())
            vm.loadBand(1)
            advanceUntilIdle()
            assertEquals(BandDetailUiState.NotFound, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadBand with IOException emits network Error`() = runTest {
        val repo = FakeBandRepository().apply { nextResult = Result.failure(IOException("x")) }
        val vm = buildVm(repo)

        vm.uiState.test {
            assertEquals(BandDetailUiState.Loading, awaitItem())
            vm.loadBand(1)
            advanceUntilIdle()
            assertEquals(BandDetailUiState.Error(isNetworkError = true), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry re-invokes use case with last id`() = runTest {
        val repo = FakeBandRepository().apply { nextResult = Result.failure(IOException("x")) }
        val vm = buildVm(repo)

        vm.loadBand(42)
        advanceUntilIdle()
        repo.nextResult = Result.success(sampleBand(42))
        vm.retry()
        advanceUntilIdle()

        assertEquals(2, repo.callCount)
    }
}
