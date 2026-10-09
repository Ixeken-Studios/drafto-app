package com.ixeken.drafto.domain.model

import androidx.compose.runtime.Immutable

/**
 * Modelo de dominio inmutable que representa una nota activa fijada en la bandeja de notificaciones.
 * Se utiliza este modelo separado para desacoplar la persistencia temporal de la base de datos principal Room.
 */
@Immutable
data class LiveNote(
    val id: String = "live_note_active",
    val text: String,
    val expirationTimestampMillis: Long,
    val isExpired: Boolean = false
)

