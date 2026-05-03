package com.misw4203.vinilos.presentation.navigation

object Destinations {
    const val AlbumList = "album_list"
    const val AlbumDetail = "album_detail/{albumId}"
    const val AlbumDetailArg = "albumId"

    const val ArtistList = "artists"

    const val Collectors = "collectors"
    const val CollectorDetail = "collector/{collectorId}"
    const val CollectorDetailArg = "collectorId"

    const val AddTrack = "album/{albumId}/track/add"
    const val AddTrackAlbumArg = "albumId"

    fun albumDetail(albumId: Long) = "album_detail/$albumId"
    fun collectorDetail(collectorId: Int) = "collector/$collectorId"
    fun addTrack(albumId: Long) = "album/$albumId/track/add"
}
