package com.ixeken.drafto.ui.collections

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ixeken.drafto.core.R
import com.ixeken.drafto.domain.model.DraftoCollection
import com.ixeken.drafto.domain.model.TabSortOption
import com.ixeken.drafto.ui.components.DraftoCollectionFormBottomSheet
import com.ixeken.drafto.ui.components.DraftoConfirmationDialog
import com.ixeken.drafto.ui.components.DraftoEmptyState
import com.ixeken.drafto.ui.components.DraftoFolderCard
import com.ixeken.drafto.ui.components.DraftoSearchableTopBar
import com.ixeken.drafto.ui.components.DraftoStickyScaffold
import com.ixeken.drafto.ui.components.DraftoTabOptionsBottomSheet
import com.ixeken.drafto.ui.components.DraftoTopBarIconButton
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.FolderGridMinSize
import com.ixeken.drafto.ui.theme.PaddingListBottomContent
import com.ixeken.drafto.ui.theme.PaddingMedium
import com.ixeken.drafto.ui.theme.PaddingScreenHorizontal
import com.ixeken.drafto.ui.theme.PaddingSmall

/**
 * Pantalla principal de Colecciones Globales (Pestaña 0 del Bottom Navigation).
 *
 * Muestra las carpetas 3D que agrupan notas, to-dos y marcadores de manera unificada.
 * Diseñada para 120 FPS siguiendo las pautas de /jetpack-compose-performance:
 * - LazyVerticalGrid con 2 columnas, claves persistentes (`key = { it.id }`) y `contentType`.
 * - Estado de scroll derivado con `derivedStateOf` para el desenfoque progresivo del encabezado.
 * - Encapsulación estructural a través de [DraftoStickyScaffold] sin números mágicos ni bucles de recomposición.
 *
 * @param uiState Estado inmutable de la pantalla.
 * @param onOpenCollection Callback al presionar una carpeta para abrir su vista detallada.
 * @param onOpenCreateSheet Callback para abrir el modal de creación de carpeta.
 * @param onOpenEditSheet Callback para abrir el modal de edición de una carpeta existente.
 * @param onDismissSheet Callback para cerrar el modal de creación/edición.
 * @param onSaveCollection Callback para persistir una carpeta (creación o actualización).
 * @param onRequestDelete Callback para solicitar confirmación de borrado seguro de una carpeta.
 * @param onDismissDeleteDialog Callback para cancelar el diálogo de borrado.
 * @param onConfirmDelete Callback para confirmar la desvinculación y eliminación de la carpeta.
 * @param onSearchQueryChange Callback para actualizar la consulta de búsqueda.
 * @param onToggleSearch Callback para alternar la visibilidad de la barra de búsqueda.
 * @param modifier Modificador Compose opcional.
 */
@Composable
fun CollectionsScreen(
    uiState: CollectionsUiState,
    onOpenCollection: (collectionId: String) -> Unit,
    onOpenCreateSheet: () -> Unit,
    onOpenEditSheet: (DraftoCollection) -> Unit,
    onDismissSheet: () -> Unit,
    onSaveCollection: (name: String, colorHex: String, iconName: String) -> Unit,
    onRequestDelete: (DraftoCollection) -> Unit,
    onDismissDeleteDialog: () -> Unit,
    onConfirmDelete: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: (Boolean?) -> Unit,
    onToggleSelection: (String) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onConfirmDeleteSelected: () -> Unit = {},
    onDismissDeleteConfirmDialog: () -> Unit = {},
    onShowOptionsSheet: (Boolean) -> Unit = {},
    onSetSortOption: (TabSortOption) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = uiState.isSelectionMode || uiState.isSearchActive) {
        if (uiState.isSelectionMode) {
            onClearSelection()
        } else {
            onToggleSearch(false)
        }
    }

    val gridState = rememberLazyGridState()
    val isScrolled by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0
        }
    }

    DraftoStickyScaffold(
        isScrolled = isScrolled,
        header = {
            DraftoSearchableTopBar(
                title = stringResource(R.string.collections_title),
                isSearchActive = uiState.isSearchActive,
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onCloseSearch = { onToggleSearch(false) },
                actions = {
                    if (!uiState.isSelectionMode) {
                        DraftoTopBarIconButton(
                            imageVector = Icons.Rounded.CreateNewFolder,
                            contentDescription = stringResource(R.string.action_new_collection),
                            onClick = onOpenCreateSheet
                        )
                        DraftoTopBarIconButton(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = stringResource(R.string.action_search),
                            onClick = { onToggleSearch(true) }
                        )
                        DraftoTopBarIconButton(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.tab_options_title_collections),
                            onClick = { onShowOptionsSheet(true) }
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { safeTopPadding ->
        if (uiState.collections.isEmpty()) {
            val emptyTitle = if (uiState.searchQuery.isNotBlank()) stringResource(R.string.empty_search_title) else stringResource(R.string.collections_empty_title)
            val emptyDesc = if (uiState.searchQuery.isNotBlank()) stringResource(R.string.empty_search_description) else stringResource(R.string.collections_empty_desc)
            val emptyIcon = if (uiState.searchQuery.isNotBlank()) Icons.Rounded.Search else Icons.Rounded.Folder

            DraftoEmptyState(
                icon = emptyIcon,
                iconTint = DraftoTheme.colors.accent,
                title = emptyTitle,
                description = emptyDesc,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = safeTopPadding)
            )
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = FolderGridMinSize),
                contentPadding = PaddingValues(
                    start = PaddingScreenHorizontal,
                    top = safeTopPadding,
                    end = PaddingScreenHorizontal,
                    bottom = PaddingListBottomContent
                ),
                horizontalArrangement = Arrangement.spacedBy(PaddingMedium),
                verticalArrangement = Arrangement.spacedBy(PaddingMedium),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = uiState.collections,
                    key = { it.id },
                    contentType = { "folder_card" }
                ) { collection ->
                    DraftoFolderCard(
                        collection = collection,
                        bookmarkCount = collection.totalCount,
                        onClick = {
                            if (uiState.isSelectionMode) {
                                onToggleSelection(collection.id)
                            } else {
                                onOpenCollection(collection.id)
                            }
                        },
                        onEdit = { onOpenEditSheet(collection) },
                        onExport = { /* Exportación unificada reservada */ },
                        onDelete = { onRequestDelete(collection) },
                        isSelected = collection.id in uiState.selectedCollectionIds,
                        isSelectionMode = uiState.isSelectionMode,
                        onLongClick = { onToggleSelection(collection.id) }
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet para Crear / Editar Colección
    if (uiState.isCreateSheetVisible) {
        DraftoCollectionFormBottomSheet(
            initialCollection = uiState.collectionToEdit,
            onDismiss = onDismissSheet,
            onSaveCollection = onSaveCollection
        )
    }

    // Diálogo de confirmación para borrado seguro individual (desvinculación sin perder items)
    val deleteTarget = uiState.collectionToDelete
    if (deleteTarget != null) {
        DraftoConfirmationDialog(
            title = stringResource(R.string.dialog_delete_collection_title),
            message = stringResource(R.string.dialog_delete_collection_confirm_message, deleteTarget.name),
            confirmText = stringResource(R.string.action_delete),
            isDestructive = true,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDeleteDialog
        )
    }

    // Diálogo de confirmación para borrado múltiple de colecciones seleccionadas
    if (uiState.showDeleteConfirmDialog) {
        val count = uiState.selectedCollectionIds.size
        DraftoConfirmationDialog(
            title = stringResource(R.string.dialog_delete_selected_collections_title),
            message = stringResource(R.string.dialog_delete_selected_collections_message, count),
            confirmText = stringResource(R.string.action_delete),
            isDestructive = true,
            onConfirm = onConfirmDeleteSelected,
            onDismiss = onDismissDeleteConfirmDialog
        )
    }

    // Modal Bottom Sheet para Opciones de Pestaña (Ordenar por)
    if (uiState.showOptionsSheet) {
        DraftoTabOptionsBottomSheet(
            title = stringResource(R.string.tab_options_title_collections),
            selectedSort = uiState.sortOption,
            onSortChange = onSetSortOption,
            onDismissRequest = { onShowOptionsSheet(false) }
        )
    }
}
