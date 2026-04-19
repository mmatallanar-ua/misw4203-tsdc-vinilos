package com.misw4203.vinilos.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.misw4203.vinilos.data.local.converter.Converters
import com.misw4203.vinilos.data.local.dao.AlbumDao
import com.misw4203.vinilos.data.local.dao.MusicianDao
import com.misw4203.vinilos.data.local.entity.AlbumDetailEntity
import com.misw4203.vinilos.data.local.entity.AlbumEntity
import com.misw4203.vinilos.data.local.entity.MusicianDetailEntity
import com.misw4203.vinilos.data.local.entity.MusicianListEntity

@Database(
    entities = [
        AlbumEntity::class,
        AlbumDetailEntity::class,
        MusicianListEntity::class,
        MusicianDetailEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class VinilosDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao
    abstract fun musicianDao(): MusicianDao

    companion object {
        const val DATABASE_NAME = "vinilos.db"
    }
}
