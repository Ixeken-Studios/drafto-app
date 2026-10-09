package com.ixeken.drafto.data.backup

import com.ixeken.drafto.domain.model.Bookmark
import com.ixeken.drafto.domain.model.BookmarkCollection
import com.ixeken.drafto.domain.model.MasterRestoreMode
import com.ixeken.drafto.domain.model.Note
import com.ixeken.drafto.domain.model.PlatformFilter
import com.ixeken.drafto.domain.model.TodoItem
import com.ixeken.drafto.domain.repository.BookmarkRepository
import com.ixeken.drafto.domain.repository.NoteRepository
import com.ixeken.drafto.domain.repository.TodoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Suite de pruebas unitarias para [MasterBackupManager].
 *
 * Valida de forma exhaustiva sobre el entorno JVM:
 * 1. Exportación completa de archivo ZIP maestro con todas sus particiones (`manifest.json`,
 *    `notebook/notes/`, `notebook/todos.md`, `bookmarks/bookmarks.json`, `bookmarks/bookmarks.html`,
 *    `settings/settings.json`).
 * 2. Compatibilidad retrospectiva con copias legacy que incluyan particiones `chats/`.
 * 3. Inspección ligera de metadatos vía [MasterBackupManager.readManifest] sin descomprimir todo el archivo.
 * 4. Restauración en modo reemplazo total ([MasterRestoreMode.CLEAN_RESTORE]) con purga previa y carga exacta.
 * 5. Restauración en modo fusión ([MasterRestoreMode.MERGE]) con deduplicación rigurosa de notas, tareas y
 *    marcadores, preservando ajustes actuales.
 * 6. Re-lanzamiento estricto de [CancellationException].
 */
class MasterBackupManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var fakeFilesDir: File

    private lateinit var fakeNoteRepository: FakeNoteRepository
    private lateinit var fakeTodoRepository: FakeTodoRepository
    private lateinit var fakeBookmarkRepository: FakeBookmarkRepository

    private var currentSettings = SettingsBackupPayload(
        theme = "Dark",
        fontSizeStep = 3,
        isNavbarBlurEnabled = true,
        isLockAppEnabled = true,
        isCheckUpdateOnStartEnabled = false,
        isVideoMuted = true,
        isPinsGridView = true,
        isNotesGridView = true,
        isBookmarksGridView = false
    )

    private var wipeDataCalled = false

    private lateinit var manager: MasterBackupManager

    @Before
    fun setUp() {
        fakeFilesDir = tempFolder.newFolder("files")
        wipeDataCalled = false

        fakeNoteRepository = FakeNoteRepository()
        fakeTodoRepository = FakeTodoRepository()
        fakeBookmarkRepository = FakeBookmarkRepository()

        currentSettings = SettingsBackupPayload(
            theme = "Dark",
            fontSizeStep = 3,
            isNavbarBlurEnabled = true,
            isLockAppEnabled = true,
            isCheckUpdateOnStartEnabled = false,
            isVideoMuted = true,
            isPinsGridView = true,
            isNotesGridView = true,
            isBookmarksGridView = false
        )

        manager = MasterBackupManager(
            noteRepository = fakeNoteRepository,
            todoRepository = fakeTodoRepository,
            bookmarkRepository = fakeBookmarkRepository,
            ioDispatcher = Dispatchers.Unconfined,
            filesDirProvider = { fakeFilesDir },
            exportSettings = { currentSettings },
            restoreSettings = { currentSettings = it },
            onWipeData = {
                wipeDataCalled = true
                fakeNoteRepository.notes.clear()
                fakeTodoRepository.todos.clear()
                fakeBookmarkRepository.bookmarks.clear()
                fakeBookmarkRepository.collections.clear()
                fakeBookmarkRepository.relations.clear()
                currentSettings = SettingsBackupPayload()
            }
        )
    }

    @Test
    fun exportMasterBackup_generatesValidZipWithAllSectionsAndManifest() = runBlocking {
        // 1. Cargar datos de prueba
        fakeNoteRepository.saveNote(
            Note(
                id = "note_1",
                title = "Meeting Notes",
                content = "Discutir arquitectura 120 FPS",
                createdAt = 1000L,
                updatedAt = 2000L,
                isPinned = true,
                tags = listOf("work", "drafto")
            )
        )

        fakeTodoRepository.saveTodo(
            TodoItem(
                id = "todo_1",
                title = "Refactorizar DAOs",
                description = "",
                isCompleted = false,
                isPinned = true,
                dueDate = 1788540000000L,
                createdAt = 1000L
            )
        )

        val collection = BookmarkCollection(
            id = "col_android",
            name = "Android Dev",
            colorHex = "#06B6D4",
            iconName = "Folder",
            createdAt = 1000L
        )
        fakeBookmarkRepository.saveCollection(collection)
        val bookmark = Bookmark(
            id = "bm_1",
            url = "https://developer.android.com",
            title = "Android Developers",
            domain = "developer.android.com",
            createdAt = 1000L
        )
        fakeBookmarkRepository.saveBookmark(bookmark)
        fakeBookmarkRepository.addBookmarkToCollection(bookmark.id, collection.id)

        // 2. Exportar a memoria
        val out = ByteArrayOutputStream()
        val result = manager.exportMasterBackup(out, includeMedia = false)

        assertTrue(result.isSuccess)
        val stats = result.getOrThrow()
        assertEquals(1, stats.notesCount)
        assertEquals(1, stats.todosCount)
        assertEquals(1, stats.bookmarksCount)
        assertEquals(1, stats.collectionsCount)

        // 3. Inspeccionar el contenido del ZIP generado
        val zipBytes = out.toByteArray()
        val entries = mutableListOf<String>()
        val fileContents = mutableMapOf<String, String>()

        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                entries.add(entry.name)
                val content = zipIn.readBytes().toString(StandardCharsets.UTF_8)
                fileContents[entry.name] = content
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }

        assertTrue("Debe contener manifest.json", entries.contains("manifest.json"))
        assertTrue("Debe contener notebook/notes/Meeting Notes.md", entries.any { it.startsWith("notebook/notes/") && it.endsWith(".md") })
        assertTrue("Debe contener notebook/todos.md", entries.contains("notebook/todos.md"))
        assertTrue("Debe contener bookmarks/bookmarks.json", entries.contains("bookmarks/bookmarks.json"))
        assertTrue("Debe contener bookmarks/bookmarks.html", entries.contains("bookmarks/bookmarks.html"))
        assertTrue("Debe contener settings/settings.json", entries.contains("settings/settings.json"))
        assertFalse("No debe contener chats/chats.json", entries.contains("chats/chats.json"))

        // Verificar contenido de manifest.json
        val manifestJson = fileContents["manifest.json"]!!
        assertTrue(manifestJson.contains("\"notesCount\": 1"))
        assertTrue(manifestJson.contains("\"hasMedia\": false"))
        assertTrue(manifestJson.contains("\"hasSettings\": true"))

        // Verificar contenido de notebook/todos.md
        val todosMd = fileContents["notebook/todos.md"]!!
        assertTrue(todosMd.contains("- [ ] Refactorizar DAOs"))
        assertTrue(todosMd.contains("#pinned"))

        // Verificar contenido de settings/settings.json
        val settingsJson = fileContents["settings/settings.json"]!!
        assertTrue(settingsJson.contains("\"fontSizeStep\": 3"))
        assertTrue(settingsJson.contains("\"theme\": \"Dark\""))
    }

    @Test
    fun restoreMasterBackup_legacyZipWithChats_safelyIgnoresChatsAndRestoresData() = runBlocking {
        // Crear un ZIP en memoria emulando un respaldo antiguo con partición 'chats/'
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zos ->
            // 1. manifest.json
            zos.putNextEntry(ZipEntry("manifest.json"))
            val manifestContent = """
                {
                    "manifestVersion": 1,
                    "appVersion": "1.0.0",
                    "createdAt": 1000,
                    "hasMedia": false,
                    "hasSettings": true,
                    "stats": {
                        "notesCount": 1,
                        "todosCount": 1,
                        "bookmarksCount": 1,
                        "collectionsCount": 0,
                        "threadsCount": 2,
                        "messagesCount": 15
                    }
                }
            """.trimIndent()
            zos.write(manifestContent.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 2. chats/chats.json (legacy)
            zos.putNextEntry(ZipEntry("chats/chats.json"))
            val legacyChats = """
                {
                    "threads": [{"threadId": "legacy_thread", "bubbleColorHex": "#38BDF8"}],
                    "messages": [{"id": "m1", "threadId": "legacy_thread", "content": "Legacy msg", "timestamp": 500}]
                }
            """.trimIndent()
            zos.write(legacyChats.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 3. notebook/notes/LegacyNote.md
            zos.putNextEntry(ZipEntry("notebook/notes/LegacyNote.md"))
            val noteContent = "---\nid: note_legacy\ntags: []\npinned: false\ncreated: 1000\nmodified: 1000\n---\n# Legacy Note\nContenido de nota legado"
            zos.write(noteContent.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 4. notebook/todos.md
            zos.putNextEntry(ZipEntry("notebook/todos.md"))
            val todosContent = "# Todos\n\n- [x] Tarea legada <!-- id: todo_legacy | created: 1000 -->"
            zos.write(todosContent.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 5. bookmarks/bookmarks.json
            zos.putNextEntry(ZipEntry("bookmarks/bookmarks.json"))
            val bookmarksContent = """
                {
                    "exportedAt": 1000,
                    "collections": [],
                    "bookmarks": [
                        {
                            "id": "bm_legacy",
                            "url": "https://legacy.example.com",
                            "title": "Legacy Site",
                            "domain": "legacy.example.com",
                            "createdAt": 1000
                        }
                    ]
                }
            """.trimIndent()
            zos.write(bookmarksContent.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 6. settings/settings.json
            zos.putNextEntry(ZipEntry("settings/settings.json"))
            val settingsContent = """
                {
                    "theme": "Light",
                    "fontSizeStep": 2,
                    "isVideoMuted": false
                }
            """.trimIndent()
            zos.write(settingsContent.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()
        }

        // Restaurar archivo legacy
        val result = manager.restoreMasterBackup(
            ByteArrayInputStream(out.toByteArray()),
            mode = MasterRestoreMode.CLEAN_RESTORE
        )

        assertTrue("La restauración de un respaldo legado con chats debe ser exitosa", result.isSuccess)
        val stats = result.getOrThrow()
        assertEquals(1, stats.notesCount)
        assertEquals(1, stats.todosCount)
        assertEquals(1, stats.bookmarksCount)

        val restoredNotes = fakeNoteRepository.getAllNotesSync()
        assertEquals(1, restoredNotes.size)
        assertEquals("Legacy Note", restoredNotes.first().title)

        val restoredTodos = fakeTodoRepository.getAllTodosSync()
        assertEquals(1, restoredTodos.size)
        assertTrue(restoredTodos.first().isCompleted)

        val restoredBookmarks = fakeBookmarkRepository.bookmarks
        assertEquals(1, restoredBookmarks.size)
        assertEquals("https://legacy.example.com", restoredBookmarks.first().url)
    }

    @Test
    fun readManifest_extractsManifestWithoutFullDecompression() = runBlocking {
        fakeNoteRepository.saveNote(Note("n1", "Test", "Content", 100L, 100L))
        val out = ByteArrayOutputStream()
        manager.exportMasterBackup(out, includeMedia = false)

        val zipIn = ByteArrayInputStream(out.toByteArray())
        val manifestResult = manager.readManifest(zipIn)

        assertTrue(manifestResult.isSuccess)
        val manifest = manifestResult.getOrThrow()
        assertEquals(1, manifest.manifestVersion)
        assertEquals("1.0.0", manifest.appVersion)
        assertEquals(1, manifest.stats.notesCount)
        assertFalse(manifest.hasMedia)
        assertTrue(manifest.hasSettings)
    }

    @Test
    fun readManifest_failsWhenZipDoesNotContainManifest() = runBlocking {
        // Crear un ZIP simple sin manifest.json
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zos ->
            zos.putNextEntry(ZipEntry("dummy.txt"))
            zos.write("Hola".toByteArray())
            zos.closeEntry()
        }

        val result = manager.readManifest(ByteArrayInputStream(out.toByteArray()))
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is IllegalArgumentException)
    }

    @Test
    fun restoreMasterBackup_inCleanRestoreMode_wipesDataAndRestoresExactState() = runBlocking {
        // 1. Preparar un respaldo en memoria
        fakeNoteRepository.saveNote(Note("id_backup", "Nota Respaldo", "Texto original", 100L, 100L))
        fakeTodoRepository.saveTodo(TodoItem("id_todo", "Tarea Respaldo", "", isCompleted = true, createdAt = 100L))
        val col = BookmarkCollection(id = "c1", name = "Web", colorHex = "#00FF00", iconName = "Folder", createdAt = 100L)
        fakeBookmarkRepository.saveCollection(col)
        val bm = Bookmark("b1", "https://drafto.app", "Drafto App", domain = "drafto.app", createdAt = 100L)
        fakeBookmarkRepository.saveBookmark(bm)
        fakeBookmarkRepository.addBookmarkToCollection("b1", "c1")

        currentSettings = SettingsBackupPayload(theme = "Light", fontSizeStep = 4, isVideoMuted = true)

        val out = ByteArrayOutputStream()
        manager.exportMasterBackup(out, includeMedia = false).getOrThrow()
        val backupZipBytes = out.toByteArray()

        // 2. Modificar el estado actual simulando datos previos que deben ser borrados
        fakeNoteRepository.saveNote(Note("id_old", "Nota Vieja", "Debe desaparecer", 500L, 500L))
        fakeTodoRepository.saveTodo(TodoItem("id_old_todo", "Tarea Vieja", "", isCompleted = false, createdAt = 500L))
        currentSettings = SettingsBackupPayload(theme = "Dark", fontSizeStep = 0)

        // 3. Ejecutar CLEAN_RESTORE
        val restoreResult = manager.restoreMasterBackup(
            ByteArrayInputStream(backupZipBytes),
            mode = MasterRestoreMode.CLEAN_RESTORE
        )

        assertTrue(restoreResult.isSuccess)
        assertTrue("Debe invocar onWipeData() antes de restaurar", wipeDataCalled)

        val stats = restoreResult.getOrThrow()
        assertEquals(1, stats.notesCount)
        assertEquals(1, stats.todosCount)
        assertEquals(1, stats.bookmarksCount)
        assertEquals(1, stats.collectionsCount)

        // Verificar que los datos viejos desaparecieron y solo están los del respaldo
        val currentNotes = fakeNoteRepository.getAllNotesSync()
        assertEquals(1, currentNotes.size)
        assertEquals("Nota Respaldo", currentNotes.first().title)

        val currentTodos = fakeTodoRepository.getAllTodosSync()
        assertEquals(1, currentTodos.size)
        assertEquals("Tarea Respaldo", currentTodos.first().title)
        assertTrue(currentTodos.first().isCompleted)

        val currentBookmarks = fakeBookmarkRepository.getAllBookmarksWithCollections()
        assertEquals(1, currentBookmarks.size)
        assertEquals("https://drafto.app", currentBookmarks.first().first.url)

        // Verificar que las preferencias se restauraron
        assertEquals("Light", currentSettings.theme)
        assertEquals(4, currentSettings.fontSizeStep)
        assertTrue(currentSettings.isVideoMuted)
    }

    @Test
    fun restoreMasterBackup_inMergeMode_deduplicatesAndPreservesExistingData() = runBlocking {
        // 1. Crear respaldo que contiene:
        // - Nota 1 (idéntica a una existente)
        // - Nota 2 (nueva)
        // - Tarea 1 (idéntica a una existente)
        // - Tarea 2 (nueva)
        // - Marcador 1 (idéntico por URL)
        // - Marcador 2 (nuevo)
        fakeNoteRepository.saveNote(Note("n_dup", "Shared Title", "Same Content", 100L, 100L))
        fakeNoteRepository.saveNote(Note("n_new", "Unique Backup Title", "Unique Content", 200L, 200L))

        fakeTodoRepository.saveTodo(TodoItem("t_dup", "Shared Task", "", isCompleted = false, createdAt = 100L))
        fakeTodoRepository.saveTodo(TodoItem("t_new", "New Backup Task", "", isCompleted = true, createdAt = 200L))

        fakeBookmarkRepository.saveBookmark(Bookmark("b_dup", "https://shared.url", "Shared", domain = "shared.url", createdAt = 100L))
        fakeBookmarkRepository.saveBookmark(Bookmark("b_new", "https://new.url", "New Site", domain = "new.url", createdAt = 200L))

        currentSettings = SettingsBackupPayload(theme = "Dark", fontSizeStep = 4)

        val out = ByteArrayOutputStream()
        manager.exportMasterBackup(out, includeMedia = false).getOrThrow()
        val backupZipBytes = out.toByteArray()

        // 2. Modificar el repositorio local para tener duplicados y datos propios
        fakeNoteRepository.notes.clear()
        fakeNoteRepository.saveNote(Note("local_1", "Shared Title", "Same Content", 50L, 50L))
        fakeNoteRepository.saveNote(Note("local_unique", "Local Note", "Local Content", 60L, 60L))

        fakeTodoRepository.todos.clear()
        fakeTodoRepository.saveTodo(TodoItem("local_t1", "Shared Task", "", isCompleted = false, createdAt = 50L))
        fakeTodoRepository.saveTodo(TodoItem("local_t2", "Only Local Task", "", isCompleted = false, createdAt = 60L))

        fakeBookmarkRepository.bookmarks.clear()
        fakeBookmarkRepository.saveBookmark(Bookmark("local_bm1", "https://shared.url", "Shared Title Local", domain = "shared.url", createdAt = 50L))

        currentSettings = SettingsBackupPayload(theme = "Light", fontSizeStep = 1) // Ajustes locales actuales

        // 3. Ejecutar MERGE
        val restoreResult = manager.restoreMasterBackup(
            ByteArrayInputStream(backupZipBytes),
            mode = MasterRestoreMode.MERGE
        )

        assertTrue(restoreResult.isSuccess)
        assertFalse("En modo MERGE NO debe invocarse onWipeData()", wipeDataCalled)

        val stats = restoreResult.getOrThrow()
        assertEquals("Solo 1 nota nueva debió ser importada", 1, stats.notesCount)
        assertEquals("Solo 1 tarea nueva debió ser importada", 1, stats.todosCount)
        assertEquals("Solo 1 marcador nuevo debió ser importado", 1, stats.bookmarksCount)

        // Verificar que en la base de datos local están los preexistentes más los nuevos
        val totalNotes = fakeNoteRepository.getAllNotesSync()
        assertEquals(3, totalNotes.size) // local_1, local_unique, Unique Backup Title

        val totalTodos = fakeTodoRepository.getAllTodosSync()
        assertEquals(3, totalTodos.size) // local_t1, local_t2, New Backup Task

        val totalBookmarks = fakeBookmarkRepository.bookmarks
        assertEquals(2, totalBookmarks.size) // https://shared.url y https://new.url

        // Verificar que los ajustes locales NO fueron alterados en MERGE
        assertEquals("Light", currentSettings.theme)
        assertEquals(1, currentSettings.fontSizeStep)
    }

    @Test
    fun exportAndRestore_rethrowsCancellationException() = runBlocking {
        val cancellingExportManager = MasterBackupManager(
            noteRepository = fakeNoteRepository,
            todoRepository = fakeTodoRepository,
            bookmarkRepository = fakeBookmarkRepository,
            ioDispatcher = Dispatchers.Unconfined,
            exportSettings = { throw CancellationException("Export cancelled") }
        )

        var exportCancelled = false
        try {
            cancellingExportManager.exportMasterBackup(ByteArrayOutputStream(), includeMedia = true)
        } catch (e: CancellationException) {
            exportCancelled = true
            assertEquals("Export cancelled", e.message)
        }
        assertTrue("CancellationException debe ser re-lanzada en exportación", exportCancelled)

        val cancellingRestoreManager = MasterBackupManager(
            noteRepository = fakeNoteRepository,
            todoRepository = fakeTodoRepository,
            bookmarkRepository = fakeBookmarkRepository,
            ioDispatcher = Dispatchers.Unconfined,
            onWipeData = { throw CancellationException("Restore cancelled") }
        )

        var restoreCancelled = false
        try {
            cancellingRestoreManager.restoreMasterBackup(
                ByteArrayInputStream(ByteArray(0)),
                mode = MasterRestoreMode.CLEAN_RESTORE
            )
        } catch (e: CancellationException) {
            restoreCancelled = true
            assertEquals("Restore cancelled", e.message)
        }
        assertTrue("CancellationException debe ser re-lanzada en restauración", restoreCancelled)
    }

    @Test
    fun restoreMasterBackup_withGeneratedTestZip_restoresAll10ItemsPerCategoryCorrectly() = runBlocking {
        val backupFile = File("drafto_test_backup.zip").takeIf { it.exists() }
            ?: File("../drafto_test_backup.zip").takeIf { it.exists() }

        org.junit.Assume.assumeTrue("El archivo drafto_test_backup.zip no existe, omitiendo prueba", backupFile != null)
        assertNotNull("El archivo drafto_test_backup.zip debe existir para esta prueba", backupFile)
        val fileBytes = backupFile!!.readBytes()

        // 1. Validar manifest
        val manifestResult = manager.readManifest(ByteArrayInputStream(fileBytes))
        assertTrue(manifestResult.isSuccess)
        val manifest = manifestResult.getOrThrow()
        assertEquals(10, manifest.stats.notesCount)
        assertEquals(10, manifest.stats.todosCount)
        assertEquals(10, manifest.stats.bookmarksCount)
        assertEquals(10, manifest.stats.collectionsCount)

        // 2. Restaurar en CLEAN_RESTORE
        val restoreResult = manager.restoreMasterBackup(
            ByteArrayInputStream(fileBytes),
            mode = MasterRestoreMode.CLEAN_RESTORE
        )
        assertTrue(restoreResult.isSuccess)
        val stats = restoreResult.getOrThrow()
        assertEquals(10, stats.notesCount)
        assertEquals(10, stats.todosCount)
        assertEquals(10, stats.bookmarksCount)
        assertEquals(10, stats.collectionsCount)

        // 3. Validar contenido restaurado
        val notes = fakeNoteRepository.getAllNotesSync()
        assertEquals(10, notes.size)
        assertTrue(notes.all { it.collectionId != null })

        val todos = fakeTodoRepository.getAllTodosSync()
        assertEquals(10, todos.size)
        val todosWithSubtasks = todos.filter { it.subtasks.isNotEmpty() }
        assertEquals(10, todosWithSubtasks.size)

        val bookmarks = fakeBookmarkRepository.getAllBookmarksWithCollections()
        assertEquals(10, bookmarks.size)

        val collections = fakeBookmarkRepository.getAllCollectionsSync()
        assertEquals(10, collections.size)
    }

    // --- Repositorios Falsos en Memoria para pruebas unitarias limpias ---

    private class FakeNoteRepository : NoteRepository {
        val notes = mutableListOf<Note>()
        override fun getAllNotes(): Flow<List<Note>> = flowOf(notes.toList())
        override suspend fun getAllNotesSync(): List<Note> = notes.toList()
        override suspend fun getNoteById(id: String): Note? = notes.find { it.id == id }
        override suspend fun saveNote(note: Note) {
            notes.removeAll { it.id == note.id }
            notes.add(note)
        }
        override suspend fun deleteNote(id: String) {
            notes.removeAll { it.id == id }
        }
        override suspend fun saveAllNotes(notes: List<Note>) {
            notes.forEach { saveNote(it) }
        }
        override fun getNotesByCollection(collectionId: String): Flow<List<Note>> =
            flowOf(notes.filter { it.collectionId == collectionId })
        override suspend fun getNotesByCollectionSync(collectionId: String): List<Note> =
            notes.filter { it.collectionId == collectionId }
    }

    private class FakeTodoRepository : TodoRepository {
        val todos = mutableListOf<TodoItem>()
        override fun observeAllTodos(): Flow<List<TodoItem>> = flowOf(todos.toList())
        override suspend fun getAllTodosSync(): List<TodoItem> = todos.toList()
        override fun observePendingTodos(): Flow<List<TodoItem>> = flowOf(todos.filter { !it.isCompleted })
        override fun observeCompletedTodos(): Flow<List<TodoItem>> = flowOf(todos.filter { it.isCompleted })
        override fun observeTodosByCollection(collectionId: String): Flow<List<TodoItem>> =
            flowOf(todos.filter { it.collectionId == collectionId })
        override suspend fun getTodosByCollectionSync(collectionId: String): List<TodoItem> =
            todos.filter { it.collectionId == collectionId }
        override suspend fun saveTodo(todo: TodoItem) {
            todos.removeAll { it.id == todo.id }
            todos.add(todo)
        }
        override suspend fun toggleCompleted(id: String, isCompleted: Boolean) {
            val idx = todos.indexOfFirst { it.id == id }
            if (idx != -1) todos[idx] = todos[idx].copy(isCompleted = isCompleted)
        }
        override suspend fun togglePinned(id: String, isPinned: Boolean) {
            val idx = todos.indexOfFirst { it.id == id }
            if (idx != -1) todos[idx] = todos[idx].copy(isPinned = isPinned)
        }
        override suspend fun getTodoById(id: String): TodoItem? = todos.find { it.id == id }
        override suspend fun toggleSubtask(todoId: String, subtaskId: String, isDone: Boolean) {
            val idx = todos.indexOfFirst { it.id == todoId }
            if (idx != -1) {
                val current = todos[idx]
                val updatedSubtasks = current.subtasks.map {
                    if (it.id == subtaskId) it.copy(isDone = isDone) else it
                }
                todos[idx] = current.copy(subtasks = updatedSubtasks)
            }
        }
        override suspend fun deleteTodo(id: String) {
            todos.removeAll { it.id == id }
        }
    }

    private class FakeBookmarkRepository : BookmarkRepository {
        val bookmarks = mutableListOf<Bookmark>()
        val collections = mutableListOf<BookmarkCollection>()
        val relations = mutableListOf<Pair<String, String>>()

        override fun observeAllBookmarks(): Flow<List<Bookmark>> = flowOf(bookmarks.toList())
        override fun observeBookmarksByCollection(collectionId: String): Flow<List<Bookmark>> {
            val ids = relations.filter { it.second == collectionId }.map { it.first }.toSet()
            return flowOf(bookmarks.filter { it.id in ids })
        }
        override fun observeBookmarksByPlatform(platform: PlatformFilter): Flow<List<Bookmark>> = flowOf(bookmarks.toList())
        override fun searchBookmarks(query: String): Flow<List<Bookmark>> = flowOf(bookmarks.filter { it.title?.contains(query) == true || it.url.contains(query) })
        override suspend fun findBookmarkByUrl(url: String): Bookmark? = bookmarks.find { it.url.equals(url, ignoreCase = true) }
        override suspend fun findBookmarkById(id: String): Bookmark? = bookmarks.find { it.id == id }
        override suspend fun saveBookmark(bookmark: Bookmark): Long {
            bookmarks.removeAll { it.id == bookmark.id }
            bookmarks.add(bookmark)
            return 1L
        }
        override suspend fun updateBookmark(bookmark: Bookmark) { saveBookmark(bookmark) }
        override suspend fun deleteBookmark(id: String) {
            val affectedCols = relations.filter { it.first == id }.map { it.second }
            bookmarks.removeAll { it.id == id }
            relations.removeAll { it.first == id }
            affectedCols.forEach { colId ->
                deleteCollectionIfEmpty(colId)
            }
        }
        override suspend fun togglePin(id: String, isPinned: Boolean) {
            val idx = bookmarks.indexOfFirst { it.id == id }
            if (idx != -1) bookmarks[idx] = bookmarks[idx].copy(isPinned = isPinned)
        }
        override suspend fun updatePreviewVisibility(id: String, isVisible: Boolean) {
            val idx = bookmarks.indexOfFirst { it.id == id }
            if (idx != -1) bookmarks[idx] = bookmarks[idx].copy(isPreviewVisible = isVisible)
        }
        override fun observeAllCollections(): Flow<List<BookmarkCollection>> = flowOf(collections.toList())
        override suspend fun saveCollection(collection: BookmarkCollection) {
            collections.removeAll { it.id == collection.id }
            collections.add(collection)
        }
        override suspend fun updateCollection(collection: BookmarkCollection) { saveCollection(collection) }
        override suspend fun deleteCollection(collectionId: String) {
            collections.removeAll { it.id == collectionId }
            relations.removeAll { it.second == collectionId }
        }
        override suspend fun deleteCollectionIfEmpty(collectionId: String): Int {
            val count = relations.count { it.second == collectionId }
            return if (count == 0) {
                collections.removeAll { it.id == collectionId }
                1
            } else 0
        }
        override suspend fun deleteEmptyCollections(): Int {
            val emptyIds = collections.map { it.id }.filter { id -> relations.none { it.second == id } }
            collections.removeAll { it.id in emptyIds }
            return emptyIds.size
        }
        override suspend fun addBookmarkToCollection(bookmarkId: String, collectionId: String) {
            if (relations.none { it.first == bookmarkId && it.second == collectionId }) {
                relations.add(bookmarkId to collectionId)
            }
        }
        override suspend fun removeBookmarkFromCollection(bookmarkId: String, collectionId: String) {
            relations.removeAll { it.first == bookmarkId && it.second == collectionId }
            deleteCollectionIfEmpty(collectionId)
        }
        override fun observeCollectionIdsForBookmark(bookmarkId: String): Flow<List<String>> =
            flowOf(relations.filter { it.first == bookmarkId }.map { it.second })
        override fun observeBookmarkCountForCollection(collectionId: String): Flow<Int> =
            flowOf(relations.count { it.second == collectionId })
        override suspend fun getAllBookmarksWithCollections(): List<Pair<Bookmark, List<BookmarkCollection>>> {
            return bookmarks.map { bm ->
                val colIds = relations.filter { it.first == bm.id }.map { it.second }.toSet()
                Pair(bm, collections.filter { it.id in colIds })
            }
        }
        override suspend fun getBookmarksByCollectionSync(collectionId: String): List<Bookmark> {
            val ids = relations.filter { it.second == collectionId }.map { it.first }.toSet()
            return bookmarks.filter { it.id in ids }
        }
        override suspend fun getAllCollectionsSync(): List<BookmarkCollection> = collections.toList()
        override suspend fun getAllRelationsSync(): List<Pair<String, String>> = relations.toList()
        override suspend fun findCollectionByName(name: String): BookmarkCollection? =
            collections.find { it.name.equals(name, ignoreCase = true) }
        override suspend fun importBookmarksBatch(bookmarksWithFolders: List<Pair<Bookmark, List<String>>>): Pair<Int, Int> {
            var newBm = 0
            var newCol = 0
            for ((bm, folders) in bookmarksWithFolders) {
                if (findBookmarkByUrl(bm.url) == null) {
                    saveBookmark(bm)
                    newBm++
                    for (folder in folders) {
                        var c = findCollectionByName(folder)
                        if (c == null) {
                            c = BookmarkCollection(id = UUID.randomUUID().toString(), name = folder, colorHex = "#000000", iconName = "Folder", bookmarkCount = 0, createdAt = System.currentTimeMillis())
                            saveCollection(c)
                            newCol++
                        }
                        addBookmarkToCollection(bm.id, c.id)
                    }
                }
            }
            return Pair(newBm, newCol)
        }
    }
}
