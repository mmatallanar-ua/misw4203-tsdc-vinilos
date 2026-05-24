package com.misw4203.vinilos.presentation.navigation

import com.misw4203.vinilos.presentation.ui.components.VinilosDestination
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure JVM test for [selectedTab]: it maps a current nav route String to the
 * [VinilosDestination] bottom-nav tab, replicating the `when` that used to live
 * inline in [VinilosNavHost]. No Android/Robolectric required.
 */
class SelectedTabTest {

    @Test
    fun `album list maps to Albums`() {
        assertEquals(VinilosDestination.Albums, selectedTab(Destinations.AlbumList))
    }

    @Test
    fun `create album maps to Albums`() {
        assertEquals(VinilosDestination.Albums, selectedTab(Destinations.CreateAlbum))
    }

    @Test
    fun `album detail route maps to Albums`() {
        assertEquals(VinilosDestination.Albums, selectedTab(Destinations.AlbumDetail))
    }

    @Test
    fun `artist list maps to Artists`() {
        assertEquals(VinilosDestination.Artists, selectedTab(Destinations.ArtistList))
    }

    @Test
    fun `artist detail concrete route maps to Artists`() {
        assertEquals(VinilosDestination.Artists, selectedTab("artist/5"))
    }

    @Test
    fun `band detail concrete route maps to Artists`() {
        assertEquals(VinilosDestination.Artists, selectedTab("band/2"))
    }

    @Test
    fun `musician add route maps to Artists`() {
        assertEquals(VinilosDestination.Artists, selectedTab("musician/3"))
    }

    @Test
    fun `collectors maps to Collectors`() {
        assertEquals(VinilosDestination.Collectors, selectedTab(Destinations.Collectors))
    }

    @Test
    fun `collector detail concrete route maps to Collectors`() {
        assertEquals(VinilosDestination.Collectors, selectedTab("collector/9"))
    }

    @Test
    fun `prizes maps to Prizes`() {
        assertEquals(VinilosDestination.Prizes, selectedTab(Destinations.Prizes))
    }

    @Test
    fun `create prize maps to Prizes`() {
        assertEquals(VinilosDestination.Prizes, selectedTab(Destinations.CreatePrize))
    }

    @Test
    fun `null route falls back to Albums`() {
        assertEquals(VinilosDestination.Albums, selectedTab(null))
    }
}
