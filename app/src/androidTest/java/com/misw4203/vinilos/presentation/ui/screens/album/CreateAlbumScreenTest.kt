package com.misw4203.vinilos.presentation.ui.screens.album

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.misw4203.vinilos.R
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.AlbumDetail
import com.misw4203.vinilos.domain.model.Comment
import com.misw4203.vinilos.domain.model.CreateAlbumInput
import com.misw4203.vinilos.domain.repository.AlbumRepository
import com.misw4203.vinilos.domain.usecase.CreateAlbumUseCase
import com.misw4203.vinilos.presentation.viewmodel.CreateAlbumViewModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class CreateAlbumScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private class FakeRepo(
        private val createResult: Result<Album> = Result.success(sampleAlbum()),
    ) : AlbumRepository {
        override suspend fun getAlbums(): List<Album> = emptyList()
        override suspend fun getAlbumById(id: Long): AlbumDetail = error("unused")
        override suspend fun addTrack(albumId: Long, request: com.misw4203.vinilos.data.remote.dto.CreateTrackRequest): com.misw4203.vinilos.domain.model.Track = error("unused")
        override suspend fun addComment(albumId: Long, description: String, rating: Int, collectorId: Int): Comment = error("unused")
        override suspend fun createAlbum(input: CreateAlbumInput): Album = createResult.getOrThrow()
    }

    private fun vm(repo: AlbumRepository = FakeRepo()): CreateAlbumViewModel =
        CreateAlbumViewModel(CreateAlbumUseCase(repo))

    // -- Rendering -----------------------------------------------------------

    /**
     * Verifica que todos los campos del formulario existen en el árbol semántico.
     * Se usa performScrollTo() para los campos que pueden estar fuera de pantalla
     * en el formulario largo con scroll vertical.
     */
    @Test
    fun rendersAllFormFields() {
        composeTestRule.setContent {
            MaterialTheme {
                CreateAlbumScreen(onBack = {}, onAlbumCreated = {}, viewModel = vm())
            }
        }

        // Campos visibles en la parte superior del formulario
        composeTestRule.onNodeWithTag("create_album_name").assertIsDisplayed()
        composeTestRule.onNodeWithTag("create_album_record_label").assertIsDisplayed()
        composeTestRule.onNodeWithTag("create_album_release_date").assertIsDisplayed()
        composeTestRule.onNodeWithTag("create_album_genre").assertIsDisplayed()

        // Campos que pueden estar fuera de pantalla: se hace scroll antes de verificar
        composeTestRule.onNodeWithTag("create_album_cover").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("create_album_description").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("create_album_submit").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun rendersScreenTitle() {
        composeTestRule.setContent {
            MaterialTheme {
                CreateAlbumScreen(onBack = {}, onAlbumCreated = {}, viewModel = vm())
            }
        }

        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText(ctx.getString(R.string.create_album_title)).assertIsDisplayed()
    }

    @Test
    fun rendersBackButton() {
        composeTestRule.setContent {
            MaterialTheme {
                CreateAlbumScreen(onBack = {}, onAlbumCreated = {}, viewModel = vm())
            }
        }

        composeTestRule.onNodeWithTag("create_album_back_button").assertIsDisplayed()
    }

    // -- Validation ----------------------------------------------------------

    /**
     * Al enviar con todos los campos vacíos deben aparecer mensajes de error.
     * Se hace scroll hasta el botón de submit y se usa useUnmergedTree = true
     * porque Material3 TextField combina los semantics del campo con los del
     * supportingText en un único nodo merged.
     */
    @Test
    fun submitWithEmptyFields_showsRequiredFieldErrors() {
        composeTestRule.setContent {
            MaterialTheme {
                CreateAlbumScreen(onBack = {}, onAlbumCreated = {}, viewModel = vm())
            }
        }

        composeTestRule.onNodeWithTag("create_album_submit").performScrollTo().performClick()

        val ctx = composeTestRule.activity
        val errorText = ctx.getString(R.string.create_album_error_required)
        val errorNodes = composeTestRule
            .onAllNodesWithText(errorText, useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue("Se esperaban errores de validación al enviar campos vacíos", errorNodes.isNotEmpty())
    }

    @Test
    fun submitWithNameOnly_stillShowsOtherFieldErrors() {
        composeTestRule.setContent {
            MaterialTheme {
                CreateAlbumScreen(onBack = {}, onAlbumCreated = {}, viewModel = vm())
            }
        }

        composeTestRule.onNodeWithTag("create_album_name").performTextInput("Mi Álbum")
        composeTestRule.onNodeWithTag("create_album_submit").performScrollTo().performClick()

        val ctx = composeTestRule.activity
        val errorText = ctx.getString(R.string.create_album_error_required)
        val errorNodes = composeTestRule
            .onAllNodesWithText(errorText, useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue("Se esperaban errores en los campos restantes", errorNodes.isNotEmpty())
    }

    // -- Interactions --------------------------------------------------------

    @Test
    fun backButton_triggersOnBackCallback() {
        var backClicked = false
        composeTestRule.setContent {
            MaterialTheme {
                CreateAlbumScreen(
                    onBack = { backClicked = true },
                    onAlbumCreated = {},
                    viewModel = vm(),
                )
            }
        }

        composeTestRule.onNodeWithTag("create_album_back_button").performClick()

        assertTrue("Se esperaba que onBack fuera llamado", backClicked)
    }

    @Test
    fun submitButton_isEnabledOnIdle() {
        composeTestRule.setContent {
            MaterialTheme {
                CreateAlbumScreen(onBack = {}, onAlbumCreated = {}, viewModel = vm())
            }
        }

        composeTestRule.onNodeWithTag("create_album_submit").performScrollTo().assertIsEnabled()
    }

    @Test
    fun nameField_acceptsTextInput() {
        composeTestRule.setContent {
            MaterialTheme {
                CreateAlbumScreen(onBack = {}, onAlbumCreated = {}, viewModel = vm())
            }
        }

        composeTestRule.onNodeWithTag("create_album_name").performTextInput("Buscando América")
        composeTestRule.onNodeWithText("Buscando América").assertIsDisplayed()
    }

    @Test
    fun coverField_acceptsUrlInput() {
        composeTestRule.setContent {
            MaterialTheme {
                CreateAlbumScreen(onBack = {}, onAlbumCreated = {}, viewModel = vm())
            }
        }

        composeTestRule.onNodeWithTag("create_album_cover")
            .performScrollTo()
            .performTextInput("https://example.com/cover.jpg")
        composeTestRule.onNodeWithText("https://example.com/cover.jpg").assertIsDisplayed()
    }

    @Test
    fun descriptionField_acceptsTextInput() {
        composeTestRule.setContent {
            MaterialTheme {
                CreateAlbumScreen(onBack = {}, onAlbumCreated = {}, viewModel = vm())
            }
        }

        composeTestRule.onNodeWithTag("create_album_description")
            .performScrollTo()
            .performTextInput("Gran álbum de salsa política.")
        composeTestRule.onNodeWithText("Gran álbum de salsa política.").assertIsDisplayed()
    }

    @Test
    fun genreDropdown_showsOptionsOnClick() {
        composeTestRule.setContent {
            MaterialTheme {
                CreateAlbumScreen(onBack = {}, onAlbumCreated = {}, viewModel = vm())
            }
        }

        composeTestRule.onNodeWithTag("create_album_genre").performClick()

        composeTestRule.onNodeWithText("Rock").assertIsDisplayed()
        composeTestRule.onNodeWithText("Salsa").assertIsDisplayed()
        composeTestRule.onNodeWithText("Classical").assertIsDisplayed()
        composeTestRule.onNodeWithText("Folk").assertIsDisplayed()
    }

    @Test
    fun genreDropdown_selectingOptionUpdatesField() {
        composeTestRule.setContent {
            MaterialTheme {
                CreateAlbumScreen(onBack = {}, onAlbumCreated = {}, viewModel = vm())
            }
        }

        composeTestRule.onNodeWithTag("create_album_genre").performClick()
        composeTestRule.onNodeWithText("Salsa").performClick()

        composeTestRule.onNodeWithText("Salsa").assertIsDisplayed()
    }

    @Test
    fun recordLabelDropdown_showsOptionsOnClick() {
        composeTestRule.setContent {
            MaterialTheme {
                CreateAlbumScreen(onBack = {}, onAlbumCreated = {}, viewModel = vm())
            }
        }

        composeTestRule.onNodeWithTag("create_album_record_label").performClick()

        composeTestRule.onNodeWithText("Sony Music").assertIsDisplayed()
        composeTestRule.onNodeWithText("EMI").assertIsDisplayed()
        composeTestRule.onNodeWithText("Discos Fuentes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Elektra").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fania Records").assertIsDisplayed()
    }

    @Test
    fun recordLabelDropdown_selectingOptionUpdatesField() {
        composeTestRule.setContent {
            MaterialTheme {
                CreateAlbumScreen(onBack = {}, onAlbumCreated = {}, viewModel = vm())
            }
        }

        composeTestRule.onNodeWithTag("create_album_record_label").performClick()
        composeTestRule.onNodeWithText("Elektra").performClick()

        composeTestRule.onNodeWithText("Elektra").assertIsDisplayed()
    }

    // -- Error state ---------------------------------------------------------

    @Test
    fun submitButton_remainsEnabledInIdle() {
        composeTestRule.setContent {
            MaterialTheme {
                CreateAlbumScreen(
                    onBack = {},
                    onAlbumCreated = {},
                    viewModel = vm(FakeRepo(Result.failure(IOException("offline")))),
                )
            }
        }

        composeTestRule.onNodeWithTag("create_album_submit").performScrollTo().assertIsEnabled()
    }
}

private fun sampleAlbum() = Album(
    id = 99L,
    name = "Test Album",
    coverUrl = "https://example.com/cover.jpg",
    artistName = "",
    releaseYear = "2024",
    genre = "Rock",
)
