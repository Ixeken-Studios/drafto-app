package com.ixeken.drafto.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ixeken.drafto.core.R
import com.ixeken.drafto.data.backup.DataStorageCoordinator
import com.ixeken.drafto.data.backup.MasterBackupManager
import com.ixeken.drafto.domain.model.MasterBackupStats
import com.ixeken.drafto.domain.model.MasterRestoreMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel reactivo para la administración integral de almacenamiento y respaldo maestro en Drafto.
 *
 * Centraliza la orquestación de métricas de disco mediante [DataStorageCoordinator],
 * la exportación y restauración segura en streaming mediante [MasterBackupManager],
 * y el restablecimiento total a valores de fábrica (Factory Wipe).
 *
 * Todas las operaciones intensivas de I/O de disco se despachan estrictamente en [Dispatchers.IO]
 * para no degradar los 120 FPS del hilo de interfaz principal.
 *
 * @param dataStorageCoordinator Coordinador para cálculo de métricas, purga de caché y factory wipe.
 * @param masterBackupManager Administrador central de compresión y descompresión del paquete maestro (.zip).
 * @param context Contexto de la aplicación para resolver flujos nativos de ContentResolver con SAF.
 */
@HiltViewModel
class DataStorageViewModel @Inject constructor(
    private val dataStorageCoordinator: DataStorageCoordinator,
    private val masterBackupManager: MasterBackupManager,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataStorageUiState())
    val uiState: StateFlow<DataStorageUiState> = _uiState.asStateFlow()

    init {
        loadMetrics()
    }

    /**
     * Calcula de forma asíncrona el almacenamiento ocupado por la base de datos, multimedia y caché.
     */
    fun loadMetrics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMetrics = true) }
            val metrics = dataStorageCoordinator.calculateStorageMetrics()
            _uiState.update { it.copy(storageMetrics = metrics, isLoadingMetrics = false) }
        }
    }

    /**
     * Limpia la memoria caché temporal de la aplicación en disco y recarga las métricas.
     *
     * @param onSuccess Notificación opcional ejecutada al vaciar la caché.
     * @param onError Notificación opcional invocada si ocurre una excepción de E/S.
     */
    fun clearCache(
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = dataStorageCoordinator.clearCache()
            result.fold(
                onSuccess = {
                    loadMetrics()
                    onSuccess()
                },
                onFailure = { error ->
                    onError(error)
                }
            )
        }
    }


    /**
     * Registra la selección de un URI de respaldo maestro para presentar la hoja modal de modo de restauración.
     *
     * @param uri URI del archivo .zip seleccionado mediante el selector nativo SAF.
     */
    fun onRestoreUriSelected(uri: Uri) {
        _uiState.update { it.copy(selectedRestoreUri = uri, showRestoreModeSheet = true) }
    }

    /**
     * Oculta la hoja modal de selección de modo de restauración y limpia el URI temporal.
     */
    fun dismissRestoreModeSheet() {
        _uiState.update { it.copy(selectedRestoreUri = null, showRestoreModeSheet = false) }
    }

    /**
     * Exporta el Respaldo Maestro Universal (.zip) al destino SAF seleccionado.
     *
     * @param uri URI de destino SAF donde se escribirá el archivo comprimido.
     * @param context Contexto opcional para resolver ContentResolver (por defecto el inyectado).
     * @param onSuccess Callback con las estadísticas finales de los elementos exportados.
     * @param onError Callback invocado ante fallas de compresión o permisos de archivo.
     */
    fun exportMasterBackup(
        uri: Uri,
        context: Context = this.context,
        onSuccess: (MasterBackupStats) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            val initialStatus = context.getString(R.string.dialog_exporting_master_backup)
            _uiState.update {
                it.copy(
                    progressState = MasterBackupProgressState(
                        isActive = true,
                        isExport = true,
                        progress = 0.05f,
                        statusText = initialStatus
                    )
                )
            }

            try {
                val outputStream = withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)
                } ?: throw IllegalStateException("No se pudo abrir el descriptor de salida para el respaldo.")

                val result = masterBackupManager.exportMasterBackup(
                    outputStream = outputStream,
                    onProgress = { progress, statusMessage ->
                        _uiState.update {
                            it.copy(
                                progressState = MasterBackupProgressState(
                                    isActive = true,
                                    isExport = true,
                                    progress = progress,
                                    statusText = statusMessage
                                )
                            )
                        }
                    }
                )

                delay(300)
                _uiState.update { it.copy(progressState = null) }
                loadMetrics()

                result.fold(
                    onSuccess = { stats -> onSuccess(stats) },
                    onFailure = { error -> onError(error) }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.update { it.copy(progressState = null) }
                onError(e)
            }
        }
    }

    /**
     * Restaura el Respaldo Maestro Universal (.zip) aplicando la estrategia seleccionada.
     *
     * @param uri URI del archivo .zip que contiene el paquete maestro.
     * @param mode Estrategia de restauración (Merge o Clean Restore).
     * @param context Contexto opcional para resolver ContentResolver (por defecto el inyectado).
     * @param onSuccess Callback con las estadísticas de entidades restauradas.
     * @param onError Callback invocado ante fallas de lectura o descompresión.
     */
    fun restoreMasterBackup(
        uri: Uri,
        mode: MasterRestoreMode,
        context: Context = this.context,
        onSuccess: (MasterBackupStats) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        dismissRestoreModeSheet()
        viewModelScope.launch {
            val initialStatus = context.getString(R.string.dialog_importing_master_backup)
            _uiState.update {
                it.copy(
                    progressState = MasterBackupProgressState(
                        isActive = true,
                        isExport = false,
                        progress = 0.05f,
                        statusText = initialStatus
                    )
                )
            }

            try {
                val inputStream = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)
                } ?: throw IllegalStateException("No se pudo abrir el descriptor de entrada para restaurar.")

                val result = masterBackupManager.restoreMasterBackup(
                    inputStream = inputStream,
                    mode = mode,
                    onProgress = { progress, statusMessage ->
                        _uiState.update {
                            it.copy(
                                progressState = MasterBackupProgressState(
                                    isActive = true,
                                    isExport = false,
                                    progress = progress,
                                    statusText = statusMessage
                                )
                            )
                        }
                    }
                )

                delay(300)
                _uiState.update { it.copy(progressState = null) }
                loadMetrics()

                result.fold(
                    onSuccess = { stats -> onSuccess(stats) },
                    onFailure = { error -> onError(error) }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.update { it.copy(progressState = null) }
                onError(e)
            }
        }
    }

    /**
     * Presenta el diálogo modal de confirmación destructiva para el borrado de fábrica.
     */
    fun showWipeConfirmDialog() {
        _uiState.update { it.copy(showWipeConfirmDialog = true) }
    }

    /**
     * Oculta el diálogo modal de confirmación destructiva de borrado de fábrica.
     */
    fun dismissWipeConfirmDialog() {
        _uiState.update { it.copy(showWipeConfirmDialog = false) }
    }

    /**
     * Ejecuta el restablecimiento integral de fábrica (Factory Wipe).
     *
     * Purga en cascada la base de datos Room, archivos multimedia, caché y preferencias DataStore.
     *
     * @param onSuccess Callback invocado al completar exitosamente la limpieza.
     * @param onError Callback invocado en caso de error durante alguna de las fases de limpieza.
     */
    fun wipeAllData(
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        dismissWipeConfirmDialog()
        viewModelScope.launch {
            _uiState.update { it.copy(isWipingData = true) }
            val result = dataStorageCoordinator.wipeAllData()
            _uiState.update { it.copy(isWipingData = false) }
            loadMetrics()

            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { error -> onError(error) }
            )
        }
    }
}
