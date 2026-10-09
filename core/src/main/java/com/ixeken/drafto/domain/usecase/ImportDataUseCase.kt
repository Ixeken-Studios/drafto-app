package com.ixeken.drafto.domain.usecase

import com.ixeken.drafto.domain.repository.BackupRepository
import com.ixeken.drafto.domain.repository.NoteRepository

class ImportDataUseCase(
    private val backupRepository: BackupRepository,
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(jsonContent: String): Result<Unit> {
        return backupRepository.importData(jsonContent).map { backup ->
            noteRepository.saveAllNotes(backup.notes)
        }
    }
}
