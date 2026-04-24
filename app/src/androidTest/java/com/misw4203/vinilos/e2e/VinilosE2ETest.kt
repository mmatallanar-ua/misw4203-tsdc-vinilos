package com.misw4203.vinilos.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.espresso.Espresso
import com.misw4203.vinilos.MainActivity
import com.misw4203.vinilos.R
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Pruebas E2E (scope B): atacan MainActivity real con Hilt y el backend levantado
 * en http://10.0.2.2:3000 (docker backvynils-web-1).
 *
 * Requisitos para ejecutar:
 *   - Emulador Android corriendo.
 *   - Backend docker-compose up en el host (puertos 3000 y 5432).
 *   - testInstrumentationRunner = com.misw4203.vinilos.HiltTestRunner.
 */
@HiltAndroidTest
class VinilosE2ETest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val timeoutMs = 10_000L

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    // -- Albums --------------------------------------------------------------

    /** AL-01: la pantalla de álbumes carga y muestra la lista. */
    @Test
    fun albumList_rendersListFromBackend() {
        waitForTag("albums_list")
        composeRule.onNodeWithTag("albums_list").assertIsDisplayed()
    }

    /** AL-05 + AD-01 + AD-02: tap en primer álbum abre detalle y back regresa. */
    @Test
    fun albumList_tapFirstCard_opensDetail_andBackReturns() {
        waitForTag("albums_list")

        val firstTag = firstExistingTag("album_card_", 1L..50L)
        composeRule.onNodeWithTag("albums_list")
            .performScrollToNode(hasTestTag(firstTag))
        composeRule.onNodeWithTag(firstTag).performClick()

        waitForTag("album_detail_root")
        composeRule.onNodeWithTag("album_detail_root").assertIsDisplayed()

        composeRule.onNodeWithTag("album_detail_back").performClick()

        waitForTag("albums_list")
        composeRule.onNodeWithTag("albums_list").assertIsDisplayed()
    }

    // -- Artists -------------------------------------------------------------

    /** ML-01: la pantalla de artistas carga al entrar desde bottom nav. */
    @Test
    fun artistList_rendersListFromBackend() {
        composeRule.onNodeWithTag("bottom_nav_artists").performClick()

        waitForTag("artists_list")
        composeRule.onNodeWithTag("artists_list").assertIsDisplayed()
    }

    /** ML-04 + MD-01 + MD-04: tap en artista abre detalle y back regresa. */
    @Test
    fun artistList_tapFirstCard_opensDetail_andBackReturns() {
        composeRule.onNodeWithTag("bottom_nav_artists").performClick()

        waitForTag("artists_list")
        val firstTag = firstExistingTag("musician_card_", 1..200)
        composeRule.onNodeWithTag(firstTag).performClick()

        waitForTag("artist_detail_root")
        composeRule.onNodeWithTag("artist_detail_root").assertIsDisplayed()

        composeRule.onNodeWithTag("artist_detail_back").performClick()

        waitForTag("artists_list")
        composeRule.onNodeWithTag("artists_list").assertIsDisplayed()
    }

    // -- Navigation ----------------------------------------------------------

    /** NAV-01: cambiar entre Albums y Artists vía bottom nav cambia el título. */
    @Test
    fun bottomNav_switchesBetweenTabs() {
        val ctx = composeRule.activity
        val albumsTitle = ctx.getString(R.string.albums_title)
        val artistsTitle = ctx.getString(R.string.artists_title)

        waitForTag("albums_list")
        composeRule.onNodeWithText(albumsTitle).assertIsDisplayed()

        composeRule.onNodeWithTag("bottom_nav_artists").performClick()
        waitForText(artistsTitle)
        composeRule.onNodeWithText(artistsTitle).assertIsDisplayed()

        composeRule.onNodeWithTag("bottom_nav_albums").performClick()
        waitForTag("albums_list")
        composeRule.onNodeWithText(albumsTitle).assertIsDisplayed()
    }

    /** NAV-03: back del sistema desde detalle de álbum regresa a lista. */
    @Test
    fun systemBack_fromAlbumDetail_returnsToList() {
        waitForTag("albums_list")

        val firstTag = firstExistingTag("album_card_", 1L..50L)
        composeRule.onNodeWithTag(firstTag).performClick()
        waitForTag("album_detail_root")

        Espresso.pressBack()

        waitForTag("albums_list")
        composeRule.onNodeWithTag("albums_list").assertIsDisplayed()
    }

    // -- Accessibility -------------------------------------------------------

    /** AD-05: si el álbum tiene comentarios, el rating expone contentDescription. */
    @Test
    fun albumDetail_ratingHasAccessibleContentDescription_ifCommentsPresent() {
        waitForTag("albums_list")
        val firstTag = firstExistingTag("album_card_", 1L..50L)
        composeRule.onNodeWithTag(firstTag).performClick()
        waitForTag("album_detail_root")

        // cd_rating en strings.xml tiene el texto "… de 5 estrellas". Si el álbum
        // no tiene comentarios este nodo no existirá y el test sale como no-op.
        val ratingNodes = composeRule
            .onAllNodesWithContentDescription(label = "de 5", substring = true)
            .fetchSemanticsNodes()
        if (ratingNodes.isEmpty()) return

        composeRule.onNodeWithContentDescription(label = "de 5", substring = true)
            .assertIsDisplayed()
    }

    // -- Helpers -------------------------------------------------------------

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMs) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMs) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun firstExistingTag(prefix: String, range: LongRange): String =
        range.map { "$prefix$it" }.first { tag ->
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }

    private fun firstExistingTag(prefix: String, range: IntRange): String =
        range.map { "$prefix$it" }.first { tag ->
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
}
