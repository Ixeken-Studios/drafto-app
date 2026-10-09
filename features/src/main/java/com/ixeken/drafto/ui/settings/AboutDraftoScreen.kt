package com.ixeken.drafto.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.ixeken.drafto.ui.theme.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ixeken.drafto.core.R

import com.ixeken.drafto.ui.components.DraftoSwitch
import com.ixeken.drafto.ui.components.DraftoStickyHeader
import com.ixeken.drafto.ui.components.DraftoTabTopBar
import com.ixeken.drafto.ui.components.DraftoConfirmationDialog
import com.ixeken.drafto.ui.components.DraftoDialog
import com.ixeken.drafto.ui.theme.DraftoTheme
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import com.ixeken.drafto.ui.components.DraftoWalkthroughBottomSheet
import com.ixeken.drafto.ui.theme.LocalHazeState
import com.ixeken.drafto.ui.utils.DraftoSystemInteractions
import com.ixeken.drafto.util.GitHubUpdateChecker
import com.ixeken.drafto.util.UpdateCheckResult
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

private const val SOURCE_CODE_URL = "https://github.com/Ixeken-Studios/drafto-app"
private const val IXEKEN_STUDIOS_URL = "https://github.com/Ixeken-Studios"

private sealed interface UpdateDialogState {
    data object None : UpdateDialogState
    data object ConsentManualCheck : UpdateDialogState
    data object ConsentStartupToggle : UpdateDialogState
    data object Checking : UpdateDialogState
    data class Available(val version: String, val changelog: String, val releaseUrl: String) : UpdateDialogState
    data class UpToDate(val version: String) : UpdateDialogState
    data class TimeTraveler(val currentVersion: String, val latestVersion: String) : UpdateDialogState
    data object RepoNotFoundOrPrivate : UpdateDialogState
    data object Error : UpdateDialogState
}

/**
 * Pantalla informativa y de actualizaciones del proyecto Drafto refactorizada con diseño cohesivo Brain OS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutDraftoScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onToggleCheckUpdateOnStart: (Boolean) -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val currentVersion = stringResource(R.string.about_version)
    val scope = rememberCoroutineScope()
    var showReleaseNotesSheet by remember { mutableStateOf(false) }
    var showWalkthroughSheet by remember { mutableStateOf(false) }
    var updateDialogState by remember { mutableStateOf<UpdateDialogState>(UpdateDialogState.None) }
    var headerHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val headerHeightDp = with(density) { headerHeightPx.toDp() }
    val safeTopPadding = if (headerHeightDp > 0.dp) headerHeightDp + PaddingSmall else 90.dp

    val hazeState = LocalHazeState.current
    val scrollState = rememberScrollState()
    val isScrolled = scrollState.value > 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(safeTopPadding))

            // Tarjeta 1: Información del Proyecto
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaddingScreenHorizontal)
                    .clip(DraftoShapeCard)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // View source code
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { DraftoSystemInteractions.openUrl(context, SOURCE_CODE_URL) }
                        .padding(SettingsItemPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(SettingsItemIconSize)
                        )
                        Spacer(modifier = Modifier.width(PaddingLarge))
                        Text(
                            text = stringResource(R.string.about_item_view_source),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BorderWidthThin)
                        .background(MaterialTheme.colorScheme.outline)
                )

                // Release notes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showReleaseNotesSheet = true }
                        .padding(SettingsItemPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(SettingsItemIconSize)
                        )
                        Spacer(modifier = Modifier.width(PaddingLarge))
                        Text(
                            text = stringResource(R.string.about_item_release_notes),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BorderWidthThin)
                        .background(MaterialTheme.colorScheme.outline)
                )

                // Welcome walkthrough
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showWalkthroughSheet = true }
                        .padding(SettingsItemPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Explore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(SettingsItemIconSize)
                        )
                        Spacer(modifier = Modifier.width(PaddingLarge))
                        Column {
                            Text(
                                text = stringResource(R.string.about_item_walkthrough),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(PaddingMicro))
                            Text(
                                text = stringResource(R.string.about_item_walkthrough_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BorderWidthThin)
                        .background(MaterialTheme.colorScheme.outline)
                )

                // By Ixeken Studios
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { DraftoSystemInteractions.openUrl(context, IXEKEN_STUDIOS_URL) }
                        .padding(SettingsItemPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ixeken_logo),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(SettingsItemIconSize)
                        )
                        Spacer(modifier = Modifier.width(PaddingLarge))
                        Column {
                            Text(
                                text = stringResource(R.string.about_item_by_ixeken),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(PaddingMicro))
                            Text(
                                text = stringResource(R.string.about_item_by_ixeken_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(SettingsGroupSpacing))

            // Tarjeta 2: Actualizaciones
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaddingScreenHorizontal)
                    .clip(DraftoShapeCard)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Check for updates
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { updateDialogState = UpdateDialogState.ConsentManualCheck }
                        .padding(SettingsItemPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(SettingsItemIconSize)
                        )
                        Spacer(modifier = Modifier.width(PaddingLarge))
                        Column {
                            Text(
                                text = stringResource(R.string.about_item_check_updates),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(PaddingMicro))
                            Text(
                                text = stringResource(R.string.about_item_check_updates_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BorderWidthThin)
                        .background(MaterialTheme.colorScheme.outline)
                )

                // Check update on start
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(SettingsItemPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RocketLaunch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(SettingsItemIconSize)
                        )
                        Spacer(modifier = Modifier.width(PaddingLarge))
                        Column {
                            Text(
                                text = stringResource(R.string.about_item_check_on_start),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(PaddingMicro))
                            Text(
                                text = stringResource(R.string.about_item_check_on_start_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DraftoSwitch(
                        checked = uiState.isCheckUpdateOnStartEnabled,
                        onCheckedChange = { isEnabled ->
                            if (isEnabled) {
                                updateDialogState = UpdateDialogState.ConsentStartupToggle
                            } else {
                                onToggleCheckUpdateOnStart(false)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(PaddingExtraLarge))

            Text(
                text = stringResource(R.string.about_stand_with_palestine),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaddingScreenHorizontal)
            )

            Spacer(modifier = Modifier.height(PaddingListBottomContent))
        }

        // Header adhesivo superior con efecto Spatial Pure Blur Ultra Thin
        DraftoStickyHeader(
            isScrolled = isScrolled,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onGloballyPositioned { headerHeightPx = it.size.height }
        ) {
            DraftoTabTopBar(
                title = stringResource(R.string.about_title),
                titleTextStyle = SubpageTitleTextStyle,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    Icon(
                        painter = painterResource(R.drawable.ic_drafto_logo),
                        contentDescription = stringResource(R.string.app_name),
                        tint = androidx.compose.ui.graphics.Color.Unspecified,
                        modifier = Modifier.size(AboutTopBarLogoSize)
                    )
                }
            )
        }

        if (showReleaseNotesSheet) {
            ReleaseNotesBottomSheet(
                onDismiss = { showReleaseNotesSheet = false }
            )
        }

        if (showWalkthroughSheet) {
            DraftoWalkthroughBottomSheet(
                onDismiss = { showWalkthroughSheet = false }
            )
        }

        when (val state = updateDialogState) {
            is UpdateDialogState.None -> Unit
            is UpdateDialogState.ConsentManualCheck -> {
                DraftoConfirmationDialog(
                    title = stringResource(R.string.update_consent_manual_title),
                    message = stringResource(R.string.update_consent_manual_message),
                    icon = Icons.Rounded.Language,
                    confirmText = stringResource(R.string.update_consent_manual_confirm),
                    cancelText = stringResource(R.string.action_cancel),
                    isDestructive = false,
                    onConfirm = {
                        updateDialogState = UpdateDialogState.Checking
                        scope.launch {
                            val result = GitHubUpdateChecker.checkUpdate(currentVersion)
                            updateDialogState = when (result) {
                                is UpdateCheckResult.UpdateAvailable -> UpdateDialogState.Available(result.version, result.changelog, result.releaseUrl)
                                is UpdateCheckResult.UpToDate -> UpdateDialogState.UpToDate(result.version)
                                is UpdateCheckResult.TimeTraveler -> UpdateDialogState.TimeTraveler(result.currentVersion, result.latestVersion)
                                is UpdateCheckResult.RepoNotFoundOrPrivate -> UpdateDialogState.RepoNotFoundOrPrivate
                                is UpdateCheckResult.Error -> UpdateDialogState.Error
                            }
                        }
                    },
                    onDismiss = { updateDialogState = UpdateDialogState.None }
                )
            }
            is UpdateDialogState.ConsentStartupToggle -> {
                DraftoConfirmationDialog(
                    title = stringResource(R.string.update_consent_startup_title),
                    message = stringResource(R.string.update_consent_startup_message),
                    icon = Icons.Rounded.RocketLaunch,
                    confirmText = stringResource(R.string.update_consent_startup_confirm),
                    cancelText = stringResource(R.string.action_cancel),
                    isDestructive = false,
                    onConfirm = {
                        onToggleCheckUpdateOnStart(true)
                        updateDialogState = UpdateDialogState.None
                    },
                    onDismiss = { updateDialogState = UpdateDialogState.None }
                )
            }
            is UpdateDialogState.Checking -> {
                DraftoDialog(onDismissRequest = {}) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PaddingLarge),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = DraftoTheme.colors.accent,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(PaddingLarge))
                        Text(
                            text = stringResource(R.string.update_checking_progress),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            is UpdateDialogState.Available -> {
                DraftoConfirmationDialog(
                    title = stringResource(R.string.update_available_title),
                    message = stringResource(R.string.update_available_message, state.version),
                    icon = Icons.Rounded.SystemUpdate,
                    confirmText = stringResource(R.string.update_available_view),
                    cancelText = stringResource(R.string.update_available_later),
                    isDestructive = false,
                    onConfirm = {
                        DraftoSystemInteractions.openUrl(context, state.releaseUrl)
                        updateDialogState = UpdateDialogState.None
                    },
                    onDismiss = { updateDialogState = UpdateDialogState.None }
                )
            }
            is UpdateDialogState.UpToDate -> {
                DraftoConfirmationDialog(
                    title = stringResource(R.string.update_up_to_date_title),
                    message = stringResource(R.string.update_up_to_date_message, state.version),
                    icon = Icons.Rounded.CheckCircle,
                    confirmText = stringResource(R.string.action_ok),
                    cancelText = null,
                    isDestructive = false,
                    onConfirm = { updateDialogState = UpdateDialogState.None },
                    onDismiss = { updateDialogState = UpdateDialogState.None }
                )
            }
            is UpdateDialogState.TimeTraveler -> {
                DraftoConfirmationDialog(
                    title = stringResource(R.string.update_time_traveler_title),
                    message = stringResource(R.string.update_time_traveler_message, state.currentVersion, state.latestVersion),
                    icon = Icons.Rounded.History,
                    confirmText = stringResource(R.string.action_ok),
                    cancelText = null,
                    isDestructive = false,
                    onConfirm = { updateDialogState = UpdateDialogState.None },
                    onDismiss = { updateDialogState = UpdateDialogState.None }
                )
            }
            is UpdateDialogState.RepoNotFoundOrPrivate -> {
                DraftoConfirmationDialog(
                    title = stringResource(R.string.update_repo_not_found_title),
                    message = stringResource(R.string.update_repo_not_found_message),
                    icon = Icons.Rounded.Lock,
                    confirmText = stringResource(R.string.action_ok),
                    cancelText = null,
                    isDestructive = false,
                    onConfirm = { updateDialogState = UpdateDialogState.None },
                    onDismiss = { updateDialogState = UpdateDialogState.None }
                )
            }
            is UpdateDialogState.Error -> {
                DraftoConfirmationDialog(
                    title = stringResource(R.string.update_error_title),
                    message = stringResource(R.string.update_error_message),
                    icon = Icons.Rounded.CloudOff,
                    confirmText = stringResource(R.string.action_ok),
                    cancelText = null,
                    isDestructive = false,
                    onConfirm = { updateDialogState = UpdateDialogState.None },
                    onDismiss = { updateDialogState = UpdateDialogState.None }
                )
            }
        }
    }
}
