package com.ixeken.drafto.data.backup

import com.ixeken.drafto.domain.model.Note
import com.ixeken.drafto.domain.model.TodoItem
import com.ixeken.drafto.domain.repository.NoteRepository
import com.ixeken.drafto.domain.repository.TodoRepository
import com.ixeken.drafto.util.MarkdownNoteParser
import com.ixeken.drafto.util.MarkdownTodoParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Resumen consolidado del resultado de una operación de exportación de libreta.
 *
 * @property notesCount Total de notas individuales exportadas exitosamente en el archivo comprimido.
 * @property todosCount Total de ítems de tareas exportados dentro del archivo `todos.md`.
 */
data class NotebookBackupSummary(
    val notesCount: Int,
    val todosCount: Int
)

/**
 * Resumen consolidado del resultado de una operación de importación de libreta.
 *
 * Permite a la interfaz informar al usuario de forma transparente cuántos elementos fueron
 * integrados y cuántos fueron omitidos por considerarse duplicados idénticos no destructivos.
 *
 * @property notesImported Cantidad de notas nuevas almacenadas en el repositorio local.
 * @property notesSkipped Cantidad de notas idénticas ya existentes que se preservaron sin duplicación.
 * @property todosImported Cantidad de tareas nuevas guardadas en la base de datos.
 * @property todosSkipped Cantidad de tareas preexistentes idénticas que no se reinsertaron.
 */
data class NotebookImportSummary(
    val notesImported: Int,
    val notesSkipped: Int,
    val todosImported: Int,
    val todosSkipped: Int
)

/**
 * Resultado de la importación de una nota individual desde un archivo Markdown.
 *
 * @property note Nota importada o nota existente que coincidió exactamente.
 * @property isDuplicate `true` si la nota ya existía de forma idéntica en la libreta y fue omitida.
 */
data class SingleNoteImportResult(
    val note: Note,
    val isDuplicate: Boolean
)

/**
 * Resultado de la importación de una lista de tareas To-do desde un archivo Markdown.
 *
 * @property importedCount Cantidad de tareas nuevas agregadas exitosamente.
 * @property skippedCount Cantidad de tareas que ya existían y fueron omitidas.
 */
data class SingleTodosImportResult(
    val importedCount: Int,
    val skippedCount: Int
)

/**
 * Estado reactivo representativo del avance temporal de operaciones de exportación o importación.
 *
 * Diseñado para alimentar barras de progreso continuas o modales de carga sin provocar recomposiciones
 * excesivas en la interfaz Compose.
 *
 * @property isActive Indica si hay una operación de I/O en curso actualmente.
 * @property isExport `true` si la operación en progreso es una exportación, `false` si es importación.
 * @property progress Valor normalizado entre 0.0f y 1.0f que representa el avance estimado.
 * @property statusText Mensaje descriptivo legible por el usuario que contextualiza el paso actual.
 */
data class NotebookBackupProgressState(
    val isActive: Boolean = false,
    val isExport: Boolean = true,
    val progress: Float = 0f,
    val statusText: String = ""
)

/**
 * Gestor centralizado de respaldo e importación universal para la libreta unificada de Drafto (Notebook).
 *
 * Decisiones de diseño arquitectónico:
 * - Streaming en memoria acotada: Todas las operaciones de exportación e importación se procesan
 *   mediante [ZipOutputStream] y [ZipInputStream] con búferes nativos de 8 KB sobre [Dispatchers.IO],
 *   garantizando que libretas con cientos de notas no eleven el consumo de memoria heap (evitando OOM).
 * - Deduplicación no destructiva: La importación evalúa la huella de (título, contenido) en notas y
 *   (título, estado de cumplimiento) en tareas para no sobreescribir ni corromper elementos preexistentes.
 * - Resolución determinista de colisiones: Durante la exportación, si múltiples notas comparten el
 *   mismo título, se indexan secuencialmente con sufijos numéricos evitando sobrescrituras en el archivo ZIP.
 * - Desacoplamiento de plataforma: No depende de URIs ni clases específicas de Android o Compose,
 *   permitiendo pruebas unitarias puras y reutilización directa en workers de sincronización en segundo plano.
 */
class NotebookBackupManager(
    private val noteRepository: NoteRepository,
    private val todoRepository: TodoRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Exporta todas las notas y tareas existentes a un flujo [OutputStream] comprimido en formato ZIP.
     *
     * Estructura generada dentro del ZIP:
     * - `notes/<sanitized_name>.md`: Una entrada por cada nota guardada con Frontmatter YAML.
     * - `todos.md`: Archivo único en la raíz con el listado de tareas en formato Markdown Task List
     *   (únicamente si existen tareas en el repositorio).
     *
     * @param outputStream Flujo de destino provisto habitualmente por el SAF de Android.
     * @param onProgress Función de retorno para notificar el progreso porcentual y el estado de la tarea.
     * @return [Result] con [NotebookBackupSummary] si la operación fue exitosa, o la excepción ocurrida.
     */
    suspend fun exportNotebookZip(
        outputStream: OutputStream,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): Result<NotebookBackupSummary> = withContext(ioDispatcher) {
        try {
            onProgress(0.1f, "Preparing notes and to-dos...")

            val notes = noteRepository.getAllNotesSync()
            val todos = todoRepository.getAllTodosSync()

            onProgress(0.3f, "Exporting notes...")

            ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                val usedNames = mutableSetOf<String>()

                for ((index, note) in notes.withIndex()) {
                    val baseFileName = MarkdownNoteParser.sanitizeFileName(note.title)
                    val nameWithoutExt = baseFileName.removeSuffix(".md")
                    var entryName = "notes/$baseFileName"
                    var counter = 1

                    while (entryName in usedNames) {
                        entryName = "notes/$nameWithoutExt ($counter).md"
                        counter++
                    }
                    usedNames.add(entryName)

                    val mdContent = MarkdownNoteParser.exportToMarkdown(note)
                    zipOut.putNextEntry(ZipEntry(entryName))
                    zipOut.write(mdContent.toByteArray(StandardCharsets.UTF_8))
                    zipOut.closeEntry()

                    if (notes.isNotEmpty()) {
                        val progress = 0.3f + (0.5f * (index + 1) / notes.size)
                        onProgress(progress, "Exporting notes (${index + 1}/${notes.size})...")
                    }
                }

                if (todos.isNotEmpty()) {
                    onProgress(0.85f, "Exporting to-dos...")
                    val todosContent = MarkdownTodoParser.exportToMarkdown(todos)
                    zipOut.putNextEntry(ZipEntry("todos.md"))
                    zipOut.write(todosContent.toByteArray(StandardCharsets.UTF_8))
                    zipOut.closeEntry()
                }

                zipOut.finish()
                zipOut.flush()
            }

            onProgress(1.0f, "Export complete")
            Result.success(
                NotebookBackupSummary(
                    notesCount = notes.size,
                    todosCount = todos.size
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Exporta una nota individual de forma aislada escribiendo su contenido Markdown en el [OutputStream].
     *
     * @param note Nota de dominio a exportar.
     * @param outputStream Flujo de salida donde se grabará el archivo `.md`.
     * @return [Result] exitoso o con el error I/O correspondiente.
     */
    suspend fun exportSingleNote(
        note: Note,
        outputStream: OutputStream
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            val markdown = MarkdownNoteParser.exportToMarkdown(note)
            BufferedOutputStream(outputStream).use { bos ->
                bos.write(markdown.toByteArray(StandardCharsets.UTF_8))
                bos.flush()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Importa una nota individual desde un [InputStream] que contiene texto Markdown plano (.md).
     *
     * Decodifica con UTF-8, analiza metadatos YAML frontmatter o encabezados `# `,
     * y aplica deduplicación no destructiva comparando con las notas existentes en SQLite.
     *
     * @param fileName Nombre original del archivo fuente (usado como respaldo para el título).
     * @param inputStream Flujo con el contenido Markdown.
     * @return [Result] con [SingleNoteImportResult] indicando la nota y si resultó duplicada.
     */
    suspend fun importSingleNote(
        fileName: String,
        inputStream: InputStream
    ): Result<SingleNoteImportResult> = withContext(ioDispatcher) {
        try {
            val content = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            val parsedNote = MarkdownNoteParser.parseFromMarkdown(fileName, content)

            val existingNotes = noteRepository.getAllNotesSync()
            val duplicate = existingNotes.firstOrNull {
                it.title.trim() == parsedNote.title.trim() && it.content.trim() == parsedNote.content.trim()
            }

            if (duplicate != null) {
                Result.success(SingleNoteImportResult(note = duplicate, isDuplicate = true))
            } else {
                noteRepository.saveNote(parsedNote)
                Result.success(SingleNoteImportResult(note = parsedNote, isDuplicate = false))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Importa tareas To-do desde un [InputStream] que contiene texto Markdown con checklist (`- [ ]` / `- [x]`).
     *
     * Decodifica con UTF-8, analiza ítems de tarea y marcas `#due:YYYY-MM-DD` o `#pinned`,
     * y aplica deduplicación no destructiva contra las tareas existentes en SQLite.
     *
     * @param inputStream Flujo con el contenido Markdown de tareas.
     * @return [Result] con [SingleTodosImportResult] indicando el número de tareas importadas y omitidas.
     */
    suspend fun importTodosMarkdown(
        inputStream: InputStream
    ): Result<SingleTodosImportResult> = withContext(ioDispatcher) {
        try {
            val content = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            val parsedTodos = MarkdownTodoParser.parseFromMarkdown(content)

            if (parsedTodos.isEmpty()) {
                return@withContext Result.success(SingleTodosImportResult(importedCount = 0, skippedCount = 0))
            }

            val existingTodos = todoRepository.getAllTodosSync()
            val existingKeys = existingTodos.map { TodoMatchKey(it.title.trim(), it.isCompleted) }.toMutableSet()

            var imported = 0
            var skipped = 0

            for (todo in parsedTodos) {
                val key = TodoMatchKey(todo.title.trim(), todo.isCompleted)
                if (key in existingKeys) {
                    skipped++
                } else {
                    existingKeys.add(key)
                    todoRepository.saveTodo(todo)
                    imported++
                }
            }

            Result.success(SingleTodosImportResult(importedCount = imported, skippedCount = skipped))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Descomprime e importa una libreta completa desde un [InputStream] ZIP mediante streaming continuo.
     *
     * Aplica deduplicación no destructiva comparando con los registros actuales de la base de datos:
     * - Notas bajo carpeta notes/: Si ya existe una nota con el mismo título y contenido exacto, se omite.
     *   De lo contrario, se guarda mediante [NoteRepository.saveNote].
     * - Tareas en `todos.md`: Si ya existe una tarea con idéntico título y estado de cumplimiento, se omite.
     *   De lo contrario, se guarda mediante [TodoRepository.saveTodo].
     *
     * @param inputStream Flujo de entrada del archivo comprimido.
     * @param onProgress Función de retorno para notificar el progreso porcentual y el estado de la tarea.
     * @return [Result] con [NotebookImportSummary] detallando ítems importados y omitidos.
     */
    suspend fun importNotebookZip(
        inputStream: InputStream,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): Result<NotebookImportSummary> = withContext(ioDispatcher) {
        try {
            onProgress(0.1f, "Reading backup archive...")

            val existingNotes = noteRepository.getAllNotesSync()
            val existingNoteKeys = existingNotes.map { NoteMatchKey(it.title, it.content) }.toMutableSet()

            val existingTodos = todoRepository.getAllTodosSync()
            val existingTodoKeys = existingTodos.map { TodoMatchKey(it.title, it.isCompleted) }.toMutableSet()

            var notesImported = 0
            var notesSkipped = 0
            var todosImported = 0
            var todosSkipped = 0

            onProgress(0.3f, "Extracting entries...")

            ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val rawName = entry.name.replace("\\", "/").trimStart('/')
                    val fileName = rawName.substringAfterLast('/')

                    // Ignorar directorios puros, archivos ocultos y artefactos de macOS
                    if (entry.isDirectory || rawName.endsWith("/") || fileName.startsWith("._") || rawName.contains("__MACOSX/")) {
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                        continue
                    }

                    when {
                        rawName.startsWith("notes/") && fileName.endsWith(".md", ignoreCase = true) -> {
                            val content = zipIn.readBytes().toString(StandardCharsets.UTF_8)
                            val parsedNote = MarkdownNoteParser.parseFromMarkdown(fileName, content)
                            val key = NoteMatchKey(parsedNote.title, parsedNote.content)
                            if (key in existingNoteKeys) {
                                notesSkipped++
                            } else {
                                existingNoteKeys.add(key)
                                noteRepository.saveNote(parsedNote)
                                notesImported++
                            }
                            onProgress(0.6f, "Processing notes ($notesImported imported, $notesSkipped skipped)...")
                        }
                        rawName == "todos.md" || rawName.endsWith("/todos.md") -> {
                            val content = zipIn.readBytes().toString(StandardCharsets.UTF_8)
                            val parsedTodos = MarkdownTodoParser.parseFromMarkdown(content)
                            for (todo in parsedTodos) {
                                val key = TodoMatchKey(todo.title, todo.isCompleted)
                                if (key in existingTodoKeys) {
                                    todosSkipped++
                                } else {
                                    existingTodoKeys.add(key)
                                    todoRepository.saveTodo(todo)
                                    todosImported++
                                }
                            }
                            onProgress(0.85f, "Processing to-dos ($todosImported imported, $todosSkipped skipped)...")
                        }
                    }

                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            onProgress(1.0f, "Import complete")
            Result.success(
                NotebookImportSummary(
                    notesImported = notesImported,
                    notesSkipped = notesSkipped,
                    todosImported = todosImported,
                    todosSkipped = todosSkipped
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    private data class NoteMatchKey(val title: String, val content: String)
    private data class TodoMatchKey(val title: String, val isCompleted: Boolean)
}
