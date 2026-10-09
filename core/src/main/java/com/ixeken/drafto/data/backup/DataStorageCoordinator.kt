package com.ixeken.drafto.data.backup

import android.content.Context
import com.ixeken.drafto.data.local.DraftoDatabase
import com.ixeken.drafto.data.local.datastore.SettingsDataStore
import com.ixeken.drafto.domain.model.StorageMetrics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Coordinador centralizado de metricas de almacenamiento y borrado de fabrica (Factory Wipe).
 *
 * Se eligio desacoplar esta logica en un coordinador dedicado para centralizar la inspeccion
 * fisica del sistema de archivos y aislar las operaciones criticas de borrado en cascada,
 * garantizando que las tareas pesadas de E/S se ejecuten exclusivamente en [Dispatchers.IO]
 * para no degradar la tasa de refresco ni provocar bloqueos en la interfaz de usuario.
 *
 * El coordinador inspecciona tres areas de almacenamiento en disco:
 * 1. Base de datos SQLite ([DraftoDatabase]): Archivos principales (`drafto_database.db`, `drafto.db`)
 *    y sus archivos auxiliares de Write-Ahead Logging (`-wal`), memoria compartida (`-shm`) y rollback journal.
 * 2. Archivos multimedia y documentos: Contenido en [context.filesDir] (`attachments/`, `media/`, fotos),
 *    excluyendo intencionalmente directorios de configuracion del sistema como `datastore/`.
 * 3. Cache temporal: Archivos volatiles y temporales en [context.cacheDir].
 *
 * @param context Contexto de la aplicacion utilizado para resolver descriptores de archivos nativos.
 * @param draftoDatabase Instancia principal de Room para purgar todas las tablas relacionales.
 * @param settingsDataStore Gestor de preferencias para restablecer configuraciones a valores de fabrica.
 * @param ioDispatcher Despachador de corrutinas para aislar el trabajo pesado de disco.
 */
class DataStorageCoordinator(
    private val context: Context? = null,
    private val draftoDatabase: DraftoDatabase? = null,
    private val settingsDataStore: SettingsDataStore? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val filesDirProvider: () -> File? = { context?.filesDir },
    private val cacheDirProvider: () -> File? = { context?.cacheDir },
    private val databasePathProvider: (String) -> File? = { context?.getDatabasePath(it) },
    private val onClearDatabase: suspend () -> Unit = { draftoDatabase?.clearAllTables() },
    private val onResetSettings: suspend () -> Unit = { settingsDataStore?.resetToDefaults() }
) {

    /**
     * Calcula las metricas de almacenamiento consolidado ocupado por la aplicacion en el dispositivo.
     *
     * Mide de forma asincrona sobre [ioDispatcher] el tamano exacto en bytes de la base de datos Room,
     * los archivos multimedia y documentos en `filesDir`, y la cache temporal en `cacheDir`.
     *
     * @return [StorageMetrics] conteniendo la suma de bytes por categoria y el total acumulado.
     */
    suspend fun calculateStorageMetrics(): StorageMetrics = withContext(ioDispatcher) {
        val databaseBytes = calculateDatabaseBytes()
        val mediaBytes = calculateMediaBytes()
        val cacheBytes = calculateDirectorySize(cacheDirProvider())

        StorageMetrics(
            databaseBytes = databaseBytes,
            mediaBytes = mediaBytes,
            cacheBytes = cacheBytes
        )
    }

    /**
     * Elimina recursivamente todos los archivos y carpetas del directorio de cache de la aplicacion.
     *
     * Se preserva el directorio raiz de cache para evitar recreaciones costosas del descriptor
     * del sistema Android, pero se purga la totalidad de su contenido interno.
     *
     * @return [Result.success] con [Unit] si la operacion concluyo exitosamente, o [Result.failure]
     * con la excepcion capturada en caso de error de E/S.
     */
    suspend fun clearCache(): Result<Unit> = withContext(ioDispatcher) {
        try {
            val cacheDir = cacheDirProvider()
            if (cacheDir != null && cacheDir.exists() && cacheDir.isDirectory) {
                cacheDir.listFiles()?.forEach { child ->
                    child.deleteRecursively()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    /**
     * Orquesta el borrado total de fabrica (Factory Wipe) de la aplicacion.
     *
     * Ejecuta una secuencia en 4 fases de limpieza sobre [ioDispatcher]:
     * 1. Purgado de tablas relacionales de Room mediante [onClearDatabase].
     * 2. Eliminacion recursiva de subcarpetas y archivos multimedia en `filesDir` (omitiendo `datastore`).
     * 3. Vaciado completo de la cache temporal mediante [clearCache].
     * 4. Restablecimiento atomico de preferencias de usuario a sus valores predeterminados via [onResetSettings].
     *
     * Se eligio excluir el directorio `datastore` del borrado directo de archivos para evitar corromper
     * el estado en memoria de los flujos activos de DataStore, delegando su reseteo limpio a la API nativa.
     *
     * @return [Result.success] con [Unit] en caso de exito total, o [Result.failure] si alguna fase falla.
     */
    suspend fun wipeAllData(): Result<Unit> = withContext(ioDispatcher) {
        try {
            // Fase 1: Limpiar todas las tablas de Room
            onClearDatabase()

            // Fase 2: Borrar subcarpetas y archivos multimedia en filesDir
            deleteMediaFiles()

            // Fase 3: Limpiar la cache temporal
            clearCache().getOrThrow()

            // Fase 4: Restablecer preferencias a valores predeterminados de fabrica
            onResetSettings()

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    /**
     * Calcula la suma de bytes de los archivos de base de datos SQLite.
     *
     * Evalua tanto el directorio padre de bases de datos como los nombres de archivos canonicos
     * de la aplicacion (`drafto_database.db`, `drafto.db`) junto con sus diarios WAL, SHM y journal,
     * garantizando que ningun archivo sea contabilizado por duplicado.
     */
    private fun calculateDatabaseBytes(): Long {
        var totalBytes = 0L
        val seenPaths = mutableSetOf<String>()

        val knownNames = listOf(
            "drafto_database.db",
            "drafto_database.db-wal",
            "drafto_database.db-shm",
            "drafto_database.db-journal",
            "drafto.db",
            "drafto.db-wal",
            "drafto.db-shm",
            "drafto.db-journal"
        )

        // 1. Si el directorio contenedor existe, inspeccionar todos los archivos que contenga
        val primaryDbFile = try {
            databasePathProvider("drafto_database.db") ?: databasePathProvider("drafto.db")
        } catch (_: Exception) {
            null
        }

        val dbParentDir = try {
            primaryDbFile?.parentFile
        } catch (_: Exception) {
            null
        }

        if (dbParentDir != null && dbParentDir.exists() && dbParentDir.isDirectory) {
            dbParentDir.listFiles()?.forEach { file ->
                if (file.isFile && seenPaths.add(file.canonicalPath)) {
                    totalBytes += file.length()
                }
            }
        }

        // 2. Inspeccion explicita de archivos conocidos si el directorio padre no esta disponible o en tests
        for (name in knownNames) {
            val file = try {
                databasePathProvider(name)
            } catch (_: Exception) {
                null
            }
            if (file != null && file.exists() && file.isFile && seenPaths.add(file.canonicalPath)) {
                totalBytes += file.length()
            }
        }

        return totalBytes
    }

    /**
     * Calcula el tamano acumulado en bytes de los archivos multimedia y documentos en filesDir.
     *
     * Se ignora la carpeta `datastore` para no mezclar metricas de configuracion con archivos de usuario.
     */
    private fun calculateMediaBytes(): Long {
        val filesDir = try {
            filesDirProvider()
        } catch (_: Exception) {
            null
        } ?: return 0L

        return calculateDirectorySize(filesDir, excludeNames = setOf("datastore"))
    }

    /**
     * Suma recursivamente el tamano en bytes de todos los archivos regulares dentro de un directorio.
     *
     * @param dir Directorio a inspeccionar.
     * @param excludeNames Conjunto de nombres de archivos o carpetas a excluir de la medicion.
     * @return Tamano acumulado en bytes.
     */
    private fun calculateDirectorySize(dir: File?, excludeNames: Set<String> = emptySet()): Long {
        if (dir == null || !dir.exists() || !dir.isDirectory) return 0L
        var totalSize = 0L
        val children = dir.listFiles() ?: return 0L

        for (child in children) {
            if (excludeNames.contains(child.name)) {
                continue
            }
            totalSize += if (child.isDirectory) {
                calculateDirectorySize(child, excludeNames)
            } else {
                child.length()
            }
        }
        return totalSize
    }

    /**
     * Elimina recursivamente el contenido de `filesDir` preservando el directorio `datastore`.
     */
    private fun deleteMediaFiles() {
        val filesDir = try {
            filesDirProvider()
        } catch (_: Exception) {
            null
        } ?: return

        if (!filesDir.exists() || !filesDir.isDirectory) return

        filesDir.listFiles()?.forEach { child ->
            if (!child.name.equals("datastore", ignoreCase = true)) {
                child.deleteRecursively()
            }
        }
    }
}
