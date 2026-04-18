package com.misw4203.vinilos.presentation.navigation

object Destinations {
    const val AlbumList = "album_list"
    const val AlbumDetail = "album_detail/{albumId}"
    const val AlbumDetailArg = "albumId"

    const val ArtistList = "artists"
    const val Collectors = "collectors"

    fun albumDetail(albumId: Long) = "album_detail/$albumId"
}
