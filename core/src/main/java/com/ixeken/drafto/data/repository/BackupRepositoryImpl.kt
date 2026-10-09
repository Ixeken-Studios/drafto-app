package com.ixeken.drafto.data.repository

import com.ixeken.drafto.domain.model.BackupData
import com.ixeken.drafto.domain.repository.BackupRepository
import com.ixeken.drafto.domain.repository.NoteRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BackupRepositoryImpl(
    private val noteRepository: NoteRepository,
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true }
) : BackupRepository {

    override suspend fun exportData(): String {
        val notes = noteRepository.getAllNotes().first()

        val backupData = BackupData(
            version = 2,
            exportedAt = System.currentTimeMillis(),
            notes = notes
        )
        return json.encodeToString(backupData)
    }

    override suspend fun importData(jsonContent: String): Result<BackupData> {
        return runCatching {
            json.decodeFromString<BackupData>(jsonContent)
        }
    }
}
