package com.ixeken.drafto.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ixeken.drafto.core.R
import com.ixeken.drafto.ui.components.DraftoModalBottomSheet
import com.ixeken.drafto.ui.components.DraftoSheetCancelButton
import com.ixeken.drafto.ui.components.DraftoSheetCardGroup
import com.ixeken.drafto.ui.components.DraftoSheetHeader
import com.ixeken.drafto.ui.components.DraftoSheetInfoRow
import com.ixeken.drafto.ui.components.DraftoSheetOptionRow
import com.ixeken.drafto.ui.theme.BookmarkFormSectionSpacing
import com.ixeken.drafto.ui.theme.BorderWidthThin
import com.ixeken.drafto.ui.theme.BorderWidthZero
import com.ixeken.drafto.ui.theme.DraftoShapePill
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.FrauncesFontFamily
import com.ixeken.drafto.ui.theme.PaddingScreenHorizontal
import com.ixeken.drafto.ui.theme.PaddingSmall
import com.ixeken.drafto.ui.theme.SheetCardGroupSpacing
import com.ixeken.drafto.ui.theme.ThemeOptionCircleInnerSize
import com.ixeken.drafto.ui.theme.ThemeOptionCircleOuterSize
import com.ixeken.drafto.ui.theme.ThemeOptionGroupPadding
import com.ixeken.drafto.ui.theme.ThemeOptionSelectedBorderWidth
import com.ixeken.drafto.ui.theme.ThemeOptionTextSpacing

/**
 * Modal Bottom Sheet que presenta los términos y detalles de privacidad de la app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyInfoBottomSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    DraftoModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaddingScreenHorizontal)
        ) {
            DraftoSheetHeader(
                title = stringResource(R.string.settings_item_privacy_info_title)
            )

            Spacer(modifier = Modifier.height(BookmarkFormSectionSpacing))

            DraftoSheetCardGroup(
                backgroundColor = DraftoTheme.colors.primaryContainer
            ) {
                DraftoSheetInfoRow(
                    title = stringResource(R.string.bs_privacy_info_telemetry_title),
                    description = stringResource(R.string.bs_privacy_info_telemetry_desc),
                    icon = Icons.Rounded.Security,
                    iconTint = DraftoTheme.colors.accent,
                    showDivider = true
                )

                DraftoSheetInfoRow(
                    title = stringResource(R.string.bs_privacy_info_network_title),
                    description = stringResource(R.string.bs_privacy_info_network_desc),
                    icon = Icons.Rounded.Language,
                    iconTint = DraftoTheme.colors.accent,
                    showDivider = true
                )

                DraftoSheetInfoRow(
                    title = stringResource(R.string.bs_privacy_info_backups_title),
                    description = stringResource(R.string.bs_privacy_info_backups_desc),
                    icon = Icons.Rounded.FolderZip,
                    iconTint = DraftoTheme.colors.accent,
                    showDivider = false
                )
            }

            Spacer(modifier = Modifier.height(SheetCardGroupSpacing))

            DraftoSheetCancelButton(
                onClick = onDismiss
            )

            Spacer(modifier = Modifier.height(PaddingSmall))
        }
    }
}

/**
 * Modal Bottom Sheet para la gestión y solicitud de permisos nativos del sistema.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsBottomSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    DraftoModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaddingScreenHorizontal)
        ) {
            DraftoSheetHeader(
                title = stringResource(R.string.settings_item_permissions_title)
            )

            Spacer(modifier = Modifier.height(BookmarkFormSectionSpacing))

            DraftoSheetCardGroup(
                backgroundColor = DraftoTheme.colors.primaryContainer
            ) {
                // Notificaciones (único permiso de runtime necesario en Drafto)
                DraftoSheetOptionRow(
                    title = stringResource(R.string.bs_permissions_notifications),
                    subtitle = stringResource(R.string.bs_permissions_notifications_desc),
                    icon = Icons.Rounded.Notifications,
                    showDivider = true,
                    onClick = {
                        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    trailingContent = {
                        if (hasNotificationPermission) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = stringResource(R.string.bs_permissions_granted),
                                tint = DraftoTheme.colors.accent,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(DraftoShapePill)
                                    .background(DraftoTheme.colors.accent)
                                    .clickable {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.bs_permissions_grant),
                                    fontFamily = FrauncesFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DraftoTheme.colors.onAccent
                                )
                            }
                        }
                    }
                )

                // Acceso a Ajustes del Sistema para configuración avanzada
                DraftoSheetOptionRow(
                    title = stringResource(R.string.bs_permissions_system_settings),
                    subtitle = stringResource(R.string.bs_permissions_system_settings_desc),
                    icon = Icons.Rounded.Settings,
                    showDivider = false,
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = DraftoTheme.colors.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(SheetCardGroupSpacing))

            DraftoSheetCancelButton(
                onClick = onDismiss
            )

            Spacer(modifier = Modifier.height(PaddingSmall))
        }
    }
}

/**
 * Modal Bottom Sheet para la selección del tema visual de la aplicación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionBottomSheet(
    selectedTheme: String,
    onSelectTheme: (String) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    DraftoModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaddingScreenHorizontal)
        ) {
            DraftoSheetHeader(
                title = stringResource(R.string.appearance_item_theme_selection_title)
            )

            val darkText = stringResource(R.string.bs_theme_dark)
            val lightText = stringResource(R.string.bs_theme_light)
            val amoledText = stringResource(R.string.bs_theme_amoled)
            val kraftText = stringResource(R.string.bs_theme_kraft)

            val isDarkSelected = selectedTheme.equals("Dark", ignoreCase = true)
            val isLightSelected = selectedTheme.equals("Light", ignoreCase = true)
            val isAmoledSelected = selectedTheme.equals("Amoled", ignoreCase = true) || selectedTheme.equals("AMOLED", ignoreCase = true)
            val isKraftSelected = selectedTheme.equals("Kraft", ignoreCase = true)

            Spacer(modifier = Modifier.height(BookmarkFormSectionSpacing))

            DraftoSheetCardGroup(
                backgroundColor = DraftoTheme.colors.primaryContainer,
                contentPadding = PaddingValues(ThemeOptionGroupPadding)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tema Dark
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            onSelectTheme("Dark")
                            onDismiss()
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(ThemeOptionCircleOuterSize)
                                .clip(CircleShape)
                                .background(Color(0xFF2B2B2B))
                                .border(
                                    width = if (isDarkSelected) ThemeOptionSelectedBorderWidth else BorderWidthZero,
                                    color = if (isDarkSelected) DraftoTheme.colors.accent else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(ThemeOptionCircleInnerSize)
                                    .clip(CircleShape)
                                    .background(Color(0xFF141414))
                            )
                        }
                        Spacer(modifier = Modifier.height(ThemeOptionTextSpacing))
                        Text(
                            text = darkText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isDarkSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Tema Light
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            onSelectTheme("Light")
                            onDismiss()
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(ThemeOptionCircleOuterSize)
                                .clip(CircleShape)
                                .background(Color(0xFFE5E7EB))
                                .border(
                                    width = if (isLightSelected) ThemeOptionSelectedBorderWidth else BorderWidthZero,
                                    color = if (isLightSelected) DraftoTheme.colors.accent else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(ThemeOptionCircleInnerSize)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                        Spacer(modifier = Modifier.height(ThemeOptionTextSpacing))
                        Text(
                            text = lightText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isLightSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Tema AMOLED
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            onSelectTheme("Amoled")
                            onDismiss()
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(ThemeOptionCircleOuterSize)
                                .clip(CircleShape)
                                .background(Color(0xFF1C1C1E))
                                .border(
                                    width = if (isAmoledSelected) ThemeOptionSelectedBorderWidth else BorderWidthZero,
                                    color = if (isAmoledSelected) DraftoTheme.colors.accent else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(ThemeOptionCircleInnerSize)
                                    .clip(CircleShape)
                                    .background(Color(0xFF000000))
                            )
                        }
                        Spacer(modifier = Modifier.height(ThemeOptionTextSpacing))
                        Text(
                            text = amoledText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isAmoledSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Tema Kraft (Cuaderno Cálido)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            onSelectTheme("Kraft")
                            onDismiss()
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(ThemeOptionCircleOuterSize)
                                .clip(CircleShape)
                                .background(Color(0xFFEDE7DF))
                                .border(
                                    width = if (isKraftSelected) ThemeOptionSelectedBorderWidth else BorderWidthThin,
                                    color = if (isKraftSelected) DraftoTheme.colors.accent else Color(0x331F1C1A),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(ThemeOptionCircleInnerSize)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFBF8F3))
                            )
                        }
                        Spacer(modifier = Modifier.height(ThemeOptionTextSpacing))
                        Text(
                            text = kraftText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isKraftSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(SheetCardGroupSpacing))

            DraftoSheetCancelButton(
                onClick = onDismiss
            )

            Spacer(modifier = Modifier.height(PaddingSmall))
        }
    }
}

/**
 * Modal Bottom Sheet que presenta las notas de lanzamiento de la versión 1.0 de Drafto.
 * Se diseñó siguiendo los estándares visuales de Drafto (Nothing OS), reutilizando
 * los componentes canónicos DraftoSheetCardGroup y DraftoSheetInfoRow para mantener
 * coherencia con el resto de hojas de la aplicación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleaseNotesBottomSheet(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    DraftoModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaddingScreenHorizontal)
                .verticalScroll(rememberScrollState())
        ) {
            DraftoSheetHeader(
                title = stringResource(R.string.about_item_release_notes),
                subtitle = stringResource(R.string.about_version)
            )

            Spacer(modifier = Modifier.height(BookmarkFormSectionSpacing))

            DraftoSheetCardGroup(
                backgroundColor = DraftoTheme.colors.primaryContainer
            ) {
                DraftoSheetInfoRow(
                    title = stringResource(R.string.release_notes_v1_notes_title),
                    description = stringResource(R.string.release_notes_v1_notes_desc),
                    icon = Icons.Rounded.Description,
                    iconTint = DraftoTheme.colors.accent,
                    showDivider = true
                )

                DraftoSheetInfoRow(
                    title = stringResource(R.string.release_notes_v1_todos_title),
                    description = stringResource(R.string.release_notes_v1_todos_desc),
                    icon = Icons.Rounded.CheckCircle,
                    iconTint = DraftoTheme.colors.accent,
                    showDivider = true
                )

                DraftoSheetInfoRow(
                    title = stringResource(R.string.release_notes_v1_bookmarks_title),
                    description = stringResource(R.string.release_notes_v1_bookmarks_desc),
                    icon = Icons.Rounded.Bookmark,
                    iconTint = DraftoTheme.colors.accent,
                    showDivider = true
                )

                DraftoSheetInfoRow(
                    title = stringResource(R.string.release_notes_v1_collections_title),
                    description = stringResource(R.string.release_notes_v1_collections_desc),
                    icon = Icons.Rounded.Folder,
                    iconTint = DraftoTheme.colors.accent,
                    showDivider = true
                )

                DraftoSheetInfoRow(
                    title = stringResource(R.string.release_notes_v1_privacy_title),
                    description = stringResource(R.string.release_notes_v1_privacy_desc),
                    icon = Icons.Rounded.Security,
                    iconTint = DraftoTheme.colors.accent,
                    showDivider = false
                )
            }

            Spacer(modifier = Modifier.height(SheetCardGroupSpacing))

            DraftoSheetCancelButton(
                onClick = onDismiss,
                text = stringResource(R.string.action_close)
            )

            Spacer(modifier = Modifier.height(PaddingSmall))
        }
    }
}
