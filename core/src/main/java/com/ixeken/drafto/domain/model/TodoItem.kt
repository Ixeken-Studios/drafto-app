package com.ixeken.drafto.domain.model

import kotlinx.serialization.Serializable

/**
 * Criterios de filtrado reactivo para la visualización y consulta de tareas en Notebook.
 *
 * Permite segmentar el flujo de tareas según su estado de cumplimiento sin obligar a la UI
 * a realizar filtrados pesados en memoria durante la composición.
 */
@Serializable
enum class TodoFilter {
    ALL,
    PENDING,
    COMPLETED
}

/**
 * Representa un ítem o lista de tareas estructurada dentro del dominio de Drafto.
 *
 * Diseñado como modelo inmutable desacoplado de dependencias de Android, Compose o Room.
 * Permite tanto tareas atómicas simples como listas jerárquicas con subtareas secuenciales,
 * cálculo de progreso y soporte de finalización parcial.
 */
@Serializable
data class TodoItem(
    val id: String,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val isPinned: Boolean = false,
    val dueDate: Long? = null,
    val createdAt: Long,
    val completedAt: Long? = null,
    val collectionId: String? = null,
    val subtasks: List<TodoSubtask> = emptyList()
) {
    val totalSubtasksCount: Int
        get() = subtasks.size

    val completedSubtasksCount: Int
        get() = subtasks.count { it.isDone }

    val hasSubtasks: Boolean
        get() = subtasks.isNotEmpty()

    val progressPercentage: Float
        get() = if (subtasks.isEmpty()) {
            if (isCompleted) 1f else 0f
        } else {
            completedSubtasksCount.toFloat() / totalSubtasksCount.toFloat()
        }

    val isAllCompleted: Boolean
        get() = if (subtasks.isEmpty()) isCompleted else completedSubtasksCount == totalSubtasksCount
}
