package com.misw4203.vinilos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.misw4203.vinilos.data.local.entity.MusicianDetailEntity
import com.misw4203.vinilos.data.local.entity.MusicianListEntity

@Dao
interface MusicianDao {

    @Query("SELECT * FROM musicians")
    suspend fun getMusicians(): List<MusicianListEntity>

    @Upsert
    suspend fun upsertMusicians(musicians: List<MusicianListEntity>)

    @Query("DELETE FROM musicians")
    suspend fun clearMusicians()

    @Transaction
    suspend fun replaceMusicians(musicians: List<MusicianListEntity>) {
        clearMusicians()
        upsertMusicians(musicians)
    }

    @Query("SELECT * FROM musician_detail WHERE id = :id")
    suspend fun getMusicianDetail(id: Int): MusicianDetailEntity?

    @Upsert
    suspend fun upsertMusicianDetail(musician: MusicianDetailEntity)
}
