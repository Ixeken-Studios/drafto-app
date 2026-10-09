package com.ixeken.drafto.di

import android.content.Context
import com.ixeken.drafto.data.local.dao.BookmarkDao
import com.ixeken.drafto.data.local.dao.NoteDao
import com.ixeken.drafto.data.local.dao.TodoDao
import com.ixeken.drafto.data.repository.BackupRepositoryImpl
import com.ixeken.drafto.data.repository.BookmarkRepositoryImpl
import com.ixeken.drafto.data.repository.NoteRepositoryImpl
import com.ixeken.drafto.data.repository.TodoRepositoryImpl
import com.ixeken.drafto.domain.repository.BackupRepository
import com.ixeken.drafto.domain.repository.BookmarkRepository
import com.ixeken.drafto.domain.repository.NoteRepository
import com.ixeken.drafto.domain.repository.TodoRepository
import com.ixeken.drafto.data.backup.DataStorageCoordinator
import com.ixeken.drafto.data.backup.MasterBackupManager
import com.ixeken.drafto.data.local.DraftoDatabase
import com.ixeken.drafto.data.local.datastore.SettingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideNoteRepository(noteDao: NoteDao): NoteRepository {
        return NoteRepositoryImpl(noteDao)
    }

    @Provides
    @Singleton
    fun provideBackupRepository(
        noteRepository: NoteRepository
    ): BackupRepository {
        return BackupRepositoryImpl(noteRepository)
    }

    @Provides
    @Singleton
    fun provideBookmarkRepository(
        bookmarkDao: BookmarkDao,
        collectionDao: com.ixeken.drafto.data.local.dao.CollectionDao
    ): BookmarkRepository {
        return BookmarkRepositoryImpl(bookmarkDao, collectionDao)
    }

    @Provides
    @Singleton
    fun provideBookmarkBackupManager(bookmarkRepository: BookmarkRepository): com.ixeken.drafto.data.backup.BookmarkBackupManager {
        return com.ixeken.drafto.data.backup.BookmarkBackupManager(bookmarkRepository)
    }

    @Provides
    @Singleton
    fun provideTodoRepository(todoDao: TodoDao): TodoRepository {
        return TodoRepositoryImpl(todoDao)
    }

    @Provides
    @Singleton
    fun provideNotebookBackupManager(
        noteRepository: NoteRepository,
        todoRepository: TodoRepository
    ): com.ixeken.drafto.data.backup.NotebookBackupManager {
        return com.ixeken.drafto.data.backup.NotebookBackupManager(noteRepository, todoRepository)
    }

    @Provides
    @Singleton
    fun provideDataStorageCoordinator(
        @ApplicationContext context: Context,
        draftoDatabase: DraftoDatabase,
        settingsDataStore: SettingsDataStore
    ): DataStorageCoordinator {
        return DataStorageCoordinator(
            context = context,
            draftoDatabase = draftoDatabase,
            settingsDataStore = settingsDataStore
        )
    }

    @Provides
    @Singleton
    fun provideMasterBackupManager(
        @ApplicationContext context: Context,
        noteRepository: NoteRepository,
        todoRepository: TodoRepository,
        bookmarkRepository: BookmarkRepository,
        settingsDataStore: SettingsDataStore,
        dataStorageCoordinator: DataStorageCoordinator
    ): MasterBackupManager {
        return MasterBackupManager(
            context = context,
            noteRepository = noteRepository,
            todoRepository = todoRepository,
            bookmarkRepository = bookmarkRepository,
            settingsDataStore = settingsDataStore,
            dataStorageCoordinator = dataStorageCoordinator
        )
    }

    @Provides
    @Singleton
    fun provideCollectionRepository(
        collectionDao: com.ixeken.drafto.data.local.dao.CollectionDao
    ): com.ixeken.drafto.domain.repository.CollectionRepository {
        return com.ixeken.drafto.data.repository.CollectionRepositoryImpl(collectionDao)
    }
}

