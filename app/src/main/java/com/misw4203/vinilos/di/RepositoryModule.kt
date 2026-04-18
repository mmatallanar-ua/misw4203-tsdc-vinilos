package com.misw4203.vinilos.di

import com.misw4203.vinilos.data.repository.MusicianRepositoryImpl
import com.misw4203.vinilos.domain.repository.MusicianRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMusicianRepository(
        impl: MusicianRepositoryImpl
    ): MusicianRepository
}
