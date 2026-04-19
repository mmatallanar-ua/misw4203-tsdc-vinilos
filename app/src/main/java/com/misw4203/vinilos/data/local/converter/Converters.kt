package com.misw4203.vinilos.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.CollectorAlbum
import com.misw4203.vinilos.domain.model.Comment
import com.misw4203.vinilos.domain.model.FavoritePerformer
import com.misw4203.vinilos.domain.model.MusicianPrize
import com.misw4203.vinilos.domain.model.Performer
import com.misw4203.vinilos.domain.model.Track

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun tracksToJson(value: List<Track>): String = gson.toJson(value)

    @TypeConverter
    fun jsonToTracks(value: String): List<Track> =
        gson.fromJson(value, object : TypeToken<List<Track>>() {}.type)

    @TypeConverter
    fun performersToJson(value: List<Performer>): String = gson.toJson(value)

    @TypeConverter
    fun jsonToPerformers(value: String): List<Performer> =
        gson.fromJson(value, object : TypeToken<List<Performer>>() {}.type)

    @TypeConverter
    fun commentsToJson(value: List<Comment>): String = gson.toJson(value)

    @TypeConverter
    fun jsonToComments(value: String): List<Comment> =
        gson.fromJson(value, object : TypeToken<List<Comment>>() {}.type)

    @TypeConverter
    fun albumsToJson(value: List<Album>): String = gson.toJson(value)

    @TypeConverter
    fun jsonToAlbums(value: String): List<Album> =
        gson.fromJson(value, object : TypeToken<List<Album>>() {}.type)

    @TypeConverter
    fun prizesToJson(value: List<MusicianPrize>): String = gson.toJson(value)

    @TypeConverter
    fun jsonToPrizes(value: String): List<MusicianPrize> =
        gson.fromJson(value, object : TypeToken<List<MusicianPrize>>() {}.type)

    @TypeConverter
    fun favoritePerformersToJson(value: List<FavoritePerformer>): String = gson.toJson(value)

    @TypeConverter
    fun jsonToFavoritePerformers(value: String): List<FavoritePerformer> =
        gson.fromJson(value, object : TypeToken<List<FavoritePerformer>>() {}.type)

    @TypeConverter
    fun collectorAlbumsToJson(value: List<CollectorAlbum>): String = gson.toJson(value)

    @TypeConverter
    fun jsonToCollectorAlbums(value: String): List<CollectorAlbum> =
        gson.fromJson(value, object : TypeToken<List<CollectorAlbum>>() {}.type)
}
