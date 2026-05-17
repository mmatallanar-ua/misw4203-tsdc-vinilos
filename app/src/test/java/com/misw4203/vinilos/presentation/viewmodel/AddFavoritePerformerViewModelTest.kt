package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.misw4203.vinilos.MainDispatcherRule
import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.model.CollectorDetail
import com.misw4203.vinilos.domain.model.CollectorSummary
import com.misw4203.vinilos.domain.model.Musician
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.model.Performer
import com.misw4203.vinilos.domain.repository.BandRepository
import com.misw4203.vinilos.domain.repository.CollectorRepository
import com.misw4203.vinilos.domain.repository.MusicianRepository
import com.misw4203.vinilos.domain.usecase.AddFavoriteBandUseCase
import com.misw4203.vinilos.domain.usecase.AddFavoriteMusicianUseCase
import com.misw4203.vinilos.domain.usecase.GetBandsUseCase
import com.misw4203.vinilos.domain.usecase.GetCollectorDetailUseCase
import com.misw4203.vinilos.domain.usecase.GetMusiciansUseCase
import com.misw4203.vinilos.presentation.navigation.Destinations
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
class AddFavoritePerformerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeMusicianRepo : MusicianRepository {
        var allMusicians: List<MusicianSummary> = emptyList()
        override suspend fun getMusicians(): List<MusicianSummary> = allMusicians
        override suspend fun getMusicianDetail(id: Int): Musician = error("not used")
    }

    private class FakeBandRepo : BandRepository {
        var allBands: List<BandSummary> = emptyList()
        override suspend fun getBands(): List<BandSummary> = allBands
        override suspend fun getBandDetail(id: Int): Band = error("not used")
        override suspend fun addMusicianToBand(bandId: Int, musicianId: Int) = error("not used")
    }

    private class FakeCollectorRepo : CollectorRepository {
        var collectorResult: Result<CollectorDetail> = Result.success(sampleCollector(emptyList()))
        var addMusicianResult: Result<Unit> = Result.success(Unit)
        var addBandResult: Result<Unit> = Result.success(Unit)
        var addMusicianCallCount = 0
        var addBandCallCount = 0
        var lastCollectorId: Int? = null
        var lastPerformerId: Int? = null
        override suspend fun getCollectors(): List<CollectorSummary> = error("not used")
        override suspend fun getCollectorDetail(id: Int): CollectorDetail = collectorResult.getOrThrow()
        override suspend fun addFavoriteMusician(collectorId: Int, musicianId: Int) {
            addMusicianCallCount++
            lastCollectorId = collectorId
            lastPerformerId = musicianId
            addMusicianResult.getOrThrow()
        }
        override suspend fun addFavoriteBand(collectorId: Int, bandId: Int) {
            addBandCallCount++
            lastCollectorId = collectorId
            lastPerformerId = bandId
            addBandResult.getOrThrow()
        }
    }

    companion object {
        fun sampleCollector(favorites: List<Performer>) = CollectorDetail(
            id = 100,
            name = "Manolo Bellon",
            telephone = "3001234567",
            email = "manolo@test.com",
            description = "",
            collectorAlbums = emptyList(),
            favoritePerformers = favorites,
            comments = emptyList(),
        )
    }

    private fun buildVm(
        musicianRepo: FakeMusicianRepo,
        bandRepo: FakeBandRepo,
        collectorRepo: FakeCollectorRepo,
        collectorId: Int = 100,
    ): AddFavoritePerformerViewModel {
        val savedStateHandle = SavedStateHandle(
            mapOf(Destinations.AddFavoritePerformerCollectorArg to collectorId),
        )
        return AddFavoritePerformerViewModel(
            getMusicians = GetMusiciansUseCase(musicianRepo),
            getBands = GetBandsUseCase(bandRepo),
            getCollectorDetail = GetCollectorDetailUseCase(collectorRepo),
            addFavoriteMusician = AddFavoriteMusicianUseCase(collectorRepo),
            addFavoriteBand = AddFavoriteBandUseCase(collectorRepo),
            savedStateHandle = savedStateHandle,
        )
    }

    @Test
    fun `initial load fetches musicians, bands and collector, excludes current favorites`() = runTest {
        val musicianRepo = FakeMusicianRepo().apply {
            allMusicians = listOf(
                MusicianSummary(10, "Rubén Blades", "", ""),
                MusicianSummary(11, "Shakira", "", ""),
            )
        }
        val bandRepo = FakeBandRepo().apply {
            allBands = listOf(
                BandSummary(20, "Queen", ""),
                BandSummary(21, "The Beatles", ""),
            )
        }
        val collectorRepo = FakeCollectorRepo().apply {
            collectorResult = Result.success(
                sampleCollector(
                    listOf(
                        Performer(10L, "Rubén Blades", ""),
                        Performer(20L, "Queen", ""),
                    ),
                ),
            )
        }
        val vm = buildVm(musicianRepo, bandRepo, collectorRepo)
        advanceUntilIdle()

        assertEquals(2, vm.form.value.allMusicians.size)
        assertEquals(2, vm.form.value.allBands.size)
        assertEquals(setOf(10), vm.form.value.favoriteMusicianIds)
        assertEquals(setOf(20), vm.form.value.favoriteBandIds)
        assertEquals(1, vm.form.value.filteredMusicians.size)
        assertEquals(1, vm.form.value.filteredBands.size)
        assertEquals(PerformerType.MUSICIAN, vm.form.value.selectedTab)
        assertEquals(AddFavoritePerformerUiState.Ready, vm.uiState.value)
    }

    @Test
    fun `add musician success transitions to Ready and updates favorites`() = runTest {
        val musicianRepo = FakeMusicianRepo().apply {
            allMusicians = listOf(MusicianSummary(10, "Rubén", "", ""))
        }
        val bandRepo = FakeBandRepo()
        val collectorRepo = FakeCollectorRepo()
        val vm = buildVm(musicianRepo, bandRepo, collectorRepo)
        advanceUntilIdle()

        vm.onAddMusician(10)
        advanceUntilIdle()

        assertEquals(AddFavoritePerformerUiState.Ready, vm.uiState.value)
        assertTrue(vm.form.value.favoriteMusicianIds.contains(10))
        assertEquals(0, vm.form.value.filteredMusicians.size)
        assertEquals(1, collectorRepo.addMusicianCallCount)
        assertEquals(0, collectorRepo.addBandCallCount)
        assertEquals(100, collectorRepo.lastCollectorId)
        assertEquals(10, collectorRepo.lastPerformerId)
    }

    @Test
    fun `add band success transitions to Ready and updates favorites`() = runTest {
        val musicianRepo = FakeMusicianRepo()
        val bandRepo = FakeBandRepo().apply {
            allBands = listOf(BandSummary(20, "Queen", ""))
        }
        val collectorRepo = FakeCollectorRepo()
        val vm = buildVm(musicianRepo, bandRepo, collectorRepo)
        advanceUntilIdle()

        vm.onAddBand(20)
        advanceUntilIdle()

        assertEquals(AddFavoritePerformerUiState.Ready, vm.uiState.value)
        assertTrue(vm.form.value.favoriteBandIds.contains(20))
        assertEquals(0, vm.form.value.filteredBands.size)
        assertEquals(1, collectorRepo.addBandCallCount)
        assertEquals(0, collectorRepo.addMusicianCallCount)
        assertEquals(100, collectorRepo.lastCollectorId)
        assertEquals(20, collectorRepo.lastPerformerId)
    }

    @Test
    fun `onTabChange updates selectedTab`() = runTest {
        val vm = buildVm(FakeMusicianRepo(), FakeBandRepo(), FakeCollectorRepo())
        advanceUntilIdle()

        assertEquals(PerformerType.MUSICIAN, vm.form.value.selectedTab)
        vm.onTabChange(PerformerType.BAND)
        assertEquals(PerformerType.BAND, vm.form.value.selectedTab)
    }

    @Test
    fun `double tap while adding ignores second call`() = runTest {
        val musicianRepo = FakeMusicianRepo().apply {
            allMusicians = listOf(MusicianSummary(10, "Rubén", "", ""))
        }
        val collectorRepo = FakeCollectorRepo()
        val vm = buildVm(musicianRepo, FakeBandRepo(), collectorRepo)
        advanceUntilIdle()

        vm.onAddMusician(10)
        vm.onAddMusician(10)
        advanceUntilIdle()

        assertEquals(1, collectorRepo.addMusicianCallCount)
    }

    @Test
    fun `add band IOException emits network error without mutating favorites`() = runTest {
        val bandRepo = FakeBandRepo().apply {
            allBands = listOf(BandSummary(20, "Queen", ""))
        }
        val collectorRepo = FakeCollectorRepo().apply {
            addBandResult = Result.failure(IOException("offline"))
        }
        val vm = buildVm(FakeMusicianRepo(), bandRepo, collectorRepo)
        advanceUntilIdle()

        vm.onAddBand(20)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is AddFavoritePerformerUiState.Error)
        assertTrue((state as AddFavoritePerformerUiState.Error).isNetworkError)
        assertEquals(PerformerType.BAND, state.type)
        assertEquals(20, state.performerId)
        assertFalse(vm.form.value.favoriteBandIds.contains(20))
        assertEquals(1, vm.form.value.filteredBands.size)
    }

    @Test
    fun `add musician HttpException emits non-network error`() = runTest {
        val http = HttpException(Response.error<Any>(409, "".toResponseBody("text/plain".toMediaType())))
        val musicianRepo = FakeMusicianRepo().apply {
            allMusicians = listOf(MusicianSummary(10, "Rubén", "", ""))
        }
        val collectorRepo = FakeCollectorRepo().apply {
            addMusicianResult = Result.failure(http)
        }
        val vm = buildVm(musicianRepo, FakeBandRepo(), collectorRepo)
        advanceUntilIdle()

        vm.onAddMusician(10)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is AddFavoritePerformerUiState.Error)
        assertFalse((state as AddFavoritePerformerUiState.Error).isNetworkError)
        assertEquals(PerformerType.MUSICIAN, state.type)
    }

    @Test
    fun `query filters both musicians and bands (diacritic insensitive)`() = runTest {
        val musicianRepo = FakeMusicianRepo().apply {
            allMusicians = listOf(
                MusicianSummary(10, "Rubén Blades", "", ""),
                MusicianSummary(11, "Shakira", "", ""),
            )
        }
        val bandRepo = FakeBandRepo().apply {
            allBands = listOf(
                BandSummary(20, "Rubén Trio", ""),
                BandSummary(21, "Queen", ""),
            )
        }
        val vm = buildVm(musicianRepo, bandRepo, FakeCollectorRepo())
        advanceUntilIdle()

        vm.onQueryChange("RUBEN")
        assertEquals(1, vm.form.value.filteredMusicians.size)
        assertEquals(1, vm.form.value.filteredBands.size)
        assertEquals("Rubén Blades", vm.form.value.filteredMusicians[0].name)
        assertEquals("Rubén Trio", vm.form.value.filteredBands[0].name)
    }

    @Test
    fun `initial load IOException emits network error with null type`() = runTest {
        val collectorRepo = FakeCollectorRepo().apply {
            collectorResult = Result.failure(IOException("offline"))
        }
        val vm = buildVm(FakeMusicianRepo(), FakeBandRepo(), collectorRepo)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is AddFavoritePerformerUiState.Error)
        assertTrue((state as AddFavoritePerformerUiState.Error).isNetworkError)
        assertEquals(null, state.type)
        assertEquals(null, state.performerId)
    }
}
