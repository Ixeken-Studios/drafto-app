package com.ixeken.drafto.data.mapper

import com.ixeken.drafto.data.local.entity.TodoEntity
import com.ixeken.drafto.domain.model.TodoItem

/**
 * Mapeadores bidireccionales entre la entidad Room de persistencia y el modelo inmutable de dominio.
 */

/**
 * Transforma una [TodoEntity] persistida en SQLite a su modelo representativo inmutable [TodoItem].
 */
fun TodoEntity.toDomain(): TodoItem {
    return TodoItem(
        id = id,
        title = title,
        description = description,
        isCompleted = isCompleted,
        isPinned = isPinned,
        dueDate = dueDate,
        createdAt = createdAt,
        completedAt = completedAt,
        collectionId = collectionId,
        subtasks = subtasks
    )
}

/**
 * Transforma un modelo inmutable de dominio [TodoItem] a su entidad [TodoEntity] para operaciones en Room.
 */
fun TodoItem.toEntity(): TodoEntity {
    return TodoEntity(
        id = id,
        title = title,
        description = description,
        isCompleted = isCompleted,
        isPinned = isPinned,
        dueDate = dueDate,
        createdAt = createdAt,
        completedAt = completedAt,
        collectionId = collectionId,
        subtasks = subtasks
    )
}
