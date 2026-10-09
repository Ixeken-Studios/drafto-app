package com.ixeken.drafto.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = 2,
    val exportedAt: Long,
    val notes: List<Note> = emptyList()
)
