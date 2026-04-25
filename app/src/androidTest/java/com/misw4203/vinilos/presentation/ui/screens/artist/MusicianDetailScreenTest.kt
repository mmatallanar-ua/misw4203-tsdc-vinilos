package com.misw4203.vinilos.presentation.ui.screens.artist

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.misw4203.vinilos.R
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.Musician
import com.misw4203.vinilos.domain.model.MusicianPrize
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.repository.MusicianRepository
import com.misw4203.vinilos.domain.usecase.GetMusicianDetailUseCase
import com.misw4203.vinilos.presentation.viewmodel.MusicianDetailViewModel
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.File
import java.io.IOException

class MusicianDetailScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun takeScreenshot(name: String) {
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val dir = composeTestRule.activity.getExternalFilesDir("screenshots") ?: return
        dir.mkdirs()
        File(dir, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private class FakeRepo(
        private val result: Result<Musician>,
    ) : MusicianRepository {
        override suspend fun getMusicians(): List<MusicianSummary> = error("unused")
        override suspend fun getMusicianDetail(id: Int): Musician = result.getOrThrow()
    }

    private class HangingRepo : MusicianRepository {
        override suspend fun getMusicians(): List<MusicianSummary> = error("unused")
        override suspend fun getMusicianDetail(id: Int): Musician = suspendCancellableCoroutine { _ -> }
    }

    private fun vm(result: Result<Musician>) =
        MusicianDetailViewModel(GetMusicianDetailUseCase(FakeRepo(result)))

    private fun sampleMusician() = Musician(
        id = 1,
        name = "Rubén Blades",
        image = "",
        description = "Cantante panameño de salsa y activista político.",
        birthDate = "1948-07-16T00:00:00.000Z",
        albums = listOf(
            Album(1L, "Siembra", "", "Rubén Blades", "1978", "Salsa"),
        ),
        prizes = listOf(
            MusicianPrize(
                id = 1,
                name = "Grammy Latino",
                organization = "Recording Academy",
                description = "Premio a la excelencia musical.",
                premiationDate = "2000-01-01T00:00:00.000Z",
            ),
        ),
    )

    // --- Success ---

    @Test
    fun rendersTopBarTitleOnSuccess() {
        val viewModel = vm(Result.success(sampleMusician()))
        composeTestRule.setContent {
            MaterialTheme {
                MusicianDetailScreen(musicianId = 1, onBack = {}, viewModel = viewModel)
            }
        }
        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.artist_detail_title)).assertIsDisplayed()
        takeScreenshot("01_topbar_title")
    }

    @Test
    fun rendersArtistNameOnSuccess() {
        val viewModel = vm(Result.success(sampleMusician()))
        composeTestRule.setContent {
            MaterialTheme {
                MusicianDetailScreen(musicianId = 1, onBack = {}, viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithText("Rubén Blades").assertIsDisplayed()
        takeScreenshot("02_artist_name")
    }

    @Test
    fun rendersArtistBadgeOnSuccess() {
        val viewModel = vm(Result.success(sampleMusician()))
        composeTestRule.setContent {
            MaterialTheme {
                MusicianDetailScreen(musicianId = 1, onBack = {}, viewModel = viewModel)
            }
        }
        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.artist_badge)).assertIsDisplayed()
        takeScreenshot("03_artist_badge")
    }

    @Test
    fun rendersBirthDateOnSuccess() {
        val viewModel = vm(Result.success(sampleMusician()))
        composeTestRule.setContent {
            MaterialTheme {
                MusicianDetailScreen(musicianId = 1, onBack = {}, viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithText("1948-07-16").assertIsDisplayed()
        takeScreenshot("04_birth_date")
    }

    @Test
    fun rendersDescriptionOnSuccess() {
        val viewModel = vm(Result.success(sampleMusician()))
        composeTestRule.setContent {
            MaterialTheme {
                MusicianDetailScreen(musicianId = 1, onBack = {}, viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithText("Cantante panameño de salsa y activista político.").assertIsDisplayed()
        takeScreenshot("05_description")
    }

    @Test
    fun rendersAlbumNameOnSuccess() {
        val viewModel = vm(Result.success(sampleMusician()))
        composeTestRule.setContent {
            MaterialTheme {
                MusicianDetailScreen(musicianId = 1, onBack = {}, viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithText("Siembra").performScrollTo().assertIsDisplayed()
        takeScreenshot("06_album_name")
    }

    @Test
    fun rendersPrizeNameOnSuccess() {
        val viewModel = vm(Result.success(sampleMusician()))
        composeTestRule.setContent {
            MaterialTheme {
                MusicianDetailScreen(musicianId = 1, onBack = {}, viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithText("Grammy Latino").performScrollTo().assertIsDisplayed()
        takeScreenshot("07_prize_name")
    }

    // --- Prize dialog ---

    @Test
    fun opensPrizeDialogOnClick() {
        val viewModel = vm(Result.success(sampleMusician()))
        composeTestRule.setContent {
            MaterialTheme {
                MusicianDetailScreen(musicianId = 1, onBack = {}, viewModel = viewModel)
            }
        }
        takeScreenshot("08_prize_dialog_before_click")
        composeTestRule.onNodeWithText("Grammy Latino").performClick()
        composeTestRule.onNodeWithText("Recording Academy").assertIsDisplayed()
        takeScreenshot("08_prize_dialog_open")
    }

    @Test
    fun closesPrizeDialogOnCloseButton() {
        val viewModel = vm(Result.success(sampleMusician()))
        composeTestRule.setContent {
            MaterialTheme {
                MusicianDetailScreen(musicianId = 1, onBack = {}, viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithText("Grammy Latino").performClick()
        takeScreenshot("09_prize_dialog_open")
        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.action_close)).performClick()
        composeTestRule.onAllNodesWithText("Recording Academy").assertCountEquals(0)
        takeScreenshot("09_prize_dialog_closed")
    }

    // --- Not found ---

    @Test
    fun rendersNotFoundStateOn404() {
        val response = Response.error<Any>(404, "".toResponseBody())
        val viewModel = vm(Result.failure(HttpException(response)))
        composeTestRule.setContent {
            MaterialTheme {
                MusicianDetailScreen(musicianId = 1, onBack = {}, viewModel = viewModel)
            }
        }
        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.artist_not_found_title)).assertIsDisplayed()
        takeScreenshot("10_not_found")
    }

    // --- Error ---

    @Test
    fun rendersNetworkErrorStateOnIOException() {
        val viewModel = vm(Result.failure(IOException("sin red")))
        composeTestRule.setContent {
            MaterialTheme {
                MusicianDetailScreen(musicianId = 1, onBack = {}, viewModel = viewModel)
            }
        }
        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.state_error_title)).assertIsDisplayed()
        takeScreenshot("11_network_error")
    }

    @Test
    fun rendersServerErrorStateOnGenericException() {
        val viewModel = vm(Result.failure(RuntimeException("error del servidor")))
        composeTestRule.setContent {
            MaterialTheme {
                MusicianDetailScreen(musicianId = 1, onBack = {}, viewModel = viewModel)
            }
        }
        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.state_error_title)).assertIsDisplayed()
        takeScreenshot("12_server_error")
    }

    // --- Back navigation ---

    @Test
    fun backButtonCallsOnBack() {
        var called = false
        composeTestRule.setContent {
            MaterialTheme {
                MusicianDetailScreen(
                    musicianId = 1,
                    onBack = { called = true },
                    viewModel = vm(Result.success(sampleMusician())),
                )
            }
        }
        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithContentDescription(ctx.getString(R.string.action_back)).performClick()
        assert(called)
        takeScreenshot("13_back_button")
    }

    // --- Not found body ---

    @Test
    fun rendersNotFoundBodyOn404() {
        val response = Response.error<Any>(404, "".toResponseBody())
        val viewModel = vm(Result.failure(HttpException(response)))
        composeTestRule.setContent {
            MaterialTheme {
                MusicianDetailScreen(musicianId = 1, onBack = {}, viewModel = viewModel)
            }
        }
        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.artist_not_found_body)).assertIsDisplayed()
        takeScreenshot("14_not_found_body")
    }

    // --- Section headers ---

    @Test
    fun rendersDescriptionSectionHeaderOnSuccess() {
        val viewModel = vm(Result.success(sampleMusician()))
        composeTestRule.setContent {
            MaterialTheme {
                MusicianDetailScreen(musicianId = 1, onBack = {}, viewModel = viewModel)
            }
        }
        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.detail_section_description)).assertIsDisplayed()
        takeScreenshot("15_description_header")
    }

    @Test
    fun rendersAlbumsSectionHeaderOnSuccess() {
        val viewModel = vm(Result.success(sampleMusician()))
        composeTestRule.setContent {
            MaterialTheme {
                MusicianDetailScreen(musicianId = 1, onBack = {}, viewModel = viewModel)
            }
        }
        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.artist_section_albums))
            .performScrollTo()
            .assertIsDisplayed()
        takeScreenshot("16_albums_header")
    }

    @Test
    fun rendersPrizesSectionHeaderOnSuccess() {
        val viewModel = vm(Result.success(sampleMusician()))
        composeTestRule.setContent {
            MaterialTheme {
                MusicianDetailScreen(musicianId = 1, onBack = {}, viewModel = viewModel)
            }
        }
        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.artist_section_prizes))
            .performScrollTo()
            .assertIsDisplayed()
        takeScreenshot("17_prizes_header")
    }

    // --- Album release year ---

    @Test
    fun rendersAlbumReleaseYearOnSuccess() {
        val viewModel = vm(Result.success(sampleMusician()))
        composeTestRule.setContent {
            MaterialTheme {
                MusicianDetailScreen(musicianId = 1, onBack = {}, viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithText("1978").performScrollTo().assertIsDisplayed()
        takeScreenshot("18_album_year")
    }

    // --- Prize date ---

    @Test
    fun rendersPrizeDateOnSuccess() {
        val viewModel = vm(Result.success(sampleMusician()))
        composeTestRule.setContent {
            MaterialTheme {
                MusicianDetailScreen(musicianId = 1, onBack = {}, viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithText("2000-01-01").performScrollTo().assertIsDisplayed()
        takeScreenshot("19_prize_date")
    }

    // --- Prize dialog description ---

    @Test
    fun rendersPrizeDescriptionInDialog() {
        val viewModel = vm(Result.success(sampleMusician()))
        composeTestRule.setContent {
            MaterialTheme {
                MusicianDetailScreen(musicianId = 1, onBack = {}, viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithText("Grammy Latino").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Premio a la excelencia musical.").assertIsDisplayed()
        takeScreenshot("20_prize_dialog_description")
    }

    // --- Error body texts ---

    @Test
    fun rendersNetworkErrorBodyOnIOException() {
        val viewModel = vm(Result.failure(IOException("sin red")))
        composeTestRule.setContent {
            MaterialTheme {
                MusicianDetailScreen(musicianId = 1, onBack = {}, viewModel = viewModel)
            }
        }
        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.state_error_body_network)).assertIsDisplayed()
        takeScreenshot("21_network_error_body")
    }

    @Test
    fun rendersServerErrorBodyOnGenericException() {
        val viewModel = vm(Result.failure(RuntimeException("error del servidor")))
        composeTestRule.setContent {
            MaterialTheme {
                MusicianDetailScreen(musicianId = 1, onBack = {}, viewModel = viewModel)
            }
        }
        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.state_error_body_server)).assertIsDisplayed()
        takeScreenshot("22_server_error_body")
    }

    // --- HTTP 500 ---

    @Test
    fun rendersServerErrorStateOn500() {
        val response = Response.error<Any>(500, "".toResponseBody())
        val viewModel = vm(Result.failure(HttpException(response)))
        composeTestRule.setContent {
            MaterialTheme {
                MusicianDetailScreen(musicianId = 1, onBack = {}, viewModel = viewModel)
            }
        }
        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.state_error_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(ctx.getString(R.string.state_error_body_server)).assertIsDisplayed()
        takeScreenshot("23_http500_error")
    }

    // --- Retry ---

    @Test
    fun retryButtonTriggersReloadOnNetworkError() {
        val viewModel = vm(Result.failure(IOException("sin red")))
        composeTestRule.setContent {
            MaterialTheme {
                MusicianDetailScreen(musicianId = 1, onBack = {}, viewModel = viewModel)
            }
        }
        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.action_retry)).performClick()
        composeTestRule.onNodeWithText(ctx.getString(R.string.state_error_title)).assertIsDisplayed()
        takeScreenshot("24_retry")
    }

    // --- Loading ---

    @Test
    fun rendersLoadingStateInitially() {
        val viewModel = MusicianDetailViewModel(GetMusicianDetailUseCase(HangingRepo()))
        composeTestRule.setContent {
            MaterialTheme {
                MusicianDetailScreen(musicianId = 1, onBack = {}, viewModel = viewModel)
            }
        }
        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.state_loading)).assertIsDisplayed()
        takeScreenshot("25_loading")
    }
}
