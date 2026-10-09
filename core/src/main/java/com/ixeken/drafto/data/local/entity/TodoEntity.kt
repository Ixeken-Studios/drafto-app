package com.ixeken.drafto.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

import com.ixeken.drafto.domain.model.TodoSubtask

/**
 * Entidad Room para la persistencia local de tareas en la tabla SQLite `todos`.
 *
 * Incluye indices B-Tree compuestos en `(isCompleted, createdAt)` y `(isPinned)` para optimizar
 * las consultas reactivas filtradas y ordenadas en la interfaz de Notebook sin generar jank.
 */
@Entity(
    tableName = "todos",
    indices = [
        Index(value = ["isCompleted", "createdAt"]),
        Index(value = ["isPinned"]),
        Index(value = ["collectionId"])
    ]
)
data class TodoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val isPinned: Boolean = false,
    val dueDate: Long? = null,
    val createdAt: Long,
    val completedAt: Long? = null,
    val collectionId: String? = null,
    val subtasks: List<TodoSubtask> = emptyList()
)
