package com.ixeken.drafto.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ixeken.drafto.core.R
import com.ixeken.drafto.domain.model.TabFilterOption
import com.ixeken.drafto.domain.model.TabSortOption
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.PaddingExtraSmall
import com.ixeken.drafto.ui.theme.PaddingLarge
import com.ixeken.drafto.ui.theme.PaddingMedium
import com.ixeken.drafto.ui.theme.PaddingScreenHorizontal
import com.ixeken.drafto.ui.theme.PoppinsFontFamily

/**
 * Modal Bottom Sheet unificado de opciones para pestañas bajo Nothing OS 5.
 * Centraliza la conmutación de vista (Bento vs Lista), acceso al respaldo, filtrado de elementos
 * sin colección asignada y criterios de ordenación con rendimiento a 120 FPS.
 *
 * Características editoriales Nothing OS:
 * - Cabecera [DraftoSheetHeader] centrada sin subtítulo.
 * - Tarjetas agrupadas [DraftoSheetCardGroup] sobre [DraftoTheme.colors.primaryContainer].
 * - Indicadores circulares [DraftoSelectionCheckmarkIndicator] reactivos a [DraftoTheme.colors.onAccent].
 * - Botón inferior [DraftoSheetCancelButton] en píldora con texto Close localizado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftoTabOptionsBottomSheet(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    isGridView: Boolean? = null,
    onToggleGridView: (() -> Unit)? = null,
    onBackupClick: (() -> Unit)? = null,
    selectedFilter: TabFilterOption? = null,
    onFilterChange: ((TabFilterOption) -> Unit)? = null,
    selectedSort: TabSortOption? = null,
    onSortChange: ((TabSortOption) -> Unit)? = null
) {
    DraftoModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaddingScreenHorizontal)
                .verticalScroll(rememberScrollState())
        ) {
            // Cabecera canónica Nothing OS sin subtítulo
            DraftoSheetHeader(
                title = title,
                subtitle = null
            )

            Spacer(modifier = Modifier.height(PaddingMedium))

            // 1. Grupo de Acciones Rápidas (Vista y Respaldo)
            val hasViewMode = isGridView != null && onToggleGridView != null
            val hasBackup = onBackupClick != null
            if (hasViewMode || hasBackup) {
                DraftoSheetCardGroup(
                    backgroundColor = DraftoTheme.colors.primaryContainer
                ) {
                    if (hasViewMode) {
                        DraftoSheetOptionRow(
                            title = stringResource(R.string.tab_options_header_view_mode),
                            subtitle = stringResource(if (isGridView == true) R.string.view_mode_grid else R.string.view_mode_list),
                            icon = if (isGridView == true) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                            showDivider = hasBackup,
                            onClick = { onToggleGridView?.invoke() }
                        )
                    }
                    if (hasBackup) {
                        DraftoSheetOptionRow(
                            title = stringResource(R.string.tab_options_backup),
                            subtitle = stringResource(R.string.tab_options_backup_desc),
                            icon = Icons.Rounded.Cloud,
                            showDivider = false,
                            onClick = {
                                onDismissRequest()
                                onBackupClick?.invoke()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(PaddingMedium))
            }

            // 2. Grupo de Filtrado por Colección
            if (selectedFilter != null && onFilterChange != null) {
                Text(
                    text = stringResource(R.string.tab_options_header_filter),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = PoppinsFontFamily,
                    color = DraftoTheme.colors.textMuted,
                    modifier = Modifier.padding(start = PaddingExtraSmall, bottom = PaddingExtraSmall)
                )

                DraftoSheetCardGroup(
                    backgroundColor = DraftoTheme.colors.primaryContainer
                ) {
                    DraftoSheetOptionRow(
                        title = stringResource(R.string.tab_options_filter_all),
                        icon = Icons.Rounded.Folder,
                        showDivider = true,
                        trailingContent = {
                            DraftoSelectionCheckmarkIndicator(
                                isSelected = selectedFilter == TabFilterOption.ALL
                            )
                        },
                        onClick = { onFilterChange(TabFilterOption.ALL) }
                    )

                    DraftoSheetOptionRow(
                        title = stringResource(R.string.tab_options_filter_without_collection),
                        subtitle = stringResource(R.string.tab_options_filter_without_collection_desc),
                        icon = Icons.Rounded.FolderOff,
                        showDivider = false,
                        trailingContent = {
                            DraftoSelectionCheckmarkIndicator(
                                isSelected = selectedFilter == TabFilterOption.WITHOUT_COLLECTION
                            )
                        },
                        onClick = { onFilterChange(TabFilterOption.WITHOUT_COLLECTION) }
                    )
                }

                Spacer(modifier = Modifier.height(PaddingMedium))
            }

            // 3. Grupo de Criterios de Ordenación
            if (selectedSort != null && onSortChange != null) {
                Text(
                    text = stringResource(R.string.tab_options_header_sort),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = PoppinsFontFamily,
                    color = DraftoTheme.colors.textMuted,
                    modifier = Modifier.padding(start = PaddingExtraSmall, bottom = PaddingExtraSmall)
                )

                DraftoSheetCardGroup(
                    backgroundColor = DraftoTheme.colors.primaryContainer
                ) {
                    DraftoSheetOptionRow(
                        title = stringResource(R.string.tab_options_sort_newest),
                        icon = Icons.Rounded.Schedule,
                        showDivider = true,
                        trailingContent = {
                            DraftoSelectionCheckmarkIndicator(
                                isSelected = selectedSort == TabSortOption.NEWEST
                            )
                        },
                        onClick = { onSortChange(TabSortOption.NEWEST) }
                    )

                    DraftoSheetOptionRow(
                        title = stringResource(R.string.tab_options_sort_oldest),
                        icon = Icons.Rounded.History,
                        showDivider = true,
                        trailingContent = {
                            DraftoSelectionCheckmarkIndicator(
                                isSelected = selectedSort == TabSortOption.OLDEST
                            )
                        },
                        onClick = { onSortChange(TabSortOption.OLDEST) }
                    )

                    DraftoSheetOptionRow(
                        title = stringResource(R.string.tab_options_sort_alphabetical),
                        icon = Icons.AutoMirrored.Rounded.Sort,
                        showDivider = false,
                        trailingContent = {
                            DraftoSelectionCheckmarkIndicator(
                                isSelected = selectedSort == TabSortOption.ALPHABETICAL
                            )
                        },
                        onClick = { onSortChange(TabSortOption.ALPHABETICAL) }
                    )
                }

                Spacer(modifier = Modifier.height(PaddingLarge))
            }

            // 4. Botón inferior Close Nothing OS
            DraftoSheetCancelButton(
                text = stringResource(R.string.action_close),
                onClick = onDismissRequest
            )

            Spacer(modifier = Modifier.height(PaddingMedium))
        }
    }
}
