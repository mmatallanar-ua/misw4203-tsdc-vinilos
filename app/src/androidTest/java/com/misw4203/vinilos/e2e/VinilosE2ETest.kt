package com.misw4203.vinilos.e2e

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
 * Pruebas E2E: lanzan MainActivity real con Hilt usando FakeRepositoryModule,
 * por lo que no requieren backend externo. Solo necesitan el emulador y
 * testInstrumentationRunner = com.misw4203.vinilos.HiltTestRunner.
 */
@HiltAndroidTest
class VinilosE2ETest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val timeoutMs = 3_000L

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    // -- Albums --------------------------------------------------------------

    /** AL-01: la pantalla de álbumes carga y muestra los álbumes del catálogo. */
    @Test
    fun albumList_rendersList() {
        waitForTag("albums_list")
        composeRule.onNodeWithTag("albums_list").assertIsDisplayed()
        composeRule.onNodeWithText("Buscando América").assertIsDisplayed()
        composeRule.onNodeWithText("A Night at the Opera").assertIsDisplayed()
    }

    /** AL-05 + AD-01 + AD-02: tap en primer álbum abre detalle con contenido y back regresa. */
    @Test
    fun albumList_tapFirstCard_opensDetail_andBackReturns() {
        waitForTag("albums_list")

        val firstCard = tagStartsWith("album_card_")
        composeRule.onNodeWithTag("albums_list").performScrollToNode(firstCard)
        composeRule.onAllNodes(firstCard)[0].performClick()

        waitForTag("album_detail_root")
        composeRule.onNodeWithTag("album_detail_root").assertIsDisplayed()
        composeRule.onNodeWithText("Buscando América").assertIsDisplayed()

        composeRule.onNodeWithTag("album_detail_back").performClick()

        waitForTag("albums_list")
        composeRule.onNodeWithTag("albums_list").assertIsDisplayed()
    }

    // -- Create album --------------------------------------------------------

    /** CA-01: el botón FAB en la lista de álbumes abre el formulario de creación. */
    @Test
    fun albumList_tapFab_opensCreateAlbumForm() {
        waitForTag("albums_list")

        composeRule.onNodeWithTag("create_album_fab").performClick()

        // Esperamos que aparezca el botón de back (siempre visible en el TopAppBar)
        composeRule.waitUntil(timeoutMs) {
            composeRule.onAllNodesWithTag("create_album_back_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("create_album_back_button").assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.create_album_title))
            .assertIsDisplayed()
    }

    /** CA-02: el botón de retroceso en el formulario regresa a la lista de álbumes. */
    @Test
    fun createAlbum_backButton_returnsToAlbumList() {
        waitForTag("albums_list")
        composeRule.onNodeWithTag("create_album_fab").performClick()
        waitForTag("create_album_back_button")

        composeRule.onNodeWithTag("create_album_back_button").performClick()

        waitForTag("albums_list")
        composeRule.onNodeWithTag("albums_list").assertIsDisplayed()
    }

    /** CA-03: back del sistema desde el formulario de creación regresa a la lista. */
    @Test
    fun systemBack_fromCreateAlbum_returnsToAlbumList() {
        waitForTag("albums_list")
        composeRule.onNodeWithTag("create_album_fab").performClick()
        waitForTag("create_album_back_button")

        Espresso.pressBack()

        waitForTag("albums_list")
        composeRule.onNodeWithTag("albums_list").assertIsDisplayed()
    }

    /**
     * CA-04: enviar formulario vacío muestra errores de validación.
     * Se hace scroll hasta el botón de submit porque el formulario tiene scroll vertical.
     * Se usa useUnmergedTree = true porque Material3 TextField combina los semantics del
     * campo con el supportingText en un único nodo merged.
     */
    @Test
    fun createAlbum_submitEmptyForm_showsValidationErrors() {
        waitForTag("albums_list")
        composeRule.onNodeWithTag("create_album_fab").performClick()
        waitForTag("create_album_back_button")

        composeRule.onNodeWithTag("create_album_submit").performScrollTo().performClick()

        val errorText = composeRule.activity.getString(R.string.create_album_error_required)
        composeRule.waitUntil(timeoutMs) {
            composeRule.onAllNodesWithText(errorText, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText(errorText, useUnmergedTree = true)[0].assertExists()
    }

    // -- Artists -------------------------------------------------------------

    /** ML-01: la pantalla de artistas carga y muestra los músicos del catálogo. */
    @Test
    fun artistList_rendersList() {
        composeRule.onNodeWithTag("bottom_nav_artists").performClick()

        waitForTag("artists_list")
        composeRule.onNodeWithTag("artists_list").assertIsDisplayed()
        composeRule.onNodeWithText("Rubén Blades").assertIsDisplayed()
        composeRule.onNodeWithText("Freddie Mercury").assertIsDisplayed()
    }

    /** ML-04 + MD-01 + MD-04: tap en artista abre detalle con contenido y back regresa. */
    @Test
    fun artistList_tapFirstCard_opensDetail_andBackReturns() {
        composeRule.onNodeWithTag("bottom_nav_artists").performClick()

        waitForTag("artists_list")
        val firstCard = tagStartsWith("musician_card_")
        composeRule.onAllNodes(firstCard)[0].performClick()

        waitForTag("artist_detail_root")
        composeRule.onNodeWithTag("artist_detail_root").assertIsDisplayed()
        composeRule.onNodeWithText("Rubén Blades").assertIsDisplayed()
        composeRule.onNodeWithText("1948-07-16").assertIsDisplayed()

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

    /** NAV-02: el tab Collectors es alcanzable sin errores. */
    @Test
    fun collectorsTab_isReachableFromBottomNav() {
        waitForTag("albums_list")
        composeRule.onNodeWithTag("bottom_nav_collectors").performClick()
        // Verificar que salimos de la lista de álbumes
        composeRule.waitUntil(timeoutMs) {
            composeRule.onAllNodesWithTag("albums_list").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag("bottom_nav_collectors").assertIsDisplayed()
    }

    // -- Collectors ----------------------------------------------------------

    /** CL-01: la pantalla de coleccionistas carga y muestra los coleccionistas del catálogo. */
    @Test
    fun collectorList_rendersList() {
        composeRule.onNodeWithTag("bottom_nav_collectors").performClick()

        waitForTag("collectors_list")
        composeRule.onNodeWithTag("collectors_list").assertIsDisplayed()
        composeRule.onNodeWithText("Jaime Andrés Monsalve").assertIsDisplayed()
        composeRule.onNodeWithText("María Alejandra Palacios").assertIsDisplayed()
    }

    /** CL-02: cada tarjeta de coleccionista expone su testTag individual. */
    @Test
    fun collectorList_cardTagsArePresent() {
        composeRule.onNodeWithTag("bottom_nav_collectors").performClick()

        waitForTag("collectors_list")
        val firstCard = tagStartsWith("collector_card_")
        composeRule.onAllNodes(firstCard).fetchSemanticsNodes().let { nodes ->
            assert(nodes.isNotEmpty()) { "No se encontraron tarjetas de coleccionista" }
        }
    }

    /** CL-03: la lista de coleccionistas muestra el título de la pantalla. */
    @Test
    fun collectorList_showsTitle() {
        composeRule.onNodeWithTag("bottom_nav_collectors").performClick()

        waitForTag("collectors_list")
        val ctx = composeRule.activity
        composeRule.onNodeWithText(ctx.getString(R.string.collectors_title)).assertIsDisplayed()
    }

    /** CD-01: tap en tarjeta de coleccionista abre el detalle con nombre y secciones. */
    @Test
    fun collectorList_tapFirstCard_opensDetail_withContent() {
        composeRule.onNodeWithTag("bottom_nav_collectors").performClick()
        waitForTag("collectors_list")

        val firstCard = tagStartsWith("collector_card_")
        composeRule.onAllNodes(firstCard)[0].performClick()

        waitForTag("collector_detail_root")
        composeRule.onNodeWithTag("collector_detail_root").assertIsDisplayed()
        composeRule.onNodeWithText("Jaime Andrés Monsalve").assertIsDisplayed()
    }

    /** CD-02: el botón de retroceso en el detalle regresa a la lista. */
    @Test
    fun collectorDetail_backButton_returnsToList() {
        composeRule.onNodeWithTag("bottom_nav_collectors").performClick()
        waitForTag("collectors_list")

        val firstCard = tagStartsWith("collector_card_")
        composeRule.onAllNodes(firstCard)[0].performClick()
        waitForTag("collector_detail_root")

        composeRule.onNodeWithTag("collector_detail_back").performClick()

        waitForTag("collectors_list")
        composeRule.onNodeWithTag("collectors_list").assertIsDisplayed()
    }

    /** CD-03: back del sistema desde el detalle de coleccionista regresa a la lista. */
    @Test
    fun systemBack_fromCollectorDetail_returnsToList() {
        composeRule.onNodeWithTag("bottom_nav_collectors").performClick()
        waitForTag("collectors_list")

        val firstCard = tagStartsWith("collector_card_")
        composeRule.onAllNodes(firstCard)[0].performClick()
        waitForTag("collector_detail_root")

        Espresso.pressBack()

        waitForTag("collectors_list")
        composeRule.onNodeWithTag("collectors_list").assertIsDisplayed()
    }

    /** CD-04: el detalle de coleccionista muestra el álbum coleccionado. */
    @Test
    fun collectorDetail_showsCollectedAlbum() {
        composeRule.onNodeWithTag("bottom_nav_collectors").performClick()
        waitForTag("collectors_list")

        val firstCard = tagStartsWith("collector_card_")
        composeRule.onAllNodes(firstCard)[0].performClick()
        waitForTag("collector_detail_root")

        composeRule.onNodeWithText("Buscando América").assertIsDisplayed()
    }

    /** CD-05: el detalle de coleccionista muestra el artista favorito. */
    @Test
    fun collectorDetail_showsFavoritePerformer() {
        composeRule.onNodeWithTag("bottom_nav_collectors").performClick()
        waitForTag("collectors_list")

        val firstCard = tagStartsWith("collector_card_")
        composeRule.onAllNodes(firstCard)[0].performClick()
        waitForTag("collector_detail_root")

        // HU010: el chip del favorito vive en una LazyRow horizontal dentro del
        // scroll vertical del detalle; en viewports pequenos (Pixel 3a) puede
        // quedar bajo del fold. assertExists basta para verificar que el layout
        // incluye al favorito (igual que AD-05 con el rating accesible).
        composeRule.onNodeWithText("Rubén Blades Bellido de Luna").assertExists()
    }

    /** CD-06: el rating del comentario expone contentDescription accesible. */
    @Test
    fun collectorDetail_ratingHasAccessibleContentDescription() {
        composeRule.onNodeWithTag("bottom_nav_collectors").performClick()
        waitForTag("collectors_list")

        val firstCard = tagStartsWith("collector_card_")
        composeRule.onAllNodes(firstCard)[0].performClick()
        waitForTag("collector_detail_root")

        composeRule.onAllNodesWithContentDescription(label = "de 5", substring = true)[0]
            .assertExists()
    }

    /** NAV-03: back del sistema desde detalle de álbum regresa a lista. */
    @Test
    fun systemBack_fromAlbumDetail_returnsToList() {
        waitForTag("albums_list")

        val firstCard = tagStartsWith("album_card_")
        composeRule.onNodeWithTag("albums_list").performScrollToNode(firstCard)
        composeRule.onAllNodes(firstCard)[0].performClick()
        waitForTag("album_detail_root")

        Espresso.pressBack()

        waitForTag("albums_list")
        composeRule.onNodeWithTag("albums_list").assertIsDisplayed()
    }

    // -- Accessibility -------------------------------------------------------

    /** AD-05: el rating del álbum expone contentDescription accesible (el fake siempre tiene comentarios). */
    @Test
    fun albumDetail_ratingHasAccessibleContentDescription() {
        waitForTag("albums_list")
        val firstCard = tagStartsWith("album_card_")
        composeRule.onNodeWithTag("albums_list").performScrollToNode(firstCard)
        composeRule.onAllNodes(firstCard)[0].performClick()
        waitForTag("album_detail_root")

        // assertExists (no assertIsDisplayed): el nodo puede estar fuera del viewport;
        // para accesibilidad basta con que exista en el semantic tree — TalkBack lo anuncia.
        composeRule.onAllNodesWithContentDescription(label = "de 5", substring = true)[0]
            .assertExists()
    }

    // -- HU012: Agregar músicos a banda --------------------------------------

    /**
     * HU012: flujo completo de agregar un músico a una banda.
     * Bottom-nav → sub-tab Bandas → detalle de banda → CTA agregar
     * (empty-state o regular) → "+" en disponibles → verificación de que
     * el músico aparece en "Integrantes actuales".
     */
    @Test
    fun hu012_addMusicianToBand_flow() {
        // 1. Bottom-nav → tab Artistas
        composeRule.onNodeWithTag("bottom_nav_artists").performClick()

        // 2. Sub-tab "Bandas"
        waitForTag("artists_tab_bands")
        composeRule.onNodeWithTag("artists_tab_bands").performClick()

        // 3. Tap primera banda disponible (band_card_<id>)
        val bandCard = tagStartsWith("band_card_")
        composeRule.waitUntil(timeoutMs) {
            composeRule.onAllNodes(bandCard).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(bandCard)[0].performClick()

        // 4. En el detalle de la banda, click en CTA empty-state o regular
        waitForTag("band_detail_root")
        val emptyCta = composeRule.activity.getString(R.string.add_first_member_cta)
        val regularCta = composeRule.activity.getString(R.string.add_musicians_cta)
        // Empty-state primero; fallback al botón regular si la banda ya tiene miembros.
        val emptyNodes = composeRule.onAllNodesWithText(emptyCta).fetchSemanticsNodes()
        if (emptyNodes.isNotEmpty()) {
            composeRule.onNodeWithText(emptyCta).performClick()
        } else {
            composeRule.onNodeWithText(regularCta).performClick()
        }

        // 5. En la pantalla de agregar músicos, esperamos a que carguen los disponibles
        // y hacemos click en el "+" del primer músico disponible.
        waitForTag("add_musicians_screen_root")
        val availableMusician = tagStartsWith("available_musician_")
        composeRule.waitUntil(timeoutMs) {
            composeRule.onAllNodes(availableMusician).fetchSemanticsNodes().isNotEmpty()
        }
        // El "+" tiene contentDescription = "Agregar {nombre} a la banda" (R.string.cd_add_musician_to_band).
        val addButton = SemanticsMatcher("ContentDescription starts with 'Agregar ' and ends with ' a la banda'") { node ->
            val cd = node.config.getOrNull(SemanticsProperties.ContentDescription) ?: return@SemanticsMatcher false
            cd.any { it.startsWith("Agregar ") && it.endsWith(" a la banda") }
        }
        composeRule.waitUntil(timeoutMs) {
            composeRule.onAllNodes(addButton).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(addButton)[0].performClick()

        // 6. Verificar que el músico ahora aparece en "Integrantes actuales".
        // El ViewModel hace optimistic update local, por lo que aparece en current_member_<id>
        // aunque el repository fake no persista el cambio.
        val currentMember = tagStartsWith("current_member_")
        composeRule.waitUntil(timeoutMs) {
            composeRule.onAllNodes(currentMember).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(currentMember)[0].assertExists()
    }

    // -- HU010: Agregar artistas favoritos -----------------------------------

    /**
     * HU010: flujo completo de agregar un músico favorito a un coleccionista.
     * Bottom-nav → lista de coleccionistas → detalle → "+" en sección
     * "Artistas favoritos" → picker → tab Músicos → "+" en disponibles →
     * verificación de que aparece en "Mis músicos favoritos".
     */
    @Test
    fun hu010_addFavoriteMusicianToCollector_flow() {
        composeRule.onNodeWithTag("bottom_nav_collectors").performClick()
        waitForTag("collectors_list")

        val firstCard = tagStartsWith("collector_card_")
        composeRule.onAllNodes(firstCard)[0].performClick()
        waitForTag("collector_detail_root")

        composeRule.onNodeWithTag("collector_detail_add_favorite_performer").performClick()

        waitForTag("add_favorite_performer_screen_root")
        val availablePerformer = tagStartsWith("available_favorite_performer_")
        composeRule.waitUntil(timeoutMs) {
            composeRule.onAllNodes(availablePerformer).fetchSemanticsNodes().isNotEmpty()
        }
        // El "+" expone contentDescription "Agregar {nombre} a favoritos".
        val addFavoriteButton = SemanticsMatcher("ContentDescription ends with ' a favoritos'") { node ->
            val cd = node.config.getOrNull(SemanticsProperties.ContentDescription) ?: return@SemanticsMatcher false
            cd.any { it.startsWith("Agregar ") && it.endsWith(" a favoritos") }
        }
        composeRule.waitUntil(timeoutMs) {
            composeRule.onAllNodes(addFavoriteButton).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(addFavoriteButton)[0].performClick()

        val currentFavorite = tagStartsWith("current_favorite_performer_")
        composeRule.waitUntil(timeoutMs) {
            composeRule.onAllNodes(currentFavorite).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(currentFavorite)[0].assertExists()
    }

    /**
     * HU010: el picker tiene tab "Bandas" que muestra bandas disponibles para
     * marcar como favoritas.
     */
    @Test
    fun hu010_addFavoriteBandToCollector_flow() {
        composeRule.onNodeWithTag("bottom_nav_collectors").performClick()
        waitForTag("collectors_list")

        val firstCard = tagStartsWith("collector_card_")
        composeRule.onAllNodes(firstCard)[0].performClick()
        waitForTag("collector_detail_root")

        composeRule.onNodeWithTag("collector_detail_add_favorite_performer").performClick()
        waitForTag("add_favorite_performer_screen_root")

        composeRule.onNodeWithTag("add_favorite_performer_tab_bands").performClick()

        val availableBand = tagStartsWith("available_favorite_band_")
        composeRule.waitUntil(timeoutMs) {
            composeRule.onAllNodes(availableBand).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(availableBand)[0].performClick()

        val currentBand = tagStartsWith("current_favorite_band_")
        composeRule.waitUntil(timeoutMs) {
            composeRule.onAllNodes(currentBand).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(currentBand)[0].assertExists()
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

    /** Matcher que encuentra cualquier nodo cuyo testTag empiece con el prefijo dado. */
    private fun tagStartsWith(prefix: String): SemanticsMatcher =
        SemanticsMatcher("TestTag starts with '$prefix'") { node ->
            val tag = node.config.getOrNull(SemanticsProperties.TestTag) ?: return@SemanticsMatcher false
            tag.startsWith(prefix)
        }
}
