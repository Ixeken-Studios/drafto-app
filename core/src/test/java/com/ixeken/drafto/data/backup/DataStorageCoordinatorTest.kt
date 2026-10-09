package com.ixeken.drafto.data.backup

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Pruebas unitarias para [DataStorageCoordinator].
 *
 * Valida de forma aislada sobre directorios temporales:
 * 1. Calculo preciso de metricas de almacenamiento (Base de datos SQLite, archivos multimedia y cache).
 * 2. Exclusion intencional del directorio de preferencias `datastore` en las metricas multimedia.
 * 3. Limpieza recursiva de la cache preservando el directorio raiz.
 * 4. Secuencia orquestada de Factory Wipe (Room, media, cache, reset de preferencias).
 * 5. Manejo de fallos y re-lanzamiento estricto de [CancellationException].
 */
class DataStorageCoordinatorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var fakeFilesDir: File
    private lateinit var fakeCacheDir: File
    private lateinit var fakeDbDir: File

    private var dbCleared = false
    private var settingsReset = false

    private lateinit var coordinator: DataStorageCoordinator

    @Before
    fun setUp() {
        fakeFilesDir = tempFolder.newFolder("files")
        fakeCacheDir = tempFolder.newFolder("cache")
        fakeDbDir = tempFolder.newFolder("databases")
        dbCleared = false
        settingsReset = false

        coordinator = DataStorageCoordinator(
            ioDispatcher = Dispatchers.Unconfined,
            filesDirProvider = { fakeFilesDir },
            cacheDirProvider = { fakeCacheDir },
            databasePathProvider = { name -> File(fakeDbDir, name) },
            onClearDatabase = { dbCleared = true },
            onResetSettings = { settingsReset = true }
        )
    }

    @Test
    fun calculateStorageMetrics_calculatesCorrectSizesForDatabaseMediaAndCache() = runBlocking {
        // 1. Crear archivos de base de datos simulados
        val dbFile = File(fakeDbDir, "drafto_database.db").apply {
            writeBytes(ByteArray(1024))
        }
        val walFile = File(fakeDbDir, "drafto_database.db-wal").apply {
            writeBytes(ByteArray(512))
        }

        // 2. Crear archivos multimedia en filesDir
        val attachDir = File(fakeFilesDir, "attachments").apply { mkdirs() }
        File(attachDir, "photo.jpg").writeBytes(ByteArray(2048))
        File(fakeFilesDir, "attach_loose.png").writeBytes(ByteArray(1000))

        // Directorio datastore que debe ser ignorado por las metricas de medios
        val datastoreDir = File(fakeFilesDir, "datastore").apply { mkdirs() }
        File(datastoreDir, "user_settings.preferences_pb").writeBytes(ByteArray(300))

        // 3. Crear archivos de cache
        val cacheSubDir = File(fakeCacheDir, "thumbnails").apply { mkdirs() }
        File(cacheSubDir, "thumb.webp").writeBytes(ByteArray(4096))
        File(fakeCacheDir, "temp_data.bin").writeBytes(ByteArray(512))

        val metrics = coordinator.calculateStorageMetrics()

        assertEquals(1024L + 512L, metrics.databaseBytes)
        assertEquals(2048L + 1000L, metrics.mediaBytes)
        assertEquals(4096L + 512L, metrics.cacheBytes)
        assertEquals((1024L + 512L) + (2048L + 1000L) + (4096L + 512L), metrics.totalBytes)
    }

    @Test
    fun calculateStorageMetrics_returnsZerosWhenDirectoriesAreEmpty() = runBlocking {
        val metrics = coordinator.calculateStorageMetrics()

        assertEquals(0L, metrics.databaseBytes)
        assertEquals(0L, metrics.mediaBytes)
        assertEquals(0L, metrics.cacheBytes)
        assertEquals(0L, metrics.totalBytes)
    }

    @Test
    fun clearCache_deletesCacheContentsButPreservesRootDirectory() = runBlocking {
        val cacheSubDir = File(fakeCacheDir, "coil_disk_cache").apply { mkdirs() }
        File(cacheSubDir, "entry1.cache").writeText("cached data 1")
        File(fakeCacheDir, "temp.log").writeText("temporary log")

        assertTrue(fakeCacheDir.exists())
        assertTrue(File(fakeCacheDir, "temp.log").exists())

        val result = coordinator.clearCache()

        assertTrue(result.isSuccess)
        assertTrue(fakeCacheDir.exists())
        val remainingFiles = fakeCacheDir.listFiles()
        assertTrue(remainingFiles == null || remainingFiles.isEmpty())
    }

    @Test
    fun wipeAllData_orchestratesFullFactoryWipeSequence() = runBlocking {
        // Preparar base de datos simulada
        File(fakeDbDir, "drafto_database.db").writeBytes(ByteArray(1024))

        // Preparar archivos en filesDir
        val mediaDir = File(fakeFilesDir, "media").apply { mkdirs() }
        val docFile = File(mediaDir, "sample.pdf").apply { writeText("document content") }
        val looseAttach = File(fakeFilesDir, "attach_123.jpg").apply { writeText("image content") }

        // Carpeta datastore que debe preservarse intacta para el reset oficial
        val datastoreDir = File(fakeFilesDir, "datastore").apply { mkdirs() }
        val prefsFile = File(datastoreDir, "prefs.pb").apply { writeText("settings") }

        // Preparar cache
        val cacheFile = File(fakeCacheDir, "temp.bin").apply { writeText("temp") }

        val result = coordinator.wipeAllData()

        assertTrue(result.isSuccess)
        assertTrue("La base de datos relacional debio purgarse", dbCleared)
        assertTrue("Las preferencias debieron restablecerse a defaults", settingsReset)

        // Verificar que los medios fueron eliminados
        assertFalse("Subcarpeta media debio ser eliminada", mediaDir.exists())
        assertFalse("Archivo sample.pdf debio ser eliminado", docFile.exists())
        assertFalse("Archivo adjunto suelto debio ser eliminado", looseAttach.exists())

        // Verificar que datastore fue preservado a nivel de sistema de archivos
        assertTrue("Directorio datastore debe ser preservado", datastoreDir.exists())
        assertTrue("Archivo de preferencias debe persistir para DataStore", prefsFile.exists())

        // Verificar que la cache fue limpiada
        assertFalse("Archivo en cache debio ser eliminado", cacheFile.exists())
        assertTrue("Directorio cache debe seguir existiendo", fakeCacheDir.exists())
    }

    @Test
    fun wipeAllData_handlesFailuresAndPropagatesError() = runBlocking {
        val failingCoordinator = DataStorageCoordinator(
            ioDispatcher = Dispatchers.Unconfined,
            filesDirProvider = { fakeFilesDir },
            cacheDirProvider = { fakeCacheDir },
            databasePathProvider = { name -> File(fakeDbDir, name) },
            onClearDatabase = { throw IllegalStateException("Database locked") },
            onResetSettings = { }
        )

        val result = failingCoordinator.wipeAllData()

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalStateException)
        assertEquals("Database locked", exception?.message)
    }

    @Test
    fun wipeAllData_rethrowsCancellationException() = runBlocking {
        val cancellingCoordinator = DataStorageCoordinator(
            ioDispatcher = Dispatchers.Unconfined,
            filesDirProvider = { fakeFilesDir },
            cacheDirProvider = { fakeCacheDir },
            databasePathProvider = { name -> File(fakeDbDir, name) },
            onClearDatabase = { throw CancellationException("Coroutine cancelled") },
            onResetSettings = { }
        )

        var cancellationThrown = false
        try {
            cancellingCoordinator.wipeAllData()
        } catch (e: CancellationException) {
            cancellationThrown = true
            assertEquals("Coroutine cancelled", e.message)
        }

        assertTrue("CancellationException debe ser re-lanzada", cancellationThrown)
    }
}
