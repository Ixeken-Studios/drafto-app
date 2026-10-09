package com.ixeken.drafto.data.local.datastore

import android.app.UiModeManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings_preferences")

/**
 * Modelo inmutable para exponer el conjunto consolidado de preferencias visuales y de seguridad.
 */
data class UserPreferences(
    val theme: String = "Dark",
    val fontSizeStep: Int = 2,
    val accentColor: String = "monochrome",
    val isNavbarBlurEnabled: Boolean = false,
    val isGlobalBlurEnabled: Boolean = isNavbarBlurEnabled,
    val isLockAppEnabled: Boolean = false,
    val isCheckUpdateOnStartEnabled: Boolean = false,
    val isClipboardAutoDetectEnabled: Boolean = false,
    val isVideoMuted: Boolean = false,
    val isPinsGridView: Boolean = false,
    val isNotesGridView: Boolean = false,
    val isBookmarksGridView: Boolean = false,
    val isFetchWebMetadataEnabled: Boolean = true,
    val lastActiveTab: Int = 0,
    val lastNotebookSection: String = "NOTES",
    val isReduceMotionEnabled: Boolean = false,
    val hasSeenWalkthrough: Boolean = false
)

/**
 * Gestor centralizado de preferencias en DataStore.
 * Se elige DataStore en lugar de SharedPreferences para asegurar lecturas reactivas
 * asíncronas libres de bloqueos en el hilo principal de la interfaz.
 */
@Singleton
class SettingsDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME = stringPreferencesKey("settings_theme")
        val FONT_SIZE_STEP = intPreferencesKey("settings_font_size_step")
        val ACCENT_COLOR = stringPreferencesKey("settings_accent_color")
        val NAVBAR_BLUR = booleanPreferencesKey("settings_navbar_blur_enabled")
        val REDUCE_MOTION = booleanPreferencesKey("settings_reduce_motion")
        val LOCK_APP = booleanPreferencesKey("settings_lock_app")
        val CHECK_UPDATE_ON_START = booleanPreferencesKey("settings_check_update_on_start")
        val CLIPBOARD_AUTO_DETECT = booleanPreferencesKey("settings_clipboard_auto_detect_enabled")
        val FETCH_WEB_METADATA = booleanPreferencesKey("settings_fetch_web_metadata")
        val IS_VIDEO_MUTED = booleanPreferencesKey("settings_is_video_muted")
        val VIEW_MODE_PINS_GRID = booleanPreferencesKey("view_mode_pins_grid")
        val VIEW_MODE_NOTES_GRID = booleanPreferencesKey("view_mode_notes_grid")
        val VIEW_MODE_BOOKMARKS_GRID = booleanPreferencesKey("view_mode_bookmarks_grid")
        val LAST_ACTIVE_TAB = intPreferencesKey("settings_last_active_tab")
        val LAST_NOTEBOOK_SECTION = stringPreferencesKey("settings_last_notebook_section")
        val HAS_SEEN_WALKTHROUGH = booleanPreferencesKey("settings_has_seen_walkthrough")
    }

    /**
     * Copia volátil en memoria de la última subsección activa de Notebook para consumo síncrono al instanciar ViewModels.
     */
    @Volatile
    var cachedLastNotebookSection: String = "NOTES"
        private set

    /**
     * Copia volátil en memoria del estado de bloqueo biométrico para acceso síncrono inmediato al iniciar la app.
     */
    @Volatile
    var cachedIsLockAppEnabled: Boolean = false
        private set

    /**
     * Copia volátil en memoria del tema visual activo para acceso síncrono inmediato al iniciar la app.
     */
    @Volatile
    var cachedTheme: String = "Dark"
        private set

    /**
     * Flujo reactivo que emite el estado actualizado de las preferencias del usuario.
     */
    val userPreferencesFlow: Flow<UserPreferences> = context.settingsDataStore.data.map { prefs ->
        val blurEnabled = prefs[Keys.NAVBAR_BLUR] ?: false
        val notebookSection = prefs[Keys.LAST_NOTEBOOK_SECTION] ?: "NOTES"
        val lockApp = prefs[Keys.LOCK_APP] ?: false
        val theme = prefs[Keys.THEME] ?: "Dark"
        cachedLastNotebookSection = notebookSection
        cachedIsLockAppEnabled = lockApp
        cachedTheme = theme

        // Persiste copia síncrona para el arranque inmediato del Splash Screen nativo
        context.getSharedPreferences("drafto_sync_theme", Context.MODE_PRIVATE)
            .edit()
            .putString("theme", theme)
            .apply()

        // Sincroniza el modo nocturno del sistema operativo para que el WindowManager use el tema correcto
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        uiModeManager?.let {
            val mode = when (theme.lowercase()) {
                "dark", "amoled" -> UiModeManager.MODE_NIGHT_YES
                "light", "kraft" -> UiModeManager.MODE_NIGHT_NO
                else -> UiModeManager.MODE_NIGHT_YES
            }
            if (it.nightMode != mode) {
                it.setApplicationNightMode(mode)
            }
        }

        UserPreferences(
            theme = theme,
            fontSizeStep = prefs[Keys.FONT_SIZE_STEP] ?: 2,
            accentColor = prefs[Keys.ACCENT_COLOR] ?: "monochrome",
            isNavbarBlurEnabled = blurEnabled,
            isGlobalBlurEnabled = blurEnabled,
            isLockAppEnabled = lockApp,
            isCheckUpdateOnStartEnabled = prefs[Keys.CHECK_UPDATE_ON_START] ?: false,
            isClipboardAutoDetectEnabled = prefs[Keys.CLIPBOARD_AUTO_DETECT] ?: false,
            isVideoMuted = prefs[Keys.IS_VIDEO_MUTED] ?: false,
            isPinsGridView = prefs[Keys.VIEW_MODE_PINS_GRID] ?: false,
            isNotesGridView = prefs[Keys.VIEW_MODE_NOTES_GRID] ?: false,
            isBookmarksGridView = prefs[Keys.VIEW_MODE_BOOKMARKS_GRID] ?: false,
            isFetchWebMetadataEnabled = prefs[Keys.FETCH_WEB_METADATA] ?: true,
            lastActiveTab = prefs[Keys.LAST_ACTIVE_TAB] ?: 0,
            lastNotebookSection = notebookSection,
            isReduceMotionEnabled = prefs[Keys.REDUCE_MOTION] ?: false,
            hasSeenWalkthrough = prefs[Keys.HAS_SEEN_WALKTHROUGH] ?: false
        )
    }

    val fetchWebMetadataFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.FETCH_WEB_METADATA] ?: true
    }

    val clipboardAutoDetectFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.CLIPBOARD_AUTO_DETECT] ?: false
    }

    val pinsGridViewFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.VIEW_MODE_PINS_GRID] ?: false
    }

    val notesGridViewFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.VIEW_MODE_NOTES_GRID] ?: false
    }

    val bookmarksGridViewFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.VIEW_MODE_BOOKMARKS_GRID] ?: false
    }

    val lastNotebookSectionFlow: Flow<String> = context.settingsDataStore.data.map { prefs ->
        val section = prefs[Keys.LAST_NOTEBOOK_SECTION] ?: "NOTES"
        cachedLastNotebookSection = section
        section
    }

    val isVideoMutedFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.IS_VIDEO_MUTED] ?: false
    }

    /**
     * Persiste la preferencia de mute para la reproducción de videos.
     */
    suspend fun setVideoMuted(muted: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.IS_VIDEO_MUTED] = muted
        }
    }

    /**
     * Persiste la selección del tema visual, actualiza la copia síncrona en memoria y SharedPreferences,
     * y sincroniza el modo de noche del sistema operativo para el splash screen nativo.
     */
    suspend fun setTheme(theme: String) {
        cachedTheme = theme
        context.getSharedPreferences("drafto_sync_theme", Context.MODE_PRIVATE)
            .edit()
            .putString("theme", theme)
            .apply()

        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        uiModeManager?.let {
            val mode = when (theme.lowercase()) {
                "dark", "amoled" -> UiModeManager.MODE_NIGHT_YES
                "light", "kraft" -> UiModeManager.MODE_NIGHT_NO
                else -> UiModeManager.MODE_NIGHT_YES
            }
            if (it.nightMode != mode) {
                it.setApplicationNightMode(mode)
            }
        }

        context.settingsDataStore.edit { prefs ->
            prefs[Keys.THEME] = theme
        }
    }

    /**
     * Persiste la selección del color de acento de la interfaz.
     */
    suspend fun setAccentColor(accentColor: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.ACCENT_COLOR] = accentColor
        }
    }

    /**
     * Persiste el nivel o paso de tamaño de fuente (0 a 4).
     */
    suspend fun setFontSizeStep(step: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.FONT_SIZE_STEP] = step
        }
    }

    /**
     * Activa o desactiva el efecto de desenfoque/blur en la barra de navegación y toda la aplicación.
     */
    suspend fun setNavbarBlurEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.NAVBAR_BLUR] = enabled
        }
    }

    /**
     * Activa o desactiva el efecto de desenfoque/blur global en barras, tarjetas y menús de la aplicación.
     */
    suspend fun setGlobalBlurEnabled(enabled: Boolean) {
        setNavbarBlurEnabled(enabled)
    }

    /**
     * Activa o desactiva la reducción de animaciones para optimizar el rendimiento en dispositivos de gama baja
     * y prevenir mareos por movimiento según las preferencias del usuario.
     */
    suspend fun setReduceMotionEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.REDUCE_MOTION] = enabled
        }
    }

    /**
     * Activa o desactiva la protección por código/biometría al iniciar la app.
     */
    suspend fun setLockAppEnabled(enabled: Boolean) {
        cachedIsLockAppEnabled = enabled
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.LOCK_APP] = enabled
        }
    }

    /**
     * Activa o desactiva la verificación automática de actualizaciones al arrancar.
     */
    suspend fun setCheckUpdateOnStartEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.CHECK_UPDATE_ON_START] = enabled
        }
    }

    /**
     * Persiste la preferencia de modo de visualización (Grid vs Lista) para la pestaña de Pins.
     */
    suspend fun setPinsGridView(isGrid: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.VIEW_MODE_PINS_GRID] = isGrid
        }
    }

    /**
     * Persiste la preferencia de modo de visualización (Grid vs Lista) para la pestaña de Notebook (Notas).
     */
    suspend fun setNotesGridView(isGrid: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.VIEW_MODE_NOTES_GRID] = isGrid
        }
    }

    /**
     * Persiste la preferencia de modo de visualización (Grid vs Lista) para la pestaña de Bookmarks (Guardados).
     */
    suspend fun setBookmarksGridView(isGrid: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.VIEW_MODE_BOOKMARKS_GRID] = isGrid
        }
    }

    /**
     * Persiste el índice de la última pestaña principal activa para reanudar el flujo en el próximo inicio.
     */
    suspend fun setLastActiveTab(tabIndex: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.LAST_ACTIVE_TAB] = tabIndex
        }
    }

    /**
     * Persiste la última subsección activa de la libreta para reanudar directamente en Notas o To-dos al iniciar.
     */
    suspend fun setLastNotebookSection(section: String) {
        cachedLastNotebookSection = section
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.LAST_NOTEBOOK_SECTION] = section
        }
    }

    /**
     * Persiste la preferencia de detección automática de enlaces en el portapapeles.
     */
    suspend fun setClipboardAutoDetectEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.CLIPBOARD_AUTO_DETECT] = enabled
        }
    }

    /**
     * Persiste la preferencia de descarga de metadatos y favicons web desde internet.
     */
    suspend fun setFetchWebMetadataEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.FETCH_WEB_METADATA] = enabled
        }
    }

    /**
     * Persiste la confirmación de que el usuario ha visualizado o completado el recorrido de bienvenida.
     */
    suspend fun setHasSeenWalkthrough(hasSeen: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.HAS_SEEN_WALKTHROUGH] = hasSeen
        }
    }

    /**
     * Restablece todas las preferencias del usuario a sus valores predeterminados de fábrica.
     *
     * Se purga atómicamente el contenido del DataStore mediante [MutablePreferences.clear],
     * permitiendo que [userPreferencesFlow] y los flujos individuales regresen de inmediato a
     * sus valores predeterminados de sistema (tema Dark, escala de texto normal y vista de lista
     * para todas las pestañas) sin dejar entradas residuales en disco.
     */
    suspend fun resetToDefaults() {
        cachedLastNotebookSection = "NOTES"
        context.settingsDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
