package com.misw4203.vinilos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.misw4203.vinilos.data.local.entity.MusicianDetailEntity
import com.misw4203.vinilos.data.local.entity.MusicianListEntity

@Dao
interface MusicianDao {

    @Query("SELECT * FROM musicians ORDER BY name ASC")
    suspend fun getAll(): List<MusicianListEntity>

    @Upsert
    suspend fun upsertAll(musicians: List<MusicianListEntity>)

    @Query("DELETE FROM musicians")
    suspend fun clear()

    @Transaction
    suspend fun replaceMusicians(musicians: List<MusicianListEntity>) {
        clear()
        upsertAll(musicians)
    }

    @Query("SELECT * FROM musician_details WHERE id = :id")
    suspend fun getDetailById(id: Int): MusicianDetailEntity?

    @Upsert
    suspend fun upsertDetail(detail: MusicianDetailEntity)
}
