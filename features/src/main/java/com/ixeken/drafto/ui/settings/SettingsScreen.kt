package com.ixeken.drafto.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.ixeken.drafto.ui.components.DraftoSearchableTopBar
import com.ixeken.drafto.ui.components.DraftoStickyHeader
import com.ixeken.drafto.ui.components.DraftoTopBarIconButton
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.ixeken.drafto.ui.theme.LocalHazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ixeken.drafto.core.R
import com.ixeken.drafto.ui.components.DraftoEmptyState
import com.ixeken.drafto.ui.components.DraftoSwitch
import com.ixeken.drafto.ui.components.DraftoTabTopBar
import com.ixeken.drafto.ui.components.DraftoToastManager
import com.ixeken.drafto.ui.security.BiometricAuthenticator
import com.ixeken.drafto.ui.theme.*

/**
 * Representación de un ítem de configuración para búsqueda y renderizado.
 */
private data class SettingSearchItem(
    val id: String,
    val title: String,
    val subtitle: String?,
    val keywords: List<String>,
    val sectionName: String,
    val icon: ImageVector? = null,
    val iconPainterRes: Int? = null,
    val iconTint: androidx.compose.ui.graphics.Color? = null,
    val onClick: (() -> Unit)? = null,
    val isSwitch: Boolean = false,
    val isSwitchChecked: Boolean = false,
    val isSwitchEnabled: Boolean = true,
    val onSwitchChange: ((Boolean) -> Unit)? = null
)

/**
 * Pantalla principal de configuración (Settings) adaptada sin colores ni cadenas hardcodeadas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onNavigateToAppearance: () -> Unit,
    onNavigateToAboutDrafto: () -> Unit,
    onNavigateToDataStorage: () -> Unit = {},
    onToggleLockApp: (Boolean) -> Unit,
    onToggleClipboardAutoDetect: (Boolean) -> Unit = {},
    onToggleFetchWebMetadata: (Boolean) -> Unit = {},
    onOpenPermissionsBottomSheet: () -> Unit,
    onOpenPrivacyInfoBottomSheet: () -> Unit,
    onOpenThemeSelectionBottomSheet: () -> Unit,
    onOpenLanguageSelectionBottomSheet: () -> Unit = {},
    onSelectTheme: (String) -> Unit,
    onDismissBottomSheet: () -> Unit,
    onBack: () -> Unit = {}
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
    }

    val context = LocalContext.current
    val isDeviceSecure = remember(context) { BiometricAuthenticator.isDeviceSecure(context) }

    val appearanceTitle = stringResource(R.string.settings_item_appearance_title)
    val appearanceSubtitle = stringResource(R.string.settings_item_appearance_subtitle)
    val languageTitle = stringResource(R.string.settings_item_language_title)
    val currentLocalesTag = remember(context) {
        val localeManager = context.getSystemService(android.app.LocaleManager::class.java)
        val locales = localeManager?.applicationLocales
        if (locales != null && !locales.isEmpty) {
            locales.toLanguageTags()
        } else {
            java.util.Locale.getDefault().toLanguageTag()
        }
    }
    val languageSubtitle = if (currentLocalesTag.startsWith("es", ignoreCase = true)) {
        stringResource(R.string.language_spanish)
    } else {
        stringResource(R.string.language_english)
    }
    val clipboardAutoDetectTitle = stringResource(R.string.settings_item_clipboard_auto_detect_title)
    val clipboardAutoDetectSubtitle = stringResource(R.string.settings_item_clipboard_auto_detect_subtitle)
    val dataStorageTitle = stringResource(R.string.settings_item_data_storage_title)
    val dataStorageSubtitle = stringResource(R.string.settings_item_data_storage_subtitle)
    val lockAppTitle = stringResource(R.string.settings_item_lock_app_title)
    val lockAppSubtitle = stringResource(R.string.settings_item_lock_app_subtitle)
    val lockAppNoLockSubtitle = stringResource(R.string.settings_item_lock_app_no_lock_subtitle)
    val webPreviewsTitle = stringResource(R.string.settings_item_web_previews_title)
    val webPreviewsSubtitle = stringResource(R.string.settings_item_web_previews_subtitle)
    val permissionsTitle = stringResource(R.string.settings_item_permissions_title)
    val permissionsSubtitle = stringResource(R.string.settings_item_permissions_subtitle)
    val privacyInfoTitle = stringResource(R.string.settings_item_privacy_info_title)
    val aboutDraftoTitle = stringResource(R.string.app_name)
    val aboutDraftoSubtitle = stringResource(R.string.settings_item_drafto_subtitle)

    val sectionPreferences = stringResource(R.string.settings_section_preferences)
    val sectionPrivacy = stringResource(R.string.settings_section_privacy_security)
    val sectionAbout = stringResource(R.string.settings_section_about)

    // Tarjeta 1: Personalización y Experiencia Visual
    val personalizationItems = remember(
        appearanceTitle, appearanceSubtitle, onNavigateToAppearance,
        languageTitle, languageSubtitle, onOpenLanguageSelectionBottomSheet,
        sectionPreferences
    ) {
        listOf(
            SettingSearchItem(
                id = "appearance",
                title = appearanceTitle,
                subtitle = appearanceSubtitle,
                keywords = listOf("appearance", "apariencia", "tema", "theming", "color", "blur", "fuente", "font", "letra", "size", "tamaño"),
                sectionName = sectionPreferences,
                icon = Icons.Filled.Brush,
                onClick = onNavigateToAppearance
            ),
            SettingSearchItem(
                id = "language",
                title = languageTitle,
                subtitle = languageSubtitle,
                keywords = listOf("language", "idioma", "lenguaje", "español", "spanish", "english", "inglés", "mexico", "traducción", "translation"),
                sectionName = sectionPreferences,
                icon = Icons.Rounded.Translate,
                onClick = onOpenLanguageSelectionBottomSheet
            )
        )
    }

    // Tarjeta 2: Flujo de Trabajo y Controles Rápidos (Switches directos)
    val interactionItems = remember(
        clipboardAutoDetectTitle, clipboardAutoDetectSubtitle, uiState.isClipboardAutoDetectEnabled, onToggleClipboardAutoDetect,
        lockAppTitle, lockAppSubtitle, lockAppNoLockSubtitle, uiState.isLockAppEnabled, onToggleLockApp, isDeviceSecure,
        webPreviewsTitle, webPreviewsSubtitle, uiState.isFetchWebMetadataEnabled, onToggleFetchWebMetadata,
        sectionPreferences, sectionPrivacy
    ) {
        listOf(
            SettingSearchItem(
                id = "clipboard_auto_detect",
                title = clipboardAutoDetectTitle,
                subtitle = clipboardAutoDetectSubtitle,
                keywords = listOf("clipboard", "portapapeles", "link", "enlace", "url", "paste", "pegar", "auto", "detect", "detectar", "toast"),
                sectionName = sectionPreferences,
                icon = Icons.Rounded.ContentPaste,
                isSwitch = true,
                isSwitchChecked = uiState.isClipboardAutoDetectEnabled,
                onSwitchChange = onToggleClipboardAutoDetect
            ),
            SettingSearchItem(
                id = "lock_app",
                title = lockAppTitle,
                subtitle = if (isDeviceSecure) lockAppSubtitle else lockAppNoLockSubtitle,
                keywords = listOf("lock", "bloqueo", "bloquear", "seguridad", "security", "biometric", "huella", "pin", "fingerprint", "protección"),
                sectionName = sectionPrivacy,
                icon = Icons.Filled.Lock,
                isSwitch = true,
                isSwitchChecked = uiState.isLockAppEnabled && isDeviceSecure,
                isSwitchEnabled = isDeviceSecure,
                onSwitchChange = if (isDeviceSecure) onToggleLockApp else null,
                onClick = if (!isDeviceSecure) {
                    {
                        DraftoToastManager.showWarning(context.getString(R.string.error_no_secure_lock))
                    }
                } else null
            ),
            SettingSearchItem(
                id = "web_previews",
                title = webPreviewsTitle,
                subtitle = webPreviewsSubtitle,
                keywords = listOf("preview", "previsualización", "favicon", "icon", "icono", "web", "link", "marcador", "bookmark", "metadata", "internet", "red", "privacy", "privacidad"),
                sectionName = sectionPrivacy,
                icon = Icons.Rounded.Language,
                isSwitch = true,
                isSwitchChecked = uiState.isFetchWebMetadataEnabled,
                onSwitchChange = onToggleFetchWebMetadata
            )
        )
    }

    // Tarjeta 3: Sistema y Almacenamiento
    val systemItems = remember(
        dataStorageTitle, dataStorageSubtitle, onNavigateToDataStorage, sectionPreferences,
        permissionsTitle, permissionsSubtitle, onOpenPermissionsBottomSheet, sectionPrivacy
    ) {
        listOf(
            SettingSearchItem(
                id = "data_storage",
                title = dataStorageTitle,
                subtitle = dataStorageSubtitle,
                keywords = listOf("storage", "almacenamiento", "backup", "respaldo", "copia", "wipe", "borrado", "limpieza", "datos", "data", "export", "import", "cache"),
                sectionName = sectionPreferences,
                icon = Icons.Rounded.Storage,
                onClick = onNavigateToDataStorage
            ),
            SettingSearchItem(
                id = "permissions",
                title = permissionsTitle,
                subtitle = permissionsSubtitle,
                keywords = listOf("permissions", "permisos", "archivos", "files", "notificaciones", "notifications", "fotos", "photos", "storage", "almacenamiento"),
                sectionName = sectionPrivacy,
                icon = Icons.Filled.Security,
                onClick = onOpenPermissionsBottomSheet
            )
        )
    }

    // Tarjeta 4: Acerca del Sistema y Privacidad
    val aboutItems = remember(
        aboutDraftoTitle, aboutDraftoSubtitle, onNavigateToAboutDrafto, sectionAbout,
        privacyInfoTitle, onOpenPrivacyInfoBottomSheet, sectionPrivacy
    ) {
        listOf(
            SettingSearchItem(
                id = "about_drafto",
                title = aboutDraftoTitle,
                subtitle = aboutDraftoSubtitle,
                keywords = listOf("about", "acerca de", "drafto", "version", "versión", "ixeken", "update", "actualización", "github", "créditos"),
                sectionName = sectionAbout,
                icon = Icons.Filled.Info,
                onClick = onNavigateToAboutDrafto
            ),
            SettingSearchItem(
                id = "privacy_info",
                title = privacyInfoTitle,
                subtitle = null,
                keywords = listOf("privacy", "privacidad", "info", "información", "datos", "data", "offline", "cifrado", "encriptación", "seguridad"),
                sectionName = sectionPrivacy,
                icon = Icons.Filled.Shield,
                onClick = onOpenPrivacyInfoBottomSheet
            )
        )
    }

    val allSettingsItems = remember(personalizationItems, interactionItems, systemItems, aboutItems) {
        personalizationItems + interactionItems + systemItems + aboutItems
    }

    val settingGroups = remember(personalizationItems, interactionItems, systemItems, aboutItems) {
        listOf(personalizationItems, interactionItems, systemItems, aboutItems)
    }

    val filteredItems = remember(allSettingsItems, searchQuery) {
        if (searchQuery.isBlank()) {
            allSettingsItems
        } else {
            val query = searchQuery.trim().lowercase()
            allSettingsItems.filter { item ->
                item.title.lowercase().contains(query) ||
                        (item.subtitle?.lowercase()?.contains(query) == true) ||
                        item.keywords.any { it.lowercase().contains(query) } ||
                        item.sectionName.lowercase().contains(query)
            }
        }
    }

    var headerHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val headerHeightDp = with(density) { headerHeightPx.toDp() }
    val safeTopPadding = if (headerHeightDp > 0.dp) headerHeightDp + PaddingSmall else 110.dp

    val hazeState = LocalHazeState.current
    val scrollState = rememberScrollState()
    val isScrolled = scrollState.value > 0 || isSearchActive

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Área scrolleable de opciones y resultados
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(safeTopPadding))

            if (searchQuery.isNotBlank()) {
                // Vista de resultados de búsqueda
                if (filteredItems.isEmpty()) {
                    DraftoEmptyState(
                        icon = Icons.Default.Search,
                        iconTint = DraftoTheme.colors.accent,
                        title = stringResource(R.string.empty_search_title),
                        description = stringResource(R.string.empty_search_description)
                    )
                } else {
                    Text(
                        text = pluralStringResource(
                            R.plurals.search_options_found,
                            filteredItems.size,
                            filteredItems.size
                        ),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = PaddingScreenHorizontal + PaddingExtraSmall, vertical = PaddingExtraSmall)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PaddingScreenHorizontal, vertical = PaddingExtraSmall)
                            .clip(DraftoShapeCard)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        filteredItems.forEachIndexed { index, item ->
                            SettingItemRow(item = item)
                            if (index < filteredItems.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(BorderWidthThin)
                                        .background(MaterialTheme.colorScheme.outline)
                                )
                            }
                        }
                    }
                }
            } else {
                // Vista estándar: 4 tarjetas agrupadas inspiradas en Nothing OS, sin títulos de sección ni píldoras
                settingGroups.forEachIndexed { groupIndex, groupItems ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PaddingScreenHorizontal)
                            .clip(DraftoShapeCard)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        groupItems.forEachIndexed { index, item ->
                            SettingItemRow(item = item)
                            if (index < groupItems.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(BorderWidthThin)
                                        .background(MaterialTheme.colorScheme.outline)
                                )
                            }
                        }
                    }
                    if (groupIndex < settingGroups.size - 1) {
                        Spacer(modifier = Modifier.height(SettingsGroupSpacing))
                    }
                }
            }

            Spacer(modifier = Modifier.height(PaddingListBottomContent))
        }

        // Header adhesivo superior con Spatial Pure Blur Ultra Thin
        DraftoStickyHeader(
            isScrolled = isScrolled,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onGloballyPositioned { headerHeightPx = it.size.height }
        ) {
            DraftoSearchableTopBar(
                title = stringResource(R.string.title_settings),
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onCloseSearch = {
                    isSearchActive = false
                    searchQuery = ""
                },
                placeholder = stringResource(R.string.placeholder_search_settings),
                actions = {
                    DraftoTopBarIconButton(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.action_search),
                        onClick = { isSearchActive = true }
                    )
                }
            )
        }
    }
}

@Composable
private fun SettingItemRow(item: SettingSearchItem) {
    val isRowClickable = (!item.isSwitch && item.onClick != null) || (item.isSwitch && !item.isSwitchEnabled && item.onClick != null)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isRowClickable) {
                    Modifier.clickable(onClick = item.onClick!!)
                } else Modifier
            )
            .padding(SettingsItemPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.icon != null) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(SettingsItemIconSize)
                )
            } else if (item.iconPainterRes != null) {
                Icon(
                    painter = painterResource(item.iconPainterRes),
                    contentDescription = null,
                    tint = item.iconTint ?: MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(SettingsItemIconSize)
                )
            }
            Spacer(modifier = Modifier.width(PaddingLarge))
            Column {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!item.subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(PaddingMicro))
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (item.isSwitch) {
            DraftoSwitch(
                checked = item.isSwitchChecked,
                enabled = item.isSwitchEnabled,
                onCheckedChange = if (item.isSwitchEnabled) item.onSwitchChange else null
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * TopBar encapsulada para la pestaña de Configuración.
 * Incluye botón de búsqueda de opciones.
 */
@Composable
fun SettingsTopBar(
    isSearchActive: Boolean = false,
    onToggleSearch: () -> Unit = {},
    modifier: Modifier = Modifier,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
        DraftoTopBarIconButton(
            imageVector = Icons.Default.Search,
            contentDescription = stringResource(R.string.action_search),
            onClick = onToggleSearch
        )
    }
) {
    DraftoTabTopBar(
        title = stringResource(R.string.title_settings),
        modifier = modifier,
        actions = actions
    )
}
