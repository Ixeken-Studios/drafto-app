package com.ixeken.drafto.domain.usecase

import com.ixeken.drafto.domain.repository.BackupRepository

class ExportDataUseCase(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(): String {
        return backupRepository.exportData()
    }
}

