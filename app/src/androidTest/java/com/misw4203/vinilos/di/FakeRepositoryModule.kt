package com.misw4203.vinilos.di

import com.misw4203.vinilos.domain.repository.AlbumRepository
import com.misw4203.vinilos.domain.repository.MusicianRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class],
)
abstract class FakeRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAlbumRepository(impl: FakeAlbumRepository): AlbumRepository

    @Binds
    @Singleton
    abstract fun bindMusicianRepository(impl: FakeMusicianRepository): MusicianRepository
}
