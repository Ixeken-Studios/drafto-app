package com.ixeken.drafto.util

import com.ixeken.drafto.domain.model.TodoItem
import com.ixeken.drafto.domain.model.TodoSubtask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Pruebas unitarias para [MarkdownTodoParser].
 *
 * Valida la serialización y deserialización de listas de tareas en formato Markdown `todos.md`,
 * incluyendo el manejo de casillas marcadas/desmarcadas, tags `#due:YYYY-MM-DD` en UTC,
 * tags `#pinned`, resiliencia ante comentarios y encabezados, y determinismo en fechas.
 */
class MarkdownTodoParserTest {

    @Test
    fun exportToMarkdown_withMixedTodos_formatsCorrectly() {
        val dueDateMillis = LocalDate.of(2026, 9, 5)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

        val todos = listOf(
            TodoItem(
                id = "todo-1",
                title = "Comprar café",
                description = "",
                isCompleted = false,
                isPinned = true,
                dueDate = dueDateMillis,
                createdAt = 1725450000000L
            ),
            TodoItem(
                id = "todo-2",
                title = "Revisar pull request",
                description = "",
                isCompleted = true,
                isPinned = false,
                dueDate = null,
                createdAt = 1725451000000L,
                completedAt = 1725452000000L
            )
        )

        val exported = MarkdownTodoParser.exportToMarkdown(todos)

        val expected = """
            # To-dos

            - [ ] Comprar café #due:2026-09-05 #pinned
            - [x] Revisar pull request

        """.trimIndent()

        assertEquals(expected, exported)
    }

    @Test
    fun exportToMarkdown_concatenatesDescriptionWhenNotContainedInTitle() {
        val todo = TodoItem(
            id = "todo-desc",
            title = "Llamar al banco",
            description = "preguntar por tarjeta de crédito",
            isCompleted = false,
            createdAt = 1725450000000L
        )

        val exported = MarkdownTodoParser.exportToMarkdown(listOf(todo))

        assertTrue(exported.contains("- [ ] Llamar al banco - preguntar por tarjeta de crédito"))
    }

    @Test
    fun exportToMarkdown_withEmptyList_returnsOnlyHeader() {
        val exported = MarkdownTodoParser.exportToMarkdown(emptyList())

        assertEquals("# To-dos\n\n", exported)
    }

    @Test
    fun parseFromMarkdown_extractsTodosWithCompletedPinnedAndDueDate() {
        val raw = """
            # To-dos

            - [ ] Comprar café #due:2026-09-05 #pinned
            - [x] Revisar pull request
            - [X] Tarea completada con X mayúscula
        """.trimIndent()

        val todos = MarkdownTodoParser.parseFromMarkdown(raw)

        assertEquals(3, todos.size)

        // Item 1
        val item1 = todos[0]
        assertNotNull(item1.id)
        assertEquals("Comprar café", item1.title)
        assertFalse(item1.isCompleted)
        assertTrue(item1.isPinned)
        assertNotNull(item1.dueDate)
        val expectedDueDate = LocalDate.of(2026, 9, 5)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        assertEquals(expectedDueDate, item1.dueDate)
        assertNull(item1.completedAt)

        // Item 2
        val item2 = todos[1]
        assertEquals("Revisar pull request", item2.title)
        assertTrue(item2.isCompleted)
        assertFalse(item2.isPinned)
        assertNull(item2.dueDate)
        assertNotNull(item2.completedAt)

        // Item 3
        val item3 = todos[2]
        assertEquals("Tarea completada con X mayúscula", item3.title)
        assertTrue(item3.isCompleted)
        assertFalse(item3.isPinned)
    }

    @Test
    fun parseFromMarkdown_ignoresHeadersCommentsAndBlankLines() {
        val raw = """
            # Encabezado Principal
            
            <!-- Comentario de prueba que debe ignorarse -->
            Texto explicativo no correspondiente a una tarea.
            
            ## Sección Secundaria
            - [ ] Tarea válida después del texto
            
            > Cita en bloque
            - [x] Tarea completada final
        """.trimIndent()

        val todos = MarkdownTodoParser.parseFromMarkdown(raw)

        assertEquals(2, todos.size)
        assertEquals("Tarea válida después del texto", todos[0].title)
        assertFalse(todos[0].isCompleted)
        assertEquals("Tarea completada final", todos[1].title)
        assertTrue(todos[1].isCompleted)
    }

    @Test
    fun parseFromMarkdown_tolerantToIndentationAndExtraSpaces() {
        val raw = """
              - [ ]   Subtarea con sangría y espacios   #pinned   #due:2026-12-31
        """.trimIndent()

        val todos = MarkdownTodoParser.parseFromMarkdown(raw)

        assertEquals(1, todos.size)
        val item = todos[0]
        assertEquals("Subtarea con sangría y espacios", item.title)
        assertTrue(item.isPinned)
        assertNotNull(item.dueDate)
        val expectedDueDate = LocalDate.of(2026, 12, 31)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        assertEquals(expectedDueDate, item.dueDate)
    }

    @Test
    fun dateConversions_deterministicUtcRoundTrip() {
        val targetEpoch = LocalDate.of(2026, 11, 20)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

        val formattedDate = MarkdownTodoParser.formatUtcDate(targetEpoch)
        assertEquals("2026-11-20", formattedDate)

        val parsedEpoch = MarkdownTodoParser.parseUtcDate(formattedDate)
        assertEquals(targetEpoch, parsedEpoch)

        // Verificación de resiliencia con fecha malformada
        assertNull(MarkdownTodoParser.parseUtcDate("fecha-invalida"))
        assertNull(MarkdownTodoParser.parseUtcDate("2026-99-99"))
    }

    @Test
    fun exportToMarkdown_withSubtasksAndCollection_formatsHierarchically() {
        val todo = TodoItem(
            id = "todo-root",
            title = "Diseño de interfaz",
            description = "",
            isCompleted = false,
            isPinned = true,
            dueDate = null,
            createdAt = 1725450000000L,
            collectionId = "col-ui-456",
            subtasks = listOf(
                TodoSubtask(id = "sub-1", text = "Buscar referencias", isDone = true),
                TodoSubtask(id = "sub-2", text = "Crear prototipo Figma", isDone = false)
            )
        )

        val exported = MarkdownTodoParser.exportToMarkdown(listOf(todo))

        val expected = """
            # To-dos

            - [ ] Diseño de interfaz #col:col-ui-456 #pinned
              - [x] Buscar referencias
              - [ ] Crear prototipo Figma

        """.trimIndent()

        assertEquals(expected, exported)
    }

    @Test
    fun parseFromMarkdown_withSubtasksAndCollection_reconstructsHierarchy() {
        val raw = """
            # To-dos

            - [ ] Lista principal #col:col-work #due:2026-10-15
              - [x] Primer paso completado
              - [ ] Segundo paso pendiente
            - [x] Tarea independiente terminada
        """.trimIndent()

        val todos = MarkdownTodoParser.parseFromMarkdown(raw)

        assertEquals(2, todos.size)

        val parent = todos[0]
        assertEquals("Lista principal", parent.title)
        assertFalse(parent.isCompleted)
        assertEquals("col-work", parent.collectionId)
        assertNotNull(parent.dueDate)
        assertEquals(2, parent.subtasks.size)
        assertEquals("Primer paso completado", parent.subtasks[0].text)
        assertTrue(parent.subtasks[0].isDone)
        assertEquals("Segundo paso pendiente", parent.subtasks[1].text)
        assertFalse(parent.subtasks[1].isDone)

        val standalone = todos[1]
        assertEquals("Tarea independiente terminada", standalone.title)
        assertTrue(standalone.isCompleted)
        assertTrue(standalone.subtasks.isEmpty())
    }
}
