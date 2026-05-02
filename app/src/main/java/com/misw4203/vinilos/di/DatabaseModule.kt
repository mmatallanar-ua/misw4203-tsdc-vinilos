package com.misw4203.vinilos.di

import android.content.Context
import androidx.room.Room
import com.misw4203.vinilos.data.local.dao.AlbumDao
import com.misw4203.vinilos.data.local.dao.CollectorDao
import com.misw4203.vinilos.data.local.dao.MusicianDao
import com.misw4203.vinilos.data.local.database.VinilosDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VinilosDatabase =
        Room.databaseBuilder(context, VinilosDatabase::class.java, "vinilos.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideAlbumDao(db: VinilosDatabase): AlbumDao = db.albumDao()

    @Provides
    fun provideMusicianDao(db: VinilosDatabase): MusicianDao = db.musicianDao()

    @Provides
    fun provideCollectorDao(db: VinilosDatabase): CollectorDao = db.collectorDao()
}
