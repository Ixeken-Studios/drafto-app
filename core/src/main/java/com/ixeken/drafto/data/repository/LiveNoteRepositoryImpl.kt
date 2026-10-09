package com.ixeken.drafto.data.repository

import com.ixeken.drafto.data.local.datastore.LiveNoteDataStore
import com.ixeken.drafto.domain.model.LiveNote
import com.ixeken.drafto.domain.repository.LiveNoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación del repositorio de notas en vivo.
 * Transforma operaciones lógicas en llamadas asíncronas hacia el DataStore local.
 */
@Singleton
class LiveNoteRepositoryImpl @Inject constructor(
    private val dataStore: LiveNoteDataStore
) : LiveNoteRepository {

    override fun getActiveLiveNote(): Flow<LiveNote?> {
        return dataStore.liveNoteFlow
    }

    override suspend fun saveLiveNote(text: String, durationHours: Long): LiveNote {
        val now = System.currentTimeMillis()
        val expiration = now + TimeUnit.HOURS.toMillis(durationHours)
        val note = LiveNote(
            id = "live_note_${now}",
            text = text.trim(),
            expirationTimestampMillis = expiration,
            isExpired = false
        )
        dataStore.saveLiveNote(note)
        return note
    }

    override suspend fun extendLiveNoteTime(additionalHours: Long) {
        val currentNote = dataStore.liveNoteFlow.firstOrNull() ?: return
        val now = System.currentTimeMillis()
        // Si ya había expirado, tomamos como base el momento actual más las horas adicionales.
        val baseTimestamp = if (currentNote.expirationTimestampMillis < now) now else currentNote.expirationTimestampMillis
        val newExpiration = baseTimestamp + TimeUnit.HOURS.toMillis(additionalHours)
        dataStore.updateExpiration(newExpirationMillis = newExpiration, isExpired = false)
    }

    override suspend fun clearLiveNote() {
        dataStore.clearLiveNote()
    }

    override suspend fun updateExpiredState(isExpired: Boolean) {
        val currentNote = dataStore.liveNoteFlow.firstOrNull() ?: return
        dataStore.updateExpiration(currentNote.expirationTimestampMillis, isExpired = isExpired)
    }
}

