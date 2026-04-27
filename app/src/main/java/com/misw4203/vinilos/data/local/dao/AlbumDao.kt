package com.misw4203.vinilos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.misw4203.vinilos.data.local.entity.AlbumDetailEntity
import com.misw4203.vinilos.data.local.entity.AlbumEntity

@Dao
interface AlbumDao {

    @Query("SELECT * FROM albums ORDER BY name ASC")
    suspend fun getAll(): List<AlbumEntity>

    @Upsert
    suspend fun upsertAll(albums: List<AlbumEntity>)

    @Query("DELETE FROM albums")
    suspend fun clear()

    @Transaction
    suspend fun replaceAlbums(albums: List<AlbumEntity>) {
        clear()
        upsertAll(albums)
    }

    @Query("SELECT * FROM album_details WHERE id = :id")
    suspend fun getDetailById(id: Long): AlbumDetailEntity?

    @Upsert
    suspend fun upsertDetail(detail: AlbumDetailEntity)
}
