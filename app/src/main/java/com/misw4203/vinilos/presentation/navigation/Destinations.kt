package com.misw4203.vinilos.presentation.navigation

object Destinations {
    /** Tab-prefix constants: literal prefix shared by every detail/add route of a tab. */
    const val ArtistRoutePrefix = "artist/"
    const val BandRoutePrefix = "band/"
    const val MusicianRoutePrefix = "musician/"
    const val CollectorRoutePrefix = "collector/"

    const val AlbumList = "album_list"
    const val AlbumDetail = "album_detail/{albumId}"
    const val AlbumDetailArg = "albumId"
    const val CreateAlbum = "create_album"

    const val ArtistList = "artists"
    const val ArtistDetail = "artist/{id}"
    const val ArtistDetailArg = "id"

    const val Collectors = "collectors"
    const val CollectorDetail = "collector/{collectorId}"
    const val CollectorDetailArg = "collectorId"

    const val AddTrack = "album/{albumId}/track/add"
    const val AddTrackAlbumArg = "albumId"

    const val AddComment = "album/{albumId}/comment/add/{collectorId}"
    const val AddCommentAlbumArg = "albumId"
    const val AddCommentCollectorArg = "collectorId"

    /**
     * The current build does not have a logged-in collector concept; HU09 spec
     * requires the `collector` field in the POST body. We use this default id
     * so the screen has a valid reference. Replace once auth is added.
     *
     * @see docs/adr/0001-sin-concepto-de-sesion.md (deuda aceptada)
     */
    const val DefaultCollectorId = 100

    const val RefreshAlbumDetailKey = "refresh_album_detail"
    const val TrackAddedKey = "track_added"

    const val AddPerformerToAlbum = "album/{albumId}/performers/add"
    const val AddPerformerAlbumArg = "albumId"

    const val BandDetail = "band/{bandId}"
    const val BandDetailArg = "bandId"
    const val AddMusiciansToBand = "band/{bandId}/musicians/add"
    const val AddMusiciansBandArg = "bandId"
    const val RefreshBandDetailKey = "refresh_band_detail"

    const val AddAlbumToBand = "band/{bandId}/albums/add"
    const val AddAlbumBandArg = "bandId"

    const val AddPrizeToBand = "band/{bandId}/prizes/add"
    const val AddPrizeBandArg = "bandId"

    const val AddAlbumToCollector = "collector/{collectorId}/albums/add"
    const val AddAlbumCollectorArg = "collectorId"
    const val RefreshCollectorDetailKey = "refresh_collector_detail"

    const val AddAlbumToMusician = "musician/{musicianId}/albums/add"
    const val AddAlbumMusicianArg = "musicianId"
    const val RefreshMusicianDetailKey = "refresh_musician_detail"

    const val Prizes = "prizes"
    const val CreatePrize = "prizes/create"
    const val RefreshPrizesKey = "refresh_prizes"

    const val AddFavoritePerformer = "collector/{collectorId}/favorites/add"
    const val AddFavoritePerformerCollectorArg = "collectorId"

    fun albumDetail(albumId: Long) = "album_detail/$albumId"
    fun artistDetail(id: Int) = "artist/$id"
    fun collectorDetail(collectorId: Int) = "collector/$collectorId"
    fun addTrack(albumId: Long) = "album/$albumId/track/add"
    fun addPerformerToAlbum(albumId: Long) = "album/$albumId/performers/add"
    fun addComment(albumId: Long, collectorId: Int = DefaultCollectorId) =
        "album/$albumId/comment/add/$collectorId"
    fun bandDetail(bandId: Int) = "band/$bandId"
    fun addMusiciansToBand(bandId: Int) = "band/$bandId/musicians/add"
    fun addAlbumToBand(bandId: Int) = "band/$bandId/albums/add"
    fun addPrizeToBand(bandId: Int) = "band/$bandId/prizes/add"
    fun addAlbumToCollector(collectorId: Int) = "collector/$collectorId/albums/add"
    fun addAlbumToMusician(musicianId: Int) = "musician/$musicianId/albums/add"

    const val AddPrizeToMusician = "musician/{musicianId}/prizes/add"
    const val AddPrizeMusicianArg = "musicianId"

    fun addPrizeToMusician(musicianId: Int) = "musician/$musicianId/prizes/add"
    fun addFavoritePerformer(collectorId: Int) = "collector/$collectorId/favorites/add"
}
