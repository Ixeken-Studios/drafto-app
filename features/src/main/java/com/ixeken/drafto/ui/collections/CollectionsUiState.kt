package com.ixeken.drafto.ui.collections

import androidx.compose.runtime.Immutable
import com.ixeken.drafto.domain.model.DraftoCollection
import com.ixeken.drafto.domain.model.TabSortOption

/**
 * Estado inmutable de la pantalla de Colecciones Globales.
 *
 * Sigue las directrices de estabilidad del compilador de Compose (/jetpack-compose-performance)
 * para permitir Smart Skipping y animaciones fluidas a 120 FPS.
 *
 * @property collections Lista de colecciones globales con sus conteos agregados (notas, tareas, marcadores).
 * @property isLoading Indica si los datos están siendo leídos o calculados.
 * @property isCreateSheetVisible Controla la visibilidad del modal para crear una nueva colección.
 * @property collectionToEdit Colección seleccionada para edición de metadatos (nombre, color, icono).
 * @property collectionToDelete Colección seleccionada pendiente de confirmación para desvinculación segura.
 * @property searchQuery Consulta de búsqueda activa para filtrar carpetas por nombre.
 * @property isSearchActive Controla si la barra de búsqueda superior está expandida.
 */
@Immutable
data class CollectionsUiState(
    val collections: List<DraftoCollection> = emptyList(),
    val isLoading: Boolean = false,
    val isCreateSheetVisible: Boolean = false,
    val collectionToEdit: DraftoCollection? = null,
    val collectionToDelete: DraftoCollection? = null,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val sortOption: TabSortOption = TabSortOption.NEWEST,
    val showOptionsSheet: Boolean = false,
    val selectedCollectionIds: Set<String> = emptySet(),
    val showDeleteConfirmDialog: Boolean = false
) {
    /**
     * Determina si la pantalla opera actualmente en modo de selección múltiple.
     */
    val isSelectionMode: Boolean
        get() = selectedCollectionIds.isNotEmpty()
}
