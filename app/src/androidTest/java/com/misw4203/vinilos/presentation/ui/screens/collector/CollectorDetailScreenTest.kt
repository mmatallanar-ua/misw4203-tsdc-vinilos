package com.misw4203.vinilos.presentation.ui.screens.collector

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.SavedStateHandle
import com.misw4203.vinilos.R
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.CollectorAlbum
import com.misw4203.vinilos.domain.model.CollectorComment
import com.misw4203.vinilos.domain.model.CollectorDetail
import com.misw4203.vinilos.domain.model.CollectorSummary
import com.misw4203.vinilos.domain.model.Performer
import com.misw4203.vinilos.domain.repository.CollectorRepository
import com.misw4203.vinilos.domain.usecase.GetCollectorDetailUseCase
import com.misw4203.vinilos.presentation.navigation.Destinations
import com.misw4203.vinilos.presentation.viewmodel.CollectorDetailViewModel
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class CollectorDetailScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private class FakeRepo(val result: Result<CollectorDetail>) : CollectorRepository {
        override suspend fun getCollectors(): List<CollectorSummary> = error("unused")
        override suspend fun getCollectorDetail(id: Int): CollectorDetail = result.getOrThrow()
    }

    private fun vm(result: Result<CollectorDetail>): CollectorDetailViewModel {
        val handle = SavedStateHandle(mapOf(Destinations.CollectorDetailArg to 100))
        return CollectorDetailViewModel(GetCollectorDetailUseCase(FakeRepo(result)), handle)
    }

    private fun sampleDetail() = CollectorDetail(
        id = 100,
        name = "Manolo Bellon",
        telephone = "3502457896",
        email = "manollo@caracol.com.co",
        description = "Coleccionista apasionado de salsa.",
        collectorAlbums = listOf(
            CollectorAlbum(
                id = 100,
                price = 35.0,
                status = "Active",
                album = Album(100L, "Buscando América", "", "Rubén Blades", "1984", "Salsa"),
            ),
        ),
        favoritePerformers = listOf(
            Performer(100L, "Rubén Blades Bellido de Luna", ""),
        ),
        comments = listOf(
            CollectorComment(100L, "The most relevant album of Ruben Blades", 5, "Buscando América"),
        ),
    )

    // Sample without albums/performers so comments appear near the top of the scroll.
    private fun sampleDetailCommentsOnly() = CollectorDetail(
        id = 100,
        name = "Manolo Bellon",
        telephone = "3502457896",
        email = "manollo@caracol.com.co",
        description = "",
        collectorAlbums = emptyList(),
        favoritePerformers = emptyList(),
        comments = listOf(
            CollectorComment(100L, "The most relevant album of Ruben Blades", 5, "Buscando América"),
        ),
    )

    // ── Structure ────────────────────────────────────────────────────────────

    @Test
    fun rendersRootTagOnSuccess() {
        composeTestRule.setContent {
            MaterialTheme { CollectorDetailScreen(100, onBack = {}, viewModel = vm(Result.success(sampleDetail()))) }
        }
        composeTestRule.onNodeWithTag("collector_detail_root").assertIsDisplayed()
    }

    @Test
    fun rendersBackButtonOnSuccess() {
        composeTestRule.setContent {
            MaterialTheme { CollectorDetailScreen(100, onBack = {}, viewModel = vm(Result.success(sampleDetail()))) }
        }
        composeTestRule.onNodeWithTag("collector_detail_back").assertIsDisplayed()
    }

    // ── Header ───────────────────────────────────────────────────────────────

    @Test
    fun rendersCollectorNameOnSuccess() {
        composeTestRule.setContent {
            MaterialTheme { CollectorDetailScreen(100, onBack = {}, viewModel = vm(Result.success(sampleDetail()))) }
        }
        composeTestRule.onNodeWithText("Manolo Bellon").assertIsDisplayed()
    }

    @Test
    fun rendersEmailOnSuccess() {
        composeTestRule.setContent {
            MaterialTheme { CollectorDetailScreen(100, onBack = {}, viewModel = vm(Result.success(sampleDetail()))) }
        }
        composeTestRule.onNodeWithText("manollo@caracol.com.co").assertIsDisplayed()
    }

    @Test
    fun rendersTelephoneOnSuccess() {
        composeTestRule.setContent {
            MaterialTheme { CollectorDetailScreen(100, onBack = {}, viewModel = vm(Result.success(sampleDetail()))) }
        }
        composeTestRule.onNodeWithText("3502457896").assertIsDisplayed()
    }

    // ── Albums section ───────────────────────────────────────────────────────

    @Test
    fun rendersAlbumsSectionHeaderWhenAlbumsPresent() {
        composeTestRule.setContent {
            MaterialTheme { CollectorDetailScreen(100, onBack = {}, viewModel = vm(Result.success(sampleDetail()))) }
        }
        val ctx = composeTestRule.activity
        val expected = ctx.getString(R.string.collector_section_albums).uppercase()
        composeTestRule.onNodeWithText(expected).assertIsDisplayed()
    }

    @Test
    fun rendersAlbumNameInAlbumsSection() {
        composeTestRule.setContent {
            MaterialTheme { CollectorDetailScreen(100, onBack = {}, viewModel = vm(Result.success(sampleDetail()))) }
        }
        composeTestRule.onNodeWithText("Buscando América").assertIsDisplayed()
    }

    @Test
    fun rendersAlbumPriceInAlbumsSection() {
        composeTestRule.setContent {
            MaterialTheme { CollectorDetailScreen(100, onBack = {}, viewModel = vm(Result.success(sampleDetail()))) }
        }
        composeTestRule.onNodeWithText("$35").assertIsDisplayed()
    }

    @Test
    fun rendersAlbumStatusBadgeInAlbumsSection() {
        composeTestRule.setContent {
            MaterialTheme { CollectorDetailScreen(100, onBack = {}, viewModel = vm(Result.success(sampleDetail()))) }
        }
        composeTestRule.onNodeWithText("Active").assertIsDisplayed()
    }

    @Test
    fun albumsSectionHiddenWhenNoAlbums() {
        val detailWithoutAlbums = sampleDetail().copy(collectorAlbums = emptyList())
        composeTestRule.setContent {
            MaterialTheme { CollectorDetailScreen(100, onBack = {}, viewModel = vm(Result.success(detailWithoutAlbums))) }
        }
        val ctx = composeTestRule.activity
        val header = ctx.getString(R.string.collector_section_albums).uppercase()
        assert(composeTestRule.onAllNodesWithText(header).fetchSemanticsNodes().isEmpty()) {
            "Albums section header should not be shown when collectorAlbums is empty"
        }
    }

    // ── Performers section ───────────────────────────────────────────────────

    @Test
    fun rendersPerformersSectionHeaderWhenPerformersPresent() {
        composeTestRule.setContent {
            MaterialTheme { CollectorDetailScreen(100, onBack = {}, viewModel = vm(Result.success(sampleDetail()))) }
        }
        val ctx = composeTestRule.activity
        val expected = ctx.getString(R.string.collector_section_performers).uppercase()
        composeTestRule.onNodeWithText(expected).assertIsDisplayed()
    }

    @Test
    fun rendersPerformerNameInPerformersSection() {
        composeTestRule.setContent {
            MaterialTheme { CollectorDetailScreen(100, onBack = {}, viewModel = vm(Result.success(sampleDetail()))) }
        }
        composeTestRule.onNodeWithText("Rubén Blades Bellido de Luna").assertIsDisplayed()
    }

    @Test
    fun performersSectionHiddenWhenNoPerformers() {
        val detailWithoutPerformers = sampleDetail().copy(favoritePerformers = emptyList())
        composeTestRule.setContent {
            MaterialTheme { CollectorDetailScreen(100, onBack = {}, viewModel = vm(Result.success(detailWithoutPerformers))) }
        }
        val ctx = composeTestRule.activity
        val header = ctx.getString(R.string.collector_section_performers).uppercase()
        assert(composeTestRule.onAllNodesWithText(header).fetchSemanticsNodes().isEmpty()) {
            "Performers section header should not be shown when favoritePerformers is empty"
        }
    }

    // ── Comments section ─────────────────────────────────────────────────────

    @Test
    fun rendersCommentsSectionHeaderWhenCommentsPresent() {
        composeTestRule.setContent {
            MaterialTheme { CollectorDetailScreen(100, onBack = {}, viewModel = vm(Result.success(sampleDetailCommentsOnly()))) }
        }
        val ctx = composeTestRule.activity
        val expected = ctx.getString(R.string.collector_section_comments).uppercase()
        composeTestRule.onNodeWithText(expected).assertIsDisplayed()
    }

    @Test
    fun rendersCommentDescriptionInCommentsSection() {
        composeTestRule.setContent {
            MaterialTheme { CollectorDetailScreen(100, onBack = {}, viewModel = vm(Result.success(sampleDetailCommentsOnly()))) }
        }
        composeTestRule.onNodeWithText("The most relevant album of Ruben Blades").assertIsDisplayed()
    }

    @Test
    fun rendersCommentAlbumReferenceInCommentsSection() {
        composeTestRule.setContent {
            MaterialTheme { CollectorDetailScreen(100, onBack = {}, viewModel = vm(Result.success(sampleDetailCommentsOnly()))) }
        }
        val ctx = composeTestRule.activity
        val expected = ctx.getString(R.string.collector_comment_album_ref, "Buscando América")
        composeTestRule.onNodeWithText(expected).assertIsDisplayed()
    }

    @Test
    fun rendersRatingAccessibleContentDescription() {
        composeTestRule.setContent {
            MaterialTheme { CollectorDetailScreen(100, onBack = {}, viewModel = vm(Result.success(sampleDetailCommentsOnly()))) }
        }
        val ctx = composeTestRule.activity
        val expected = ctx.getString(R.string.cd_rating, 5)
        composeTestRule.onNodeWithContentDescription(expected).assertExists()
    }

    @Test
    fun commentsSectionHiddenWhenNoComments() {
        val detailWithoutComments = sampleDetail().copy(comments = emptyList())
        composeTestRule.setContent {
            MaterialTheme { CollectorDetailScreen(100, onBack = {}, viewModel = vm(Result.success(detailWithoutComments))) }
        }
        val ctx = composeTestRule.activity
        val header = ctx.getString(R.string.collector_section_comments).uppercase()
        assert(composeTestRule.onAllNodesWithText(header).fetchSemanticsNodes().isEmpty()) {
            "Comments section header should not be shown when comments is empty"
        }
    }

    // ── Error / NotFound states ───────────────────────────────────────────────

    @Test
    fun rendersNetworkErrorState() {
        composeTestRule.setContent {
            MaterialTheme { CollectorDetailScreen(100, onBack = {}, viewModel = vm(Result.failure(IOException("sin red")))) }
        }
        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.state_error_body_network)).assertIsDisplayed()
    }

    @Test
    fun rendersServerErrorState() {
        composeTestRule.setContent {
            MaterialTheme { CollectorDetailScreen(100, onBack = {}, viewModel = vm(Result.failure(RuntimeException("servidor")))) }
        }
        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.state_error_body_server)).assertIsDisplayed()
    }

    @Test
    fun rendersNotFoundState() {
        val error = HttpException(
            Response.error<Any>(404, "".toResponseBody("text/plain".toMediaType()))
        )
        composeTestRule.setContent {
            MaterialTheme { CollectorDetailScreen(100, onBack = {}, viewModel = vm(Result.failure(error))) }
        }
        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.collector_not_found_title)).assertIsDisplayed()
    }
}
