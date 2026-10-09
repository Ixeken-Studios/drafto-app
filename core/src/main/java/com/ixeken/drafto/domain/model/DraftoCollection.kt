package com.ixeken.drafto.domain.model

import kotlinx.serialization.Serializable

/**
 * Modelo de dominio puro inmutable que representa una colección o carpeta global en Drafto.
 *
 * Permite agrupar de forma unificada notas, to-dos y marcadores bajo un mismo paraguas temático,
 * con color de acento personalizado e icono vectorial representativo.
 *
 * @property id Identificador único universal de la colección.
 * @property name Nombre asignado a la colección.
 * @property colorHex Código de color hexadecimal asociado para distintivos visuales Nothing OS.
 * @property iconName Nombre clave del icono Material representativo de la colección.
 * @property noteCount Número total de notas asociadas a esta colección.
 * @property todoCount Número total de tareas asociadas a esta colección.
 * @property bookmarkCount Número total de marcadores asociados a esta colección.
 * @property createdAt Marca temporal en milisegundos de la creación de la colección.
 */
@Serializable
data class DraftoCollection(
    val id: String,
    val name: String,
    val colorHex: String,
    val iconName: String,
    val noteCount: Int = 0,
    val todoCount: Int = 0,
    val bookmarkCount: Int = 0,
    val createdAt: Long
) {
    /**
     * Número total acumulado de elementos contenidos en la colección (notas + tareas + enlaces).
     */
    val totalCount: Int get() = noteCount + todoCount + bookmarkCount
}

/**
 * Paleta canónica de 12 colores semánticos predeterminados para colecciones Nothing OS.
 */
val DraftoCollectionPresetColors: List<String> = listOf(
    "monochrome",
    "crimson",
    "orange",
    "amber",
    "lime",
    "emerald",
    "cyan",
    "cobalt",
    "indigo",
    "purple",
    "pink",
    "clay"
)

