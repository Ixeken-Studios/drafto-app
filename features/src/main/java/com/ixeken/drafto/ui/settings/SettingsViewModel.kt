package com.ixeken.drafto.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ixeken.drafto.data.local.datastore.SettingsDataStore
import com.ixeken.drafto.domain.usecase.ExportDataUseCase
import com.ixeken.drafto.domain.usecase.ImportDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Gestor de estado para las pantallas y hojas modales de configuración.
 * Se encarga de sincronizar los cambios de UI con el almacenamiento persistente en DataStore.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val exportDataUseCase: ExportDataUseCase,
    private val importDataUseCase: ImportDataUseCase
) : ViewModel() {

    private val _internalState = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> = combine(
        _internalState,
        settingsDataStore.userPreferencesFlow
    ) { state, prefs ->
        state.copy(
            theme = prefs.theme,
            fontSizeStep = prefs.fontSizeStep,
            accentColor = prefs.accentColor,
            isNavbarBlurEnabled = prefs.isNavbarBlurEnabled,
            isGlobalBlurEnabled = prefs.isGlobalBlurEnabled,
            isReduceMotionEnabled = prefs.isReduceMotionEnabled,
            isLockAppEnabled = prefs.isLockAppEnabled,
            isCheckUpdateOnStartEnabled = prefs.isCheckUpdateOnStartEnabled,
            isClipboardAutoDetectEnabled = prefs.isClipboardAutoDetectEnabled,
            isFetchWebMetadataEnabled = prefs.isFetchWebMetadataEnabled,
            isPinsGridView = prefs.isPinsGridView,
            isNotesGridView = prefs.isNotesGridView,
            isBookmarksGridView = prefs.isBookmarksGridView,
            lastActiveTab = prefs.lastActiveTab,
            lastNotebookSection = prefs.lastNotebookSection,
            hasSeenWalkthrough = prefs.hasSeenWalkthrough,
            isPreferencesLoaded = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SettingsUiState()
    )

    /**
     * Muestra una hoja modal específica.
     */
    fun openBottomSheet(sheetType: SettingsSheetType) {
        _internalState.update { it.copy(activeBottomSheet = sheetType) }
    }

    /**
     * Oculta cualquier hoja modal activa.
     */
    fun dismissBottomSheet() {
        _internalState.update { it.copy(activeBottomSheet = SettingsSheetType.NONE) }
    }

    /**
     * Actualiza y persiste el tema visual seleccionado.
     */
    fun setTheme(theme: String) {
        viewModelScope.launch {
            settingsDataStore.setTheme(theme)
        }
    }

    /**
     * Actualiza y persiste el color de acento de la interfaz.
     */
    fun setAccentColor(accentColor: String) {
        viewModelScope.launch {
            settingsDataStore.setAccentColor(accentColor)
        }
    }

    /**
     * Actualiza y persiste el tamaño de letra en la escala discreta.
     */
    fun setFontSizeStep(step: Int) {
        viewModelScope.launch {
            settingsDataStore.setFontSizeStep(step)
        }
    }

    /**
     * Alterna la activación del efecto blur global en toda la aplicación.
     */
    fun setNavbarBlurEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setNavbarBlurEnabled(enabled)
        }
    }

    /**
     * Alterna la activación del efecto blur global en toda la aplicación.
     */
    fun setGlobalBlurEnabled(enabled: Boolean) {
        setNavbarBlurEnabled(enabled)
    }

    /**
     * Alterna la reducción de animaciones en la interfaz de usuario.
     */
    fun setReduceMotionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setReduceMotionEnabled(enabled)
        }
    }

    /**
     * Alterna la configuración de protección por biometría/PIN.
     */
    fun setLockAppEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setLockAppEnabled(enabled)
        }
    }

    /**
     * Alterna la búsqueda automática de actualizaciones al inicio.
     */
    fun setCheckUpdateOnStartEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setCheckUpdateOnStartEnabled(enabled)
        }
    }

    /**
     * Alterna la activación de la detección automática de enlaces en el portapapeles.
     */
    fun setClipboardAutoDetectEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setClipboardAutoDetectEnabled(enabled)
        }
    }

    /**
     * Alterna la descarga de metadatos web (previsualizaciones y favicons de internet).
     */
    fun setFetchWebMetadataEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setFetchWebMetadataEnabled(enabled)
        }
    }

    /**
     * Persiste la preferencia de modo de visualización para la pestaña Pins.
     */
    fun setPinsGridView(isGrid: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setPinsGridView(isGrid)
        }
    }

    /**
     * Persiste la preferencia de modo de visualización para la pestaña Notebook (Notas).
     */
    fun setNotesGridView(isGrid: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setNotesGridView(isGrid)
        }
    }

    /**
     * Persiste la preferencia de modo de visualización para la pestaña Bookmarks (Guardados).
     */
    fun setBookmarksGridView(isGrid: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setBookmarksGridView(isGrid)
        }
    }

    /**
     * Exporta el respaldo de datos completo en formato JSON.
     */
    fun exportBackup() {
        viewModelScope.launch {
            _internalState.update { it.copy(isExporting = true, error = null) }
            runCatching { exportDataUseCase() }
                .onSuccess { json ->
                    _internalState.update { it.copy(isExporting = false, exportedJson = json) }
                }
                .onFailure { exception ->
                    _internalState.update { it.copy(isExporting = false, error = exception.message) }
                }
        }
    }

    /**
     * Importa el respaldo de datos desde un JSON seleccionado por el usuario.
     */
    fun importBackup(jsonContent: String) {
        viewModelScope.launch {
            _internalState.update { it.copy(isImporting = true, importSuccess = false, error = null) }
            importDataUseCase(jsonContent)
                .onSuccess {
                    _internalState.update { it.copy(isImporting = false, importSuccess = true) }
                }
                .onFailure { exception ->
                    _internalState.update { it.copy(isImporting = false, error = exception.message) }
                }
        }
    }

    /**
     * Persiste el índice de la última pestaña principal activa (0: Colecciones, 1: Notas, 2: Marcadores, 3: Ajustes).
     */
    fun setLastActiveTab(tabIndex: Int) {
        viewModelScope.launch {
            settingsDataStore.setLastActiveTab(tabIndex)
        }
    }

    /**
     * Persiste la última subsección activa de la libreta (Notas o To-dos) para reanudar el flujo en el próximo inicio.
     */
    fun setLastNotebookSection(section: String) {
        viewModelScope.launch {
            settingsDataStore.setLastNotebookSection(section)
        }
    }

    /**
     * Persiste la confirmación de si el usuario ya vio el recorrido de bienvenida de la aplicación.
     */
    fun setHasSeenWalkthrough(hasSeen: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setHasSeenWalkthrough(hasSeen)
        }
    }

    /**
     * Restablece los estados temporales de exportación e importación.
     */
    fun clearState() {
        _internalState.update { SettingsUiState() }
    }
}
