package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.misw4203.vinilos.MainDispatcherRule
import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.model.MusicianPrize
import com.misw4203.vinilos.domain.model.Prize
import com.misw4203.vinilos.domain.repository.BandRepository
import com.misw4203.vinilos.domain.repository.PrizeRepository
import com.misw4203.vinilos.domain.usecase.AddPrizeToBandUseCase
import com.misw4203.vinilos.domain.usecase.GetBandDetailUseCase
import com.misw4203.vinilos.domain.usecase.GetPrizesUseCase
import com.misw4203.vinilos.presentation.navigation.Destinations
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AddPrizeToBandViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeBandRepo(
        var bandResult: Result<Band> = Result.success(sampleBand()),
        var addError: Throwable? = null,
    ) : BandRepository {
        val awarded = mutableListOf<Triple<Int, Int, String>>()
        override suspend fun getBands(): List<BandSummary> = emptyList()
        override suspend fun getBandDetail(id: Int): Band = bandResult.getOrThrow()
        override suspend fun addMusicianToBand(bandId: Int, musicianId: Int) = Unit
        override suspend fun addPrizeToBand(bandId: Int, prizeId: Int, premiationDate: String) {
            addError?.let { throw it }
            awarded += Triple(bandId, prizeId, premiationDate)
        }
    }

    private class FakePrizeRepo(private val prizes: List<Prize>) : PrizeRepository {
        override suspend fun getPrizes(): List<Prize> = prizes
        override suspend fun createPrize(name: String, description: String, organization: String): Prize =
            error("unused")
    }

    private fun build(
        bandRepo: FakeBandRepo = FakeBandRepo(),
        prizeRepo: FakePrizeRepo = FakePrizeRepo(
            listOf(Prize(10, "Grammy", "Best", "Recording Academy")),
        ),
        bandId: Int = 1,
    ) = AddPrizeToBandViewModel(
        getBandDetail = GetBandDetailUseCase(bandRepo),
        getPrizes = GetPrizesUseCase(prizeRepo),
        addPrizeToBand = AddPrizeToBandUseCase(bandRepo),
        savedStateHandle = SavedStateHandle(mapOf(Destinations.AddPrizeBandArg to bandId)),
    )

    @Test
    fun `loads prizes and band context`() = runTest {
        val vm = build()
        vm.uiState.test {
            assertEquals(AddPrizeToBandUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(AddPrizeToBandUiState.Ready, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("Fania All-Stars", vm.form.value.bandName)
        assertEquals(1, vm.form.value.filteredPrizes.size)
    }

    @Test
    fun `onConfirm awards prize to band and emits success`() = runTest {
        val bandRepo = FakeBandRepo()
        val vm = build(bandRepo = bandRepo)
        advanceUntilIdle()

        vm.onPrizeSelected(Prize(10, "Grammy", "Best", "Recording Academy"))
        vm.onDateSelected(0L) // 1970-01-01, not future

        vm.events.test {
            vm.onConfirm()
            advanceUntilIdle()
            assertEquals(AddPrizeToBandEvent.AddedSuccessfully("Grammy"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, bandRepo.awarded.size)
        assertEquals(1, bandRepo.awarded[0].first)
        assertEquals(10, bandRepo.awarded[0].second)
        assertTrue(vm.form.value.currentPrizes.any { it.id == 10 })
    }

    @Test
    fun `onConfirm failure emits AddFailed`() = runTest {
        val bandRepo = FakeBandRepo(addError = IOException("offline"))
        val vm = build(bandRepo = bandRepo)
        advanceUntilIdle()

        vm.onPrizeSelected(Prize(10, "Grammy", "Best", "Recording Academy"))
        vm.onDateSelected(0L)

        vm.events.test {
            vm.onConfirm()
            advanceUntilIdle()
            assertEquals(AddPrizeToBandEvent.AddFailed(isNetworkError = true), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `future date sets validation error and blocks confirm`() = runTest {
        val bandRepo = FakeBandRepo()
        val vm = build(bandRepo = bandRepo)
        advanceUntilIdle()

        vm.onPrizeSelected(Prize(10, "Grammy", "Best", "Recording Academy"))
        vm.onDateSelected(System.currentTimeMillis() + 86_400_000L)
        vm.onConfirm()
        advanceUntilIdle()

        assertEquals(DateValidationError.FUTURE, vm.form.value.dateError)
        assertTrue(bandRepo.awarded.isEmpty())
    }
}

private fun sampleBand() = Band(
    id = 1,
    name = "Fania All-Stars",
    image = "",
    description = "",
    creationDate = "1968-01-01T00:00:00.000Z",
    members = emptyList(),
    albums = emptyList(),
    prizes = listOf(MusicianPrize(99, "Old Prize", "Org", "", "1980-01-01")),
)
