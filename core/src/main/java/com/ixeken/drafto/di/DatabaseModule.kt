package com.ixeken.drafto.di

import android.content.Context
import androidx.room.Room
import com.ixeken.drafto.data.local.DraftoDatabase
import com.ixeken.drafto.data.local.dao.BookmarkDao
import com.ixeken.drafto.data.local.dao.NoteDao
import com.ixeken.drafto.data.local.dao.TodoDao
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
    fun provideDraftoDatabase(
        @ApplicationContext context: Context
    ): DraftoDatabase {
        return Room.databaseBuilder(
            context,
            DraftoDatabase::class.java,
            "drafto_database.db"
        )
            .addMigrations(
                DraftoDatabase.MIGRATION_4_5,
                DraftoDatabase.MIGRATION_5_6,
                DraftoDatabase.MIGRATION_6_7,
                DraftoDatabase.MIGRATION_7_8,
                DraftoDatabase.MIGRATION_8_9,
                DraftoDatabase.MIGRATION_9_10
            )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    @Singleton
    fun provideNoteDao(database: DraftoDatabase): NoteDao {
        return database.noteDao()
    }

    @Provides
    @Singleton
    fun provideBookmarkDao(database: DraftoDatabase): BookmarkDao {
        return database.bookmarkDao()
    }

    @Provides
    @Singleton
    fun provideTodoDao(database: DraftoDatabase): TodoDao {
        return database.todoDao()
    }

    @Provides
    @Singleton
    fun provideCollectionDao(database: DraftoDatabase): com.ixeken.drafto.data.local.dao.CollectionDao {
        return database.collectionDao()
    }
}


