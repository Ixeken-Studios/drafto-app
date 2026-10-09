package com.ixeken.drafto.domain.repository

import com.ixeken.drafto.domain.model.BackupData

interface BackupRepository {
    suspend fun exportData(): String
    suspend fun importData(jsonContent: String): Result<BackupData>
}

