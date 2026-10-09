package com.ixeken.drafto.domain.usecase

import com.ixeken.drafto.domain.model.LiveNote
import com.ixeken.drafto.domain.repository.LiveNoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Caso de uso enfocado en administrar el ciclo de vida de la nota activa en vivo (crear, extender tiempo y completar).
 */
@Singleton
class ManageLiveNoteUseCase @Inject constructor(
    private val repository: LiveNoteRepository
) {
    /**
     * Retorna el flujo reactivo de la nota activa.
     */
    fun getActiveLiveNote(): Flow<LiveNote?> {
        return repository.getActiveLiveNote()
    }

    /**
     * Guarda una nueva nota con una duración de 6 horas por defecto.
     */
    suspend fun createLiveNote(text: String): LiveNote {
        return repository.saveLiveNote(text, durationHours = 6)
    }

    /**
     * Extiende la duración de la nota actual en 3 horas adicionales.
     */
    suspend fun extendLiveNoteTime() {
        repository.extendLiveNoteTime(additionalHours = 3)
    }

    /**
     * Finaliza y elimina la nota activa.
     */
    suspend fun completeLiveNote() {
        repository.clearLiveNote()
    }

    /**
     * Modifica el estado de expiración de la nota activa.
     */
    suspend fun markExpired(isExpired: Boolean) {
        repository.updateExpiredState(isExpired)
    }
}

