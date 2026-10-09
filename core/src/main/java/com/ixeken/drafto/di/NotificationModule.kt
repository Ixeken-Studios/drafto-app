package com.ixeken.drafto.di

import com.ixeken.drafto.data.repository.LiveNoteRepositoryImpl
import com.ixeken.drafto.domain.repository.LiveNoteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de inyección de dependencias Hilt para enlazar el repositorio de notas en vivo.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    @Binds
    @Singleton
    abstract fun bindLiveNoteRepository(
        impl: LiveNoteRepositoryImpl
    ): LiveNoteRepository
}

