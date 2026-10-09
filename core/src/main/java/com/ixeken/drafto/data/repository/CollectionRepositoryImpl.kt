package com.ixeken.drafto.data.repository

import com.ixeken.drafto.data.local.dao.CollectionDao
import com.ixeken.drafto.data.local.dao.CollectionWithCounts
import com.ixeken.drafto.data.local.entity.CollectionEntity
import com.ixeken.drafto.domain.model.DraftoCollection
import com.ixeken.drafto.domain.repository.CollectionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionRepositoryImpl @Inject constructor(
    private val collectionDao: CollectionDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : CollectionRepository {

    override fun getCollections(): Flow<List<DraftoCollection>> {
        return collectionDao.getAllCollectionsWithCounts()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override fun getCollectionById(id: String): Flow<DraftoCollection?> {
        return collectionDao.getCollectionById(id)
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)
    }

    override suspend fun findCollectionById(id: String): DraftoCollection? = withContext(ioDispatcher) {
        collectionDao.findCollectionById(id)?.toDomain()
    }

    override suspend fun findCollectionByName(name: String): DraftoCollection? = withContext(ioDispatcher) {
        collectionDao.findCollectionByName(name.trim())?.toDomain()
    }

    override suspend fun getAllCollectionsSync(): List<DraftoCollection> = withContext(ioDispatcher) {
        collectionDao.getAllCollections().map { it.toDomain() }
    }

    override suspend fun saveCollection(collection: DraftoCollection) = withContext(ioDispatcher) {
        collectionDao.insertCollection(collection.toEntity())
    }

    override suspend fun updateCollection(collection: DraftoCollection) = withContext(ioDispatcher) {
        collectionDao.updateCollection(collection.toEntity())
    }

    override suspend fun deleteCollection(id: String) = withContext(ioDispatcher) {
        collectionDao.deleteCollectionSafely(id)
    }
}

private fun CollectionWithCounts.toDomain(): DraftoCollection = DraftoCollection(
    id = id,
    name = name,
    colorHex = colorHex,
    iconName = iconName,
    noteCount = noteCount,
    todoCount = todoCount,
    bookmarkCount = bookmarkCount,
    createdAt = createdAt
)

private fun CollectionEntity.toDomain(noteCount: Int = 0, todoCount: Int = 0, bookmarkCount: Int = 0): DraftoCollection = DraftoCollection(
    id = id,
    name = name,
    colorHex = colorHex,
    iconName = iconName,
    noteCount = noteCount,
    todoCount = todoCount,
    bookmarkCount = bookmarkCount,
    createdAt = createdAt
)

private fun DraftoCollection.toEntity(): CollectionEntity = CollectionEntity(
    id = id,
    name = name,
    colorHex = colorHex,
    iconName = iconName,
    createdAt = createdAt
)
