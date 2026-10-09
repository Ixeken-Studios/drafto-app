package com.ixeken.drafto.data.backup

import com.ixeken.drafto.domain.model.Bookmark
import com.ixeken.drafto.domain.model.BookmarkBackupPayload
import com.ixeken.drafto.domain.model.BookmarkCollection
import com.ixeken.drafto.domain.model.BookmarkCollectionRelationPayload
import com.ixeken.drafto.domain.model.PlatformFilter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkBackupManagerTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Test
    fun jsonSerialization_encodesAndDecodesPayloadCorrectly() {
        val payload = BookmarkBackupPayload(
            version = 1,
            exportedAt = 123456789L,
            collections = listOf(
                BookmarkCollection(
                    id = "col1",
                    name = "Tech",
                    colorHex = "#00E5FF",
                    iconName = "Code",
                    bookmarkCount = 1,
                    createdAt = 123456L
                )
            ),
            bookmarks = listOf(
                Bookmark(
                    id = "bm1",
                    url = "https://github.com",
                    title = "GitHub",
                    description = "Where the world builds software",
                    imageUrl = null,
                    domain = "github.com",
                    faviconUrl = null,
                    createdAt = 123456L,
                    isPinned = true,
                    platform = PlatformFilter.GITHUB
                )
            ),
            relations = listOf(
                BookmarkCollectionRelationPayload("bm1", "col1")
            )
        )

        val encoded = json.encodeToString(payload)
        val decoded = json.decodeFromString<BookmarkBackupPayload>(encoded)

        assertEquals(1, decoded.version)
        assertEquals(1, decoded.collections.size)
        assertEquals("Tech", decoded.collections[0].name)
        assertEquals(1, decoded.bookmarks.size)
        assertEquals("GitHub", decoded.bookmarks[0].title)
        assertEquals(PlatformFilter.GITHUB, decoded.bookmarks[0].platform)
        assertEquals(1, decoded.relations.size)
    }

    @Test
    fun jsonSerialization_handlesUtf8BomCorrectly() {
        val payload = BookmarkBackupPayload(
            version = 1,
            exportedAt = 123456789L,
            collections = emptyList(),
            bookmarks = listOf(
                Bookmark(
                    id = "bm1",
                    url = "https://example.com",
                    domain = "example.com",
                    createdAt = 123L
                )
            ),
            relations = emptyList()
        )

        val encoded = json.encodeToString(payload)
        val withBom = "\uFEFF$encoded"

        val trimmed = withBom.trim().removePrefix("\uFEFF")
        val isJson = trimmed.startsWith("{") && trimmed.contains("\"version\"")
        assertTrue(isJson)

        val decoded = json.decodeFromString<BookmarkBackupPayload>(trimmed)
        assertEquals(1, decoded.bookmarks.size)
    }
}
