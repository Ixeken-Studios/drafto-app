package com.ixeken.drafto.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean = false,
    val tags: List<String> = emptyList(),
    val collectionId: String? = null
)

