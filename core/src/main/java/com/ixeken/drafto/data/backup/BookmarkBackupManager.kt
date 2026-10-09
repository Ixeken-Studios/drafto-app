package com.ixeken.drafto.data.backup

import android.content.Context
import android.net.Uri
import com.ixeken.drafto.domain.model.Bookmark
import com.ixeken.drafto.domain.model.BookmarkBackupPayload
import com.ixeken.drafto.domain.model.BookmarkCollection
import com.ixeken.drafto.domain.model.BookmarkCollectionRelationPayload
import com.ixeken.drafto.domain.repository.BookmarkRepository
import com.ixeken.drafto.util.BookmarkHtmlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Formatos admitidos para la exportación de marcadores en Drafto.
 */
enum class BookmarkExportFormat {
    /** Formato estándar universal Netscape Bookmark compatible con todos los navegadores. */
    HTML,

    /** Formato estructurado nativo de Drafto que preserva 100% de los metadatos internos. */
    JSON
}

/**
 * Gestor centralizado para la exportación e importación de marcadores y colecciones.
 *
 * Trabaja de forma asíncrona en [Dispatchers.IO] utilizando flujos de entrada y salida nativos,
 * permitiendo interacción transparente con el Storage Access Framework (SAF) de Android.
 */
class BookmarkBackupManager(
    private val bookmarkRepository: BookmarkRepository,
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }
) {

    /**
     * Exporta los marcadores hacia un [OutputStream] en el formato solicitado.
     *
     * @param outputStream Flujo de destino provisto por el ContentResolver a partir del URI de SAF.
     * @param format Formato elegido ([BookmarkExportFormat.HTML] o [BookmarkExportFormat.JSON]).
     * @param collectionId ID opcional de una colección específica a exportar; si es nulo, exporta toda la biblioteca.
     * @param onProgress Callback para reportar el progreso y mensaje descriptivo de estado.
     * @return [Result] con el conteo de marcadores exportados.
     */
    suspend fun exportBookmarks(
        outputStream: OutputStream,
        format: BookmarkExportFormat,
        collectionId: String? = null,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            onProgress(0.1f, "Preparing bookmarks...")

            val itemsToExport: List<Pair<Bookmark, List<BookmarkCollection>>>
            val totalCount: Int

            if (collectionId != null) {
                val targetCollection = bookmarkRepository.getAllCollectionsSync().find { it.id == collectionId }
                val bookmarks = bookmarkRepository.getBookmarksByCollectionSync(collectionId)
                totalCount = bookmarks.size
                itemsToExport = bookmarks.map { bm ->
                    Pair(bm, listOfNotNull(targetCollection))
                }
            } else {
                itemsToExport = bookmarkRepository.getAllBookmarksWithCollections()
                totalCount = itemsToExport.size
            }

            onProgress(0.4f, "Generating export data...")

            val content = when (format) {
                BookmarkExportFormat.HTML -> {
                    val title = if (collectionId != null) {
                        itemsToExport.firstOrNull()?.second?.firstOrNull()?.name ?: "Drafto Bookmarks"
                    } else {
                        "Drafto Bookmarks"
                    }
                    BookmarkHtmlParser.exportToHtml(itemsToExport, title)
                }

                BookmarkExportFormat.JSON -> {
                    val collections = if (collectionId != null) {
                        bookmarkRepository.getAllCollectionsSync().filter { it.id == collectionId }
                    } else {
                        bookmarkRepository.getAllCollectionsSync()
                    }

                    val bookmarks = itemsToExport.map { it.first }
                    val allRelations = bookmarkRepository.getAllRelationsSync()
                    val targetBookmarkIds = bookmarks.map { it.id }.toSet()
                    val relations = allRelations
                        .filter { targetBookmarkIds.contains(it.first) }
                        .map { BookmarkCollectionRelationPayload(bookmarkId = it.first, collectionId = it.second) }

                    val payload = BookmarkBackupPayload(
                        version = 1,
                        exportedAt = System.currentTimeMillis(),
                        collections = collections,
                        bookmarks = bookmarks,
                        relations = relations
                    )
                    json.encodeToString(payload)
                }
            }

            onProgress(0.8f, "Writing file...")
            outputStream.use { stream ->
                stream.write(content.toByteArray(StandardCharsets.UTF_8))
                stream.flush()
            }

            onProgress(1.0f, "Export completed!")
            totalCount
        }
    }

    /**
     * Importa marcadores desde un [InputStream] detectando automáticamente si es HTML o JSON.
     *
     * @param inputStream Flujo de origen provisto por el ContentResolver a partir del URI de SAF.
     * @param onProgress Callback para reportar el porcentaje de avance y descripción del paso actual.
     * @return [Result] con un par conteniendo (marcadoresImportados, coleccionesCreadas).
     */
    suspend fun importBookmarks(
        inputStream: InputStream,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        runCatching {
            onProgress(0.1f, "Reading bookmarks file...")
            val rawContent = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            val cleanContent = rawContent.trim().removePrefix("\uFEFF").trim()

            val isJson = cleanContent.startsWith("{") && (
                cleanContent.contains("\"bookmarks\"") ||
                cleanContent.contains("\"collections\"") ||
                cleanContent.contains("\"version\"")
            )
            val isHtml = cleanContent.contains("NETSCAPE-Bookmark-file-1", ignoreCase = true) ||
                cleanContent.contains("<DL", ignoreCase = true) ||
                cleanContent.contains("<A ", ignoreCase = true)

            if (!isJson && !isHtml) {
                throw IllegalArgumentException("Unsupported or invalid bookmark file format")
            }

            onProgress(0.3f, "Parsing bookmarks and collections...")

            if (isJson) {
                val payload = json.decodeFromString<BookmarkBackupPayload>(cleanContent)
                val bookmarksCount = payload.bookmarks.size
                val collectionsCount = payload.collections.size

                onProgress(0.6f, "Restoring database...")
                for (col in payload.collections) {
                    bookmarkRepository.saveCollection(col)
                }
                for (bm in payload.bookmarks) {
                    bookmarkRepository.saveBookmark(bm)
                }
                val validColIds = payload.collections.map { it.id }.toSet()
                val validBmIds = payload.bookmarks.map { it.id }.toSet()
                for (rel in payload.relations) {
                    if (validColIds.contains(rel.collectionId) && validBmIds.contains(rel.bookmarkId)) {
                        runCatching {
                            bookmarkRepository.addBookmarkToCollection(rel.bookmarkId, rel.collectionId)
                        }
                    }
                }

                onProgress(1.0f, "Import completed!")
                Pair(bookmarksCount, collectionsCount)
            } else {
                val result = BookmarkHtmlParser.parseHtml(cleanContent)
                if (result.bookmarks.isEmpty()) {
                    return@runCatching Pair(0, 0)
                }

                onProgress(0.5f, "Processing ${result.bookmarks.size} bookmarks...")

                val domainItems = result.bookmarks.map { parsed ->
                    val domain = extractDomain(parsed.url)
                    val bookmark = Bookmark(
                        id = UUID.randomUUID().toString(),
                        url = parsed.url,
                        title = parsed.title,
                        description = parsed.description,
                        imageUrl = null,
                        domain = domain,
                        faviconUrl = parsed.faviconUrl,
                        createdAt = parsed.addedAt ?: System.currentTimeMillis()
                    )
                    Pair(bookmark, parsed.folderHierarchy)
                }

                onProgress(0.7f, "Saving to database...")
                val counts = bookmarkRepository.importBookmarksBatch(domainItems)

                onProgress(1.0f, "Import completed!")
                counts
            }
        }.onFailure { exception ->
            android.util.Log.e("BookmarkBackupManager", "Error importing bookmarks: ${exception.message}", exception)
        }
    }

    private fun extractDomain(url: String): String {
        return try {
            val uri = java.net.URI(url)
            val host = uri.host ?: url
            if (host.startsWith("www.", ignoreCase = true)) host.substring(4) else host
        } catch (_: Exception) {
            url
        }
    }
}
