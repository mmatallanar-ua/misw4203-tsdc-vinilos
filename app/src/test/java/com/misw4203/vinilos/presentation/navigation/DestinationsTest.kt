package com.misw4203.vinilos.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM test for [Destinations]: it is a plain object of String constants
 * and route-builder functions, so no Android/Robolectric is required.
 */
class DestinationsTest {

    @Test
    fun `albumDetail builds album_detail route`() {
        assertEquals("album_detail/7", Destinations.albumDetail(7L))
    }

    @Test
    fun `artistDetail builds artist route`() {
        assertEquals("artist/3", Destinations.artistDetail(3))
    }

    @Test
    fun `collectorDetail builds collector route`() {
        assertEquals("collector/9", Destinations.collectorDetail(9))
    }

    @Test
    fun `bandDetail builds band route`() {
        assertEquals("band/2", Destinations.bandDetail(2))
    }

    @Test
    fun `addTrack builds add track route`() {
        assertEquals("album/4/track/add", Destinations.addTrack(4L))
    }

    @Test
    fun `addPerformerToAlbum builds add performer route`() {
        assertEquals("album/4/performers/add", Destinations.addPerformerToAlbum(4L))
    }

    @Test
    fun `addComment builds add comment route with default collector`() {
        assertEquals(
            "album/4/comment/add/${Destinations.DefaultCollectorId}",
            Destinations.addComment(4L),
        )
    }

    @Test
    fun `addComment builds add comment route with explicit collector`() {
        assertEquals("album/4/comment/add/8", Destinations.addComment(4L, 8))
    }

    @Test
    fun `addMusiciansToBand builds route`() {
        assertEquals("band/2/musicians/add", Destinations.addMusiciansToBand(2))
    }

    @Test
    fun `addAlbumToBand builds route`() {
        assertEquals("band/2/albums/add", Destinations.addAlbumToBand(2))
    }

    @Test
    fun `addPrizeToBand builds route`() {
        assertEquals("band/2/prizes/add", Destinations.addPrizeToBand(2))
    }

    @Test
    fun `addAlbumToCollector builds route`() {
        assertEquals("collector/9/albums/add", Destinations.addAlbumToCollector(9))
    }

    @Test
    fun `addAlbumToMusician builds route`() {
        assertEquals("musician/3/albums/add", Destinations.addAlbumToMusician(3))
    }

    @Test
    fun `addPrizeToMusician builds route`() {
        assertEquals("musician/3/prizes/add", Destinations.addPrizeToMusician(3))
    }

    @Test
    fun `addFavoritePerformer builds route`() {
        assertEquals("collector/9/favorites/add", Destinations.addFavoritePerformer(9))
    }

    @Test
    fun `TrackAddedKey is track_added`() {
        assertEquals("track_added", Destinations.TrackAddedKey)
    }

    @Test
    fun `ArtistRoutePrefix matches artist detail route prefix`() {
        assertEquals("artist/", Destinations.ArtistRoutePrefix)
        assertTrue(Destinations.ArtistDetail.startsWith(Destinations.ArtistRoutePrefix))
    }

    @Test
    fun `BandRoutePrefix matches band detail route prefix`() {
        assertEquals("band/", Destinations.BandRoutePrefix)
        assertTrue(Destinations.BandDetail.startsWith(Destinations.BandRoutePrefix))
    }

    @Test
    fun `MusicianRoutePrefix matches add album to musician route prefix`() {
        assertEquals("musician/", Destinations.MusicianRoutePrefix)
        assertTrue(Destinations.AddAlbumToMusician.startsWith(Destinations.MusicianRoutePrefix))
    }

    @Test
    fun `CollectorRoutePrefix matches collector detail route prefix`() {
        assertEquals("collector/", Destinations.CollectorRoutePrefix)
        assertTrue(Destinations.CollectorDetail.startsWith(Destinations.CollectorRoutePrefix))
    }
}
