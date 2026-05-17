package com.misw4203.vinilos.presentation.viewmodel

import app.cash.turbine.test
import com.misw4203.vinilos.MainDispatcherRule
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.Musician
import com.misw4203.vinilos.domain.model.MusicianPrize
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.model.Prize
import com.misw4203.vinilos.domain.repository.MusicianRepository
import com.misw4203.vinilos.domain.repository.PrizeRepository
import com.misw4203.vinilos.domain.usecase.AddPrizeToMusicianUseCase
import com.misw4203.vinilos.domain.usecase.GetMusicianDetailUseCase
import com.misw4203.vinilos.domain.usecase.GetPrizesUseCase
import com.misw4203.vinilos.presentation.navigation.Destinations
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import androidx.lifecycle.SavedStateHandle
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AddPrizeToMusicianViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val musicianId = 42

    private val sampleMusician = Musician(
        id = musicianId,
        name = "Gustavo Cerati",
        image = "",
        description = "",
        birthDate = "1959-08-11T00:00:00.000Z",
        albums = listOf(Album(1L, "Soda Stereo", "", "", "1984", "")),
        prizes = emptyList(),
    )

    private val samplePrizes = listOf(
        Prize(1, "Grammy Latino", "Descripción 1", "Academia Latina"),
        Prize(2, "Premio Konex", "Descripción 2", "Fundación Konex"),
    )

    private fun buildVm(
        musician: Musician = sampleMusician,
        prizes: List<Prize> = samplePrizes,
        addResult: (() -> Unit)? = null,
    ): AddPrizeToMusicianViewModel {
        val musicianRepo = object : MusicianRepository {
            override suspend fun getMusicians(): List<MusicianSummary> = error("not used")
            override suspend fun getMusicianDetail(id: Int): Musician = musician
            override suspend fun addAlbumToMusician(musicianId: Int, albumId: Int) = Unit
            override suspend fun addPrizeToMusician(mId: Int, prizeId: Int, date: String) {
                addResult?.invoke() ?: Unit
            }
        }
        val prizeRepo = object : PrizeRepository {
            override suspend fun getPrizes(): List<Prize> = prizes
            override suspend fun createPrize(name: String, description: String, organization: String): Prize =
                error("not used")
        }
        return AddPrizeToMusicianViewModel(
            getMusicianDetail = GetMusicianDetailUseCase(musicianRepo),
            getPrizes = GetPrizesUseCase(prizeRepo),
            addPrizeToMusician = AddPrizeToMusicianUseCase(musicianRepo),
            savedStateHandle = SavedStateHandle(mapOf(Destinations.AddPrizeMusicianArg to musicianId)),
        )
    }

    @Test
    fun `starts Loading then transitions to Ready with prizes and musician loaded`() = runTest {
        val vm = buildVm()

        vm.uiState.test {
            assertEquals(AddPrizeToMusicianUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(AddPrizeToMusicianUiState.Ready, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        val form = vm.form.value
        assertEquals("Gustavo Cerati", form.musicianName)
        assertEquals(2, form.allPrizes.size)
        assertEquals(2, form.filteredPrizes.size)
    }

    @Test
    fun `emits network Error when musician load throws IOException`() = runTest {
        val brokenRepo = object : MusicianRepository {
            override suspend fun getMusicians(): List<MusicianSummary> = error("not used")
            override suspend fun getMusicianDetail(id: Int): Musician = throw IOException("offline")
            override suspend fun addAlbumToMusician(musicianId: Int, albumId: Int) = Unit
            override suspend fun addPrizeToMusician(mId: Int, prizeId: Int, date: String) = Unit
        }
        val prizeRepo = object : PrizeRepository {
            override suspend fun getPrizes(): List<Prize> = samplePrizes
            override suspend fun createPrize(name: String, description: String, organization: String): Prize =
                error("not used")
        }
        val vm = AddPrizeToMusicianViewModel(
            getMusicianDetail = GetMusicianDetailUseCase(brokenRepo),
            getPrizes = GetPrizesUseCase(prizeRepo),
            addPrizeToMusician = AddPrizeToMusicianUseCase(brokenRepo),
            savedStateHandle = SavedStateHandle(mapOf(Destinations.AddPrizeMusicianArg to musicianId)),
        )

        vm.uiState.test {
            assertEquals(AddPrizeToMusicianUiState.Loading, awaitItem())
            advanceUntilIdle()
            val s = awaitItem()
            assertTrue(s is AddPrizeToMusicianUiState.Error)
            assertTrue((s as AddPrizeToMusicianUiState.Error).isNetworkError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `query filters prizes by name and organization case-insensitive`() = runTest {
        val vm = buildVm()
        advanceUntilIdle()

        vm.onQueryChange("grAMMy")
        assertEquals(1, vm.form.value.filteredPrizes.size)
        assertEquals("Grammy Latino", vm.form.value.filteredPrizes[0].name)
    }

    @Test
    fun `year validation rejects blank on confirm`() = runTest {
        val vm = buildVm()
        advanceUntilIdle()

        vm.onPrizeSelected(samplePrizes[0])
        vm.onConfirm()

        assertEquals(YearValidationError.EMPTY, vm.form.value.yearError)
    }

    @Test
    fun `year validation rejects future year`() = runTest {
        val vm = buildVm()
        advanceUntilIdle()

        vm.onYearChange("2999")
        assertEquals(YearValidationError.FUTURE, vm.form.value.yearError)
    }

    @Test
    fun `year validation rejects non-numeric input`() = runTest {
        val vm = buildVm()
        advanceUntilIdle()

        vm.onYearChange("abcd")
        assertEquals(YearValidationError.INVALID, vm.form.value.yearError)
    }

    @Test
    fun `duplicate prize and year is rejected`() = runTest {
        val musicianWithPrize = sampleMusician.copy(
            prizes = listOf(
                MusicianPrize(1, "Grammy Latino", "Academia Latina", "Desc", "2020-01-01T00:00:00.000Z"),
            ),
        )
        val vm = buildVm(musician = musicianWithPrize)
        advanceUntilIdle()

        vm.onPrizeSelected(samplePrizes[0])
        vm.onYearChange("2020")
        vm.onConfirm()

        assertEquals(YearValidationError.DUPLICATE, vm.form.value.yearError)
    }

    @Test
    fun `successful add emits AddedSuccessfully event and updates currentPrizes`() = runTest {
        val vm = buildVm()
        advanceUntilIdle()

        vm.onPrizeSelected(samplePrizes[0])
        vm.onYearChange("2020")

        vm.events.test {
            vm.onConfirm()
            advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is AddPrizeToMusicianEvent.AddedSuccessfully)
            assertEquals("Grammy Latino", (event as AddPrizeToMusicianEvent.AddedSuccessfully).prizeName)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(1, vm.form.value.currentPrizes.size)
        assertNull(vm.form.value.selectedPrize)
        assertTrue(vm.form.value.year.isEmpty())
    }

    @Test
    fun `network error on add emits AddFailed and preserves form data`() = runTest {
        val failingRepo = object : MusicianRepository {
            override suspend fun getMusicians(): List<MusicianSummary> = error("not used")
            override suspend fun getMusicianDetail(id: Int): Musician = sampleMusician
            override suspend fun addAlbumToMusician(musicianId: Int, albumId: Int) = Unit
            override suspend fun addPrizeToMusician(mId: Int, prizeId: Int, date: String) =
                throw IOException("offline")
        }
        val prizeRepo = object : PrizeRepository {
            override suspend fun getPrizes(): List<Prize> = samplePrizes
            override suspend fun createPrize(name: String, description: String, organization: String): Prize =
                error("not used")
        }
        val vm = AddPrizeToMusicianViewModel(
            getMusicianDetail = GetMusicianDetailUseCase(failingRepo),
            getPrizes = GetPrizesUseCase(prizeRepo),
            addPrizeToMusician = AddPrizeToMusicianUseCase(failingRepo),
            savedStateHandle = SavedStateHandle(mapOf(Destinations.AddPrizeMusicianArg to musicianId)),
        )
        advanceUntilIdle()

        vm.onPrizeSelected(samplePrizes[0])
        vm.onYearChange("2020")

        vm.events.test {
            vm.onConfirm()
            advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is AddPrizeToMusicianEvent.AddFailed)
            assertTrue((event as AddPrizeToMusicianEvent.AddFailed).isNetworkError)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals("2020", vm.form.value.year)
        assertEquals(samplePrizes[0].id, vm.form.value.selectedPrize?.id)
    }

    @Test
    fun `retry after load error recovers to Ready`() = runTest {
        var fail = true
        val musicianRepo = object : MusicianRepository {
            override suspend fun getMusicians(): List<MusicianSummary> = error("not used")
            override suspend fun getMusicianDetail(id: Int): Musician =
                if (fail) throw IOException("offline") else sampleMusician
            override suspend fun addAlbumToMusician(musicianId: Int, albumId: Int) = Unit
            override suspend fun addPrizeToMusician(mId: Int, prizeId: Int, date: String) = Unit
        }
        val prizeRepo = object : PrizeRepository {
            override suspend fun getPrizes(): List<Prize> = samplePrizes
            override suspend fun createPrize(name: String, description: String, organization: String): Prize =
                error("not used")
        }
        val vm = AddPrizeToMusicianViewModel(
            getMusicianDetail = GetMusicianDetailUseCase(musicianRepo),
            getPrizes = GetPrizesUseCase(prizeRepo),
            addPrizeToMusician = AddPrizeToMusicianUseCase(musicianRepo),
            savedStateHandle = SavedStateHandle(mapOf(Destinations.AddPrizeMusicianArg to musicianId)),
        )

        vm.uiState.test {
            assertEquals(AddPrizeToMusicianUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertTrue(awaitItem() is AddPrizeToMusicianUiState.Error)

            fail = false
            vm.retry()
            assertEquals(AddPrizeToMusicianUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(AddPrizeToMusicianUiState.Ready, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
