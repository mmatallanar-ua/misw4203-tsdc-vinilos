package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.misw4203.vinilos.MainDispatcherRule
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.CollectorAlbum
import com.misw4203.vinilos.domain.model.CollectorComment
import com.misw4203.vinilos.domain.model.CollectorDetail
import com.misw4203.vinilos.domain.model.CollectorSummary
import com.misw4203.vinilos.domain.model.Performer
import com.misw4203.vinilos.domain.model.PerformerKind
import com.misw4203.vinilos.domain.repository.CollectorRepository
import com.misw4203.vinilos.domain.usecase.GetCollectorDetailUseCase
import com.misw4203.vinilos.domain.usecase.RemoveAlbumFromCollectorUseCase
import com.misw4203.vinilos.domain.usecase.RemoveFavoriteBandUseCase
import com.misw4203.vinilos.domain.usecase.RemoveFavoriteMusicianUseCase
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

    private class FakeRepo(var result: Result<CollectorDetail>) : CollectorRepository {
        var removeError: Throwable? = null
        val removedMusicians = mutableListOf<Pair<Int, Int>>()
        val removedBands = mutableListOf<Pair<Int, Int>>()
        val removedAlbums = mutableListOf<Pair<Int, Long>>()

        override suspend fun getCollectors(): List<CollectorSummary> = emptyList()
        override suspend fun getCollectorDetail(id: Int): CollectorDetail = result.getOrThrow()
        override suspend fun addAlbumToCollector(collectorId: Int, albumId: Long, price: Double, status: String) = Unit
        override suspend fun addFavoriteMusician(collectorId: Int, musicianId: Int) = Unit
        override suspend fun addFavoriteBand(collectorId: Int, bandId: Int) = Unit

        override suspend fun removeFavoriteMusician(collectorId: Int, musicianId: Int) {
            removeError?.let { throw it }
            removedMusicians += collectorId to musicianId
        }

        override suspend fun removeFavoriteBand(collectorId: Int, bandId: Int) {
            removeError?.let { throw it }
            removedBands += collectorId to bandId
        }

        override suspend fun removeAlbumFromCollector(collectorId: Int, albumId: Long) {
            removeError?.let { throw it }
            removedAlbums += collectorId to albumId
        }
    }

    private fun buildViewModel(
        repo: FakeRepo,
        collectorId: Int = 100,
    ) = CollectorDetailViewModel(
        GetCollectorDetailUseCase(repo),
        RemoveFavoriteMusicianUseCase(repo),
        RemoveFavoriteBandUseCase(repo),
        RemoveAlbumFromCollectorUseCase(repo),
        SavedStateHandle(mapOf(Destinations.CollectorDetailArg to collectorId)),
    )

    @Test
    fun `starts in Loading then emits Success with collector data`() = runTest {
        val repo = FakeRepo(Result.success(sampleDetail()))
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(CollectorDetailUiState.Loading, awaitItem())
            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state is CollectorDetailUiState.Success)
            assertEquals("Manolo Bellon", (state as CollectorDetailUiState.Success).collector.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits NotFound when repository throws 404 HttpException`() = runTest {
        val error = HttpException(
            Response.error<Any>(404, "".toResponseBody("text/plain".toMediaType()))
        )
        val viewModel = buildViewModel(FakeRepo(Result.failure(error)))

        viewModel.uiState.test {
            assertEquals(CollectorDetailUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(CollectorDetailUiState.NotFound, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits server Error when repository throws non-404 HttpException`() = runTest {
        val error = HttpException(
            Response.error<Any>(500, "".toResponseBody("text/plain".toMediaType()))
        )
        val viewModel = buildViewModel(FakeRepo(Result.failure(error)))

        viewModel.uiState.test {
            assertEquals(CollectorDetailUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(CollectorDetailUiState.Error(isNetworkError = false), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits network Error when repository throws IOException`() = runTest {
        val viewModel = buildViewModel(FakeRepo(Result.failure(IOException("no connection"))))

        viewModel.uiState.test {
            assertEquals(CollectorDetailUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(CollectorDetailUiState.Error(isNetworkError = true), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry recovers from Error to Success`() = runTest {
        val repo = FakeRepo(Result.failure(IOException("offline")))
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(CollectorDetailUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(CollectorDetailUiState.Error(isNetworkError = true), awaitItem())

            repo.result = Result.success(sampleDetail())
            viewModel.retry()

            assertEquals(CollectorDetailUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertTrue(awaitItem() is CollectorDetailUiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Success state exposes albums, performers and comments`() = runTest {
        val repo = FakeRepo(Result.success(sampleDetailWithSections()))
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            awaitItem()
            advanceUntilIdle()
            val state = awaitItem() as CollectorDetailUiState.Success
            with(state.collector) {
                assertEquals(1, collectorAlbums.size)
                assertEquals("Buscando América", collectorAlbums[0].album?.name)
                assertEquals(35.0, collectorAlbums[0].price, 0.0)
                assertEquals("Active", collectorAlbums[0].status)
                assertEquals(2, favoritePerformers.size)
                assertEquals("Rubén Blades Bellido de Luna", favoritePerformers[0].name)
                assertEquals(PerformerKind.MUSICIAN, favoritePerformers[0].kind)
                assertEquals(PerformerKind.BAND, favoritePerformers[1].kind)
                assertEquals(1, comments.size)
                assertEquals(5, comments[0].rating)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `removeFavorite musician calls musician endpoint and removes optimistically`() = runTest {
        val repo = FakeRepo(Result.success(sampleDetailWithSections()))
        val viewModel = buildViewModel(repo)
        advanceUntilIdle()
        val performer = Performer(100L, "Rubén Blades Bellido de Luna", "", PerformerKind.MUSICIAN)

        viewModel.events.test {
            viewModel.removeFavorite(performer)
            advanceUntilIdle()
            assertEquals(CollectorDetailEvent.Removed("Rubén Blades Bellido de Luna"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(listOf(100 to 100), repo.removedMusicians)
        assertTrue(repo.removedBands.isEmpty())
        val state = viewModel.uiState.value as CollectorDetailUiState.Success
        assertTrue(state.collector.favoritePerformers.none { it.id == 100L })
    }

    @Test
    fun `removeFavorite band calls band endpoint`() = runTest {
        val repo = FakeRepo(Result.success(sampleDetailWithSections()))
        val viewModel = buildViewModel(repo)
        advanceUntilIdle()
        val band = Performer(200L, "Guayacán Orquesta", "", PerformerKind.BAND)

        viewModel.removeFavorite(band)
        advanceUntilIdle()

        assertEquals(listOf(100 to 200), repo.removedBands)
        assertTrue(repo.removedMusicians.isEmpty())
    }

    @Test
    fun `removeFavorite with UNKNOWN kind refuses and emits failure`() = runTest {
        val repo = FakeRepo(Result.success(sampleDetailWithSections()))
        val viewModel = buildViewModel(repo)
        advanceUntilIdle()
        val unknown = Performer(100L, "Rubén Blades Bellido de Luna", "", PerformerKind.UNKNOWN)

        viewModel.events.test {
            viewModel.removeFavorite(unknown)
            advanceUntilIdle()
            assertEquals(
                CollectorDetailEvent.RemoveFailed(isNetworkError = false),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }

        assertTrue(repo.removedMusicians.isEmpty())
        assertTrue(repo.removedBands.isEmpty())
        val state = viewModel.uiState.value as CollectorDetailUiState.Success
        assertEquals(2, state.collector.favoritePerformers.size)
    }

    @Test
    fun `removeAlbum calls endpoint and removes optimistically`() = runTest {
        val repo = FakeRepo(Result.success(sampleDetailWithSections()))
        val viewModel = buildViewModel(repo)
        advanceUntilIdle()
        val collectorAlbum = (viewModel.uiState.value as CollectorDetailUiState.Success)
            .collector.collectorAlbums.first()

        viewModel.removeAlbum(collectorAlbum)
        advanceUntilIdle()

        assertEquals(listOf(100 to 100L), repo.removedAlbums)
        val state = viewModel.uiState.value as CollectorDetailUiState.Success
        assertTrue(state.collector.collectorAlbums.isEmpty())
    }

    @Test
    fun `failed removal restores state and emits network failure`() = runTest {
        val repo = FakeRepo(Result.success(sampleDetailWithSections()))
        repo.removeError = IOException("offline")
        val viewModel = buildViewModel(repo)
        advanceUntilIdle()
        val performer = Performer(100L, "Rubén Blades Bellido de Luna", "", PerformerKind.MUSICIAN)

        viewModel.events.test {
            viewModel.removeFavorite(performer)
            advanceUntilIdle()
            assertEquals(
                CollectorDetailEvent.RemoveFailed(isNetworkError = true),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }

        val state = viewModel.uiState.value as CollectorDetailUiState.Success
        assertEquals(2, state.collector.favoritePerformers.size)
    }
}

private fun sampleDetail() = CollectorDetail(
    id = 100,
    name = "Manolo Bellon",
    telephone = "3502457896",
    email = "manollo@caracol.com.co",
    description = "",
    collectorAlbums = emptyList(),
    favoritePerformers = emptyList(),
    comments = emptyList(),
)

private fun sampleDetailWithSections() = CollectorDetail(
    id = 100,
    name = "Manolo Bellon",
    telephone = "3502457896",
    email = "manollo@caracol.com.co",
    description = "Coleccionista de salsa.",
    collectorAlbums = listOf(
        CollectorAlbum(
            id = 100,
            price = 35.0,
            status = "Active",
            album = Album(100L, "Buscando América", "https://cover.jpg", "Rubén Blades Bellido de Luna", "1984", "Salsa"),
        ),
    ),
    favoritePerformers = listOf(
        Performer(100L, "Rubén Blades Bellido de Luna", "https://image.jpg", PerformerKind.MUSICIAN),
        Performer(200L, "Guayacán Orquesta", "https://band.jpg", PerformerKind.BAND),
    ),
    comments = listOf(
        CollectorComment(100L, "The most relevant album of Ruben Blades", 5, "Buscando América"),
    ),
)
