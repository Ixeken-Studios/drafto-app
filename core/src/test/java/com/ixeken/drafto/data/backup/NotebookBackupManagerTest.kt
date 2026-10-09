package com.ixeken.drafto.data.backup

import com.ixeken.drafto.domain.model.Note
import com.ixeken.drafto.domain.model.TodoItem
import com.ixeken.drafto.domain.repository.NoteRepository
import com.ixeken.drafto.domain.repository.TodoRepository
import com.ixeken.drafto.util.MarkdownNoteParser
import com.ixeken.drafto.util.MarkdownTodoParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Pruebas unitarias para validar [NotebookBackupManager].
 *
 * Evalua el streaming de exportacion e importacion de archivos ZIP, la serializacion
 * de notas individuales en Markdown, la deduplicacion no destructiva y la resolucion
 * de colisiones en nombres de archivo.
 */
class NotebookBackupManagerTest {

    private lateinit var fakeNoteRepository: FakeNoteRepository
    private lateinit var fakeTodoRepository: FakeTodoRepository
    private lateinit var backupManager: NotebookBackupManager

    @Before
    fun setUp() {
        fakeNoteRepository = FakeNoteRepository()
        fakeTodoRepository = FakeTodoRepository()
        backupManager = NotebookBackupManager(
            noteRepository = fakeNoteRepository,
            todoRepository = fakeTodoRepository,
            ioDispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun exportNotebookZip_exportsNotesAndTodosToZipSuccessfully() = runBlocking {
        val note1 = Note(
            id = "n1",
            title = "Project Ideas",
            content = "Exciting architecture concepts.",
            createdAt = 1000L,
            updatedAt = 2000L,
            isPinned = true,
            tags = listOf("architecture", "drafto")
        )
        val note2 = Note(
            id = "n2",
            title = "Groceries",
            content = "Apples, Milk, Bread",
            createdAt = 3000L,
            updatedAt = 4000L
        )
        fakeNoteRepository.saveNote(note1)
        fakeNoteRepository.saveNote(note2)

        val todo1 = TodoItem(
            id = "t1",
            title = "Review PR",
            isCompleted = false,
            createdAt = 1000L
        )
        val todo2 = TodoItem(
            id = "t2",
            title = "Submit release",
            isCompleted = true,
            createdAt = 2000L
        )
        fakeTodoRepository.saveTodo(todo1)
        fakeTodoRepository.saveTodo(todo2)

        val outputStream = ByteArrayOutputStream()
        val progressUpdates = mutableListOf<Float>()

        val result = backupManager.exportNotebookZip(outputStream) { progress, _ ->
            progressUpdates.add(progress)
        }

        assertTrue(result.isSuccess)
        val summary = result.getOrThrow()
        assertEquals(2, summary.notesCount)
        assertEquals(2, summary.todosCount)
        assertTrue(progressUpdates.contains(1.0f))

        // Inspeccionar el contenido del ZIP generado
        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(outputStream.toByteArray())).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entries[entry.name] = zipIn.readBytes().toString(StandardCharsets.UTF_8)
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }

        assertTrue(entries.containsKey("notes/Project Ideas.md"))
        assertTrue(entries.containsKey("notes/Groceries.md"))
        assertTrue(entries.containsKey("todos.md"))

        // Verificar el contenido de una nota exportada
        val projectIdeasMd = entries["notes/Project Ideas.md"]!!
        assertTrue(projectIdeasMd.contains("title: \"Project Ideas\""))
        assertTrue(projectIdeasMd.contains("Exciting architecture concepts."))

        // Verificar el contenido del archivo de tareas
        val todosMd = entries["todos.md"]!!
        assertTrue(todosMd.contains("- [ ] Review PR"))
        assertTrue(todosMd.contains("- [x] Submit release"))
    }

    @Test
    fun exportNotebookZip_avoidsFileNameCollisionsForSameTitle() = runBlocking {
        val note1 = Note(id = "n1", title = "Daily Log", content = "Day 1 log", createdAt = 100L, updatedAt = 100L)
        val note2 = Note(id = "n2", title = "Daily Log", content = "Day 2 log", createdAt = 200L, updatedAt = 200L)
        val note3 = Note(id = "n3", title = "Daily Log", content = "Day 3 log", createdAt = 300L, updatedAt = 300L)

        fakeNoteRepository.saveNote(note1)
        fakeNoteRepository.saveNote(note2)
        fakeNoteRepository.saveNote(note3)

        val outputStream = ByteArrayOutputStream()
        val result = backupManager.exportNotebookZip(outputStream)

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrThrow().notesCount)

        val entryNames = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(outputStream.toByteArray())).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entryNames.add(entry.name)
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }

        assertTrue(entryNames.contains("notes/Daily Log.md"))
        assertTrue(entryNames.contains("notes/Daily Log (1).md"))
        assertTrue(entryNames.contains("notes/Daily Log (2).md"))
    }

    @Test
    fun exportNotebookZip_whenNoTodos_doesNotCreateTodosMd() = runBlocking {
        val note = Note(id = "n1", title = "Solo Note", content = "Only note", createdAt = 100L, updatedAt = 100L)
        fakeNoteRepository.saveNote(note)

        val outputStream = ByteArrayOutputStream()
        val result = backupManager.exportNotebookZip(outputStream)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().notesCount)
        assertEquals(0, result.getOrThrow().todosCount)

        val entryNames = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(outputStream.toByteArray())).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entryNames.add(entry.name)
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }

        assertTrue(entryNames.contains("notes/Solo Note.md"))
        assertFalse(entryNames.contains("todos.md"))
    }

    @Test
    fun exportSingleNote_exportsMarkdownCorrectly() = runBlocking {
        val note = Note(
            id = "n1",
            title = "Meeting Summary",
            content = "Agreed on API contract.",
            createdAt = 1000L,
            updatedAt = 2000L,
            isPinned = true,
            tags = listOf("work")
        )

        val outputStream = ByteArrayOutputStream()
        val result = backupManager.exportSingleNote(note, outputStream)

        assertTrue(result.isSuccess)
        val exportedMd = outputStream.toByteArray().toString(StandardCharsets.UTF_8)
        assertTrue(exportedMd.contains("title: \"Meeting Summary\""))
        assertTrue(exportedMd.contains("pinned: true"))
        assertTrue(exportedMd.contains("- work"))
        assertTrue(exportedMd.contains("Agreed on API contract."))
    }

    @Test
    fun importNotebookZip_importsNewNotesAndTodosCorrectly() = runBlocking {
        // Generar un ZIP sintético en memoria
        val zipBytes = createTestZip(
            notes = listOf(
                "notes/Intro.md" to "---\ntitle: \"Intro\"\npinned: false\n---\n\nWelcome to Drafto",
                "notes/Setup.md" to "# Setup Guide\n\nRun the build command."
            ),
            todosContent = "# To-dos\n\n- [ ] Install dependencies\n- [x] Clone repository"
        )

        val result = backupManager.importNotebookZip(ByteArrayInputStream(zipBytes))

        assertTrue(result.isSuccess)
        val summary = result.getOrThrow()
        assertEquals(2, summary.notesImported)
        assertEquals(0, summary.notesSkipped)
        assertEquals(2, summary.todosImported)
        assertEquals(0, summary.todosSkipped)

        val allNotes = fakeNoteRepository.getAllNotesSync()
        assertEquals(2, allNotes.size)
        assertTrue(allNotes.any { it.title == "Intro" && it.content == "Welcome to Drafto" })
        assertTrue(allNotes.any { it.title == "Setup Guide" })

        val allTodos = fakeTodoRepository.getAllTodosSync()
        assertEquals(2, allTodos.size)
        assertTrue(allTodos.any { it.title == "Install dependencies" && !it.isCompleted })
        assertTrue(allTodos.any { it.title == "Clone repository" && it.isCompleted })
    }

    @Test
    fun importNotebookZip_skipsExistingDuplicatesNonDestructively() = runBlocking {
        // Pre-poblar el repositorio con una nota y una tarea existentes
        val preExistingNote = Note(
            id = "existing_1",
            title = "Existing Note",
            content = "Same exact content",
            createdAt = 100L,
            updatedAt = 100L
        )
        fakeNoteRepository.saveNote(preExistingNote)

        val preExistingTodo = TodoItem(
            id = "existing_t1",
            title = "Existing Task",
            isCompleted = false,
            createdAt = 100L
        )
        fakeTodoRepository.saveTodo(preExistingTodo)

        // ZIP contiene 1 nota duplicada y 1 nueva, más 1 tarea duplicada y 1 nueva
        val zipBytes = createTestZip(
            notes = listOf(
                "notes/Existing_Note.md" to "---\ntitle: \"Existing Note\"\n---\n\nSame exact content",
                "notes/Brand_New.md" to "---\ntitle: \"Brand New\"\n---\n\nFresh content"
            ),
            todosContent = "# To-dos\n\n- [ ] Existing Task\n- [x] Brand New Task"
        )

        val result = backupManager.importNotebookZip(ByteArrayInputStream(zipBytes))

        assertTrue(result.isSuccess)
        val summary = result.getOrThrow()
        assertEquals(1, summary.notesImported)
        assertEquals(1, summary.notesSkipped)
        assertEquals(1, summary.todosImported)
        assertEquals(1, summary.todosSkipped)

        val finalNotes = fakeNoteRepository.getAllNotesSync()
        assertEquals(2, finalNotes.size)
        // La nota preexistente conserva su ID original
        val foundPreExisting = finalNotes.firstOrNull { it.title == "Existing Note" }
        assertNotNull(foundPreExisting)
        assertEquals("existing_1", foundPreExisting?.id)

        val finalTodos = fakeTodoRepository.getAllTodosSync()
        assertEquals(2, finalTodos.size)
        val foundPreExistingTodo = finalTodos.firstOrNull { it.title == "Existing Task" }
        assertNotNull(foundPreExistingTodo)
        assertEquals("existing_t1", foundPreExistingTodo?.id)
    }

    @Test
    fun importNotebookZip_ignoresMacOsAndHiddenFiles() = runBlocking {
        val zipBytes = createTestZip(
            notes = listOf(
                "notes/valid.md" to "# Valid Note\n\nContent",
                "notes/._hidden.md" to "macos artifact",
                "__MACOSX/notes/._valid.md" to "finder metadata"
            ),
            todosContent = null
        )

        val result = backupManager.importNotebookZip(ByteArrayInputStream(zipBytes))

        assertTrue(result.isSuccess)
        val summary = result.getOrThrow()
        assertEquals(1, summary.notesImported)
        assertEquals(0, summary.notesSkipped)
        assertEquals(1, fakeNoteRepository.getAllNotesSync().size)
    }

    @Test
    fun importSingleNote_newNote_savesSuccessfully() = runBlocking {
        val markdown = """
            ---
            title: "Obsidian Meeting Note"
            pinned: true
            createdAt: 1000
            updatedAt: 2000
            tags:
              - meetings
            ---
            Discuss Q3 roadmap.
        """.trimIndent()

        val inputStream = ByteArrayInputStream(markdown.toByteArray(StandardCharsets.UTF_8))
        val result = backupManager.importSingleNote("Obsidian Meeting Note.md", inputStream)

        assertTrue(result.isSuccess)
        val singleResult = result.getOrThrow()
        assertFalse(singleResult.isDuplicate)
        assertEquals("Obsidian Meeting Note", singleResult.note.title)
        assertEquals("Discuss Q3 roadmap.", singleResult.note.content)
        assertTrue(singleResult.note.isPinned)
        assertEquals(listOf("meetings"), singleResult.note.tags)
        assertEquals(1, fakeNoteRepository.getAllNotesSync().size)
    }

    @Test
    fun importSingleNote_duplicateNote_returnsDuplicateWithoutReSaving() = runBlocking {
        val existingNote = Note(
            id = "n-exist",
            title = "Design Principles",
            content = "Keep it simple.",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        fakeNoteRepository.saveNote(existingNote)

        val markdown = MarkdownNoteParser.exportToMarkdown(existingNote)

        val inputStream = ByteArrayInputStream(markdown.toByteArray(StandardCharsets.UTF_8))
        val result = backupManager.importSingleNote("Design Principles.md", inputStream)

        assertTrue(result.isSuccess)
        val singleResult = result.getOrThrow()
        assertTrue(singleResult.isDuplicate)
        assertEquals(existingNote.id, singleResult.note.id)
        // No se debe duplicar el registro en el repositorio
        assertEquals(1, fakeNoteRepository.getAllNotesSync().size)
    }

    @Test
    fun importTodosMarkdown_parsesAndSavesTodosWithDeduplication() = runBlocking {
        val existingTodo = TodoItem(
            id = "t-exist",
            title = "Existing Task",
            isCompleted = false,
            createdAt = 1000L
        )
        fakeTodoRepository.saveTodo(existingTodo)

        val markdown = """
            # My Tasks
            
            - [ ] Existing Task
            - [ ] New Task 1 #due:2026-10-15 #pinned
            - [x] Finished Task
        """.trimIndent()

        val inputStream = ByteArrayInputStream(markdown.toByteArray(StandardCharsets.UTF_8))
        val result = backupManager.importTodosMarkdown(inputStream)

        assertTrue(result.isSuccess)
        val summary = result.getOrThrow()
        assertEquals(2, summary.importedCount)
        assertEquals(1, summary.skippedCount)

        val allTodos = fakeTodoRepository.getAllTodosSync()
        assertEquals(3, allTodos.size)
        val newPinned = allTodos.first { it.title == "New Task 1" }
        assertTrue(newPinned.isPinned)
        assertNotNull(newPinned.dueDate)
    }

    @Test
    fun importTodosMarkdown_emptyContent_returnsZero() = runBlocking {
        val inputStream = ByteArrayInputStream("".toByteArray(StandardCharsets.UTF_8))
        val result = backupManager.importTodosMarkdown(inputStream)

        assertTrue(result.isSuccess)
        val summary = result.getOrThrow()
        assertEquals(0, summary.importedCount)
        assertEquals(0, summary.skippedCount)
        assertEquals(0, fakeTodoRepository.getAllTodosSync().size)
    }

    private fun createTestZip(
        notes: List<Pair<String, String>>,
        todosContent: String?
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zipOut ->
            for ((path, content) in notes) {
                zipOut.putNextEntry(ZipEntry(path))
                zipOut.write(content.toByteArray(StandardCharsets.UTF_8))
                zipOut.closeEntry()
            }
            if (todosContent != null) {
                zipOut.putNextEntry(ZipEntry("todos.md"))
                zipOut.write(todosContent.toByteArray(StandardCharsets.UTF_8))
                zipOut.closeEntry()
            }
            zipOut.finish()
            zipOut.flush()
        }
        return baos.toByteArray()
    }

    private class FakeNoteRepository : NoteRepository {
        val notes = mutableMapOf<String, Note>()

        override fun getAllNotes(): Flow<List<Note>> = flowOf(notes.values.toList())

        override suspend fun getAllNotesSync(): List<Note> = notes.values.toList()

        override suspend fun getNoteById(id: String): Note? = notes[id]

        override suspend fun saveNote(note: Note) {
            notes[note.id] = note
        }

        override suspend fun deleteNote(id: String) {
            notes.remove(id)
        }

        override suspend fun saveAllNotes(notes: List<Note>) {
            notes.forEach { this.notes[it.id] = it }
        }

        override fun getNotesByCollection(collectionId: String): Flow<List<Note>> =
            flowOf(notes.values.filter { it.collectionId == collectionId })

        override suspend fun getNotesByCollectionSync(collectionId: String): List<Note> =
            notes.values.filter { it.collectionId == collectionId }
    }

    private class FakeTodoRepository : TodoRepository {
        val todos = mutableMapOf<String, TodoItem>()

        override fun observeAllTodos(): Flow<List<TodoItem>> = flowOf(todos.values.toList())

        override suspend fun getAllTodosSync(): List<TodoItem> = todos.values.toList()

        override fun observePendingTodos(): Flow<List<TodoItem>> = flowOf(todos.values.filter { !it.isCompleted })

        override fun observeCompletedTodos(): Flow<List<TodoItem>> = flowOf(todos.values.filter { it.isCompleted })

        override fun observeTodosByCollection(collectionId: String): Flow<List<TodoItem>> =
            flowOf(todos.values.filter { it.collectionId == collectionId })

        override suspend fun getTodosByCollectionSync(collectionId: String): List<TodoItem> =
            todos.values.filter { it.collectionId == collectionId }

        override suspend fun saveTodo(todo: TodoItem) {
            todos[todo.id] = todo
        }

        override suspend fun toggleCompleted(id: String, isCompleted: Boolean) {
            todos[id]?.let {
                todos[id] = it.copy(isCompleted = isCompleted, completedAt = if (isCompleted) 1000L else null)
            }
        }

        override suspend fun togglePinned(id: String, isPinned: Boolean) {
            todos[id]?.let {
                todos[id] = it.copy(isPinned = isPinned)
            }
        }

        override suspend fun getTodoById(id: String): TodoItem? = todos[id]

        override suspend fun toggleSubtask(todoId: String, subtaskId: String, isDone: Boolean) {
            val current = todos[todoId] ?: return
            val updatedSubtasks = current.subtasks.map {
                if (it.id == subtaskId) it.copy(isDone = isDone) else it
            }
            todos[todoId] = current.copy(subtasks = updatedSubtasks)
        }

        override suspend fun deleteTodo(id: String) {
            todos.remove(id)
        }
    }
}
