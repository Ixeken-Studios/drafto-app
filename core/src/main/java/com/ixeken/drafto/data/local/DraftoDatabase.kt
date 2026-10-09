package com.ixeken.drafto.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ixeken.drafto.data.local.converter.Converters
import com.ixeken.drafto.data.local.dao.BookmarkDao
import com.ixeken.drafto.data.local.dao.CollectionDao
import com.ixeken.drafto.data.local.dao.NoteDao
import com.ixeken.drafto.data.local.dao.TodoDao
import com.ixeken.drafto.data.local.entity.BookmarkEntity
import com.ixeken.drafto.data.local.entity.CollectionEntity
import com.ixeken.drafto.data.local.entity.NoteEntity
import com.ixeken.drafto.data.local.entity.TodoEntity

@Database(
    entities = [
        NoteEntity::class,
        BookmarkEntity::class,
        CollectionEntity::class,
        TodoEntity::class
    ],
    version = 10,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DraftoDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun todoDao(): TodoDao
    abstract fun collectionDao(): CollectionDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Crear tabla bookmarks
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS bookmarks (
                        id TEXT NOT NULL,
                        url TEXT NOT NULL,
                        title TEXT,
                        description TEXT,
                        imageUrl TEXT,
                        domain TEXT NOT NULL,
                        faviconUrl TEXT,
                        createdAt INTEGER NOT NULL,
                        isPinned INTEGER NOT NULL,
                        platform TEXT NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )

                // Crear índices de bookmarks
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_url ON bookmarks(url)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_createdAt ON bookmarks(createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_isPinned ON bookmarks(isPinned)")

                // Crear tabla bookmark_collections
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS bookmark_collections (
                        id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        colorHex TEXT NOT NULL,
                        iconName TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )

                // Crear índice de bookmark_collections
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bookmark_collections_name ON bookmark_collections(name)")

                // Crear tabla bookmark_collection_cross_ref
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS bookmark_collection_cross_ref (
                        bookmarkId TEXT NOT NULL,
                        collectionId TEXT NOT NULL,
                        PRIMARY KEY(bookmarkId, collectionId),
                        FOREIGN KEY(bookmarkId) REFERENCES bookmarks(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(collectionId) REFERENCES bookmark_collections(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                // Crear índice de bookmark_collection_cross_ref
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bookmark_collection_cross_ref_collectionId ON bookmark_collection_cross_ref(collectionId)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `todos` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `isPinned` INTEGER NOT NULL,
                        `dueDate` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `completedAt` INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_todos_isCompleted_createdAt` ON `todos` (`isCompleted`, `createdAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_todos_isPinned` ON `todos` (`isPinned`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bookmarks ADD COLUMN isPreviewVisible INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `chat_messages`")
                db.execSQL("DROP TABLE IF EXISTS `chat_threads`")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Renombrar tabla bookmark_collections a collections
                db.execSQL("ALTER TABLE bookmark_collections RENAME TO collections")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_collections_name ON collections(name)")

                // 2. Agregar columna collectionId a notes
                db.execSQL("ALTER TABLE notes ADD COLUMN collectionId TEXT DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_collectionId ON notes(collectionId)")

                // 3. Agregar columna collectionId a todos
                db.execSQL("ALTER TABLE todos ADD COLUMN collectionId TEXT DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_todos_collectionId ON todos(collectionId)")

                // 4. Agregar columna collectionId a bookmarks
                db.execSQL("ALTER TABLE bookmarks ADD COLUMN collectionId TEXT DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_collectionId ON bookmarks(collectionId)")

                // 5. Migrar asociaciones existentes desde bookmark_collection_cross_ref a bookmarks.collectionId
                db.execSQL(
                    """
                    UPDATE bookmarks SET collectionId = (
                        SELECT collectionId FROM bookmark_collection_cross_ref
                        WHERE bookmark_collection_cross_ref.bookmarkId = bookmarks.id
                        LIMIT 1
                    )
                    """.trimIndent()
                )

                // 6. Eliminar tabla intermedia de cruce
                db.execSQL("DROP TABLE IF EXISTS bookmark_collection_cross_ref")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Agregar columna subtasks para listas de tareas estructuradas con formato JSON por defecto vacío
                db.execSQL("ALTER TABLE todos ADD COLUMN subtasks TEXT NOT NULL DEFAULT '[]'")
            }
        }
    }
}
