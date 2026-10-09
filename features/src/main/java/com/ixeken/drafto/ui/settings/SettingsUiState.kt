package com.ixeken.drafto.ui.settings

/**
 * Representa los diferentes tipos de Modal Bottom Sheets que se pueden presentar
 * en el flujo de configuración.
 */
enum class SettingsSheetType {
    NONE,
    PRIVACY_INFO,
    PERMISSIONS,
    THEME_SELECTION,
    LANGUAGE_SELECTION
}

/**
 * Estado UI inmutable consumido por las pantallas de configuración.
 */
data class SettingsUiState(
    val theme: String = "Dark",
    val fontSizeStep: Int = 2,
    val accentColor: String = "monochrome",
    val isNavbarBlurEnabled: Boolean = false,
    val isGlobalBlurEnabled: Boolean = isNavbarBlurEnabled,
    val isReduceMotionEnabled: Boolean = false,
    val isLockAppEnabled: Boolean = false,
    val isCheckUpdateOnStartEnabled: Boolean = false,
    val isClipboardAutoDetectEnabled: Boolean = false,
    val isFetchWebMetadataEnabled: Boolean = true,
    val isPinsGridView: Boolean = false,
    val isNotesGridView: Boolean = false,
    val isBookmarksGridView: Boolean = false,
    val lastActiveTab: Int = 0,
    val lastNotebookSection: String = "NOTES",
    val isPreferencesLoaded: Boolean = false,
    val hasSeenWalkthrough: Boolean = false,
    val activeBottomSheet: SettingsSheetType = SettingsSheetType.NONE,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val exportedJson: String? = null,
    val importSuccess: Boolean = false,
    val error: String? = null
)
