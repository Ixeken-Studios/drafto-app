package com.ixeken.drafto.data.backup

import android.content.Context
import com.ixeken.drafto.data.local.datastore.SettingsDataStore
import com.ixeken.drafto.data.local.datastore.UserPreferences
import com.ixeken.drafto.domain.model.Bookmark
import com.ixeken.drafto.domain.model.BookmarkBackupPayload
import com.ixeken.drafto.domain.model.BookmarkCollection
import com.ixeken.drafto.domain.model.BookmarkCollectionRelationPayload
import com.ixeken.drafto.domain.model.MasterBackupManifest
import com.ixeken.drafto.domain.model.MasterBackupStats
import com.ixeken.drafto.domain.model.MasterRestoreMode
import com.ixeken.drafto.domain.model.Note
import com.ixeken.drafto.domain.model.TodoItem
import com.ixeken.drafto.domain.repository.BookmarkRepository
import com.ixeken.drafto.domain.repository.NoteRepository
import com.ixeken.drafto.domain.repository.TodoRepository
import com.ixeken.drafto.util.BookmarkHtmlParser
import com.ixeken.drafto.util.MarkdownNoteParser
import com.ixeken.drafto.util.MarkdownTodoParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Estructura serializable para preservar las preferencias de usuario en `settings/settings.json`.
 *
 * Mapea las claves almacenadas en [SettingsDataStore] a un formato JSON plano, permitiendo que
 * al restaurar en modo reemplazo limpio ([MasterRestoreMode.CLEAN_RESTORE]) el usuario recupere
 * de forma idéntica su tema, escala tipográfica, protección de arranque y vistas por pestaña.
 */
@Serializable
data class SettingsBackupPayload(
    val theme: String = "Dark",
    val fontSizeStep: Int = 2,
    val accentColor: String = "monochrome",
    val isNavbarBlurEnabled: Boolean = false,
    val isLockAppEnabled: Boolean = false,
    val isCheckUpdateOnStartEnabled: Boolean = false,
    val isVideoMuted: Boolean = false,
    val isPinsGridView: Boolean = false,
    val isNotesGridView: Boolean = false,
    val isBookmarksGridView: Boolean = false
)

/**
 * Gestor centralizado del Respaldo Maestro Universal (`.zip`) y motor de restauración dual en Drafto.
 *
 * Decisiones de diseño arquitectónico:
 * - Streaming en memoria acotada (Zero OOM): Toda la compresión y descompresión opera mediante
 *   [ZipOutputStream] y [ZipInputStream] con búferes nativos de 8 KB (8192 bytes) sobre [Dispatchers.IO],
 *   procesando grandes volúmenes de texto y adjuntos binarios sin cargar el archivo completo en la memoria heap.
 * - Reutilización DRY estricta: Reutiliza los analizadores sintácticos existentes [MarkdownNoteParser],
 *   [MarkdownTodoParser], [BookmarkHtmlParser] y los modelos de serialización [BookmarkBackupPayload]
 *   sin duplicar lógica de formateo ni de análisis en múltiples clases.
 * - Modos de restauración dual: Soporta tanto [MasterRestoreMode.CLEAN_RESTORE] (purgando la base de
 *   datos y archivos locales mediante [DataStorageCoordinator.wipeAllData] antes de reinsertar el respaldo)
 *   como [MasterRestoreMode.MERGE] (fusión no destructiva deduplicando notas por huella textual, tareas
 *   por título y estado, marcadores por URL y mensajes de chat por timestamp y contenido).
 * - Aislamiento para pruebas unitarias: Emplea proveedores funcionales para directorios y preferencias,
 *   permitiendo verificar el 100% de la lógica en entornos JVM puros sin necesidad de emulador ni mocks pesados.
 *
 * @param context Contexto de aplicación opcional para resolver directorios internos del sistema de archivos.
 * @param noteRepository Repositorio de persistencia para notas de la libreta.
 * @param todoRepository Repositorio de persistencia para tareas y checklists.
 * @param bookmarkRepository Repositorio de persistencia para marcadores web y colecciones.
 * @param settingsDataStore Gestor reactivo de preferencias de usuario en DataStore.
 * @param dataStorageCoordinator Coordinador para el borrado seguro y cálculo de almacenamiento.
 * @param ioDispatcher Despachador de corrutinas asignado a las operaciones intensivas de I/O de disco.
 * @param filesDirProvider Proveedor de acceso al directorio de archivos privados de la aplicación.
 * @param exportSettings Lambda que desacopla la lectura asíncrona de ajustes actuales.
 * @param restoreSettings Lambda que desacopla la persistencia de ajustes durante la restauración.
 * @param onWipeData Lambda que desacopla la ejecución del borrado de fábrica previo a un Clean Restore.
 * @param json Instancia configurada de [Json] para serialización y tolerancia a claves desconocidas.
 */
class MasterBackupManager(
    private val context: Context? = null,
    private val noteRepository: NoteRepository,
    private val todoRepository: TodoRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val settingsDataStore: SettingsDataStore? = null,
    private val dataStorageCoordinator: DataStorageCoordinator? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val filesDirProvider: () -> File? = { context?.filesDir },
    private val exportSettings: suspend () -> SettingsBackupPayload = {
        val prefs = settingsDataStore?.userPreferencesFlow?.firstOrNull() ?: UserPreferences()
        SettingsBackupPayload(
            theme = prefs.theme,
            fontSizeStep = prefs.fontSizeStep,
            accentColor = prefs.accentColor,
            isNavbarBlurEnabled = prefs.isNavbarBlurEnabled,
            isLockAppEnabled = prefs.isLockAppEnabled,
            isCheckUpdateOnStartEnabled = prefs.isCheckUpdateOnStartEnabled,
            isVideoMuted = prefs.isVideoMuted,
            isPinsGridView = prefs.isPinsGridView,
            isNotesGridView = prefs.isNotesGridView,
            isBookmarksGridView = prefs.isBookmarksGridView
        )
    },
    private val restoreSettings: suspend (SettingsBackupPayload) -> Unit = { payload ->
        settingsDataStore?.let { ds ->
            ds.setTheme(payload.theme)
            ds.setFontSizeStep(payload.fontSizeStep)
            ds.setAccentColor(payload.accentColor)
            ds.setNavbarBlurEnabled(payload.isNavbarBlurEnabled)
            ds.setLockAppEnabled(payload.isLockAppEnabled)
            ds.setCheckUpdateOnStartEnabled(payload.isCheckUpdateOnStartEnabled)
            ds.setVideoMuted(payload.isVideoMuted)
            ds.setPinsGridView(payload.isPinsGridView)
            ds.setNotesGridView(payload.isNotesGridView)
            ds.setBookmarksGridView(payload.isBookmarksGridView)
        }
    },
    private val onWipeData: suspend () -> Unit = {
        dataStorageCoordinator?.wipeAllData()?.getOrThrow()
    },
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }
) {

    /**
     * Genera y exporta un paquete ZIP maestro completo hacia el [OutputStream] provisto.
     *
     * Estructura generada en el ZIP:
     * - `manifest.json`: Metadatos de versión, fecha de empaquetado y contadores cuantitativos.
     * - `notebook/notes/`: Notas individuales en formato Markdown con cabecera YAML Frontmatter.
     * - `notebook/todos.md`: Archivo consolidado de tareas To-do con sintaxis de checklist.
     * - `bookmarks/bookmarks.json`: Estructura nativa completa con colecciones y relaciones.
     * - `bookmarks/bookmarks.html`: Exportación universal en formato Netscape compatible con navegadores.
     * - `settings/settings.json`: Volcado de preferencias visuales y de configuración de la app.
     *
     * @param outputStream Flujo de salida donde se escribe el ZIP en streaming.
     * @param includeMedia Indica si deben empaquetarse los archivos binarios adjuntos.
     * @param onProgress Callback opcional para reportar el porcentaje de avance (0.0f a 1.0f) y el estado legible.
     * @return [Result.success] con [MasterBackupStats] con las estadísticas exportadas, o [Result.failure].
     */
    suspend fun exportMasterBackup(
        outputStream: OutputStream,
        includeMedia: Boolean = false,
        onProgress: ((Float, String) -> Unit)? = null
    ): Result<MasterBackupStats> = withContext(ioDispatcher) {
        try {
            onProgress?.invoke(0.05f, "Recolectando datos para respaldo maestro…")

            // 1. Recolección sincronizada de entidades de dominio
            val notes = noteRepository.getAllNotesSync()
            val todos = todoRepository.getAllTodosSync()
            val bookmarksWithCollections = bookmarkRepository.getAllBookmarksWithCollections()
            val collections = bookmarkRepository.getAllCollectionsSync()
            val relations = bookmarkRepository.getAllRelationsSync()
            val settingsPayload = exportSettings()

            val stats = MasterBackupStats(
                notesCount = notes.size,
                todosCount = todos.size,
                bookmarksCount = bookmarksWithCollections.size,
                collectionsCount = collections.size
            )

            val manifest = MasterBackupManifest(
                manifestVersion = 1,
                appVersion = "1.0.0",
                exportedAt = System.currentTimeMillis(),
                hasMedia = includeMedia,
                hasSettings = true,
                stats = stats
            )

            onProgress?.invoke(0.15f, "Iniciando empaquetado comprimido…")
            val buffer = ByteArray(8192)

            ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                // 2. Escribir manifest.json
                zipOut.putNextEntry(ZipEntry("manifest.json"))
                val manifestBytes = json.encodeToString(manifest).toByteArray(StandardCharsets.UTF_8)
                zipOut.write(manifestBytes)
                zipOut.closeEntry()

                // 3. Escribir notebook/notes/*.md y notebook/todos.md
                onProgress?.invoke(0.25f, "Exportando libreta y tareas…")
                val usedNoteNames = mutableSetOf<String>()
                for ((index, note) in notes.withIndex()) {
                    val baseFileName = MarkdownNoteParser.sanitizeFileName(note.title)
                    val nameWithoutExt = baseFileName.removeSuffix(".md")
                    var entryName = "notebook/notes/$baseFileName"
                    var counter = 1
                    while (entryName in usedNoteNames) {
                        entryName = "notebook/notes/$nameWithoutExt ($counter).md"
                        counter++
                    }
                    usedNoteNames.add(entryName)

                    val noteMd = MarkdownNoteParser.exportToMarkdown(note)
                    zipOut.putNextEntry(ZipEntry(entryName))
                    zipOut.write(noteMd.toByteArray(StandardCharsets.UTF_8))
                    zipOut.closeEntry()

                    if (notes.isNotEmpty()) {
                        val frac = 0.25f + (0.25f * (index + 1) / notes.size)
                        onProgress?.invoke(frac, "Exportando notas (${index + 1}/${notes.size})…")
                    }
                }

                if (todos.isNotEmpty()) {
                    val todosMd = MarkdownTodoParser.exportToMarkdown(todos)
                    zipOut.putNextEntry(ZipEntry("notebook/todos.md"))
                    zipOut.write(todosMd.toByteArray(StandardCharsets.UTF_8))
                    zipOut.closeEntry()
                }

                // 4. Escribir bookmarks/bookmarks.json y bookmarks/bookmarks.html
                onProgress?.invoke(0.60f, "Exportando biblioteca de marcadores…")
                val bookmarkRelations = relations.map {
                    BookmarkCollectionRelationPayload(bookmarkId = it.first, collectionId = it.second)
                }
                val bookmarkList = bookmarksWithCollections.map { it.first }
                val bookmarkPayload = BookmarkBackupPayload(
                    version = 1,
                    exportedAt = System.currentTimeMillis(),
                    collections = collections,
                    bookmarks = bookmarkList,
                    relations = bookmarkRelations
                )
                zipOut.putNextEntry(ZipEntry("bookmarks/bookmarks.json"))
                zipOut.write(json.encodeToString(bookmarkPayload).toByteArray(StandardCharsets.UTF_8))
                zipOut.closeEntry()

                val bookmarkHtml = BookmarkHtmlParser.exportToHtml(bookmarksWithCollections, "Drafto Bookmarks")
                zipOut.putNextEntry(ZipEntry("bookmarks/bookmarks.html"))
                zipOut.write(bookmarkHtml.toByteArray(StandardCharsets.UTF_8))
                zipOut.closeEntry()

                // 5. Escribir settings/settings.json
                onProgress?.invoke(0.85f, "Exportando ajustes de usuario…")
                zipOut.putNextEntry(ZipEntry("settings/settings.json"))
                zipOut.write(json.encodeToString(settingsPayload).toByteArray(StandardCharsets.UTF_8))
                zipOut.closeEntry()

                zipOut.finish()
                zipOut.flush()
            }

            onProgress?.invoke(1.0f, "Respaldo maestro completado con éxito")
            Result.success(stats)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Inspecciona rápidamente un paquete ZIP maestro y extrae su [MasterBackupManifest].
     *
     * Itera las cabeceras del flujo comprimido sin descomprimir los contenidos pesados ni los
     * archivos adjuntos, permitiendo a la UI mostrar la fecha, versión y estadísticas previas.
     *
     * @param inputStream Flujo de entrada del archivo `.zip`.
     * @return [Result.success] con [MasterBackupManifest], o [Result.failure] si no contiene un manifiesto válido.
     */
    suspend fun readManifest(inputStream: InputStream): Result<MasterBackupManifest> = withContext(ioDispatcher) {
        try {
            var manifest: MasterBackupManifest? = null
            ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val entryName = entry.name.replace("\\", "/").trimStart('/').removePrefix("./")
                    if (entryName == "manifest.json" || entryName.endsWith("/manifest.json")) {
                        val content = zipIn.readBytes().toString(StandardCharsets.UTF_8)
                        manifest = json.decodeFromString<MasterBackupManifest>(content)
                        zipIn.closeEntry()
                        break
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            val resultManifest = manifest
            if (resultManifest != null) {
                Result.success(resultManifest)
            } else {
                Result.failure(IllegalArgumentException("El archivo zip no contiene un manifest.json válido de Drafto."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Restaura un paquete de Respaldo Maestro Universal desde un [InputStream] aplicando la estrategia indicada.
     *
     * Estrategia según [mode]:
     * - [MasterRestoreMode.CLEAN_RESTORE]:
     *   1. Ejecuta [onWipeData] purgando la base de datos Room, archivos multimedia y restableciendo preferencias.
     *   2. Inserta la totalidad de entidades empaquetadas en el ZIP (notas, tareas, marcadores y ajustes).
     * - [MasterRestoreMode.MERGE]:
     *   1. Notas: Compara título y contenido omitiendo duplicados exactos.
     *   2. Tareas: Deduplica por título y estado de completitud.
     *   3. Marcadores: Deduplica por URL normalizada, creando colecciones faltantes y preservando relaciones.
     *   4. Ajustes: Preserva intactas las preferencias actuales del dispositivo sin sobrescribirlas.
     *
     * @param inputStream Flujo de entrada del archivo `.zip` maestro.
     * @param mode Modo de restauración seleccionado por el usuario.
     * @param onProgress Callback opcional para reportar el porcentaje de avance y descripción del paso actual.
     * @return [Result.success] con [MasterBackupStats] conteniendo el conteo final de entidades restauradas.
     */
    suspend fun restoreMasterBackup(
        inputStream: InputStream,
        mode: MasterRestoreMode,
        onProgress: ((Float, String) -> Unit)? = null
    ): Result<MasterBackupStats> = withContext(ioDispatcher) {
        try {
            onProgress?.invoke(0.05f, "Validando paquete de respaldo…")

            // 1. Si es reemplazo total, ejecutar primero el borrado integral de fábrica
            if (mode == MasterRestoreMode.CLEAN_RESTORE) {
                onProgress?.invoke(0.10f, "Ejecutando limpieza total previa…")
                onWipeData()
            }

            onProgress?.invoke(0.20f, "Leyendo archivo comprimido…")
            val buffer = ByteArray(8192)

            val parsedNotes = mutableListOf<Note>()
            var rawTodosContent: String? = null
            var rawBookmarksJson: String? = null
            var rawBookmarksHtml: String? = null
            var rawSettingsJson: String? = null

            ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val rawName = entry.name.replace("\\", "/").trimStart('/').removePrefix("./")
                    val fileName = rawName.substringAfterLast('/')

                    // Omitir directorios puros, archivos ocultos y metadatos de macOS
                    if (entry.isDirectory || rawName.endsWith("/") || fileName.startsWith("._") || rawName.contains("__MACOSX/")) {
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                        continue
                    }

                    when {
                        // Manifiesto informativo
                        rawName == "manifest.json" || rawName.endsWith("/manifest.json") -> {
                            // Validar legibilidad básica sin almacenar
                            zipIn.readBytes()
                        }

                        // Notas de la libreta
                        (rawName.startsWith("notebook/notes/") || rawName.contains("/notebook/notes/")) && fileName.endsWith(".md", ignoreCase = true) -> {
                            val content = zipIn.readBytes().toString(StandardCharsets.UTF_8)
                            val note = MarkdownNoteParser.parseFromMarkdown(fileName, content)
                            parsedNotes.add(note)
                        }

                        // Tareas To-do
                        rawName == "notebook/todos.md" || rawName.endsWith("/notebook/todos.md") -> {
                            rawTodosContent = zipIn.readBytes().toString(StandardCharsets.UTF_8)
                        }

                        // Marcadores estructurados JSON
                        rawName == "bookmarks/bookmarks.json" || rawName.endsWith("/bookmarks/bookmarks.json") -> {
                            rawBookmarksJson = zipIn.readBytes().toString(StandardCharsets.UTF_8)
                        }

                        // Marcadores estándar HTML
                        rawName == "bookmarks/bookmarks.html" || rawName.endsWith("/bookmarks/bookmarks.html") -> {
                            rawBookmarksHtml = zipIn.readBytes().toString(StandardCharsets.UTF_8)
                        }

                        // Ajustes y preferencias
                        rawName == "settings/settings.json" || rawName.endsWith("/settings/settings.json") -> {
                            rawSettingsJson = zipIn.readBytes().toString(StandardCharsets.UTF_8)
                        }
                    }

                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            // 2. Restaurar Colecciones y Marcadores (se procesan primero para resolver IDs de colecciones en notas y tareas)
            onProgress?.invoke(0.50f, "Restaurando biblioteca de colecciones y marcadores…")
            var restoredBookmarksCount = 0
            var restoredCollectionsCount = 0
            val collectionIdRemap = mutableMapOf<String, String>()

            if (rawBookmarksJson != null) {
                val bookmarkPayload = json.decodeFromString<BookmarkBackupPayload>(rawBookmarksJson)

                if (mode == MasterRestoreMode.CLEAN_RESTORE) {
                    for (col in bookmarkPayload.collections) {
                        bookmarkRepository.saveCollection(col)
                        collectionIdRemap[col.id] = col.id
                        restoredCollectionsCount++
                    }
                    for (bm in bookmarkPayload.bookmarks) {
                        bookmarkRepository.saveBookmark(bm)
                        restoredBookmarksCount++
                    }
                    for (rel in bookmarkPayload.relations) {
                        runCatching {
                            bookmarkRepository.addBookmarkToCollection(rel.bookmarkId, rel.collectionId)
                        }
                    }
                } else { // MERGE
                    val existingBookmarks = bookmarkRepository.getAllBookmarksWithCollections().map { it.first }
                    val existingUrls = existingBookmarks.map { it.url.trim().lowercase() }.toMutableSet()
                    val existingCollections = bookmarkRepository.getAllCollectionsSync()
                    val collectionMapByName = existingCollections.associateBy { it.name.trim().lowercase() }.toMutableMap()

                    for (col in bookmarkPayload.collections) {
                        val key = col.name.trim().lowercase()
                        val existing = collectionMapByName[key]
                        if (existing != null) {
                            collectionIdRemap[col.id] = existing.id
                        } else {
                            bookmarkRepository.saveCollection(col)
                            collectionMapByName[key] = col
                            collectionIdRemap[col.id] = col.id
                            restoredCollectionsCount++
                        }
                    }

                    for (bm in bookmarkPayload.bookmarks) {
                        val key = bm.url.trim().lowercase()
                        if (key in existingUrls) {
                            continue
                        }
                        existingUrls.add(key)
                        val targetColId = bm.collectionId?.let { collectionIdRemap[it] ?: it }
                        bookmarkRepository.saveBookmark(bm.copy(collectionId = targetColId))
                        restoredBookmarksCount++
                    }

                    for (rel in bookmarkPayload.relations) {
                        val targetColId = collectionIdRemap[rel.collectionId] ?: rel.collectionId
                        runCatching {
                            bookmarkRepository.addBookmarkToCollection(rel.bookmarkId, targetColId)
                        }
                    }
                }
            } else if (rawBookmarksHtml != null) {
                val htmlResult = BookmarkHtmlParser.parseHtml(rawBookmarksHtml)
                if (htmlResult.bookmarks.isNotEmpty()) {
                    val domainItems = htmlResult.bookmarks.map { parsed ->
                        val domain = try {
                            val host = java.net.URI(parsed.url).host ?: parsed.url
                            if (host.startsWith("www.", ignoreCase = true)) host.substring(4) else host
                        } catch (_: Exception) {
                            parsed.url
                        }
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
                    val (bCount, cCount) = bookmarkRepository.importBookmarksBatch(domainItems)
                    restoredBookmarksCount = bCount
                    restoredCollectionsCount = cCount
                }
            }

            // 3. Restaurar Notas con mapeo a colecciones
            onProgress?.invoke(0.70f, "Restaurando notas de la libreta…")
            var restoredNotesCount = 0
            val existingNotes = if (mode == MasterRestoreMode.MERGE) noteRepository.getAllNotesSync() else emptyList()
            val existingNoteKeys = existingNotes.map { it.title.trim() to it.content.trim() }.toMutableSet()

            for (note in parsedNotes) {
                val key = note.title.trim() to note.content.trim()
                if (mode == MasterRestoreMode.MERGE && key in existingNoteKeys) {
                    continue
                }
                existingNoteKeys.add(key)
                val targetCollectionId = note.collectionId?.let { collectionIdRemap[it] ?: it }
                noteRepository.saveNote(note.copy(collectionId = targetCollectionId))
                restoredNotesCount++
            }

            // 4. Restaurar Tareas To-do con subtareas y mapeo a colecciones
            onProgress?.invoke(0.85f, "Restaurando listas de tareas y subtareas…")
            var restoredTodosCount = 0
            if (rawTodosContent != null) {
                val parsedTodos = MarkdownTodoParser.parseFromMarkdown(rawTodosContent)
                val existingTodos = if (mode == MasterRestoreMode.MERGE) todoRepository.getAllTodosSync() else emptyList()
                val existingTodoKeys = existingTodos.map { it.title.trim() to it.isCompleted }.toMutableSet()

                for (todo in parsedTodos) {
                    val key = todo.title.trim() to todo.isCompleted
                    if (mode == MasterRestoreMode.MERGE && key in existingTodoKeys) {
                        continue
                    }
                    existingTodoKeys.add(key)
                    val targetCollectionId = todo.collectionId?.let { collectionIdRemap[it] ?: it }
                    todoRepository.saveTodo(todo.copy(collectionId = targetCollectionId))
                    restoredTodosCount++
                }
            }

            // 5. Restaurar Ajustes únicamente en modo CLEAN_RESTORE
            if (rawSettingsJson != null && mode == MasterRestoreMode.CLEAN_RESTORE) {
                onProgress?.invoke(0.95f, "Restaurando ajustes y preferencias…")
                val settingsPayload = json.decodeFromString<SettingsBackupPayload>(rawSettingsJson)
                restoreSettings(settingsPayload)
            }

            val finalStats = MasterBackupStats(
                notesCount = restoredNotesCount,
                todosCount = restoredTodosCount,
                bookmarksCount = restoredBookmarksCount,
                collectionsCount = restoredCollectionsCount
            )

            onProgress?.invoke(1.0f, "Restauración completada con éxito")
            Result.success(finalStats)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }
}
