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

    private class FakeRepo(var result: Result<CollectorDetail>) : CollectorRepository {
        override suspend fun getCollectors(): List<CollectorSummary> = emptyList()
        override suspend fun getCollectorDetail(id: Int): CollectorDetail = result.getOrThrow()
    }

    private fun buildViewModel(
        repo: FakeRepo,
        collectorId: Int = 100,
    ) = CollectorDetailViewModel(
        GetCollectorDetailUseCase(repo),
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
                assertEquals(1, favoritePerformers.size)
                assertEquals("Rubén Blades Bellido de Luna", favoritePerformers[0].name)
                assertEquals(1, comments.size)
                assertEquals(5, comments[0].rating)
            }
            cancelAndIgnoreRemainingEvents()
        }
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
        Performer(100L, "Rubén Blades Bellido de Luna", "https://image.jpg"),
    ),
    comments = listOf(
        CollectorComment(100L, "The most relevant album of Ruben Blades", 5, "Buscando América"),
    ),
)
