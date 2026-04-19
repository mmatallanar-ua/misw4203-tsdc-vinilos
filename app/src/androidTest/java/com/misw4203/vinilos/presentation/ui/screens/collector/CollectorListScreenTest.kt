package com.misw4203.vinilos.presentation.ui.screens.collector

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.misw4203.vinilos.domain.model.Collector
import com.misw4203.vinilos.domain.model.CollectorSummary
import com.misw4203.vinilos.domain.repository.CollectorRepository
import com.misw4203.vinilos.domain.usecase.GetCollectorsUseCase
import com.misw4203.vinilos.presentation.viewmodel.CollectorListViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class CollectorListScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private class FakeRepo(
        private val result: Result<List<CollectorSummary>>,
    ) : CollectorRepository {
        override suspend fun getCollectors(): List<CollectorSummary> = result.getOrThrow()
        override suspend fun getCollectorById(id: Int): Collector = error("unused")
    }

    private fun vm(result: Result<List<CollectorSummary>>) =
        CollectorListViewModel(GetCollectorsUseCase(FakeRepo(result)))

    @Test
    fun rendersCollectorsOnSuccess() {
        val viewModel = vm(
            Result.success(
                listOf(
                    CollectorSummary(100, "Manolo Bellon", "manollo@c.co", "3500"),
                    CollectorSummary(101, "Jaime Monsalve", "jm@r.co", "3012"),
                )
            )
        )
        composeTestRule.setContent {
            MaterialTheme {
                CollectorListScreen(onCollectorClick = {}, viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText("Manolo Bellon").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jaime Monsalve").assertIsDisplayed()
    }

    @Test
    fun clickingCollectorPropagatesId() {
        var clickedId = -1
        val viewModel = vm(
            Result.success(listOf(CollectorSummary(100, "Manolo Bellon", "m@c.co", "3500")))
        )
        composeTestRule.setContent {
            MaterialTheme {
                CollectorListScreen(
                    onCollectorClick = { clickedId = it },
                    viewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText("Manolo Bellon").performClick()
        assertEquals(100, clickedId)
    }

    @Test
    fun rendersErrorStateOnIOException() {
        val viewModel = vm(Result.failure(IOException("offline")))
        composeTestRule.setContent {
            MaterialTheme {
                CollectorListScreen(onCollectorClick = {}, viewModel = viewModel)
            }
        }
        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(
            ctx.getString(com.misw4203.vinilos.R.string.state_error_body_network)
        ).assertIsDisplayed()
    }
}
