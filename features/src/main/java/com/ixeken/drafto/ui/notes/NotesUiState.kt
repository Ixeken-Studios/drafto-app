package com.ixeken.drafto.ui.notes

import androidx.compose.runtime.Immutable
import com.ixeken.drafto.data.backup.NotebookBackupProgressState
import com.ixeken.drafto.domain.model.DraftoCollection
import com.ixeken.drafto.domain.model.Note
import com.ixeken.drafto.domain.model.TabFilterOption
import com.ixeken.drafto.domain.model.TabSortOption
import com.ixeken.drafto.domain.model.TodoFilter
import com.ixeken.drafto.domain.model.TodoItem

/**
 * Secciones primarias que componen la experiencia unificada de Notebook.
 */
enum class NotebookSection {
    NOTES,
    TODOS
}

/**
 * Estado inmutable de interfaz gráfica para la pantalla unificada de Notebook.
 *
 * Consolida en una sola estructura reactiva el flujo de notas y tareas pendientes,
 * permitiendo transiciones fluidas de 120 FPS y Smart Skipping en Jetpack Compose.
 */
@Immutable
data class NotesUiState(
    val isLoading: Boolean = false,
    val notes: List<Note> = emptyList(),
    val todos: List<TodoItem> = emptyList(),
    val filteredNotes: List<Note> = emptyList(),
    val filteredTodos: List<TodoItem> = emptyList(),
    val availableCollections: List<DraftoCollection> = emptyList(),
    val selectedTodoFilter: TodoFilter = TodoFilter.ALL,
    val showAddTodoSheet: Boolean = false,
    val editingTodo: TodoItem? = null,
    val selectedSection: NotebookSection = NotebookSection.NOTES,
    val isGridView: Boolean = false,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val filterOption: TabFilterOption = TabFilterOption.ALL,
    val sortOption: TabSortOption = TabSortOption.NEWEST,
    val showOptionsSheet: Boolean = false,
    val error: String? = null,
    val backupProgressState: NotebookBackupProgressState? = null,
    val showBackupSheet: Boolean = false,
    val selectedNoteIds: Set<String> = emptySet(),
    val showDeleteSelectedNotesDialog: Boolean = false,
    val selectedTodoIds: Set<String> = emptySet(),
    val showDeleteSelectedTodosDialog: Boolean = false
) {
    /**
     * Determina si la pestaña de notas opera en modo de selección múltiple.
     */
    val isNotesSelectionMode: Boolean
        get() = selectedSection == NotebookSection.NOTES && selectedNoteIds.isNotEmpty()

    /**
     * Determina si la pestaña de tareas to-do opera en modo de selección múltiple.
     */
    val isTodosSelectionMode: Boolean
        get() = selectedSection == NotebookSection.TODOS && selectedTodoIds.isNotEmpty()
}


