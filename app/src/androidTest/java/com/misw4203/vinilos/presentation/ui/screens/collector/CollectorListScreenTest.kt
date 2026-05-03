package com.misw4203.vinilos.presentation.ui.screens.collector

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.misw4203.vinilos.R
import com.misw4203.vinilos.domain.model.CollectorDetail
import com.misw4203.vinilos.domain.model.CollectorSummary
import com.misw4203.vinilos.domain.repository.CollectorRepository
import com.misw4203.vinilos.domain.usecase.GetCollectorsUseCase
import com.misw4203.vinilos.presentation.viewmodel.CollectorListViewModel
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
        override suspend fun getCollectorDetail(id: Int): CollectorDetail = error("unused")
    }

    private fun vm(result: Result<List<CollectorSummary>>) =
        CollectorListViewModel(GetCollectorsUseCase(FakeRepo(result)))

    private fun sampleCollectors() = listOf(
        CollectorSummary(1, "Jaime Andrés Monsalve", "3102178976", "j.monsalve@gmail.com"),
        CollectorSummary(2, "María Alejandra Palacios", "3502889087", "j.palacios@outlook.es"),
    )

    @Test
    fun rendersCollectorNamesOnSuccess() {
        val viewModel = vm(Result.success(sampleCollectors()))
        composeTestRule.setContent {
            MaterialTheme {
                CollectorListScreen(onCollectorClick = {}, viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText("Jaime Andrés Monsalve").assertIsDisplayed()
        composeTestRule.onNodeWithText("María Alejandra Palacios").assertIsDisplayed()
    }

    @Test
    fun rendersCollectorsListTagOnSuccess() {
        val viewModel = vm(Result.success(sampleCollectors()))
        composeTestRule.setContent {
            MaterialTheme {
                CollectorListScreen(onCollectorClick = {}, viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithTag("collectors_list").assertIsDisplayed()
    }

    @Test
    fun rendersEmptyStateOnEmptyList() {
        val viewModel = vm(Result.success(emptyList()))
        composeTestRule.setContent {
            MaterialTheme {
                CollectorListScreen(onCollectorClick = {}, viewModel = viewModel)
            }
        }

        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.state_empty_title)).assertIsDisplayed()
    }

    @Test
    fun rendersSearchBarOnSuccess() {
        val viewModel = vm(Result.success(sampleCollectors()))
        composeTestRule.setContent {
            MaterialTheme {
                CollectorListScreen(onCollectorClick = {}, viewModel = viewModel)
            }
        }

        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.search_placeholder_collectors)).assertIsDisplayed()
    }

    @Test
    fun rendersNetworkErrorState() {
        val viewModel = vm(Result.failure(IOException("sin red")))
        composeTestRule.setContent {
            MaterialTheme {
                CollectorListScreen(onCollectorClick = {}, viewModel = viewModel)
            }
        }

        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.state_error_body_network))
            .assertIsDisplayed()
    }

    @Test
    fun rendersServerErrorState() {
        val viewModel = vm(Result.failure(RuntimeException("error servidor")))
        composeTestRule.setContent {
            MaterialTheme {
                CollectorListScreen(onCollectorClick = {}, viewModel = viewModel)
            }
        }

        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.state_error_body_server))
            .assertIsDisplayed()
    }
}
