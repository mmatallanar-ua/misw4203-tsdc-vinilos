package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.misw4203.vinilos.MainDispatcherRule
import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.model.Musician
import com.misw4203.vinilos.domain.repository.BandRepository
import com.misw4203.vinilos.domain.repository.MusicianRepository
import com.misw4203.vinilos.domain.usecase.AddMusicianToBandUseCase
import com.misw4203.vinilos.domain.usecase.GetBandDetailUseCase
import com.misw4203.vinilos.domain.usecase.GetMusiciansUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
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
class AddMusiciansToBandViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeMusicianRepo : MusicianRepository {
        var allMusicians: List<MusicianSummary> = emptyList()
        override suspend fun getMusicians(): List<MusicianSummary> = allMusicians
        override suspend fun getMusicianDetail(id: Int): Musician = error("not used")
        override suspend fun addAlbumToMusician(musicianId: Int, albumId: Int) = Unit
        override suspend fun addPrizeToMusician(musicianId: Int, prizeId: Int, premiationDate: String) = Unit
    }

    private class FakeBandRepo : BandRepository {
        var bandResult: Result<Band> = Result.success(sampleBand(emptyList()))
        var addResult: Result<Unit> = Result.success(Unit)
        var addCallCount = 0
        override suspend fun getBands(): List<BandSummary> = error("not used")
        override suspend fun getBandDetail(id: Int): Band = bandResult.getOrThrow()
        override suspend fun addMusicianToBand(bandId: Int, musicianId: Int) {
            addCallCount++
            addResult.getOrThrow()
        }
    }

    companion object {
        fun sampleBand(members: List<MusicianSummary>) = Band(
            id = 1, name = "Queen", image = "", description = "",
            creationDate = "", members = members, albums = emptyList(),
        )
    }

    private fun buildVm(
        musicianRepo: FakeMusicianRepo,
        bandRepo: FakeBandRepo,
        bandId: Int = 1,
    ): AddMusiciansToBandViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("bandId" to bandId))
        return AddMusiciansToBandViewModel(
            getMusicians = GetMusiciansUseCase(musicianRepo),
            getBandDetail = GetBandDetailUseCase(bandRepo),
            addMusicianToBand = AddMusicianToBandUseCase(bandRepo),
            savedStateHandle = savedStateHandle,
        )
    }

    @Test
    fun `initial load fetches catalog and band, excludes existing members`() = runTest {
        val musicianRepo = FakeMusicianRepo().apply {
            allMusicians = listOf(
                MusicianSummary(10, "Freddie Mercury", "", ""),
                MusicianSummary(11, "Brian May", "", ""),
                MusicianSummary(12, "John Deacon", "", ""),
            )
        }
        val bandRepo = FakeBandRepo().apply {
            bandResult = Result.success(sampleBand(listOf(MusicianSummary(10, "Freddie Mercury", "", ""))))
        }
        val vm = buildVm(musicianRepo, bandRepo)

        vm.form.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(3, state.allMusicians.size)
            assertEquals(setOf(10), state.currentMemberIds)
            assertEquals(2, state.filteredAvailable.size)
            assertTrue(state.filteredAvailable.none { it.id == 10 })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `query change filters by normalized name`() = runTest {
        // Filtering is synchronous (no debounce), consistent with the sibling
        // AddAlbumToMusician / AddFavoritePerformer view models.
        val musicianRepo = FakeMusicianRepo().apply {
            allMusicians = listOf(
                MusicianSummary(10, "José Pérez", "", ""),
                MusicianSummary(11, "Brian May", "", ""),
            )
        }
        val bandRepo = FakeBandRepo()
        val vm = buildVm(musicianRepo, bandRepo)
        advanceUntilIdle()

        vm.onQueryChange("jose")

        assertEquals(1, vm.form.value.filteredAvailable.size)
        assertEquals("José Pérez", vm.form.value.filteredAvailable[0].name)
    }

    @Test
    fun `query is case and diacritic insensitive`() = runTest {
        val musicianRepo = FakeMusicianRepo().apply {
            allMusicians = listOf(MusicianSummary(10, "José Pérez", "", ""))
        }
        val bandRepo = FakeBandRepo()
        val vm = buildVm(musicianRepo, bandRepo)
        advanceUntilIdle()

        vm.onQueryChange("PEREZ")
        advanceTimeBy(300L)
        advanceUntilIdle()

        assertEquals(1, vm.form.value.filteredAvailable.size)
    }

    @Test
    fun `add musician success transitions Adding to Ready and updates memberIds`() = runTest {
        val musicianRepo = FakeMusicianRepo().apply {
            allMusicians = listOf(MusicianSummary(10, "Freddie", "", ""))
        }
        val bandRepo = FakeBandRepo()
        val vm = buildVm(musicianRepo, bandRepo)
        advanceUntilIdle()

        vm.onAddMusician(10)
        advanceUntilIdle()

        assertEquals(AddMusiciansUiState.Ready, vm.uiState.value)
        assertTrue(vm.form.value.currentMemberIds.contains(10))
        assertEquals(0, vm.form.value.filteredAvailable.size)
        assertEquals(1, bandRepo.addCallCount)
    }

    @Test
    fun `add musician double tap ignores second call while Adding`() = runTest {
        val musicianRepo = FakeMusicianRepo().apply {
            allMusicians = listOf(MusicianSummary(10, "Freddie", "", ""))
        }
        val bandRepo = FakeBandRepo()
        val vm = buildVm(musicianRepo, bandRepo)
        advanceUntilIdle()

        vm.onAddMusician(10)
        vm.onAddMusician(10)
        advanceUntilIdle()

        assertEquals(1, bandRepo.addCallCount)
    }

    @Test
    fun `add musician IOException emits network Error without mutating lists`() = runTest {
        val musicianRepo = FakeMusicianRepo().apply {
            allMusicians = listOf(MusicianSummary(10, "Freddie", "", ""))
        }
        val bandRepo = FakeBandRepo().apply { addResult = Result.failure(IOException("x")) }
        val vm = buildVm(musicianRepo, bandRepo)
        advanceUntilIdle()

        vm.onAddMusician(10)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is AddMusiciansUiState.Error)
        assertTrue((state as AddMusiciansUiState.Error).isNetworkError)
        assertEquals(10, state.musicianId)
        assertFalse(vm.form.value.currentMemberIds.contains(10))
        assertEquals(1, vm.form.value.filteredAvailable.size)
    }

    @Test
    fun `add musician HttpException emits non-network Error`() = runTest {
        val http = HttpException(Response.error<Any>(409, "".toResponseBody("text/plain".toMediaType())))
        val musicianRepo = FakeMusicianRepo().apply {
            allMusicians = listOf(MusicianSummary(10, "Freddie", "", ""))
        }
        val bandRepo = FakeBandRepo().apply { addResult = Result.failure(http) }
        val vm = buildVm(musicianRepo, bandRepo)
        advanceUntilIdle()

        vm.onAddMusician(10)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is AddMusiciansUiState.Error)
        assertFalse((state as AddMusiciansUiState.Error).isNetworkError)
    }
}
