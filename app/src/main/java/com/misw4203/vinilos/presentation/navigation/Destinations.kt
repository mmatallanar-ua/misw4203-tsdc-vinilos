package com.misw4203.vinilos.presentation.navigation

object Destinations {
    const val AlbumList = "album_list"
    const val AlbumDetail = "album_detail/{albumId}"
    const val AlbumDetailArg = "albumId"
    const val CreateAlbum = "create_album"

    const val ArtistList = "artists"

    const val Collectors = "collectors"
    const val CollectorDetail = "collector/{collectorId}"
    const val CollectorDetailArg = "collectorId"

    fun albumDetail(albumId: Long) = "album_detail/$albumId"
    fun collectorDetail(collectorId: Int) = "collector/$collectorId"
}
