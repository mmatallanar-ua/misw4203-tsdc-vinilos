package com.misw4203.vinilos.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.CollectorAlbum
import com.misw4203.vinilos.domain.model.CollectorComment
import com.misw4203.vinilos.domain.model.Comment
import com.misw4203.vinilos.domain.model.MusicianPrize
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.model.Performer
import com.misw4203.vinilos.domain.model.Track
import java.lang.reflect.Type

class Converters {

    private companion object {
        val GSON = Gson()
        val TRACK_LIST: Type = object : TypeToken<List<Track>>() {}.type
        val PERFORMER_LIST: Type = object : TypeToken<List<Performer>>() {}.type
        val COMMENT_LIST: Type = object : TypeToken<List<Comment>>() {}.type
        val ALBUM_LIST: Type = object : TypeToken<List<Album>>() {}.type
        val PRIZE_LIST: Type = object : TypeToken<List<MusicianPrize>>() {}.type
        val COLLECTOR_ALBUM_LIST: Type = object : TypeToken<List<CollectorAlbum>>() {}.type
        val COLLECTOR_COMMENT_LIST: Type = object : TypeToken<List<CollectorComment>>() {}.type
        val MUSICIAN_SUMMARY_LIST: Type = object : TypeToken<List<MusicianSummary>>() {}.type
    }

    /**
     * Deserializa un blob JSON a lista. Devuelve [emptyList] ante cualquier
     * fallo de parseo (null/blob malformado/cadena en blanco); la recuperación
     * real depende de la estrategia network-first del repositorio.
     */
    private inline fun <T> decode(value: String, type: Type): List<T> =
        runCatching { GSON.fromJson<List<T>>(value, type) }.getOrNull() ?: emptyList()

    @TypeConverter
    fun tracksToJson(value: List<Track>): String = GSON.toJson(value)

    @TypeConverter
    fun jsonToTracks(value: String): List<Track> = decode(value, TRACK_LIST)

    @TypeConverter
    fun performersToJson(value: List<Performer>): String = GSON.toJson(value)

    @TypeConverter
    fun jsonToPerformers(value: String): List<Performer> = decode(value, PERFORMER_LIST)

    @TypeConverter
    fun commentsToJson(value: List<Comment>): String = GSON.toJson(value)

    @TypeConverter
    fun jsonToComments(value: String): List<Comment> = decode(value, COMMENT_LIST)

    @TypeConverter
    fun albumsToJson(value: List<Album>): String = GSON.toJson(value)

    @TypeConverter
    fun jsonToAlbums(value: String): List<Album> = decode(value, ALBUM_LIST)

    @TypeConverter
    fun prizesToJson(value: List<MusicianPrize>): String = GSON.toJson(value)

    @TypeConverter
    fun jsonToPrizes(value: String): List<MusicianPrize> = decode(value, PRIZE_LIST)

    @TypeConverter
    fun collectorAlbumsToJson(value: List<CollectorAlbum>): String = GSON.toJson(value)

    @TypeConverter
    fun jsonToCollectorAlbums(value: String): List<CollectorAlbum> =
        decode(value, COLLECTOR_ALBUM_LIST)

    @TypeConverter
    fun collectorCommentsToJson(value: List<CollectorComment>): String = GSON.toJson(value)

    @TypeConverter
    fun jsonToCollectorComments(value: String): List<CollectorComment> =
        decode(value, COLLECTOR_COMMENT_LIST)

    @TypeConverter
    fun musicianSummariesToJson(value: List<MusicianSummary>): String = GSON.toJson(value)

    @TypeConverter
    fun jsonToMusicianSummaries(value: String): List<MusicianSummary> =
        decode(value, MUSICIAN_SUMMARY_LIST)
}
