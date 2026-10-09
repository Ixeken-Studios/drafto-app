package com.ixeken.drafto.data.mapper

import com.ixeken.drafto.data.local.entity.BookmarkEntity
import com.ixeken.drafto.data.local.entity.CollectionEntity
import com.ixeken.drafto.domain.model.Bookmark
import com.ixeken.drafto.domain.model.DraftoCollection
import com.ixeken.drafto.domain.model.PlatformFilter

/**
 * Funciones de mapeo bidireccional entre entidades Room de SQLite y modelos puros de dominio.
 */

/**
 * Convierte una [BookmarkEntity] persistida a su correspondiente modelo inmutable de dominio [Bookmark].
 */
fun BookmarkEntity.toDomain(): Bookmark {
    val platformFilter = runCatching {
        PlatformFilter.valueOf(platform)
    }.getOrDefault(PlatformFilter.GENERIC)

    return Bookmark(
        id = id,
        url = url,
        title = title,
        description = description,
        imageUrl = imageUrl,
        domain = domain,
        faviconUrl = faviconUrl,
        createdAt = createdAt,
        isPinned = isPinned,
        platform = platformFilter,
        isPreviewVisible = isPreviewVisible,
        collectionId = collectionId
    )
}

/**
 * Convierte un modelo de dominio [Bookmark] a su entidad Room [BookmarkEntity] para persistencia.
 */
fun Bookmark.toEntity(): BookmarkEntity {
    return BookmarkEntity(
        id = id,
        url = url,
        title = title,
        description = description,
        imageUrl = imageUrl,
        domain = domain,
        faviconUrl = faviconUrl,
        createdAt = createdAt,
        isPinned = isPinned,
        platform = platform.name,
        isPreviewVisible = isPreviewVisible,
        collectionId = collectionId
    )
}

/**
 * Convierte una [CollectionEntity] a su modelo de dominio [DraftoCollection] incorporando el conteo de marcadores.
 */
fun CollectionEntity.toDomain(bookmarkCount: Int = 0): DraftoCollection {
    return DraftoCollection(
        id = id,
        name = name,
        colorHex = colorHex,
        iconName = iconName,
        bookmarkCount = bookmarkCount,
        createdAt = createdAt
    )
}

/**
 * Convierte un modelo de dominio [DraftoCollection] a su entidad Room [CollectionEntity] para persistencia.
 */
fun DraftoCollection.toEntity(): CollectionEntity {
    return CollectionEntity(
        id = id,
        name = name,
        colorHex = colorHex,
        iconName = iconName,
        createdAt = createdAt
    )
}
