package com.ixeken.drafto.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa una colección o carpeta global en SQLite.
 *
 * Permite agrupar de forma unificada notas, tareas y marcadores bajo un mismo paraguas temático.
 * Mantiene un índice B-Tree sobre el nombre para acelerar búsquedas y ordenamientos.
 *
 * @property id Identificador primario universal de la colección.
 * @property name Nombre descriptivo de la colección.
 * @property colorHex Color hexadecimal representativo para el distintivo visual Nothing OS.
 * @property iconName Nombre clave del icono Material representativo de la carpeta.
 * @property createdAt Marca temporal en milisegundos de la creación.
 */
@Entity(
    tableName = "collections",
    indices = [
        Index("name")
    ]
)
data class CollectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorHex: String,
    val iconName: String,
    val createdAt: Long
)
