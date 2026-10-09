package com.ixeken.drafto.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ixeken.drafto.core.R
import com.ixeken.drafto.domain.model.MasterRestoreMode
import com.ixeken.drafto.domain.model.StorageMetrics
import com.ixeken.drafto.ui.components.DraftoConfirmationDialog
import com.ixeken.drafto.ui.components.DraftoMasterRestoreModeBottomSheet
import com.ixeken.drafto.ui.components.DraftoProgressDialog
import com.ixeken.drafto.ui.components.DraftoSecondaryTopBar
import com.ixeken.drafto.ui.theme.*
import dev.chrisbanes.haze.hazeSource

/**
 * Pantalla integral de Datos y Almacenamiento (Data & Storage) en los Ajustes de Drafto.
 *
 * Módulos funcionales integrados:
 * 1. Storage Overview: Muestra el desglose de Base de Datos, Multimedia y Caché en disco con acción "Clear cache".
 * 2. Master Backup: Generación y restauración de paquetes completos `.zip` con SAF.
 * 3. Danger Zone: Borrado total de fábrica (Factory Wipe) con diálogo de seguridad y temporizador forzado de 3 segundos.
 *
 * Cumple con el estándar de rendimiento a 120 FPS mediante lectura diferida de scroll (`derivedStateOf`),
 * reutilización modular de componentes `:core` y desenfoque esmerilado Pure Blur Ultra Thin de Haze.
 *
 * @param uiState Estado inmutable con las métricas y banderas de diálogo.
 * @param onBack Callback de navegación para regresar a la pantalla previa.
 * @param onClearCache Callback para vaciar la memoria temporal en disco.
 * @param onExportMasterBackup Callback para escribir el archivo .zip en el destino provisto por SAF.
 * @param onRestoreUriSelected Callback cuando el usuario selecciona un archivo .zip para restaurar.
 * @param onRestoreMasterBackup Callback para ejecutar la restauración con el modo elegido (Merge o Clean Restore).
 * @param onDismissRestoreModeSheet Callback para cancelar el selector modal de estrategia de restauración.
 * @param onOpenWipeConfirmDialog Callback para desplegar el diálogo destructivo con cuenta regresiva.
 * @param onDismissWipeConfirmDialog Callback para cancelar el borrado de fábrica.
 * @param onWipeAllData Callback para ejecutar la purga total de la base de datos y archivos locales.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataStorageScreen(
    uiState: DataStorageUiState,
    onBack: () -> Unit,
    onClearCache: () -> Unit,
    onExportMasterBackup: (Uri) -> Unit,
    onRestoreUriSelected: (Uri) -> Unit,
    onRestoreMasterBackup: (Uri, MasterRestoreMode) -> Unit,
    onDismissRestoreModeSheet: () -> Unit,
    onOpenWipeConfirmDialog: () -> Unit,
    onDismissWipeConfirmDialog: () -> Unit,
    onWipeAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var headerHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val headerHeightDp = with(density) { headerHeightPx.toDp() }
    val safeTopPadding = if (headerHeightDp > 0.dp) headerHeightDp + PaddingSmall else 90.dp

    val hazeState = LocalHazeState.current
    val scrollState = rememberScrollState()
    val isScrolled by remember { derivedStateOf { scrollState.value > 0 } }

    // SAF Launchers para exportación e importación
    val exportMasterBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            onExportMasterBackup(uri)
        }
    }

    val importMasterBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onRestoreUriSelected(uri)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier)
                .verticalScroll(scrollState)
                .padding(horizontal = PaddingScreenHorizontal)
        ) {
            Spacer(modifier = Modifier.height(safeTopPadding))

            // Tarjeta 1: Storage Overview (Métricas)
            StorageOverviewCard(
                metrics = uiState.storageMetrics,
                isLoading = uiState.isLoadingMetrics,
                onClearCache = onClearCache
            )

            Spacer(modifier = Modifier.height(SettingsGroupSpacing))

            // Tarjeta 2: Master Backup (.zip)
            MasterBackupCard(
                onExportClick = {
                    val defaultFileName = "drafto_master_backup_${System.currentTimeMillis()}.zip"
                    exportMasterBackupLauncher.launch(defaultFileName)
                },
                onImportClick = {
                    importMasterBackupLauncher.launch(
                        arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream", "*/*")
                    )
                }
            )

            Spacer(modifier = Modifier.height(SettingsGroupSpacing))

            // Tarjeta 3: Danger Zone (Factory Wipe)
            DangerZoneCard(
                onWipeClick = onOpenWipeConfirmDialog
            )

            Spacer(modifier = Modifier.height(PaddingListBottomContent))
        }

        // TopBar adhesiva con efecto Pure Blur Ultra Thin
        DraftoSecondaryTopBar(
            title = stringResource(R.string.settings_item_data_storage_title),
            onBack = onBack,
            isScrolled = isScrolled,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onGloballyPositioned { headerHeightPx = it.size.height }
        )
    }

    // Modal para seleccionar estrategia de restauración (Merge vs Clean Restore)
    if (uiState.showRestoreModeSheet && uiState.selectedRestoreUri != null) {
        DraftoMasterRestoreModeBottomSheet(
            onDismissRequest = onDismissRestoreModeSheet,
            onSelectMode = { mode ->
                onRestoreMasterBackup(uiState.selectedRestoreUri, mode)
            }
        )
    }

    // Diálogo de confirmación destructiva para Factory Wipe con temporizador de 3 segundos
    if (uiState.showWipeConfirmDialog) {
        DraftoConfirmationDialog(
            title = stringResource(R.string.wipe_dialog_title),
            message = stringResource(R.string.wipe_dialog_message),
            icon = Icons.Rounded.Warning,
            countdownSeconds = 3,
            confirmText = stringResource(R.string.wipe_dialog_confirm_ready),
            cancelText = stringResource(R.string.action_cancel),
            isDestructive = true,
            onConfirm = onWipeAllData,
            onDismiss = onDismissWipeConfirmDialog
        )
    }

    // Diálogo de progreso para exportación o restauración en streaming
    uiState.progressState?.let { progressState ->
        if (progressState.isActive) {
            DraftoProgressDialog(
                title = if (progressState.isExport) {
                    stringResource(R.string.dialog_exporting_master_backup)
                } else {
                    stringResource(R.string.dialog_importing_master_backup)
                },
                statusMessage = progressState.statusText,
                progress = progressState.progress,
                isExport = progressState.isExport
            )
        }
    }

    // Indicador modal mientras se ejecuta la purga de datos
    if (uiState.isWipingData) {
        DraftoProgressDialog(
            title = stringResource(R.string.action_wipe_all_data),
            statusMessage = stringResource(R.string.wipe_dialog_message),
            progress = 0.5f,
            isExport = false
        )
    }
}

/**
 * Tarjeta de desglose de métricas de almacenamiento físico con barra segmentada Nothing OS y acción de caché.
 *
 * Sigue la estética minimalista de Nothing OS 5: superficie plana monocromática, tipografía editorial Fraunces
 * para el valor cuantitativo total, barra de distribución proporcional continua y filas agrupadas con divisores
 * ultrafinos de 1px ([BorderWidthThin]), sin contenedores artificiales alrededor de los iconos.
 */
@Composable
private fun StorageOverviewCard(
    metrics: StorageMetrics,
    isLoading: Boolean,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(DraftoShapeCard)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Cabecera interna: Métrica acumulada y barra de distribución
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SettingsItemPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.storage_metrics_section_title),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(PaddingExtraSmall))
                    Text(
                        text = StorageMetrics.formatBytes(metrics.totalBytes),
                        fontFamily = FrauncesFontFamily,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(SettingsItemIconSize),
                        strokeWidth = 2.dp,
                        color = DraftoTheme.colors.accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(PaddingMedium))

            // Barra segmentada proporcional (Nothing OS Storage Bar)
            val totalBytesSafe = metrics.totalBytes.coerceAtLeast(1L).toFloat()
            val dbRatio = (metrics.databaseBytes.toFloat() / totalBytesSafe).coerceIn(0f, 1f)
            val mediaRatio = (metrics.mediaBytes.toFloat() / totalBytesSafe).coerceIn(0f, 1f)
            val cacheRatio = (metrics.cacheBytes.toFloat() / totalBytesSafe).coerceIn(0f, 1f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(StorageProgressBarHeight)
                    .clip(DraftoShapePill)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                horizontalArrangement = Arrangement.spacedBy(StorageProgressBarSegmentSpacing)
            ) {
                if (dbRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(dbRatio)
                            .fillMaxHeight()
                            .clip(DraftoShapePill)
                            .background(DraftoTheme.colors.accent)
                    )
                }
                if (mediaRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(mediaRatio)
                            .fillMaxHeight()
                            .clip(DraftoShapePill)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f))
                    )
                }
                if (cacheRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(cacheRatio)
                            .fillMaxHeight()
                            .clip(DraftoShapePill)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                    )
                }
                if (metrics.totalBytes <= 0L) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(DraftoShapePill)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    )
                }
            }
        }

        // Divisor ultrafino de 1px
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BorderWidthThin)
                .background(MaterialTheme.colorScheme.outline)
        )

        // Fila 1: Base de datos (Icono plano desnudo)
        StorageMetricRow(
            icon = Icons.Rounded.Storage,
            label = stringResource(R.string.storage_metric_database),
            value = StorageMetrics.formatBytes(metrics.databaseBytes)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BorderWidthThin)
                .background(MaterialTheme.colorScheme.outline)
        )

        // Fila 2: Archivos Multimedia (Icono plano desnudo)
        StorageMetricRow(
            icon = Icons.Rounded.PermMedia,
            label = stringResource(R.string.storage_metric_media),
            value = StorageMetrics.formatBytes(metrics.mediaBytes)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BorderWidthThin)
                .background(MaterialTheme.colorScheme.outline)
        )

        // Fila 3: Caché temporal con acción integrada (Icono plano desnudo)
        StorageMetricRow(
            icon = Icons.Rounded.CleaningServices,
            label = stringResource(R.string.storage_metric_cache),
            value = StorageMetrics.formatBytes(metrics.cacheBytes)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BorderWidthThin)
                .background(MaterialTheme.colorScheme.outline)
        )

        // Fila 4: Acción de vaciado de caché
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClearCache)
                .padding(SettingsItemPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.DeleteSweep,
                contentDescription = null,
                tint = DraftoTheme.colors.accent,
                modifier = Modifier.size(SettingsItemIconSize)
            )
            Spacer(modifier = Modifier.width(PaddingLarge))
            Text(
                text = stringResource(R.string.storage_action_clear_cache),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = DraftoTheme.colors.accent
            )
        }
    }
}

/**
 * Fila individual para métricas de almacenamiento con icono plano desnudo y divisor Nothing OS.
 */
@Composable
private fun StorageMetricRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(SettingsItemPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(SettingsItemIconSize)
            )
            Spacer(modifier = Modifier.width(PaddingLarge))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Tarjeta interactiva para exportación e importación del Respaldo Maestro (.zip).
 *
 * Estructura idéntica a las tarjetas de Ajustes Nothing OS:
 * - Fila 1: Opción de exportación SAF con icono plano [Icons.Rounded.FolderZip] y chevron derecho.
 * - Divisor de 1px.
 * - Fila 2: Opción de importación SAF con icono plano [Icons.Rounded.CloudDownload] y chevron derecho.
 */
@Composable
private fun MasterBackupCard(
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(DraftoShapeCard)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Fila 1: Exportar Respaldo Maestro
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onExportClick)
                .padding(SettingsItemPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.FolderZip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(SettingsItemIconSize)
                )
                Spacer(modifier = Modifier.width(PaddingLarge))
                Column {
                    Text(
                        text = stringResource(R.string.action_export_master_backup),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(PaddingMicro))
                    Text(
                        text = stringResource(R.string.master_backup_section_desc),
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

        // Fila 2: Importar Respaldo Maestro
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onImportClick)
                .padding(SettingsItemPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.CloudDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(SettingsItemIconSize)
                )
                Spacer(modifier = Modifier.width(PaddingLarge))
                Column {
                    Text(
                        text = stringResource(R.string.action_import_master_backup),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(PaddingMicro))
                    Text(
                        text = stringResource(R.string.import_type_master_backup_desc),
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
}

/**
 * Tarjeta sobria para el borrado total de fábrica (Factory Wipe).
 *
 * Mantiene la superficie monocromática de Nothing OS 5 sin fondos rojos alarmistas en reposo.
 * Al interactuar, abre el diálogo destructivo modal seguro con temporizador forzado de 3 segundos.
 */
@Composable
private fun DangerZoneCard(
    onWipeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(DraftoShapeCard)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onWipeClick)
                .padding(SettingsItemPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(SettingsItemIconSize)
                )
                Spacer(modifier = Modifier.width(PaddingLarge))
                Column {
                    Text(
                        text = stringResource(R.string.action_wipe_all_data),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(PaddingMicro))
                    Text(
                        text = stringResource(R.string.danger_zone_section_desc),
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
}


