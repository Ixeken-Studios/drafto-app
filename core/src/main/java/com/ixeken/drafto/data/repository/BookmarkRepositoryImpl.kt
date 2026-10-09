package com.ixeken.drafto.data.repository

import com.ixeken.drafto.data.local.dao.BookmarkDao
import com.ixeken.drafto.data.local.dao.CollectionDao
import com.ixeken.drafto.data.local.entity.CollectionEntity
import com.ixeken.drafto.data.mapper.toDomain
import com.ixeken.drafto.data.mapper.toEntity
import com.ixeken.drafto.domain.model.Bookmark
import com.ixeken.drafto.domain.model.BookmarkCollection
import com.ixeken.drafto.domain.model.DraftoCollectionPresetColors
import com.ixeken.drafto.domain.model.PlatformFilter
import com.ixeken.drafto.domain.repository.BookmarkRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Implementación de [BookmarkRepository] respaldada por Room DAO en SQLite.
 *
 * Ejecuta todas las operaciones de I/O suspendidas y despacha flujos reactivos
 * sobre [Dispatchers.IO] para garantizar cero jank y evitar bloqueos en el hilo principal.
 */
class BookmarkRepositoryImpl(
    private val bookmarkDao: BookmarkDao,
    private val collectionDao: CollectionDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BookmarkRepository {

    override fun observeAllBookmarks(): Flow<List<Bookmark>> {
        return bookmarkDao.observeAllBookmarks()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override fun observeBookmarksByCollection(collectionId: String): Flow<List<Bookmark>> {
        return bookmarkDao.observeBookmarksByCollection(collectionId)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override fun observeBookmarksByPlatform(platform: PlatformFilter): Flow<List<Bookmark>> {
        return if (platform == PlatformFilter.ALL) {
            observeAllBookmarks()
        } else {
            bookmarkDao.observeBookmarksByPlatform(platform.name)
                .map { entities -> entities.map { it.toDomain() } }
                .flowOn(ioDispatcher)
        }
    }

    override fun searchBookmarks(query: String): Flow<List<Bookmark>> {
        return bookmarkDao.searchBookmarks(query.trim())
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun findBookmarkByUrl(url: String): Bookmark? = withContext(ioDispatcher) {
        bookmarkDao.findBookmarkByUrl(url.trim())?.toDomain()
    }

    override suspend fun findBookmarkById(id: String): Bookmark? = withContext(ioDispatcher) {
        bookmarkDao.findBookmarkById(id)?.toDomain()
    }

    override suspend fun saveBookmark(bookmark: Bookmark): Long = withContext(ioDispatcher) {
        bookmarkDao.insertBookmark(bookmark.toEntity())
    }

    override suspend fun updateBookmark(bookmark: Bookmark) = withContext(ioDispatcher) {
        bookmarkDao.updateBookmark(bookmark.toEntity())
    }

    override suspend fun deleteBookmark(id: String) = withContext(ioDispatcher) {
        bookmarkDao.deleteBookmarkById(id)
    }

    override suspend fun togglePin(id: String, isPinned: Boolean) = withContext(ioDispatcher) {
        bookmarkDao.togglePin(id, isPinned)
    }

    override suspend fun updatePreviewVisibility(id: String, isVisible: Boolean) = withContext(ioDispatcher) {
        bookmarkDao.updatePreviewVisibility(id, isVisible)
    }

    override fun observeAllCollections(): Flow<List<BookmarkCollection>> {
        return collectionDao.getAllCollectionsWithCounts().map { list ->
            list.map { it.toDomain() }
        }.flowOn(ioDispatcher)
    }

    override suspend fun saveCollection(collection: BookmarkCollection) = withContext(ioDispatcher) {
        collectionDao.insertCollection(collection.toEntity())
    }

    override suspend fun updateCollection(collection: BookmarkCollection) = withContext(ioDispatcher) {
        collectionDao.updateCollection(collection.toEntity())
    }

    override suspend fun deleteCollection(collectionId: String) = withContext(ioDispatcher) {
        collectionDao.deleteCollectionSafely(collectionId)
    }

    override suspend fun deleteCollectionIfEmpty(collectionId: String): Int = withContext(ioDispatcher) {
        0
    }

    override suspend fun deleteEmptyCollections(): Int = withContext(ioDispatcher) {
        0
    }

    override suspend fun addBookmarkToCollection(bookmarkId: String, collectionId: String) = withContext(ioDispatcher) {
        bookmarkDao.updateBookmarkCollection(bookmarkId, collectionId)
    }

    override suspend fun removeBookmarkFromCollection(bookmarkId: String, collectionId: String): Unit = withContext(ioDispatcher) {
        bookmarkDao.updateBookmarkCollection(bookmarkId, null)
    }

    override fun observeCollectionIdsForBookmark(bookmarkId: String): Flow<List<String>> {
        return bookmarkDao.observeCollectionIdsForBookmark(bookmarkId).flowOn(ioDispatcher)
    }

    override fun observeBookmarkCountForCollection(collectionId: String): Flow<Int> {
        return bookmarkDao.observeBookmarkCountForCollection(collectionId).flowOn(ioDispatcher)
    }

    override suspend fun getAllBookmarksWithCollections(): List<Pair<Bookmark, List<BookmarkCollection>>> = withContext(ioDispatcher) {
        val bookmarks = bookmarkDao.getAllBookmarks().map { it.toDomain() }
        val collections = collectionDao.getAllCollections().associateBy { it.id }
        bookmarks.map { bm ->
            val col = bm.collectionId?.let { collections[it] }?.toDomain()
            Pair(bm, if (col != null) listOf(col) else emptyList())
        }
    }

    override suspend fun getBookmarksByCollectionSync(collectionId: String): List<Bookmark> = withContext(ioDispatcher) {
        bookmarkDao.getBookmarksByCollection(collectionId).map { it.toDomain() }
    }

    override suspend fun getAllCollectionsSync(): List<BookmarkCollection> = withContext(ioDispatcher) {
        collectionDao.getAllCollections().map { it.toDomain() }
    }

    override suspend fun getAllRelationsSync(): List<Pair<String, String>> = withContext(ioDispatcher) {
        bookmarkDao.getAllBookmarks().mapNotNull { bm ->
            bm.collectionId?.let { Pair(bm.id, it) }
        }
    }

    override suspend fun findCollectionByName(name: String): BookmarkCollection? = withContext(ioDispatcher) {
        collectionDao.findCollectionByName(name)?.toDomain()
    }

    override suspend fun importBookmarksBatch(
        bookmarksWithFolders: List<Pair<Bookmark, List<String>>>
    ): Pair<Int, Int> = withContext(ioDispatcher) {
        val existingCollections = collectionDao.getAllCollections().associateBy { it.name.lowercase() }.toMutableMap()
        var newBookmarksCount = 0
        var newCollectionsCount = 0

        val presetColors = DraftoCollectionPresetColors
        var colorIdx = 0

        for ((importedBookmark, folderHierarchy) in bookmarksWithFolders) {
            val folderName = folderHierarchy.firstOrNull()?.trim()
            val collectionId = if (!folderName.isNullOrBlank()) {
                val lowerName = folderName.lowercase()
                val collectionEntity = existingCollections[lowerName] ?: run {
                    val assignedColor = presetColors[colorIdx % presetColors.size]
                    colorIdx++
                    val newCol = CollectionEntity(
                        id = UUID.randomUUID().toString(),
                        name = folderName,
                        colorHex = assignedColor,
                        iconName = "folder",
                        createdAt = System.currentTimeMillis()
                    )
                    collectionDao.insertCollection(newCol)
                    existingCollections[lowerName] = newCol
                    newCollectionsCount++
                    newCol
                }
                collectionEntity.id
            } else {
                null
            }

            val bookmarkWithCol = importedBookmark.copy(collectionId = collectionId)
            val existingBookmark = bookmarkDao.findBookmarkByUrl(bookmarkWithCol.url)
            if (existingBookmark != null) {
                if (existingBookmark.title.isNullOrBlank() && !bookmarkWithCol.title.isNullOrBlank()) {
                    bookmarkDao.updateBookmark(existingBookmark.copy(title = bookmarkWithCol.title, collectionId = collectionId))
                } else if (existingBookmark.collectionId == null && collectionId != null) {
                    bookmarkDao.updateBookmark(existingBookmark.copy(collectionId = collectionId))
                }
            } else {
                bookmarkDao.insertBookmark(bookmarkWithCol.toEntity())
                newBookmarksCount++
            }
        }

        Pair(newBookmarksCount, newCollectionsCount)
    }
}

private fun com.ixeken.drafto.data.local.dao.CollectionWithCounts.toDomain(): BookmarkCollection = BookmarkCollection(
    id = id,
    name = name,
    colorHex = colorHex,
    iconName = iconName,
    noteCount = noteCount,
    todoCount = todoCount,
    bookmarkCount = bookmarkCount,
    createdAt = createdAt
)
