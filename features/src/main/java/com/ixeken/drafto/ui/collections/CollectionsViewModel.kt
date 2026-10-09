package com.ixeken.drafto.ui.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ixeken.drafto.domain.model.DraftoCollection
import com.ixeken.drafto.domain.model.TabSortOption
import com.ixeken.drafto.domain.repository.CollectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Flags de UI locales agrupados para simplificar el flujo reactivo sin sobrecargar combinaciones.
 */
private data class LocalUiFlags(
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isCreateSheetVisible: Boolean = false,
    val collectionToEdit: DraftoCollection? = null,
    val collectionToDelete: DraftoCollection? = null,
    val selectedCollectionIds: Set<String> = emptySet(),
    val showDeleteConfirmDialog: Boolean = false,
    val sortOption: TabSortOption = TabSortOption.NEWEST,
    val showOptionsSheet: Boolean = false
)

/**
 * ViewModel reactivo que orquesta el estado de la pantalla de Colecciones Globales.
 *
 * Mantiene la regla UDF (Unidirectional Data Flow) combinando el flujo reactivo de Room
 * con el estado efímero de UI (búsquedas, modales de edición/borrado) sin recomposiciones
 * innecesarias, despachando operaciones de base de datos en [Dispatchers.IO].
 */
@HiltViewModel
class CollectionsViewModel @Inject constructor(
    private val collectionRepository: CollectionRepository
) : ViewModel() {

    private val _flags = MutableStateFlow(LocalUiFlags())

    val uiState: StateFlow<CollectionsUiState> = combine(
        collectionRepository.getCollections().flowOn(Dispatchers.IO),
        _flags
    ) { collections, flags ->
        var filteredCollections = if (flags.searchQuery.isBlank()) {
            collections
        } else {
            val q = flags.searchQuery.trim().lowercase()
            collections.filter { it.name.lowercase().contains(q) }
        }

        filteredCollections = when (flags.sortOption) {
            TabSortOption.NEWEST -> filteredCollections.sortedByDescending { it.createdAt }
            TabSortOption.OLDEST -> filteredCollections.sortedBy { it.createdAt }
            TabSortOption.ALPHABETICAL -> filteredCollections.sortedBy { it.name.lowercase() }
        }

        CollectionsUiState(
            collections = filteredCollections,
            isLoading = false,
            isCreateSheetVisible = flags.isCreateSheetVisible,
            collectionToEdit = flags.collectionToEdit,
            collectionToDelete = flags.collectionToDelete,
            searchQuery = flags.searchQuery,
            isSearchActive = flags.isSearchActive,
            sortOption = flags.sortOption,
            showOptionsSheet = flags.showOptionsSheet,
            selectedCollectionIds = flags.selectedCollectionIds,
            showDeleteConfirmDialog = flags.showDeleteConfirmDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CollectionsUiState(isLoading = true)
    )

    fun setSortOption(option: TabSortOption) {
        _flags.update { it.copy(sortOption = option) }
    }

    fun setShowOptionsSheet(show: Boolean) {
        _flags.update { it.copy(showOptionsSheet = show) }
    }

    fun openCreateSheet() {
        _flags.update { it.copy(isCreateSheetVisible = true, collectionToEdit = null) }
    }

    fun openEditSheet(collection: DraftoCollection) {
        _flags.update { it.copy(isCreateSheetVisible = true, collectionToEdit = collection) }
    }

    fun dismissSheet() {
        _flags.update { it.copy(isCreateSheetVisible = false, collectionToEdit = null) }
    }

    fun saveCollection(name: String, colorHex: String, iconName: String) {
        val currentEdit = _flags.value.collectionToEdit
        viewModelScope.launch(Dispatchers.IO) {
            if (currentEdit != null) {
                collectionRepository.updateCollection(
                    currentEdit.copy(
                        name = name.trim(),
                        colorHex = colorHex,
                        iconName = iconName
                    )
                )
            } else {
                val newCollection = DraftoCollection(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    colorHex = colorHex,
                    iconName = iconName,
                    createdAt = System.currentTimeMillis()
                )
                collectionRepository.saveCollection(newCollection)
            }
            dismissSheet()
        }
    }

    fun requestDeleteCollection(collection: DraftoCollection) {
        _flags.update { it.copy(collectionToDelete = collection) }
    }

    fun dismissDeleteDialog() {
        _flags.update { it.copy(collectionToDelete = null) }
    }

    fun confirmDeleteCollection() {
        val target = _flags.value.collectionToDelete ?: return
        viewModelScope.launch(Dispatchers.IO) {
            collectionRepository.deleteCollection(target.id)
            _flags.update { it.copy(collectionToDelete = null) }
        }
    }

    fun setSearchQuery(query: String) {
        _flags.update { it.copy(searchQuery = query) }
    }

    fun toggleSearch(active: Boolean? = null) {
        _flags.update {
            val nextActive = active ?: !it.isSearchActive
            it.copy(
                isSearchActive = nextActive,
                searchQuery = if (!nextActive) "" else it.searchQuery
            )
        }
    }

    /**
     * Alterna la selección de una colección en modo multiselección.
     */
    fun toggleSelection(collectionId: String) {
        _flags.update { current ->
            val newSelection = if (current.selectedCollectionIds.contains(collectionId)) {
                current.selectedCollectionIds - collectionId
            } else {
                current.selectedCollectionIds + collectionId
            }
            current.copy(selectedCollectionIds = newSelection)
        }
    }

    /**
     * Limpia la selección múltiple activa de colecciones.
     */
    fun clearSelection() {
        _flags.update { it.copy(selectedCollectionIds = emptySet(), showDeleteConfirmDialog = false) }
    }

    /**
     * Controla la visibilidad del diálogo de confirmación para eliminar las colecciones seleccionadas.
     */
    fun setShowDeleteConfirmDialog(show: Boolean) {
        _flags.update { it.copy(showDeleteConfirmDialog = show) }
    }

    /**
     * Elimina en lote todas las colecciones seleccionadas en la base de datos de forma segura.
     */
    fun deleteSelectedCollections() {
        val toDelete = _flags.value.selectedCollectionIds
        if (toDelete.isEmpty()) {
            _flags.update { it.copy(showDeleteConfirmDialog = false) }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            toDelete.forEach { id ->
                collectionRepository.deleteCollection(id)
            }
            clearSelection()
        }
    }
}
