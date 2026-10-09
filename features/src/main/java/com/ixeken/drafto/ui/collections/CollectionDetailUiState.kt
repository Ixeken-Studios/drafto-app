package com.ixeken.drafto.ui.collections

import androidx.compose.runtime.Immutable
import com.ixeken.drafto.domain.model.Bookmark
import com.ixeken.drafto.domain.model.DraftoCollection
import com.ixeken.drafto.domain.model.Note
import com.ixeken.drafto.domain.model.TodoItem

/**
 * Pestañas secundarias de contenido dentro de una colección.
 */
enum class CollectionSubTab {
    NOTES,
    TODOS,
    BOOKMARKS
}

/**
 * Acción semántica asignada al botón flotante contextual (+) de la colección.
 */
enum class CollectionFabAction {
    OPEN_TYPE_CHOOSER,
    CREATE_NOTE,
    CREATE_TODO,
    CREATE_BOOKMARK
}

/**
 * Estado inmutable de la pantalla de Detalle de Colección.
 *
 * Sigue las directrices de Compose Compiler Stability (/jetpack-compose-performance)
 * para Smart Skipping y animaciones a 120 FPS.
 */
@Immutable
data class CollectionDetailUiState(
    val collection: DraftoCollection? = null,
    val selectedSubTab: CollectionSubTab = CollectionSubTab.NOTES,
    val activeTabs: List<CollectionSubTab> = emptyList(),
    val effectiveSubTab: CollectionSubTab = CollectionSubTab.NOTES,
    val isCapsuleSelectorVisible: Boolean = false,
    val isCollectionEmpty: Boolean = true,
    val fabAction: CollectionFabAction = CollectionFabAction.OPEN_TYPE_CHOOSER,
    val notes: List<Note> = emptyList(),
    val todos: List<TodoItem> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isAddTodoSheetVisible: Boolean = false,
    val editingTodo: TodoItem? = null,
    val isAddBookmarkSheetVisible: Boolean = false,
    val isEditCollectionSheetVisible: Boolean = false,
    val isTypeChooserSheetVisible: Boolean = false,
    val isLoading: Boolean = false
)
