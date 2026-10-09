package com.ixeken.drafto.util

import com.ixeken.drafto.domain.model.TodoItem
import com.ixeken.drafto.domain.model.TodoSubtask
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

/**
 * Analizador sintáctico y generador puro en Kotlin para serialización y deserialización
 * bidireccional entre la lista de tareas inmutables [TodoItem] y archivos Markdown `todos.md`.
 *
 * Decisiones de diseño arquitectónico:
 * - Pureza total: Cero dependencias de Android, Room o Compose. Toda la lógica de fechas opera
 *   en UTC mediante `java.time.*` para garantizar determinismo absoluto entre husos horarios y entornos.
 * - Sintaxis estándar Markdown Task List: Compatible de forma nativa con GitHub Flavored Markdown,
 *   Obsidian Checklist Plugin y editores estándar (`- [ ]`, `- [x]`, `- [X]`).
 * - Soporte jerárquico de subtareas: Reconoce subtareas sangradas (`  - [ ]`, `    - [x]`) y las anida
 *   directamente bajo el ítem padre precedente sin desestructurar el orden del documento.
 * - Metadatos embebidos en tags: Los atributos adicionales de Drafto (fecha de vencimiento `#due:YYYY-MM-DD`,
 *   estado fijado `#pinned` y colección `#col:<id>`) se codifican como etiquetas inline sin ensuciar la legibilidad humana.
 * - Resiliencia sintáctica: Tolera encabezados `# `, líneas vacías, comentarios y sangrías sin interrumpir
 *   el procesamiento secuencial.
 */
object MarkdownTodoParser {

    private val ROOT_TASK_REGEX = Regex("""^[-*]\s*\[([ xX])\]\s*(.*)$""")
    private val SUBTASK_REGEX = Regex("""^\s{2,}[-*]\s*\[([ xX])\]\s*(.*)$""")
    private val PINNED_TAG_REGEX = Regex("""#pinned\b""", RegexOption.IGNORE_CASE)
    private val DUE_TAG_REGEX = Regex("""#due:(\d{4}-\d{2}-\d{2})\b""", RegexOption.IGNORE_CASE)
    private val COL_TAG_REGEX = Regex("""#col:([a-zA-Z0-9_-]+)\b""", RegexOption.IGNORE_CASE)

    /**
     * Serializa una lista de ítems de tarea a la representación canónica en formato Markdown `todos.md`.
     *
     * @param todos Lista inmutable de tareas a exportar.
     * @return Contenido textual en formato Markdown con encabezado, casillas raíz y subtareas sangradas.
     */
    fun exportToMarkdown(todos: List<TodoItem>): String {
        val sb = StringBuilder()
        sb.append("# To-dos\n\n")

        for (todo in todos) {
            val checkbox = if (todo.isCompleted) "- [x]" else "- [ ]"
            val rawTitle = when {
                todo.title.isBlank() && todo.description.isNotBlank() -> todo.description
                todo.description.isNotBlank() && !todo.title.contains(todo.description) -> "${todo.title} - ${todo.description}"
                else -> todo.title
            }

            val parts = mutableListOf<String>()
            parts.add(checkbox)

            if (rawTitle.isNotBlank()) {
                parts.add(rawTitle.trim())
            }

            if (todo.dueDate != null) {
                parts.add("#due:${formatUtcDate(todo.dueDate)}")
            }

            if (!todo.collectionId.isNullOrBlank()) {
                parts.add("#col:${todo.collectionId.trim()}")
            }

            if (todo.isPinned) {
                parts.add("#pinned")
            }

            sb.append(parts.joinToString(" ")).append("\n")

            // Serializar subtareas jerárquicas con sangría de 2 espacios
            for (subtask in todo.subtasks) {
                val subCheckbox = if (subtask.isDone) "  - [x]" else "  - [ ]"
                sb.append(subCheckbox).append(" ").append(subtask.text.trim()).append("\n")
            }
        }

        return sb.toString()
    }

    /**
     * Analiza el contenido de un documento Markdown y extrae una lista inmutable de tareas [TodoItem].
     *
     * @param rawContent Cadena de texto bruta obtenida de `todos.md`.
     * @return Lista de tareas reconstruidas con identificadores UUID únicos y sus subtareas anidadas.
     */
    fun parseFromMarkdown(rawContent: String): List<TodoItem> {
        val cleaned = rawContent.removePrefix("\uFEFF").trim()
        if (cleaned.isBlank()) {
            return emptyList()
        }

        val todos = mutableListOf<TodoItem>()
        val lines = cleaned.lines()

        var currentParent: TodoItem? = null
        var currentSubtasks = mutableListOf<TodoSubtask>()

        fun commitCurrentParent() {
            val parent = currentParent ?: return
            todos.add(parent.copy(subtasks = currentSubtasks.toList()))
            currentParent = null
            currentSubtasks = mutableListOf()
        }

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith("#") || trimmed.startsWith("<!--")) {
                continue
            }

            // 1. Evaluar si es una subtarea sangrada subordinada a la tarea raíz actual
            val subMatch = SUBTASK_REGEX.matchEntire(line)
            if (subMatch != null && currentParent != null) {
                val isDone = subMatch.groupValues[1].equals("x", ignoreCase = true)
                val text = subMatch.groupValues[2].trim()
                if (text.isNotBlank()) {
                    currentSubtasks.add(
                        TodoSubtask(
                            id = UUID.randomUUID().toString(),
                            text = text,
                            isDone = isDone
                        )
                    )
                }
                continue
            }

            // 2. Evaluar si es una tarea raíz
            val rootMatch = ROOT_TASK_REGEX.matchEntire(trimmed)
            if (rootMatch != null) {
                commitCurrentParent()

                val isCompleted = rootMatch.groupValues[1].equals("x", ignoreCase = true)
                var text = rootMatch.groupValues[2].trim()

                var isPinned = false
                if (PINNED_TAG_REGEX.containsMatchIn(text)) {
                    isPinned = true
                    text = text.replace(PINNED_TAG_REGEX, "").trim()
                }

                var dueDate: Long? = null
                val dueMatch = DUE_TAG_REGEX.find(text)
                if (dueMatch != null) {
                    val dateStr = dueMatch.groupValues[1]
                    dueDate = parseUtcDate(dateStr)
                    text = text.replace(DUE_TAG_REGEX, "").trim()
                }

                var collectionId: String? = null
                val colMatch = COL_TAG_REGEX.find(text)
                if (colMatch != null) {
                    collectionId = colMatch.groupValues[1]
                    text = text.replace(COL_TAG_REGEX, "").trim()
                }

                val cleanTitle = text.replace(Regex("""\s+"""), " ").trim()
                val title = if (cleanTitle.isNotBlank()) cleanTitle else "Sin título"
                val now = System.currentTimeMillis()

                currentParent = TodoItem(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    description = "",
                    isCompleted = isCompleted,
                    isPinned = isPinned,
                    dueDate = dueDate,
                    createdAt = now,
                    completedAt = if (isCompleted) now else null,
                    collectionId = collectionId
                )
            }
        }

        commitCurrentParent()
        return todos
    }

    /**
     * Convierte una marca de tiempo en milisegundos a formato de fecha ISO-8601 UTC `YYYY-MM-DD`.
     */
    fun formatUtcDate(epochMillis: Long): String {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .toString()
    }

    /**
     * Convierte una cadena de fecha ISO-8601 `YYYY-MM-DD` a milisegundos de época en el inicio del día UTC.
     * Retorna null si el formato es inválido o no corresponde a una fecha del calendario gregoriano.
     */
    fun parseUtcDate(dateStr: String): Long? {
        return try {
            LocalDate.parse(dateStr)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}
