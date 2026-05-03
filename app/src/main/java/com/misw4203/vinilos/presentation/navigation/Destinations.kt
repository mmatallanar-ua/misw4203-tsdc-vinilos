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

    const val AddComment = "album/{albumId}/comment/add/{collectorId}"
    const val AddCommentAlbumArg = "albumId"
    const val AddCommentCollectorArg = "collectorId"

    /**
     * The current build does not have a logged-in collector concept; HU09 spec
     * requires the `collector` field in the POST body. We use this default id
     * so the screen has a valid reference. Replace once auth is added.
     */
    const val DefaultCollectorId = 100

    const val RefreshAlbumDetailKey = "refresh_album_detail"

    fun albumDetail(albumId: Long) = "album_detail/$albumId"
    fun collectorDetail(collectorId: Int) = "collector/$collectorId"
    fun addComment(albumId: Long, collectorId: Int = DefaultCollectorId) =
        "album/$albumId/comment/add/$collectorId"
}
