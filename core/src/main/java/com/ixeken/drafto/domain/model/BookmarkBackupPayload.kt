package com.ixeken.drafto.domain.model

import kotlinx.serialization.Serializable

/**
 * Modelo de datos inmutable para el respaldo estructurado y serialización JSON de marcadores en Drafto.
 *
 * Preserva con fidelidad absoluta los datos de dominio: identificadores, marcas de tiempo,
 * configuraciones cromáticas de colecciones, iconos asociados y relaciones many-to-many.
 *
 * @property version Versión del esquema de respaldo para garantizar compatibilidad hacia adelante.
 * @property exportedAt Marca de tiempo en milisegundos correspondiente a la generación del archivo.
 * @property collections Colecciones temáticas respaldadas.
 * @property bookmarks Marcadores individuales respaldados.
 * @property relations Relaciones cruzadas entre marcadores y colecciones.
 */
@Serializable
data class BookmarkBackupPayload(
    val version: Int = 1,
    val exportedAt: Long = 0L,
    val collections: List<BookmarkCollection> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val relations: List<BookmarkCollectionRelationPayload> = emptyList()
)

/**
 * Representación serializable de la relación asociativa entre un marcador y una colección.
 *
 * @property bookmarkId Identificador único del marcador.
 * @property collectionId Identificador único de la colección a la que pertenece.
 */
@Serializable
data class BookmarkCollectionRelationPayload(
    val bookmarkId: String,
    val collectionId: String
)
