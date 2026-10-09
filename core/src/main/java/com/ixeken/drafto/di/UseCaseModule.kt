package com.ixeken.drafto.di

import com.ixeken.drafto.domain.repository.BackupRepository
import com.ixeken.drafto.domain.repository.NoteRepository
import com.ixeken.drafto.domain.usecase.DeleteNoteUseCase
import com.ixeken.drafto.domain.usecase.ExportDataUseCase
import com.ixeken.drafto.domain.usecase.GetNotesUseCase
import com.ixeken.drafto.domain.usecase.ImportDataUseCase
import com.ixeken.drafto.domain.usecase.SaveNoteUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideGetNotesUseCase(repository: NoteRepository): GetNotesUseCase {
        return GetNotesUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideSaveNoteUseCase(repository: NoteRepository): SaveNoteUseCase {
        return SaveNoteUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideDeleteNoteUseCase(repository: NoteRepository): DeleteNoteUseCase {
        return DeleteNoteUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideExportDataUseCase(backupRepository: BackupRepository): ExportDataUseCase {
        return ExportDataUseCase(backupRepository)
    }

    @Provides
    @Singleton
    fun provideImportDataUseCase(
        backupRepository: BackupRepository,
        noteRepository: NoteRepository
    ): ImportDataUseCase {
        return ImportDataUseCase(backupRepository, noteRepository)
    }
}

