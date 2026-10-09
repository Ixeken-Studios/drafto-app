package com.ixeken.drafto.domain.repository

import com.ixeken.drafto.domain.model.LiveNote
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de repositorio para administrar el flujo reactivo y la persistencia de la nota activa en vivo.
 * Permite que los casos de uso y ViewModels interactúen con el almacenamiento sin conocer DataStore directamente.
 */
interface LiveNoteRepository {
    /**
     * Obtenemos un flujo reactivo con la nota activa o null si no existe ninguna.
     */
    fun getActiveLiveNote(): Flow<LiveNote?>

    /**
     * Guarda o reemplaza la nota activa con una nueva duración.
     */
    suspend fun saveLiveNote(text: String, durationHours: Long = 6): LiveNote

    /**
     * Incrementa el tiempo de expiración de la nota activa en las horas especificadas.
     */
    suspend fun extendLiveNoteTime(additionalHours: Long = 3)

    /**
     * Elimina completamente la nota activa y borra su registro.
     */
    suspend fun clearLiveNote()

    /**
     * Actualiza el estado de expiración de la nota.
     */
    suspend fun updateExpiredState(isExpired: Boolean)
}

