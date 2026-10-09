package com.ixeken.drafto.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Representa una subtarea atómica dentro de una lista de tareas estructurada.
 *
 * Diseñado como modelo inmutable desacoplado de Room y Compose, permitiendo
 * serialización directa y estabilidad garantizada por compose_compiler_config.conf.
 *
 * @property id Identificador único de la subtarea.
 * @property text Descripción textual de la subtarea.
 * @property isDone Indica si la subtarea fue completada.
 */
@Serializable
data class TodoSubtask(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isDone: Boolean = false
)
