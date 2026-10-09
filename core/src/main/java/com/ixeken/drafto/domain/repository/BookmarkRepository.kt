package com.ixeken.drafto.domain.repository

import com.ixeken.drafto.domain.model.Bookmark
import com.ixeken.drafto.domain.model.BookmarkCollection
import com.ixeken.drafto.domain.model.PlatformFilter
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de dominio para la persistencia y consulta reactiva de marcadores y colecciones.
 *
 * Mantiene la abstracción de almacenamiento 100% en Kotlin puro, permitiendo que la capa de
 * presentación y casos de uso interactúen con modelos de dominio inmutables sin dependencias
 * de SQLite o Room.
 */
interface BookmarkRepository {

    /**
     * Emite la lista completa de marcadores ordenados por fijados primero y fecha de creación descendente.
     */
    fun observeAllBookmarks(): Flow<List<Bookmark>>

    /**
     * Emite los marcadores asociados a una colección específica identificada por su ID.
     */
    fun observeBookmarksByCollection(collectionId: String): Flow<List<Bookmark>>

    /**
     * Emite los marcadores filtrados por una plataforma o categoría específica.
     * Si la plataforma es [PlatformFilter.ALL], emite todos los marcadores.
     */
    fun observeBookmarksByPlatform(platform: PlatformFilter): Flow<List<Bookmark>>

    /**
     * Emite los marcadores que coincidan parcialmente con la consulta de búsqueda en título, URL o descripción.
     */
    fun searchBookmarks(query: String): Flow<List<Bookmark>>

    /**
     * Busca un marcador existente por su URL exacta.
     */
    suspend fun findBookmarkByUrl(url: String): Bookmark?

    /**
     * Obtiene un marcador existente a partir de su ID universal.
     */
    suspend fun findBookmarkById(id: String): Bookmark?

    /**
     * Guarda o actualiza un marcador en la base de datos local.
     */
    suspend fun saveBookmark(bookmark: Bookmark): Long

    /**
     * Actualiza los datos de un marcador persistido.
     */
    suspend fun updateBookmark(bookmark: Bookmark)

    /**
     * Elimina un marcador por su identificador único.
     */
    suspend fun deleteBookmark(id: String)

    /**
     * Alterna el estado de fijado (pin) de un marcador.
     */
    suspend fun togglePin(id: String, isPinned: Boolean)

    /**
     * Alterna la visibilidad de la imagen previa en el marcador persistido.
     */
    suspend fun updatePreviewVisibility(id: String, isVisible: Boolean)

    /**
     * Emite todas las colecciones temáticas con su conteo de marcadores actualizado.
     */
    fun observeAllCollections(): Flow<List<BookmarkCollection>>

    /**
     * Guarda o crea una nueva colección temática en el almacén persistente.
     */
    suspend fun saveCollection(collection: BookmarkCollection)

    /**
     * Actualiza una colección temática existente.
     */
    suspend fun updateCollection(collection: BookmarkCollection)

    /**
     * Elimina una colección temática por su ID (las referencias cruzadas se eliminan en cascada).
     */
    suspend fun deleteCollection(collectionId: String)

    /**
     * Elimina una colección temática únicamente si no contiene ningún marcador asociado.
     *
     * @return El número de colecciones eliminadas (1 si estaba vacía, 0 si aún contenía marcadores).
     */
    suspend fun deleteCollectionIfEmpty(collectionId: String): Int

    /**
     * Elimina de forma atómica todas las colecciones que no contienen ningún marcador.
     *
     * @return El número total de colecciones vacías eliminadas.
     */
    suspend fun deleteEmptyCollections(): Int

    /**
     * Asocia un marcador a una colección en la tabla cruzada.
     */
    suspend fun addBookmarkToCollection(bookmarkId: String, collectionId: String)

    /**
     * Desvincula un marcador de una colección específica.
     */
    suspend fun removeBookmarkFromCollection(bookmarkId: String, collectionId: String)

    /**
     * Emite la lista de IDs de colecciones a las que pertenece el marcador especificado.
     */
    fun observeCollectionIdsForBookmark(bookmarkId: String): Flow<List<String>>

    /**
     * Emite el conteo total de marcadores que contiene una colección temática.
     */
    fun observeBookmarkCountForCollection(collectionId: String): Flow<Int>

    // --- Métodos síncronos para Respaldo, Exportación e Importación masiva ---

    /**
     * Obtiene todos los marcadores junto con sus colecciones asignadas de forma síncrona.
     */
    suspend fun getAllBookmarksWithCollections(): List<Pair<Bookmark, List<BookmarkCollection>>>

    /**
     * Obtiene los marcadores pertenecientes a una colección específica de forma síncrona.
     */
    suspend fun getBookmarksByCollectionSync(collectionId: String): List<Bookmark>

    /**
     * Obtiene todas las colecciones temáticas persistidas de forma síncrona.
     */
    suspend fun getAllCollectionsSync(): List<BookmarkCollection>

    /**
     * Obtiene todos los pares asociativos (bookmarkId, collectionId) persistidos.
     */
    suspend fun getAllRelationsSync(): List<Pair<String, String>>

    /**
     * Busca una colección existente por su nombre (insensible a mayúsculas y minúsculas).
     */
    suspend fun findCollectionByName(name: String): BookmarkCollection?

    /**
     * Importa un lote de marcadores y recrea las colecciones especificadas, deduplicando por URL.
     *
     * @return Par con (conteoDeMarcadoresNuevos, conteoDeColeccionesCreadas).
     */
    suspend fun importBookmarksBatch(
        bookmarksWithFolders: List<Pair<Bookmark, List<String>>>
    ): Pair<Int, Int>
}
