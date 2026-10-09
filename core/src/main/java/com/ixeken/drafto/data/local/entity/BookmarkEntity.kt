package com.ixeken.drafto.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa un marcador persistido en la base de datos local SQLite.
 *
 * Incluye índices B-Tree en campos clave (url, createdAt, isPinned) para acelerar
 * la ordenación y filtrado rápido sin degradación en listas extensas.
 *
 * @property id Identificador primario único del marcador.
 * @property url Dirección URL original del recurso web.
 * @property title Título extraído o asignado al marcador.
 * @property description Resumen o descripción del recurso web.
 * @property imageUrl URL de imagen previa destacada para visualización enriquecida.
 * @property domain Host o dominio web simplificado.
 * @property faviconUrl Icono de sitio web de alta resolución.
 * @property createdAt Marca temporal de almacenamiento en milisegundos.
 * @property isPinned Indicador booleano de fijado prioritario.
 * @property platform Categoría de plataforma (INSTAGRAM, YOUTUBE, ARTICLE, etc.).
 */
@Entity(
    tableName = "bookmarks",
    indices = [
        Index("url"),
        Index("createdAt"),
        Index("isPinned"),
        Index("collectionId")
    ]
)
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val domain: String,
    val faviconUrl: String? = null,
    val createdAt: Long,
    val isPinned: Boolean = false,
    val platform: String = "GENERIC",
    val isPreviewVisible: Boolean = true,
    val collectionId: String? = null
)
