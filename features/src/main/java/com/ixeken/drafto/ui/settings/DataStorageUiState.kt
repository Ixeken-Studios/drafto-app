package com.ixeken.drafto.ui.settings

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.ixeken.drafto.domain.model.StorageMetrics

/**
 * Estado reactivo para el progreso de operaciones maestras de compresión y descompresión (.zip).
 *
 * @property isActive Indica si la operación de exportación o restauración está ejecutándose.
 * @property isExport True si la operación activa es de exportación; false si es de restauración.
 * @property progress Fracción de avance cuantitativo normalizado entre 0.0f y 1.0f.
 * @property statusText Descripción contextual del paso de procesamiento actual en ejecución.
 */
@Immutable
data class MasterBackupProgressState(
    val isActive: Boolean = false,
    val isExport: Boolean = true,
    val progress: Float = 0f,
    val statusText: String = ""
)

/**
 * Estado inmutable de la interfaz de usuario para la pantalla de Datos y Almacenamiento ([DataStorageScreen]).
 *
 * Sigue los principios de Unidirectional Data Flow (UDF) centralizando métricas de disco,
 * configuraciones de empaquetado de medios, estados de progreso y visibilidad de diálogos de confirmación.
 *
 * @property storageMetrics Métricas de ocupación física en disco (base de datos, medios y caché).
 * @property isLoadingMetrics Indica si el coordinador está calculando los tamaños en disco en segundo plano.
 * @property progressState Estado dinámico para el diálogo de progreso de exportación o restauración continua.
 * @property isWipingData Refleja si se está ejecutando la purga integral de fábrica (Factory Wipe).
 * @property selectedRestoreUri URI del archivo .zip seleccionado mediante SAF para restaurar.
 * @property showRestoreModeSheet Controla la visibilidad del modal para elegir entre Merge y Clean Restore.
 * @property showWipeConfirmDialog Controla la visibilidad del diálogo de confirmación con cuenta regresiva.
 */
@Immutable
data class DataStorageUiState(
    val storageMetrics: StorageMetrics = StorageMetrics(),
    val isLoadingMetrics: Boolean = false,
    val progressState: MasterBackupProgressState? = null,
    val isWipingData: Boolean = false,
    val selectedRestoreUri: Uri? = null,
    val showRestoreModeSheet: Boolean = false,
    val showWipeConfirmDialog: Boolean = false
)
