package com.ixeken.drafto.domain.model

import kotlinx.serialization.Serializable

/**
 * Contadores cuantitativos de los elementos exportados en el respaldo maestro.
 *
 * Permite al usuario y al sistema validar rápidamente el volumen de datos empaquetados
 * (notas, tareas, marcadores y colecciones) antes de proceder con una restauración
 * parcial o total, facilitando auditorías sin desempaquetar la base de datos completa.
 *
 * @property notesCount Total de notas Markdown exportadas.
 * @property todosCount Total de tareas To-do exportadas.
 * @property bookmarksCount Total de enlaces o marcadores web guardados.
 * @property collectionsCount Total de colecciones temáticas registradas.
 */
@Serializable
data class MasterBackupStats(
    val notesCount: Int = 0,
    val todosCount: Int = 0,
    val bookmarksCount: Int = 0,
    val collectionsCount: Int = 0
)

/**
 * Manifiesto raíz (`manifest.json`) del paquete de respaldo maestro (.zip) de Drafto.
 *
 * Centraliza la versión de compatibilidad del esquema, la fecha de exportación,
 * las banderas que indican la inclusión de activos binarios (medios y ajustes) y el resumen
 * cuantitativo de entidades. Garantiza que la capa de datos pueda verificar la compatibilidad
 * del archivo antes de ejecutar operaciones de descompresión o reemplazo de base de datos.
 *
 * @property manifestVersion Versión estructural del manifiesto para soportar migraciones hacia adelante.
 * @property appVersion Versión semántica de Drafto con la que se empaquetó el respaldo.
 * @property exportedAt Marca de tiempo UNIX en milisegundos en el momento de la exportación.
 * @property hasMedia Indica si el archivo .zip contiene la carpeta `media/` con fotos, audios o adjuntos.
 * @property hasSettings Indica si el archivo .zip incluye `settings.json` con las preferencias del usuario.
 * @property stats Desglose estadístico del contenido incluido en el respaldo.
 */
@Serializable
data class MasterBackupManifest(
    val manifestVersion: Int = 1,
    val appVersion: String = "1.0.0",
    val exportedAt: Long = System.currentTimeMillis(),
    val hasMedia: Boolean = true,
    val hasSettings: Boolean = true,
    val stats: MasterBackupStats = MasterBackupStats()
)

/**
 * Estrategia de restauración seleccionada por el usuario al procesar un respaldo maestro.
 *
 * - [MERGE]: Fusiona las entidades del respaldo con las existentes resolviendo conflictos por identificador único.
 * - [CLEAN_RESTORE]: Limpia las tablas y directorios locales antes de restaurar para replicar exactamente el estado del respaldo.
 */
enum class MasterRestoreMode {
    MERGE,
    CLEAN_RESTORE
}

/**
 * Métricas de ocupación de almacenamiento calculadas para los distintos componentes del sistema local.
 *
 * Facilita a la interfaz mostrar el impacto de la base de datos, los archivos multimedia adjuntos
 * y la memoria caché en disco, permitiendo al usuario tomar decisiones informadas antes de respaldar
 * o ejecutar un restablecimiento de fábrica (factory wipe).
 *
 * @property databaseBytes Espacio ocupado por el archivo de base de datos SQLite y sus diarios/WAL.
 * @property mediaBytes Espacio ocupado por imágenes, grabaciones y documentos en el directorio interno de medios.
 * @property cacheBytes Espacio ocupado por la caché temporal de miniaturas, metadatos y descargas.
 */
data class StorageMetrics(
    val databaseBytes: Long = 0L,
    val mediaBytes: Long = 0L,
    val cacheBytes: Long = 0L
) {
    /**
     * Suma acumulada del almacenamiento ocupado por Drafto en disco.
     */
    val totalBytes: Long get() = databaseBytes + mediaBytes + cacheBytes

    companion object {
        /**
         * Transforma una cantidad de bytes en una representación textual legible (B, KB, MB, GB).
         *
         * Utiliza base binaria (1024) y un formato decimal fijo con punto estándar para garantizar
         * consistencia visual entre diferentes configuraciones regionales sin ambigüedades.
         *
         * @param bytes Cantidad de bytes a convertir. Valores <= 0 retornan "0 B".
         * @return Cadena formateada (ej. "1.5 MB", "500.0 KB", "0 B").
         */
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0L) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
            val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
            return String.format(java.util.Locale.US, "%.1f %s", value, units[digitGroups])
        }
    }
}
