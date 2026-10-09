package com.ixeken.drafto.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MasterBackupManifestTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Test
    fun masterBackupManifest_jsonSerialization_encodesAndDecodesCorrectly() {
        val stats = MasterBackupStats(
            notesCount = 42,
            todosCount = 15,
            bookmarksCount = 35,
            collectionsCount = 4
        )
        val manifest = MasterBackupManifest(
            manifestVersion = 1,
            appVersion = "2.0.0",
            exportedAt = 1756987654321L,
            hasMedia = true,
            hasSettings = true,
            stats = stats
        )

        val encodedJson = json.encodeToString(manifest)
        val decoded = json.decodeFromString<MasterBackupManifest>(encodedJson)

        assertEquals(1, decoded.manifestVersion)
        assertEquals("2.0.0", decoded.appVersion)
        assertEquals(1756987654321L, decoded.exportedAt)
        assertTrue(decoded.hasMedia)
        assertTrue(decoded.hasSettings)
        assertEquals(42, decoded.stats.notesCount)
        assertEquals(15, decoded.stats.todosCount)
        assertEquals(35, decoded.stats.bookmarksCount)
        assertEquals(4, decoded.stats.collectionsCount)
    }

    @Test
    fun masterBackupManifest_defaultValues_areConsistent() {
        val manifest = MasterBackupManifest()

        assertEquals(1, manifest.manifestVersion)
        assertEquals("1.0.0", manifest.appVersion)
        assertTrue(manifest.hasMedia)
        assertTrue(manifest.hasSettings)
        assertEquals(0, manifest.stats.notesCount)
        assertEquals(0, manifest.stats.todosCount)
        assertEquals(0, manifest.stats.bookmarksCount)
        assertEquals(0, manifest.stats.collectionsCount)
    }

    @Test
    fun masterRestoreMode_containsExpectedValues() {
        val modes = MasterRestoreMode.values()
        assertEquals(2, modes.size)
        assertEquals(MasterRestoreMode.MERGE, MasterRestoreMode.valueOf("MERGE"))
        assertEquals(MasterRestoreMode.CLEAN_RESTORE, MasterRestoreMode.valueOf("CLEAN_RESTORE"))
    }

    @Test
    fun storageMetrics_calculatesTotalBytesCorrectly() {
        val metrics = StorageMetrics(
            databaseBytes = 2_000_000L,
            mediaBytes = 5_000_000L,
            cacheBytes = 3_000_000L
        )

        assertEquals(10_000_000L, metrics.totalBytes)
    }

    @Test
    fun storageMetrics_defaultValuesAreZero() {
        val metrics = StorageMetrics()

        assertEquals(0L, metrics.databaseBytes)
        assertEquals(0L, metrics.mediaBytes)
        assertEquals(0L, metrics.cacheBytes)
        assertEquals(0L, metrics.totalBytes)
    }

    @Test
    fun storageMetrics_formatBytes_formatsUnitsAccurately() {
        assertEquals("0 B", StorageMetrics.formatBytes(0L))
        assertEquals("0 B", StorageMetrics.formatBytes(-500L))
        assertEquals("500.0 B", StorageMetrics.formatBytes(500L))
        assertEquals("1.0 KB", StorageMetrics.formatBytes(1024L))
        assertEquals("1.5 KB", StorageMetrics.formatBytes(1536L))
        assertEquals("1.0 MB", StorageMetrics.formatBytes(1024L * 1024L))
        assertEquals("2.5 MB", StorageMetrics.formatBytes((2.5 * 1024L * 1024L).toLong()))
        assertEquals("1.0 GB", StorageMetrics.formatBytes(1024L * 1024L * 1024L))
        assertEquals("3.0 GB", StorageMetrics.formatBytes(3L * 1024L * 1024L * 1024L))
    }
}
