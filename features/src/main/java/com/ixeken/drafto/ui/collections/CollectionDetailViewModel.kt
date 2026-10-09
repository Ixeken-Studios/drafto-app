package com.ixeken.drafto.ui.collections

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ixeken.drafto.domain.model.Bookmark
import com.ixeken.drafto.domain.model.DraftoCollection
import com.ixeken.drafto.domain.model.Note
import com.ixeken.drafto.domain.model.TodoItem
import com.ixeken.drafto.domain.repository.BookmarkRepository
import com.ixeken.drafto.domain.repository.CollectionRepository
import com.ixeken.drafto.domain.repository.NoteRepository
import com.ixeken.drafto.domain.repository.TodoRepository
import com.ixeken.drafto.util.LinkMetadataExtractor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Agrupador interno de flags de UI para evitar sobrecargar combinaciones de flujos.
 */
private data class DetailLocalFlags(
    val selectedSubTab: CollectionSubTab = CollectionSubTab.NOTES,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isAddTodoSheetVisible: Boolean = false,
    val editingTodo: TodoItem? = null,
    val isAddBookmarkSheetVisible: Boolean = false,
    val isEditCollectionSheetVisible: Boolean = false,
    val isTypeChooserSheetVisible: Boolean = false
)

/**
 * ViewModel que orquesta la vista detallada de una colección unificada.
 *
 * Muestra el contenido filtrado por la clave foránea `collectionId` directa en SQLite,
 * despachando todas las operaciones de persistencia en [Dispatchers.IO].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CollectionDetailViewModel @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val noteRepository: NoteRepository,
    private val todoRepository: TodoRepository,
    private val bookmarkRepository: BookmarkRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialId: String = savedStateHandle.get<String>("collectionId") ?: ""
    private val _collectionId = MutableStateFlow(initialId)
    private val _flags = MutableStateFlow(DetailLocalFlags())

    val uiState: StateFlow<CollectionDetailUiState> = _collectionId.flatMapLatest { id ->
        if (id.isBlank()) {
            flowOf(CollectionDetailUiState())
        } else {
            val collectionFlow = collectionRepository.getCollectionById(id).flowOn(Dispatchers.IO)
            val notesFlow = noteRepository.getNotesByCollection(id).flowOn(Dispatchers.IO)
            val todosFlow = todoRepository.observeTodosByCollection(id).flowOn(Dispatchers.IO)
            val bookmarksFlow = bookmarkRepository.observeBookmarksByCollection(id).flowOn(Dispatchers.IO)

            val dataFlow = combine(collectionFlow, notesFlow, todosFlow, bookmarksFlow) { col, notes, todos, bookmarks ->
                Tuple4(col, notes, todos, bookmarks)
            }

            combine(dataFlow, _flags) { (col, notes, todos, bookmarks), flags ->
                val query = flags.searchQuery.trim().lowercase()
                val filteredNotes = if (query.isBlank()) notes else notes.filter {
                    it.title.lowercase().contains(query) || it.content.lowercase().contains(query)
                }
                val filteredTodos = if (query.isBlank()) todos else todos.filter {
                    it.title.lowercase().contains(query) || (it.description.lowercase().contains(query))
                }
                val filteredBookmarks = if (query.isBlank()) bookmarks else bookmarks.filter {
                    (it.title?.lowercase()?.contains(query) == true) || it.url.lowercase().contains(query)
                }

                val activeTabs = buildList {
                    if (filteredNotes.isNotEmpty()) add(CollectionSubTab.NOTES)
                    if (filteredTodos.isNotEmpty()) add(CollectionSubTab.TODOS)
                    if (filteredBookmarks.isNotEmpty()) add(CollectionSubTab.BOOKMARKS)
                }

                val effectiveSubTab = when {
                    activeTabs.isEmpty() -> CollectionSubTab.NOTES
                    activeTabs.contains(flags.selectedSubTab) -> flags.selectedSubTab
                    else -> activeTabs.first()
                }

                val isCapsuleSelectorVisible = activeTabs.size >= 2
                val isCollectionEmpty = activeTabs.isEmpty()

                val fabAction = when {
                    isCollectionEmpty -> CollectionFabAction.OPEN_TYPE_CHOOSER
                    activeTabs.size == 1 -> when (activeTabs.first()) {
                        CollectionSubTab.NOTES -> CollectionFabAction.CREATE_NOTE
                        CollectionSubTab.TODOS -> CollectionFabAction.CREATE_TODO
                        CollectionSubTab.BOOKMARKS -> CollectionFabAction.CREATE_BOOKMARK
                    }
                    else -> when (effectiveSubTab) {
                        CollectionSubTab.NOTES -> CollectionFabAction.CREATE_NOTE
                        CollectionSubTab.TODOS -> CollectionFabAction.CREATE_TODO
                        CollectionSubTab.BOOKMARKS -> CollectionFabAction.CREATE_BOOKMARK
                    }
                }

                CollectionDetailUiState(
                    collection = col,
                    selectedSubTab = flags.selectedSubTab,
                    activeTabs = activeTabs,
                    effectiveSubTab = effectiveSubTab,
                    isCapsuleSelectorVisible = isCapsuleSelectorVisible,
                    isCollectionEmpty = isCollectionEmpty,
                    fabAction = fabAction,
                    notes = filteredNotes,
                    todos = filteredTodos,
                    bookmarks = filteredBookmarks,
                    searchQuery = flags.searchQuery,
                    isSearchActive = flags.isSearchActive,
                    isAddTodoSheetVisible = flags.isAddTodoSheetVisible,
                    editingTodo = flags.editingTodo,
                    isAddBookmarkSheetVisible = flags.isAddBookmarkSheetVisible,
                    isEditCollectionSheetVisible = flags.isEditCollectionSheetVisible,
                    isTypeChooserSheetVisible = flags.isTypeChooserSheetVisible,
                    isLoading = false
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CollectionDetailUiState(isLoading = true)
    )

    fun setCollectionId(id: String) {
        _collectionId.value = id
    }

    fun setSubTab(subTab: CollectionSubTab) {
        _flags.update { it.copy(selectedSubTab = subTab) }
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

    fun openAddTodoSheet() {
        _flags.update { it.copy(isAddTodoSheetVisible = true, editingTodo = null) }
    }

    fun openEditTodo(todo: TodoItem) {
        _flags.update { it.copy(isAddTodoSheetVisible = true, editingTodo = todo) }
    }

    fun dismissAddTodoSheet() {
        _flags.update { it.copy(isAddTodoSheetVisible = false, editingTodo = null) }
    }

    fun saveTodo(title: String, description: String?, isPinned: Boolean, dueDate: Long?) {
        saveTodo(title, description, isPinned, dueDate, _flags.value.editingTodo?.subtasks ?: emptyList())
    }

    fun saveTodo(
        title: String,
        description: String?,
        isPinned: Boolean,
        dueDate: Long?,
        subtasks: List<com.ixeken.drafto.domain.model.TodoSubtask>
    ) {
        val editing = _flags.value.editingTodo
        val colId = _collectionId.value
        val safeDesc = description?.trim().orEmpty()
        val allDone = subtasks.isNotEmpty() && subtasks.all { it.isDone }
        viewModelScope.launch(Dispatchers.IO) {
            if (editing != null) {
                todoRepository.saveTodo(
                    editing.copy(
                        title = title.trim(),
                        description = safeDesc,
                        isPinned = isPinned,
                        dueDate = dueDate,
                        collectionId = colId,
                        subtasks = subtasks,
                        isCompleted = if (subtasks.isNotEmpty()) allDone else editing.isCompleted,
                        completedAt = if (subtasks.isNotEmpty() && allDone) System.currentTimeMillis() else editing.completedAt
                    )
                )
            } else {
                todoRepository.saveTodo(
                    TodoItem(
                        id = UUID.randomUUID().toString(),
                        title = title.trim(),
                        description = safeDesc,
                        isCompleted = allDone,
                        isPinned = isPinned,
                        dueDate = dueDate,
                        createdAt = System.currentTimeMillis(),
                        completedAt = if (allDone) System.currentTimeMillis() else null,
                        collectionId = colId,
                        subtasks = subtasks
                    )
                )
            }
            dismissAddTodoSheet()
        }
    }

    fun toggleTodoCompleted(todo: TodoItem, isCompleted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            todoRepository.toggleCompleted(todo.id, isCompleted)
        }
    }

    fun toggleTodoPinned(todo: TodoItem, isPinned: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            todoRepository.togglePinned(todo.id, isPinned)
        }
    }

    fun toggleSubtask(todoId: String, subtaskId: String, isDone: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            todoRepository.toggleSubtask(todoId, subtaskId, isDone)
        }
    }

    fun deleteTodo(todo: TodoItem) {
        viewModelScope.launch(Dispatchers.IO) {
            todoRepository.deleteTodo(todo.id)
        }
    }

    fun toggleNotePinned(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            noteRepository.saveNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            noteRepository.deleteNote(noteId)
        }
    }

    fun toggleBookmarkPin(bookmark: Bookmark) {
        viewModelScope.launch(Dispatchers.IO) {
            bookmarkRepository.updateBookmark(bookmark.copy(isPinned = !bookmark.isPinned))
        }
    }

    fun deleteBookmark(bookmarkId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            bookmarkRepository.deleteBookmark(bookmarkId)
        }
    }

    fun openEditCollectionSheet() {
        _flags.update { it.copy(isEditCollectionSheetVisible = true) }
    }

    fun dismissEditCollectionSheet() {
        _flags.update { it.copy(isEditCollectionSheetVisible = false) }
    }

    fun updateCollection(name: String, colorHex: String, iconName: String) {
        val current = uiState.value.collection ?: return
        viewModelScope.launch(Dispatchers.IO) {
            collectionRepository.updateCollection(
                current.copy(
                    name = name.trim(),
                    colorHex = colorHex,
                    iconName = iconName
                )
            )
            dismissEditCollectionSheet()
        }
    }

    fun openAddBookmarkSheet() {
        _flags.update { it.copy(isAddBookmarkSheetVisible = true) }
    }

    fun dismissAddBookmarkSheet() {
        _flags.update { it.copy(isAddBookmarkSheetVisible = false) }
    }

    fun openTypeChooser() {
        _flags.update { it.copy(isTypeChooserSheetVisible = true) }
    }

    fun dismissTypeChooser() {
        _flags.update { it.copy(isTypeChooserSheetVisible = false) }
    }

    fun selectTodoFromChooser() {
        _flags.update { it.copy(isTypeChooserSheetVisible = false, isAddTodoSheetVisible = true, editingTodo = null) }
    }

    fun selectBookmarkFromChooser() {
        _flags.update { it.copy(isTypeChooserSheetVisible = false, isAddBookmarkSheetVisible = true) }
    }

    fun onFabClick(onNewNote: () -> Unit) {
        when (uiState.value.fabAction) {
            CollectionFabAction.OPEN_TYPE_CHOOSER -> openTypeChooser()
            CollectionFabAction.CREATE_NOTE -> onNewNote()
            CollectionFabAction.CREATE_TODO -> openAddTodoSheet()
            CollectionFabAction.CREATE_BOOKMARK -> openAddBookmarkSheet()
        }
    }

    fun saveBookmark(url: String, title: String?, description: String?) {
        val colId = _collectionId.value
        val trimmed = url.trim()
        val normalized = if (
            trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
        viewModelScope.launch(Dispatchers.IO) {
            val domain = LinkMetadataExtractor.extractDomain(normalized)
            val platform = LinkMetadataExtractor.detectPlatform(normalized)
            val initialTitle = title?.trim()?.takeIf { it.isNotBlank() } ?: domain
            val initialDesc = description?.trim()?.takeIf { it.isNotBlank() }
            val bookmark = Bookmark(
                id = UUID.randomUUID().toString(),
                url = normalized,
                title = initialTitle,
                description = initialDesc,
                domain = domain,
                platform = platform,
                collectionId = colId.ifBlank { null },
                createdAt = System.currentTimeMillis()
            )
            bookmarkRepository.saveBookmark(bookmark)
            dismissAddBookmarkSheet()
        }
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
