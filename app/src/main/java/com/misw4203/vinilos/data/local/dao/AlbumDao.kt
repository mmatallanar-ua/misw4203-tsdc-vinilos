package com.misw4203.vinilos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.misw4203.vinilos.data.local.entity.AlbumDetailEntity
import com.misw4203.vinilos.data.local.entity.AlbumEntity

@Dao
interface AlbumDao {

    @Query("SELECT * FROM albums")
    suspend fun getAlbums(): List<AlbumEntity>

    @Upsert
    suspend fun upsertAlbums(albums: List<AlbumEntity>)

    @Query("DELETE FROM albums")
    suspend fun clearAlbums()

    @Transaction
    suspend fun replaceAlbums(albums: List<AlbumEntity>) {
        clearAlbums()
        upsertAlbums(albums)
    }

    @Query("SELECT * FROM album_detail WHERE id = :id")
    suspend fun getAlbumDetail(id: Long): AlbumDetailEntity?

    @Upsert
    suspend fun upsertAlbumDetail(detail: AlbumDetailEntity)
}
