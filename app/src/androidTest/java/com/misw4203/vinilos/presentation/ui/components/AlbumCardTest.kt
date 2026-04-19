package com.misw4203.vinilos.presentation.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.misw4203.vinilos.domain.model.Album
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AlbumCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sample = Album(
        id = 1L,
        name = "Buscando América",
        coverUrl = "",
        artistName = "Rubén Blades",
        releaseYear = "1984",
        genre = "Salsa",
    )

    @Test
    fun rendersAlbumNameAndArtist() {
        composeTestRule.setContent {
            MaterialTheme {
                AlbumCard(album = sample, onClick = {})
            }
        }

        composeTestRule.onNodeWithText("Buscando América").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rubén Blades").assertIsDisplayed()
    }

    @Test
    fun rendersMetadataWithYearAndGenre() {
        composeTestRule.setContent {
            MaterialTheme {
                AlbumCard(album = sample, onClick = {})
            }
        }

        composeTestRule.onNodeWithText("1984 • SALSA").assertIsDisplayed()
    }

    @Test
    fun clickTriggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            MaterialTheme {
                AlbumCard(album = sample, onClick = { clicked = true })
            }
        }

        composeTestRule.onNodeWithText("Buscando América").performClick()
        assertTrue(clicked)
    }
}
