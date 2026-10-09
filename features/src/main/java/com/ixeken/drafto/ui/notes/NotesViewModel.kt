package com.ixeken.drafto.ui.notes

import android.content.Context
import android.net.Uri
import com.ixeken.drafto.ui.components.DraftoToastManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ixeken.drafto.core.R
import com.ixeken.drafto.data.backup.NotebookBackupManager
import com.ixeken.drafto.data.backup.NotebookBackupProgressState
import com.ixeken.drafto.data.backup.NotebookBackupSummary
import com.ixeken.drafto.data.backup.NotebookImportSummary
import com.ixeken.drafto.data.backup.SingleNoteImportResult
import com.ixeken.drafto.data.backup.SingleTodosImportResult
import com.ixeken.drafto.data.local.datastore.SettingsDataStore
import com.ixeken.drafto.domain.model.DraftoCollection
import com.ixeken.drafto.domain.model.Note
import com.ixeken.drafto.domain.model.TabFilterOption
import com.ixeken.drafto.domain.model.TabSortOption
import com.ixeken.drafto.domain.model.TodoFilter
import com.ixeken.drafto.domain.model.TodoItem
import com.ixeken.drafto.domain.repository.CollectionRepository
import com.ixeken.drafto.domain.repository.TodoRepository
import com.ixeken.drafto.domain.usecase.DeleteNoteUseCase
import com.ixeken.drafto.domain.usecase.GetNotesUseCase
import com.ixeken.drafto.domain.usecase.SaveNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel reactivo para la experiencia unificada de Notebook (Notas y To-dos).
 *
 * Implementa el patrón Unidirectional Data Flow (UDF) orquestando flujos reactivos de notas
 * y tareas mediante [GetNotesUseCase] y [TodoRepository]. Provee mutaciones asíncronas
 * delegadas estrictamente en [Dispatchers.IO] para garantizar una tasa de refresco constante de 120 FPS.
 *
 * @property getNotesUseCase Caso de uso para observar el flujo de notas persistidas.
 * @property saveNoteUseCase Caso de uso para crear o actualizar notas.
 * @property deleteNoteUseCase Caso de uso para eliminar notas por identificador.
 * @property todoRepository Repositorio de dominio para la persistencia reactiva de tareas.
 * @property notebookBackupManager Administrador central de respaldo e importación Markdown ZIP.
 * @property settingsDataStore Almacén de preferencias para persistencia de modo de vista (cuadrícula vs lista).
 * @property collectionRepository Repositorio reactivo de colecciones temáticas unificadas.
 * @property context Contexto de aplicación para apertura de flujos SAF y recursos localizados.
 */
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val getNotesUseCase: GetNotesUseCase,
    private val saveNoteUseCase: SaveNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val todoRepository: TodoRepository,
    private val notebookBackupManager: NotebookBackupManager,
    private val settingsDataStore: SettingsDataStore,
    private val collectionRepository: CollectionRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _selectedTodoFilter = MutableStateFlow(TodoFilter.ALL)
    private val _showAddTodoSheet = MutableStateFlow(false)
    private val _editingTodo = MutableStateFlow<TodoItem?>(null)
    private val initialSection: NotebookSection = when (settingsDataStore.cachedLastNotebookSection) {
        "TODOS" -> NotebookSection.TODOS
        else -> NotebookSection.NOTES
    }
    private val _selectedSection = MutableStateFlow(initialSection)
    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _showBackupSheet = MutableStateFlow(false)
    private val _backupProgressState = MutableStateFlow<NotebookBackupProgressState?>(null)
    val backupProgressState: StateFlow<NotebookBackupProgressState?> = _backupProgressState.asStateFlow()

    private val _selectedNoteIds = MutableStateFlow<Set<String>>(emptySet())
    private val _showDeleteSelectedNotesDialog = MutableStateFlow(false)
    private val _selectedTodoIds = MutableStateFlow<Set<String>>(emptySet())
    private val _showDeleteSelectedTodosDialog = MutableStateFlow(false)
    private val _filterOption = MutableStateFlow(TabFilterOption.ALL)
    private val _sortOption = MutableStateFlow(TabSortOption.NEWEST)
    private val _showOptionsSheet = MutableStateFlow(false)

    private val todosFlow = todoRepository.observeAllTodos()

    private val dataFlow = combine(
        getNotesUseCase().catch { exception ->
            _error.value = exception.message
            emit(emptyList())
        },
        todosFlow.catch { exception ->
            _error.value = exception.message
            emit(emptyList())
        }
    ) { notes, todos -> notes to todos }

    private val todoControlsFlow = combine(
        _selectedTodoFilter,
        _showAddTodoSheet,
        _editingTodo
    ) { filter, showAdd, editing ->
        Triple(filter, showAdd, editing)
    }

    private val backupControlsFlow = combine(
        _showBackupSheet,
        _backupProgressState
    ) { showBackup, progressState ->
        showBackup to progressState
    }

    private val generalControlsFlow = combine(
        _searchQuery,
        _isSearchActive,
        settingsDataStore.notesGridViewFlow,
        _selectedSection,
        _error
    ) { query, isSearch, isGrid, section, error ->
        GeneralControlsData(query, isSearch, isGrid, section, error)
    }

    private val selectionControlsFlow = combine(
        _selectedNoteIds,
        _showDeleteSelectedNotesDialog,
        _selectedTodoIds,
        _showDeleteSelectedTodosDialog
    ) { selNotes, delNotes, selTodos, delTodos ->
        SelectionControlsData(selNotes, delNotes, selTodos, delTodos)
    }

    private val optionsControlsFlow = combine(
        _filterOption,
        _sortOption,
        _showOptionsSheet
    ) { filter, sort, showOptions ->
        Triple(filter, sort, showOptions)
    }

    private val controlsFlow = combine(
        todoControlsFlow,
        backupControlsFlow,
        generalControlsFlow,
        selectionControlsFlow,
        optionsControlsFlow
    ) { (filter, showAdd, editing), (showBackup, progressState), general, selection, (tabFilter, sort, showOptions) ->
        ControlsData(
            filter = filter,
            showAdd = showAdd,
            editingTodo = editing,
            section = general.section,
            query = general.query,
            isSearchActive = general.isSearchActive,
            isGridView = general.isGridView,
            error = general.error,
            showBackupSheet = showBackup,
            backupProgressState = progressState,
            selectedNoteIds = selection.selectedNoteIds,
            showDeleteSelectedNotesDialog = selection.showDeleteSelectedNotesDialog,
            selectedTodoIds = selection.selectedTodoIds,
            showDeleteSelectedTodosDialog = selection.showDeleteSelectedTodosDialog,
            filterOption = tabFilter,
            sortOption = sort,
            showOptionsSheet = showOptions
        )
    }

    val uiState: StateFlow<NotesUiState> = combine(
        dataFlow,
        collectionRepository.getCollections().catch { emit(emptyList()) },
        controlsFlow
    ) { (notes, todos): Pair<List<Note>, List<TodoItem>>, collections: List<DraftoCollection>, controls: ControlsData ->
        var filteredNotes = if (controls.query.isBlank()) {
            notes
        } else {
            notes.filter {
                it.title.contains(controls.query, ignoreCase = true) ||
                    it.content.contains(controls.query, ignoreCase = true)
            }
        }
        if (controls.filterOption == TabFilterOption.WITHOUT_COLLECTION) {
            filteredNotes = filteredNotes.filter { it.collectionId.isNullOrBlank() }
        }
        filteredNotes = when (controls.sortOption) {
            TabSortOption.NEWEST -> filteredNotes.sortedByDescending { it.updatedAt }
            TabSortOption.OLDEST -> filteredNotes.sortedBy { it.createdAt }
            TabSortOption.ALPHABETICAL -> filteredNotes.sortedBy { it.title.lowercase() }
        }

        var filteredTodos = if (controls.query.isBlank()) {
            todos
        } else {
            todos.filter {
                it.title.contains(controls.query, ignoreCase = true) ||
                    it.description.contains(controls.query, ignoreCase = true)
            }
        }
        if (controls.filterOption == TabFilterOption.WITHOUT_COLLECTION) {
            filteredTodos = filteredTodos.filter { it.collectionId.isNullOrBlank() }
        }
        filteredTodos = when (controls.sortOption) {
            TabSortOption.NEWEST -> filteredTodos.sortedByDescending { it.createdAt }
            TabSortOption.OLDEST -> filteredTodos.sortedBy { it.createdAt }
            TabSortOption.ALPHABETICAL -> filteredTodos.sortedBy { it.title.lowercase() }
        }

        NotesUiState(
            isLoading = false,
            notes = notes,
            todos = todos,
            filteredNotes = filteredNotes,
            filteredTodos = filteredTodos,
            availableCollections = collections,
            selectedTodoFilter = controls.filter,
            showAddTodoSheet = controls.showAdd,
            editingTodo = controls.editingTodo,
            selectedSection = controls.section,
            isGridView = controls.isGridView,
            isSearchActive = controls.isSearchActive,
            searchQuery = controls.query,
            filterOption = controls.filterOption,
            sortOption = controls.sortOption,
            showOptionsSheet = controls.showOptionsSheet,
            error = controls.error,
            showBackupSheet = controls.showBackupSheet,
            backupProgressState = controls.backupProgressState,
            selectedNoteIds = controls.selectedNoteIds,
            showDeleteSelectedNotesDialog = controls.showDeleteSelectedNotesDialog,
            selectedTodoIds = controls.selectedTodoIds,
            showDeleteSelectedTodosDialog = controls.showDeleteSelectedTodosDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotesUiState(isLoading = true, selectedSection = initialSection)
    )

    init {
        viewModelScope.launch {
            settingsDataStore.lastNotebookSectionFlow.collect { sectionStr ->
                val section = when (sectionStr) {
                    "TODOS" -> NotebookSection.TODOS
                    else -> NotebookSection.NOTES
                }
                if (_selectedSection.value != section) {
                    _selectedSection.value = section
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    /**
     * Conmuta la subsección activa de Notebook entre Notas y To-dos, persistiendo la selección en DataStore.
     */
    fun setSelectedSection(section: NotebookSection) {
        if (_selectedSection.value != section) {
            clearNoteSelection()
            clearTodoSelection()
            _selectedSection.value = section
            viewModelScope.launch(Dispatchers.IO) {
                settingsDataStore.setLastNotebookSection(section.name)
            }
        }
    }

    /**
     * Alterna la selección de una nota en modo multiselección.
     */
    fun toggleNoteSelection(noteId: String) {
        _selectedNoteIds.update { if (it.contains(noteId)) it - noteId else it + noteId }
    }

    /**
     * Limpia la selección múltiple activa de notas.
     */
    fun clearNoteSelection() {
        _selectedNoteIds.value = emptySet()
        _showDeleteSelectedNotesDialog.value = false
    }

    /**
     * Controla la visibilidad del diálogo de confirmación para eliminar las notas seleccionadas.
     */
    fun setShowDeleteSelectedNotesDialog(show: Boolean) {
        _showDeleteSelectedNotesDialog.value = show
    }

    /**
     * Elimina en lote todas las notas seleccionadas en la base de datos de forma segura.
     */
    fun deleteSelectedNotes() {
        val toDelete = _selectedNoteIds.value
        if (toDelete.isEmpty()) {
            _showDeleteSelectedNotesDialog.value = false
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            toDelete.forEach { deleteNoteUseCase(it) }
            clearNoteSelection()
        }
    }

    /**
     * Fija o desfija en lote las notas seleccionadas.
     */
    fun togglePinSelectedNotes(isPinned: Boolean) {
        val toPin = _selectedNoteIds.value
        if (toPin.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val allNotes = uiState.value.notes
            toPin.forEach { noteId ->
                allNotes.find { it.id == noteId }?.let { note ->
                    saveNoteUseCase(note.copy(isPinned = isPinned, updatedAt = System.currentTimeMillis()))
                }
            }
        }
    }

    /**
     * Alterna la selección de una tarea en modo multiselección.
     */
    fun toggleTodoSelection(todoId: String) {
        _selectedTodoIds.update { if (it.contains(todoId)) it - todoId else it + todoId }
    }

    /**
     * Limpia la selección múltiple activa de tareas to-do.
     */
    fun clearTodoSelection() {
        _selectedTodoIds.value = emptySet()
        _showDeleteSelectedTodosDialog.value = false
    }

    /**
     * Controla la visibilidad del diálogo de confirmación para eliminar las tareas seleccionadas.
     */
    fun setShowDeleteSelectedTodosDialog(show: Boolean) {
        _showDeleteSelectedTodosDialog.value = show
    }

    /**
     * Elimina en lote todas las tareas seleccionadas en la base de datos de forma segura.
     */
    fun deleteSelectedTodos() {
        val toDelete = _selectedTodoIds.value
        if (toDelete.isEmpty()) {
            _showDeleteSelectedTodosDialog.value = false
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            toDelete.forEach { todoRepository.deleteTodo(it) }
            clearTodoSelection()
        }
    }

    /**
     * Fija o desfija en lote las tareas seleccionadas.
     */
    fun togglePinSelectedTodos(isPinned: Boolean) {
        val toPin = _selectedTodoIds.value
        if (toPin.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            toPin.forEach { todoRepository.togglePinned(it, isPinned) }
        }
    }

    /**
     * Marca como completadas o pendientes en lote las tareas seleccionadas.
     */
    fun toggleCompleteSelectedTodos(isCompleted: Boolean) {
        val toComplete = _selectedTodoIds.value
        if (toComplete.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            toComplete.forEach { todoRepository.toggleCompleted(it, isCompleted) }
        }
    }

    fun setTodoFilter(filter: TodoFilter) {
        _selectedTodoFilter.value = filter
    }

    fun setShowAddTodoSheet(show: Boolean) {
        _showAddTodoSheet.value = show
        if (!show) {
            _editingTodo.value = null
        }
    }

    fun startEditingTodo(todo: TodoItem) {
        _editingTodo.value = todo
        _showAddTodoSheet.value = true
    }

    fun dismissAddTodoSheet() {
        _showAddTodoSheet.value = false
        _editingTodo.value = null
    }

    fun toggleTodoCompleted(todo: TodoItem, isCompleted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                todoRepository.toggleCompleted(todo.id, isCompleted)
            }.onFailure { exception ->
                _error.value = exception.message
            }
        }
    }

    fun toggleTodoPinned(todo: TodoItem, isPinned: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                todoRepository.togglePinned(todo.id, isPinned)
            }.onFailure { exception ->
                _error.value = exception.message
            }
        }
    }

    fun toggleSubtask(todoId: String, subtaskId: String, isDone: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                todoRepository.toggleSubtask(todoId, subtaskId, isDone)
            }.onFailure { exception ->
                _error.value = exception.message
            }
        }
    }

    fun deleteTodo(todo: TodoItem) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                todoRepository.deleteTodo(todo.id)
            }.onFailure { exception ->
                _error.value = exception.message
            }
        }
    }

    fun saveTodo(title: String, description: String, isPinned: Boolean, dueDate: Long?) {
        saveTodo(title, description, isPinned, dueDate, _editingTodo.value?.collectionId, _editingTodo.value?.subtasks ?: emptyList())
    }

    fun saveTodo(title: String, description: String, isPinned: Boolean, dueDate: Long?, collectionId: String?) {
        saveTodo(title, description, isPinned, dueDate, collectionId, _editingTodo.value?.subtasks ?: emptyList())
    }

    fun saveTodo(
        title: String,
        description: String,
        isPinned: Boolean,
        dueDate: Long?,
        collectionId: String?,
        subtasks: List<com.ixeken.drafto.domain.model.TodoSubtask>
    ) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) return

        val currentEditing = _editingTodo.value
        val allDone = subtasks.isNotEmpty() && subtasks.all { it.isDone }
        val todo = if (currentEditing != null) {
            currentEditing.copy(
                title = trimmedTitle,
                description = description.trim(),
                isPinned = isPinned,
                dueDate = dueDate,
                collectionId = collectionId,
                subtasks = subtasks,
                isCompleted = if (subtasks.isNotEmpty()) allDone else currentEditing.isCompleted,
                completedAt = if (subtasks.isNotEmpty() && allDone) System.currentTimeMillis() else currentEditing.completedAt
            )
        } else {
            TodoItem(
                id = UUID.randomUUID().toString(),
                title = trimmedTitle,
                description = description.trim(),
                isCompleted = allDone,
                isPinned = isPinned,
                dueDate = dueDate,
                collectionId = collectionId,
                subtasks = subtasks,
                createdAt = System.currentTimeMillis(),
                completedAt = if (allDone) System.currentTimeMillis() else null
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                todoRepository.saveTodo(todo)
            }.onSuccess {
                _showAddTodoSheet.value = false
                _editingTodo.value = null
            }.onFailure { exception ->
                _error.value = exception.message
            }
        }
    }

    fun createNote(title: String, content: String, tags: List<String> = emptyList(), collectionId: String? = null) {
        val note = Note(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { "Sin título" },
            content = content,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            tags = tags,
            collectionId = collectionId
        )
        saveNote(note)
    }

    fun saveNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { saveNoteUseCase(note) }
                .onFailure { exception ->
                    _error.value = exception.message
                }
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { deleteNoteUseCase(id) }
                .onFailure { exception ->
                    _error.value = exception.message
                }
        }
    }

    /**
     * Controla la visibilidad de la hoja modal de respaldo y restauración del cuaderno.
     *
     * @param show `true` para desplegar el modal [DraftoNotebookBackupBottomSheet], `false` para ocultarlo.
     */
    fun setShowBackupSheet(show: Boolean) {
        _showBackupSheet.value = show
    }

    /**
     * Exporta todas las notas individuales y tareas a un archivo comprimido .zip en formato Markdown
     * interoperable, emitiendo estados reactivos continuos para retroalimentar la interfaz de usuario.
     *
     * @param uri URI destino provisto por el Storage Access Framework (SAF) vía CreateDocument.
     * @param onSuccess Callback invocado al completar la exportación con el conteo de notas y tareas.
     * @param onError Callback invocado si ocurre una excepción de E/S o permisos durante el proceso.
     */
    fun exportNotebook(
        uri: Uri,
        onSuccess: (NotebookBackupSummary) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            val initialStatus = context.getString(R.string.dialog_exporting_notebook)
            val initialProgress = NotebookBackupProgressState(
                isActive = true,
                isExport = true,
                progress = 0.1f,
                statusText = initialStatus
            )
            _backupProgressState.value = initialProgress

            try {
                val outputStream = withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)
                } ?: throw IllegalStateException("No se pudo abrir el flujo de escritura para la exportación.")

                val result = notebookBackupManager.exportNotebookZip(
                    outputStream = outputStream,
                    onProgress = { progress, statusMessage ->
                        _backupProgressState.value = NotebookBackupProgressState(
                            isActive = true,
                            isExport = true,
                            progress = progress,
                            statusText = statusMessage
                        )
                    }
                )

                kotlinx.coroutines.delay(350)
                _backupProgressState.value = null

                result.fold(
                    onSuccess = { summary -> onSuccess(summary) },
                    onFailure = { error ->
                        _error.value = error.message
                        onError(error)
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _backupProgressState.value = null
                _error.value = e.message
                onError(e)
            }
        }
    }

    /**
     * Importa y restaura notas y tareas desde un archivo comprimido .zip de libreta universal,
     * aplicando deduplicación no destructiva frente a los registros existentes.
     *
     * @param uri URI del archivo .zip seleccionado mediante OpenDocument de SAF.
     * @param onSuccess Callback invocado al finalizar con el desglose de ítems importados y omitidos.
     * @param onError Callback invocado ante fallas de lectura o descompresión.
     */
    fun importNotebook(
        uri: Uri,
        onSuccess: (NotebookImportSummary) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            val initialStatus = context.getString(R.string.dialog_importing_notebook)
            val initialProgress = NotebookBackupProgressState(
                isActive = true,
                isExport = false,
                progress = 0.1f,
                statusText = initialStatus
            )
            _backupProgressState.value = initialProgress

            try {
                val inputStream = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)
                } ?: throw IllegalStateException("No se pudo abrir el archivo de respaldo seleccionado.")

                val result = notebookBackupManager.importNotebookZip(
                    inputStream = inputStream,
                    onProgress = { progress, statusMessage ->
                        _backupProgressState.value = NotebookBackupProgressState(
                            isActive = true,
                            isExport = false,
                            progress = progress,
                            statusText = statusMessage
                        )
                    }
                )

                kotlinx.coroutines.delay(350)
                _backupProgressState.value = null

                result.fold(
                    onSuccess = { summary -> onSuccess(summary) },
                    onFailure = { error ->
                        _error.value = error.message
                        onError(error)
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _backupProgressState.value = null
                _error.value = e.message
                onError(e)
            }
        }
    }

    /**
     * Exporta una única nota de texto enriquecido hacia un archivo Markdown plano (.md) mediante SAF.
     *
     * @param note Nota de dominio a ser serializada con Frontmatter YAML y cuerpo Markdown.
     * @param uri URI destino donde se escribirá el archivo individual generado.
     * @param onSuccess Callback invocado tras la escritura satisfactoria.
     * @param onError Callback notificado en caso de error de E/S.
     */
    fun exportSingleNote(
        note: Note,
        uri: Uri,
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val outputStream = withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)
                } ?: throw IllegalStateException("No se pudo abrir el archivo para exportar la nota.")

                val result = notebookBackupManager.exportSingleNote(note, outputStream)
                result.fold(
                    onSuccess = { onSuccess() },
                    onFailure = { error ->
                        _error.value = error.message
                        onError(error)
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _error.value = e.message
                onError(e)
            }
        }
    }

    /**
     * Importa una nota individual desde un archivo Markdown seleccionado por el usuario (.md).
     *
     * @param uri URI del archivo Markdown provisto por SAF.
     * @param onSuccess Callback con la nota parseada y un booleano indicando si ya existía como duplicado.
     * @param onError Callback invocado si ocurre un error de lectura o parseo.
     */
    fun importSingleNote(
        uri: Uri,
        onSuccess: (Note, Boolean) -> Unit = { _, _ -> },
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val fileName = getFileNameFromUri(uri)
                val inputStream = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)
                } ?: throw IllegalStateException("No se pudo abrir el archivo Markdown.")

                val result = notebookBackupManager.importSingleNote(fileName, inputStream)
                result.fold(
                    onSuccess = { singleResult ->
                        onSuccess(singleResult.note, singleResult.isDuplicate)
                    },
                    onFailure = { error ->
                        _error.value = error.message
                        onError(error)
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _error.value = e.message
                onError(e)
            }
        }
    }

    /**
     * Importa tareas To-do desde un archivo Markdown con checklist (.md).
     *
     * @param uri URI del archivo Markdown provisto por SAF.
     * @param onSuccess Callback con el conteo de tareas importadas y omitidas.
     * @param onError Callback invocado si ocurre un error de lectura o parseo.
     */
    fun importTodosMarkdown(
        uri: Uri,
        onSuccess: (SingleTodosImportResult) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val inputStream = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)
                } ?: throw IllegalStateException("No se pudo abrir el archivo de tareas Markdown.")

                val result = notebookBackupManager.importTodosMarkdown(inputStream)
                result.fold(
                    onSuccess = { summary ->
                        onSuccess(summary)
                    },
                    onFailure = { error ->
                        _error.value = error.message
                        onError(error)
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _error.value = e.message
                onError(e)
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var name = "note.md"
        if (uri.scheme == "content") {
            context.contentResolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        cursor.getString(index)?.let { name = it }
                    }
                }
            }
        } else {
            uri.path?.let { path ->
                name = path.substringAfterLast('/')
            }
        }
        return name
    }

    /**
     * Alterna el modo de visualización entre cuadrícula Bento y lista compacta,
     * persistiendo la preferencia en SettingsDataStore.
     */
    fun toggleGridView() {
        viewModelScope.launch {
            val current = uiState.value.isGridView
            settingsDataStore.setNotesGridView(!current)
        }
    }

    /**
     * Alterna la visibilidad de la barra de búsqueda animada.
     */
    fun toggleSearch(active: Boolean? = null) {
        val newActive = active ?: !_isSearchActive.value
        _isSearchActive.value = newActive
        if (!newActive) {
            _searchQuery.value = ""
        }
    }

    /**
     * Cierra el modo de búsqueda y restablece la consulta.
     */
    fun closeSearch() {
        _isSearchActive.value = false
        _searchQuery.value = ""
    }

    /**
     * Alterna el estado de fijado (pin) de una nota.
     */
    fun togglePinNote(note: Note) {
        saveNote(note.copy(isPinned = !note.isPinned))
    }

    /**
     * Genera el nombre de archivo estandarizado con marca de tiempo para exportación ZIP.
     */
    fun getExportFileName(): String {
        val timestamp = synchronized(BackupFileDateFormat) {
            BackupFileDateFormat.format(Date(System.currentTimeMillis()))
        }
        return "drafto_notebook_backup_$timestamp.zip"
    }

    /**
     * Procesa la selección del URI destino de SAF para exportación del cuaderno ZIP,
     * mostrando feedback mediante Toast.
     */
    fun onExportNotebookUriSelected(
        uri: Uri,
        onSuccess: (NotebookBackupSummary) -> Unit = {
            DraftoToastManager.showSuccess(context.getString(R.string.toast_notebook_exported))
        },
        onError: (Throwable) -> Unit = {
            DraftoToastManager.showWarning(context.getString(R.string.toast_notebook_export_error))
        }
    ) {
        exportNotebook(uri = uri, onSuccess = onSuccess, onError = onError)
    }

    /**
     * Procesa la selección del URI origen de SAF para importación del cuaderno ZIP,
     * mostrando feedback mediante Toast.
     */
    fun onImportNotebookZipSelected(
        uri: Uri,
        onSuccess: (NotebookImportSummary) -> Unit = { summary ->
            if (summary.notesImported == 0 && summary.todosImported == 0) {
                DraftoToastManager.showInfo(context.getString(R.string.toast_notebook_import_empty))
            } else {
                DraftoToastManager.showSuccess(
                    context.getString(R.string.toast_notebook_imported, summary.notesImported, summary.todosImported)
                )
            }
        },
        onError: (Throwable) -> Unit = {
            DraftoToastManager.showWarning(context.getString(R.string.toast_notebook_import_error))
        }
    ) {
        importNotebook(uri = uri, onSuccess = onSuccess, onError = onError)
    }

    /**
     * Procesa la selección del URI destino de SAF para exportar una nota individual Markdown (.md).
     */
    fun onExportSingleNoteSelected(
        note: Note,
        uri: Uri,
        onSuccess: () -> Unit = {
            DraftoToastManager.showSuccess(context.getString(R.string.toast_note_exported))
        },
        onError: (Throwable) -> Unit = {
            DraftoToastManager.showWarning(context.getString(R.string.toast_note_export_error))
        }
    ) {
        exportSingleNote(note = note, uri = uri, onSuccess = onSuccess, onError = onError)
    }

    /**
     * Procesa la selección de un archivo individual de nota Markdown (.md).
     */
    fun onImportSingleNoteSelected(
        uri: Uri,
        onSuccess: (Note, Boolean) -> Unit = { _, isDuplicate ->
            val msg = if (isDuplicate) context.getString(R.string.toast_note_duplicate) else context.getString(R.string.toast_note_imported)
            DraftoToastManager.showSuccess(msg)
        },
        onError: (Throwable) -> Unit = {
            DraftoToastManager.showWarning(context.getString(R.string.toast_note_import_error))
        }
    ) {
        importSingleNote(uri = uri, onSuccess = onSuccess, onError = onError)
    }

    /**
     * Procesa la selección de un archivo individual de tareas Markdown (.md).
     */
    fun onImportSingleTodosSelected(
        uri: Uri,
        onSuccess: (SingleTodosImportResult) -> Unit = { summary ->
            val msg = if (summary.importedCount == 0 && summary.skippedCount == 0) {
                context.getString(R.string.toast_todos_import_empty)
            } else {
                context.getString(R.string.toast_todos_imported, summary.importedCount, summary.skippedCount)
            }
            DraftoToastManager.showSuccess(msg)
        },
        onError: (Throwable) -> Unit = {
            DraftoToastManager.showWarning(context.getString(R.string.toast_todos_import_error))
        }
    ) {
        importTodosMarkdown(uri = uri, onSuccess = onSuccess, onError = onError)
    }

    /**
     * Actualiza el criterio de filtrado por pertenencia a colecciones de notas y tareas.
     */
    fun setFilterOption(option: TabFilterOption) {
        _filterOption.value = option
    }

    /**
     * Actualiza el criterio de ordenación activo para notas y tareas.
     */
    fun setSortOption(option: TabSortOption) {
        _sortOption.value = option
    }

    /**
     * Controla la visibilidad del modal de opciones de la pestaña Notebook.
     */
    fun setShowOptionsSheet(show: Boolean) {
        _showOptionsSheet.value = show
    }

    companion object {
        val MIME_ZIP_TYPES = arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream", "*/*")
        val MIME_MARKDOWN_TYPES = arrayOf("text/markdown", "text/plain", "*/*")
        private val BackupFileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    }
}

private data class GeneralControlsData(
    val query: String,
    val isSearchActive: Boolean,
    val isGridView: Boolean,
    val section: NotebookSection,
    val error: String?
)

private data class SelectionControlsData(
    val selectedNoteIds: Set<String>,
    val showDeleteSelectedNotesDialog: Boolean,
    val selectedTodoIds: Set<String>,
    val showDeleteSelectedTodosDialog: Boolean
)

private data class ControlsData(
    val filter: TodoFilter,
    val showAdd: Boolean,
    val editingTodo: TodoItem?,
    val section: NotebookSection,
    val query: String,
    val isSearchActive: Boolean,
    val isGridView: Boolean,
    val error: String?,
    val showBackupSheet: Boolean,
    val backupProgressState: NotebookBackupProgressState?,
    val selectedNoteIds: Set<String>,
    val showDeleteSelectedNotesDialog: Boolean,
    val selectedTodoIds: Set<String>,
    val showDeleteSelectedTodosDialog: Boolean,
    val filterOption: TabFilterOption,
    val sortOption: TabSortOption,
    val showOptionsSheet: Boolean
)


