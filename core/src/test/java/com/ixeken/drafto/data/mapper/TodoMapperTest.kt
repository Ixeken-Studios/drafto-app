package com.ixeken.drafto.data.mapper

import com.ixeken.drafto.data.local.entity.TodoEntity
import com.ixeken.drafto.domain.model.TodoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pruebas unitarias para verificar la conversion bidireccional entre [TodoEntity] y [TodoItem].
 */
class TodoMapperTest {

    @Test
    fun todoEntity_toDomain_mapsAllFieldsCorrectly() {
        val entity = TodoEntity(
            id = "todo-123",
            title = "Comprar insumos",
            description = "Papel y tinta",
            isCompleted = true,
            isPinned = true,
            dueDate = 1700000000L,
            createdAt = 1690000000L,
            completedAt = 1695000000L
        )

        val domain = entity.toDomain()

        assertEquals("todo-123", domain.id)
        assertEquals("Comprar insumos", domain.title)
        assertEquals("Papel y tinta", domain.description)
        assertEquals(true, domain.isCompleted)
        assertEquals(true, domain.isPinned)
        assertEquals(1700000000L, domain.dueDate)
        assertEquals(1690000000L, domain.createdAt)
        assertEquals(1695000000L, domain.completedAt)
    }

    @Test
    fun todoItem_toEntity_mapsAllFieldsCorrectly() {
        val domain = TodoItem(
            id = "todo-456",
            title = "Revisar arquitectura",
            description = "Validar Room v6",
            isCompleted = false,
            isPinned = false,
            dueDate = null,
            createdAt = 1691000000L,
            completedAt = null
        )

        val entity = domain.toEntity()

        assertEquals("todo-456", entity.id)
        assertEquals("Revisar arquitectura", entity.title)
        assertEquals("Validar Room v6", entity.description)
        assertEquals(false, entity.isCompleted)
        assertEquals(false, entity.isPinned)
        assertNull(entity.dueDate)
        assertEquals(1691000000L, entity.createdAt)
        assertNull(entity.completedAt)
    }
}
