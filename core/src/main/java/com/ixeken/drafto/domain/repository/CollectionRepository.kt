package com.ixeken.drafto.domain.repository

import com.ixeken.drafto.domain.model.DraftoCollection
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de dominio 100% puro para la gestión de colecciones globales en Drafto.
 *
 * Desacoplado de frameworks de persistencia o interfaz gráfica para garantizar
 * máxima testeabilidad e inmutabilidad arquitectónica.
 */
interface CollectionRepository {

    /**
     * Emite la lista reactiva de colecciones con sus conteos agregados (notas, to-dos y marcadores).
     */
    fun getCollections(): Flow<List<DraftoCollection>>

    /**
     * Emite la colección correspondiente al identificador provisto.
     */
    fun getCollectionById(id: String): Flow<DraftoCollection?>

    /**
     * Busca de forma síncrona una colección por su ID.
     */
    suspend fun findCollectionById(id: String): DraftoCollection?

    /**
     * Busca una colección por su nombre (sin distinguir mayúsculas/minúsculas).
     */
    suspend fun findCollectionByName(name: String): DraftoCollection?

    /**
     * Obtiene de forma directa todas las colecciones para respaldos o exportación.
     */
    suspend fun getAllCollectionsSync(): List<DraftoCollection>

    /**
     * Guarda una nueva colección o reemplaza una existente.
     */
    suspend fun saveCollection(collection: DraftoCollection)

    /**
     * Actualiza metadatos de una colección existente (nombre, color, icono).
     */
    suspend fun updateCollection(collection: DraftoCollection)

    /**
     * Elimina una colección desvinculando de forma segura sus notas, to-dos y marcadores asociados.
     */
    suspend fun deleteCollection(id: String)
}
