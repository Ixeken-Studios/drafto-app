package com.ixeken.drafto.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ixeken.drafto.data.local.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO reactivo para operaciones CRUD y consultas avanzadas sobre marcadores y colecciones.
 *
 * Utiliza flujos de corrutinas (`Flow`) para emitir actualizaciones automáticas en la UI
 * ante cualquier mutación en las tablas SQLite de marcadores.
 */
@Dao
interface BookmarkDao {

    @Query("SELECT * FROM bookmarks ORDER BY isPinned DESC, createdAt DESC")
    fun observeAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE collectionId = :collectionId ORDER BY isPinned DESC, createdAt DESC")
    fun observeBookmarksByCollection(collectionId: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE platform = :platform ORDER BY isPinned DESC, createdAt DESC")
    fun observeBookmarksByPlatform(platform: String): Flow<List<BookmarkEntity>>

    @Query(
        """
        SELECT * FROM bookmarks 
        WHERE title LIKE '%' || :query || '%' 
           OR url LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%' 
        ORDER BY isPinned DESC, createdAt DESC
        """
    )
    fun searchBookmarks(query: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    suspend fun findBookmarkByUrl(url: String): BookmarkEntity?

    @Query("SELECT * FROM bookmarks WHERE id = :id LIMIT 1")
    suspend fun findBookmarkById(id: String): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Update
    suspend fun updateBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: String)

    @Query("UPDATE bookmarks SET isPinned = :isPinned WHERE id = :id")
    suspend fun togglePin(id: String, isPinned: Boolean)

    @Query("UPDATE bookmarks SET isPreviewVisible = :isVisible WHERE id = :id")
    suspend fun updatePreviewVisibility(id: String, isVisible: Boolean)

    @Query("UPDATE bookmarks SET collectionId = :collectionId WHERE id = :bookmarkId")
    suspend fun updateBookmarkCollection(bookmarkId: String, collectionId: String?)

    @Query("SELECT collectionId FROM bookmarks WHERE id = :bookmarkId AND collectionId IS NOT NULL")
    fun observeCollectionIdsForBookmark(bookmarkId: String): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM bookmarks WHERE collectionId = :collectionId")
    fun observeBookmarkCountForCollection(collectionId: String): Flow<Int>

    // --- Consultas masivas y operaciones por lotes para Respaldo / Exportación / Importación ---

    @Query("SELECT * FROM bookmarks ORDER BY isPinned DESC, createdAt DESC")
    suspend fun getAllBookmarks(): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE collectionId = :collectionId ORDER BY isPinned DESC, createdAt DESC")
    suspend fun getBookmarksByCollection(collectionId: String): List<BookmarkEntity>

    @Query("SELECT collectionId FROM bookmarks WHERE id = :bookmarkId AND collectionId IS NOT NULL")
    suspend fun getCollectionIdsForBookmark(bookmarkId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmarks(bookmarks: List<BookmarkEntity>)
}
