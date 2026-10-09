package com.ixeken.drafto.data.mapper

import com.ixeken.drafto.data.local.entity.NoteEntity
import com.ixeken.drafto.domain.model.Note

fun NoteEntity.toDomain(): Note {
    return Note(
        id = id,
        title = title,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isPinned = isPinned,
        tags = tags,
        collectionId = collectionId
    )
}

fun Note.toEntity(): NoteEntity {
    return NoteEntity(
        id = id,
        title = title,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isPinned = isPinned,
        tags = tags,
        collectionId = collectionId
    )
}

