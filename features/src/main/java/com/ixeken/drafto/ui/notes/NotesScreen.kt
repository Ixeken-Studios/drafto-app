package com.ixeken.drafto.ui.notes

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import com.ixeken.drafto.domain.model.DraftoCollection
import com.ixeken.drafto.domain.model.TodoItem
import com.ixeken.drafto.ui.components.DraftoAddTodoBottomSheet
import com.ixeken.drafto.ui.components.DraftoCapsuleOption
import com.ixeken.drafto.ui.components.DraftoCapsuleSelector
import com.ixeken.drafto.ui.components.DraftoConfirmationDialog
import com.ixeken.drafto.ui.components.DraftoEmptyState
import com.ixeken.drafto.ui.components.DraftoNotebookBackupBottomSheet
import com.ixeken.drafto.ui.components.DraftoProgressDialog
import com.ixeken.drafto.ui.components.DraftoSearchableTopBar
import com.ixeken.drafto.ui.components.DraftoStickyHeader
import com.ixeken.drafto.ui.components.DraftoTabTopBar
import com.ixeken.drafto.ui.components.DraftoNoteCard
import com.ixeken.drafto.ui.components.DraftoTodoItemCard
import com.ixeken.drafto.ui.components.DraftoTopBarIconButton
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TaskAlt
import com.ixeken.drafto.domain.model.TabFilterOption
import com.ixeken.drafto.domain.model.TabSortOption
import com.ixeken.drafto.ui.components.DraftoTabOptionsBottomSheet
import com.ixeken.drafto.ui.theme.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.ixeken.drafto.ui.components.DraftoDropdownMenuItem
import com.ixeken.drafto.ui.components.DraftoVerticalMenu

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.ixeken.drafto.core.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ixeken.drafto.domain.model.Note
import com.ixeken.drafto.ui.theme.HeightTopBar
import com.ixeken.drafto.ui.theme.PaddingScreenHorizontal
import com.ixeken.drafto.ui.theme.DraftoTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.lazy.rememberLazyListState
import com.ixeken.drafto.ui.theme.LocalHazeState
import dev.chrisbanes.haze.hazeSource

import androidx.compose.runtime.collectAsState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

/**
 * Contenedor reactivo principal para la pantalla de Notebook (Notas y To-dos).
 *
 * Conecta el estado provisto por [NotesViewModel] con los lanzadores de contratos de
 * Storage Access Framework (SAF) y delega la renderización a [NotesContent].
 *
 * @param onNavigateToEditor Navegación hacia el editor de notas con el ID opcional de la nota.
 * @param modifier Modificador Compose opcional.
 * @param viewModel Instancia de [NotesViewModel] inyectada por Hilt.
 */
@Composable
fun NotesScreen(
    onNavigateToEditor: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    BackHandler(enabled = uiState.isSearchActive && !uiState.isNotesSelectionMode && !uiState.isTodosSelectionMode) {
        viewModel.closeSearch()
    }

    val exportZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            viewModel.onExportNotebookUriSelected(uri)
        }
    }

    val importZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onImportNotebookZipSelected(uri)
        }
    }

    val importSingleNoteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onImportSingleNoteSelected(uri)
        }
    }

    val importSingleTodosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onImportSingleTodosSelected(uri)
        }
    }

    var pendingExportNote by remember { mutableStateOf<Note?>(null) }

    val exportSingleNoteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null) {
            pendingExportNote?.let { note ->
                viewModel.onExportSingleNoteSelected(note, uri)
            }
        }
        pendingExportNote = null
    }

    var notePendingDelete by remember { mutableStateOf<Note?>(null) }
    var todoPendingDelete by remember { mutableStateOf<TodoItem?>(null) }

    NotesContent(
        uiState = uiState,
        onSectionChange = viewModel::setSelectedSection,
        onToggleTodoCompleted = viewModel::toggleTodoCompleted,
        onToggleTodoPinned = viewModel::toggleTodoPinned,
        onToggleSubtask = viewModel::toggleSubtask,
        onDeleteTodo = { todo -> todoPendingDelete = todo },
        onEditTodo = viewModel::startEditingTodo,
        onDismissAddTodoSheet = viewModel::dismissAddTodoSheet,
        onSaveTodo = { title, description, isPinned, dueDate, collectionId, subtasks ->
            viewModel.saveTodo(title, description, isPinned, dueDate, collectionId, subtasks)
        },
        onToggleGridView = viewModel::toggleGridView,
        onToggleSearch = viewModel::toggleSearch,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onCloseSearch = viewModel::closeSearch,
        onNoteClick = { note -> onNavigateToEditor(note.id) },
        onPinNote = viewModel::togglePinNote,
        onDeleteNote = { note -> notePendingDelete = note },
        onExportNote = { note ->
            pendingExportNote = note
            val sanitizedTitle = note.title.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "note" }
            exportSingleNoteLauncher.launch("$sanitizedTitle.md")
        },
        onShowBackupSheet = viewModel::setShowBackupSheet,
        onExportZip = {
            exportZipLauncher.launch(viewModel.getExportFileName())
        },
        onImportZip = {
            importZipLauncher.launch(NotesViewModel.MIME_ZIP_TYPES)
        },
        onImportSingleNote = {
            importSingleNoteLauncher.launch(NotesViewModel.MIME_MARKDOWN_TYPES)
        },
        onImportSingleTodos = {
            importSingleTodosLauncher.launch(NotesViewModel.MIME_MARKDOWN_TYPES)
        },
        onToggleNoteSelection = viewModel::toggleNoteSelection,
        onToggleTodoSelection = viewModel::toggleTodoSelection,
        onShowOptionsSheet = viewModel::setShowOptionsSheet,
        onSetFilterOption = viewModel::setFilterOption,
        onSetSortOption = viewModel::setSortOption,
        modifier = modifier
    )

    if (notePendingDelete != null) {
        DraftoConfirmationDialog(
            title = stringResource(R.string.dialog_delete_note_title),
            message = stringResource(R.string.dialog_delete_note_message),
            icon = Icons.Rounded.Delete,
            confirmText = stringResource(R.string.action_delete),
            cancelText = stringResource(R.string.action_cancel),
            isDestructive = true,
            onConfirm = {
                notePendingDelete?.let { viewModel.deleteNote(it.id) }
                notePendingDelete = null
            },
            onDismiss = { notePendingDelete = null }
        )
    }

    if (todoPendingDelete != null) {
        DraftoConfirmationDialog(
            title = stringResource(R.string.dialog_delete_todo_title),
            message = stringResource(R.string.dialog_delete_todo_message),
            icon = Icons.Rounded.Delete,
            confirmText = stringResource(R.string.action_delete),
            cancelText = stringResource(R.string.action_cancel),
            isDestructive = true,
            onConfirm = {
                todoPendingDelete?.let { viewModel.deleteTodo(it) }
                todoPendingDelete = null
            },
            onDismiss = { todoPendingDelete = null }
        )
    }

    if (uiState.showDeleteSelectedNotesDialog) {
        DraftoConfirmationDialog(
            title = stringResource(R.string.dialog_delete_selected_notes_title),
            message = stringResource(R.string.dialog_delete_selected_notes_message, uiState.selectedNoteIds.size),
            icon = Icons.Rounded.Delete,
            confirmText = stringResource(R.string.action_delete),
            cancelText = stringResource(R.string.action_cancel),
            isDestructive = true,
            onConfirm = {
                viewModel.deleteSelectedNotes()
            },
            onDismiss = { viewModel.setShowDeleteSelectedNotesDialog(false) }
        )
    }

    if (uiState.showDeleteSelectedTodosDialog) {
        DraftoConfirmationDialog(
            title = stringResource(R.string.dialog_delete_selected_todos_title),
            message = stringResource(R.string.dialog_delete_selected_todos_message, uiState.selectedTodoIds.size),
            icon = Icons.Rounded.Delete,
            confirmText = stringResource(R.string.action_delete),
            cancelText = stringResource(R.string.action_cancel),
            isDestructive = true,
            onConfirm = {
                viewModel.deleteSelectedTodos()
            },
            onDismiss = { viewModel.setShowDeleteSelectedTodosDialog(false) }
        )
    }
}

/**
 * Componente puramente sin estado (Stateless Presentation) para Notebook (Notas y To-dos).
 * Optimizado para Strong Skipping y renderizado a 120 FPS sin recomposiciones accidentales.
 */
@Composable
fun NotesContent(
    uiState: NotesUiState,
    onSectionChange: (NotebookSection) -> Unit,
    onToggleTodoCompleted: (TodoItem, Boolean) -> Unit,
    onToggleTodoPinned: (TodoItem, Boolean) -> Unit,
    onToggleSubtask: (todoId: String, subtaskId: String, isDone: Boolean) -> Unit,
    onDeleteTodo: (TodoItem) -> Unit,
    onEditTodo: (TodoItem) -> Unit,
    onDismissAddTodoSheet: () -> Unit,
    onSaveTodo: (title: String, description: String, isPinned: Boolean, dueDate: Long?, collectionId: String?, subtasks: List<com.ixeken.drafto.domain.model.TodoSubtask>) -> Unit,
    onToggleGridView: () -> Unit,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    onNoteClick: (Note) -> Unit,
    onPinNote: (Note) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onExportNote: (Note) -> Unit,
    onShowBackupSheet: (Boolean) -> Unit,
    onExportZip: () -> Unit,
    onImportZip: () -> Unit,
    onImportSingleNote: () -> Unit,
    onImportSingleTodos: () -> Unit,
    onToggleNoteSelection: (String) -> Unit = {},
    onToggleTodoSelection: (String) -> Unit = {},
    onShowOptionsSheet: (Boolean) -> Unit = {},
    onSetFilterOption: (TabFilterOption) -> Unit = {},
    onSetSortOption: (TabSortOption) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val gridState = rememberLazyStaggeredGridState()
    val todoListState = rememberLazyListState()
    var isCompletedExpanded by rememberSaveable { mutableStateOf(false) }
    var isPendingExpanded by rememberSaveable { mutableStateOf(true) }

    val collectionMap = remember(uiState.availableCollections) {
        uiState.availableCollections.associate { it.id to it.name }
    }

    val isScrolled by remember(uiState.selectedSection, uiState.isGridView, uiState.isSearchActive) {
        derivedStateOf {
            uiState.isSearchActive || when (uiState.selectedSection) {
                NotebookSection.NOTES -> if (uiState.isGridView) {
                    gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0
                } else {
                    listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
                }
                NotebookSection.TODOS -> {
                    todoListState.firstVisibleItemIndex > 0 || todoListState.firstVisibleItemScrollOffset > 0
                }
            }
        }
    }

    var headerHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val headerHeightDp = with(density) { headerHeightPx.toDp() }
    val safeTopPadding = if (headerHeightDp > 0.dp) headerHeightDp + PaddingSmall else NotebookFallbackTopPadding

    val hazeState = LocalHazeState.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier)
        ) {
            Crossfade(
                targetState = uiState.selectedSection,
                animationSpec = DraftoSprings.SnappyFloat,
                label = "notebookSectionTransition"
            ) { section ->
                when (section) {
                    NotebookSection.NOTES -> {
                        if (uiState.filteredNotes.isEmpty() && !uiState.isLoading) {
                            val title = if (uiState.searchQuery.isNotBlank()) stringResource(R.string.empty_search_title) else stringResource(R.string.empty_notes_title)
                            val description = if (uiState.searchQuery.isNotBlank()) stringResource(R.string.empty_search_description) else stringResource(R.string.empty_notes_description)
                            val icon = if (uiState.searchQuery.isNotBlank()) Icons.Default.Search else Icons.Rounded.Book

                            DraftoEmptyState(
                                icon = icon,
                                iconTint = DraftoTheme.colors.accent,
                                title = title,
                                description = description,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = safeTopPadding)
                            )
                        } else if (uiState.isGridView) {
                            // Cuadrícula Bento con acomodo inteligente escalonado (Staggered Grid)
                            LazyVerticalStaggeredGrid(
                                state = gridState,
                                columns = StaggeredGridCells.Fixed(2),
                                contentPadding = PaddingValues(
                                    start = PaddingScreenHorizontal,
                                    top = safeTopPadding,
                                    end = PaddingScreenHorizontal,
                                    bottom = PaddingListBottomContent
                                ),
                                horizontalArrangement = Arrangement.spacedBy(PaddingMedium),
                                verticalItemSpacing = PaddingMedium,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = uiState.filteredNotes,
                                    key = { it.id },
                                    contentType = { "note_card" }
                                ) { note ->
                                    DraftoNoteCard(
                                        note = note,
                                        collectionName = collectionMap[note.collectionId],
                                        isSelected = uiState.selectedNoteIds.contains(note.id),
                                        isSelectionMode = uiState.isNotesSelectionMode,
                                        onClick = {
                                            if (uiState.isNotesSelectionMode) {
                                                onToggleNoteSelection(note.id)
                                            } else {
                                                onNoteClick(note)
                                            }
                                        },
                                        onLongClick = { onToggleNoteSelection(note.id) },
                                        onTogglePinned = { onPinNote(note) },
                                        onDelete = { onDeleteNote(note) },
                                        onExport = { onExportNote(note) },
                                        maxBodyLines = 6
                                    )
                                }
                            }
                        } else {
                            // List View
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(start = PaddingScreenHorizontal, top = safeTopPadding, end = PaddingScreenHorizontal, bottom = PaddingListBottomContent),
                                verticalArrangement = Arrangement.spacedBy(PaddingSmall),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = uiState.filteredNotes,
                                    key = { it.id },
                                    contentType = { "note_card" }
                                ) { note ->
                                    DraftoNoteCard(
                                        note = note,
                                        collectionName = collectionMap[note.collectionId],
                                        isSelected = uiState.selectedNoteIds.contains(note.id),
                                        isSelectionMode = uiState.isNotesSelectionMode,
                                        onClick = {
                                            if (uiState.isNotesSelectionMode) {
                                                onToggleNoteSelection(note.id)
                                            } else {
                                                onNoteClick(note)
                                            }
                                        },
                                        onLongClick = { onToggleNoteSelection(note.id) },
                                        onTogglePinned = { onPinNote(note) },
                                        onDelete = { onDeleteNote(note) },
                                        onExport = { onExportNote(note) },
                                        maxBodyLines = 8
                                    )
                                }
                            }
                        }
                    }
                    NotebookSection.TODOS -> {
                        if (uiState.filteredTodos.isEmpty() && !uiState.isLoading) {
                            val title = if (uiState.searchQuery.isNotBlank()) stringResource(R.string.empty_search_title) else stringResource(R.string.todo_empty_title)
                            val description = if (uiState.searchQuery.isNotBlank()) stringResource(R.string.empty_search_description) else stringResource(R.string.todo_empty_description)
                            val icon = if (uiState.searchQuery.isNotBlank()) Icons.Default.Search else Icons.Rounded.TaskAlt

                            DraftoEmptyState(
                                icon = icon,
                                iconTint = DraftoTheme.colors.accent,
                                title = title,
                                description = description,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = safeTopPadding)
                            )
                        } else {
                            val (completedTodos, pendingTodos) = remember(uiState.filteredTodos) {
                                uiState.filteredTodos.partition { it.isCompleted }
                            }

                            LazyColumn(
                                state = todoListState,
                                contentPadding = PaddingValues(
                                    start = PaddingScreenHorizontal,
                                    top = safeTopPadding,
                                    end = PaddingScreenHorizontal,
                                    bottom = PaddingListBottomContent
                                ),
                                verticalArrangement = Arrangement.spacedBy(PaddingSmall),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // 1. Sección superior: Tareas Completadas (colapsada por defecto)
                                item(key = "section_header_completed", contentType = "section_header") {
                                    TodoSectionHeader(
                                        title = stringResource(R.string.todo_filter_completed),
                                        count = completedTodos.size,
                                        isExpanded = isCompletedExpanded,
                                        onToggle = { isCompletedExpanded = !isCompletedExpanded }
                                    )
                                }

                                if (isCompletedExpanded) {
                                    if (completedTodos.isEmpty()) {
                                        item(key = "empty_completed_hint", contentType = "section_empty") {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = PaddingSmall),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.todo_empty_title),
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontFamily = PoppinsFontFamily
                                                    ),
                                                    color = DraftoTheme.colors.textMuted
                                                )
                                            }
                                        }
                                    } else {
                                        items(
                                            items = completedTodos,
                                            key = { it.id },
                                            contentType = { "todo_item" }
                                        ) { todo ->
                                            val collectionName = remember(todo.collectionId, uiState.availableCollections) {
                                                uiState.availableCollections.firstOrNull { it.id == todo.collectionId }?.name
                                            }
                                            val isReducedMotion = LocalDraftoReducedMotion.current
                                            val itemModifier = if (isReducedMotion) {
                                                Modifier.animateItem(
                                                    fadeInSpec = snap(),
                                                    fadeOutSpec = snap(),
                                                    placementSpec = snap()
                                                )
                                            } else {
                                                Modifier.animateItem(
                                                    fadeInSpec = DraftoSprings.SnappyFloat,
                                                    fadeOutSpec = DraftoSprings.SnappyFloat,
                                                    placementSpec = DraftoSprings.SnappyOffset
                                                )
                                            }
                                            DraftoTodoItemCard(
                                                todo = todo,
                                                collectionName = collectionName,
                                                modifier = itemModifier,
                                                isSelected = uiState.selectedTodoIds.contains(todo.id),
                                                isSelectionMode = uiState.isTodosSelectionMode,
                                                onClick = {
                                                    if (uiState.isTodosSelectionMode) {
                                                        onToggleTodoSelection(todo.id)
                                                    } else {
                                                        onEditTodo(todo)
                                                    }
                                                },
                                                onLongClick = { onToggleTodoSelection(todo.id) },
                                                onEdit = { onEditTodo(todo) },
                                                onToggleCompleted = { isCompleted ->
                                                    onToggleTodoCompleted(todo, isCompleted)
                                                },
                                                onTogglePinned = { isPinned ->
                                                    onToggleTodoPinned(todo, isPinned)
                                                },
                                                onToggleSubtask = { subtaskId, isDone ->
                                                    onToggleSubtask(todo.id, subtaskId, isDone)
                                                },
                                                onDelete = {
                                                    onDeleteTodo(todo)
                                                }
                                            )
                                        }
                                    }
                                }

                                // 2. Sección inferior: Tareas Pendientes (abierta por defecto)
                                item(key = "section_header_pending", contentType = "section_header") {
                                    TodoSectionHeader(
                                        title = stringResource(R.string.todo_filter_pending),
                                        count = pendingTodos.size,
                                        isExpanded = isPendingExpanded,
                                        onToggle = { isPendingExpanded = !isPendingExpanded }
                                    )
                                }

                                if (isPendingExpanded) {
                                    if (pendingTodos.isEmpty()) {
                                        item(key = "empty_pending_hint", contentType = "section_empty") {
                                            val isAllDone = completedTodos.isNotEmpty()
                                            val isReducedMotion = LocalDraftoReducedMotion.current
                                            val celebrationScale by animateFloatAsState(
                                                targetValue = 1f,
                                                animationSpec = if (isReducedMotion) snap() else DraftoSprings.BouncyFloat,
                                                label = "celebrationScale"
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = PaddingSmall)
                                                    .graphicsLayer {
                                                        scaleX = celebrationScale
                                                        scaleY = celebrationScale
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(PaddingExtraSmall)
                                                ) {
                                                    if (isAllDone) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.TaskAlt,
                                                            contentDescription = null,
                                                            tint = DraftoTheme.colors.emerald,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = if (isAllDone) stringResource(R.string.todo_all_done) else stringResource(R.string.todo_empty_title),
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontFamily = PoppinsFontFamily,
                                                            fontWeight = if (isAllDone) FontWeight.SemiBold else FontWeight.Normal
                                                        ),
                                                        color = if (isAllDone) DraftoTheme.colors.emerald else DraftoTheme.colors.textMuted
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        items(
                                            items = pendingTodos,
                                            key = { it.id },
                                            contentType = { "todo_item" }
                                        ) { todo ->
                                            val collectionName = remember(todo.collectionId, uiState.availableCollections) {
                                                uiState.availableCollections.firstOrNull { it.id == todo.collectionId }?.name
                                            }
                                            val isReducedMotion = LocalDraftoReducedMotion.current
                                            val itemModifier = if (isReducedMotion) {
                                                Modifier.animateItem(
                                                    fadeInSpec = snap(),
                                                    fadeOutSpec = snap(),
                                                    placementSpec = snap()
                                                )
                                            } else {
                                                Modifier.animateItem(
                                                    fadeInSpec = DraftoSprings.SnappyFloat,
                                                    fadeOutSpec = DraftoSprings.SnappyFloat,
                                                    placementSpec = DraftoSprings.SnappyOffset
                                                )
                                            }
                                            DraftoTodoItemCard(
                                                todo = todo,
                                                collectionName = collectionName,
                                                modifier = itemModifier,
                                                isSelected = uiState.selectedTodoIds.contains(todo.id),
                                                isSelectionMode = uiState.isTodosSelectionMode,
                                                onClick = {
                                                    if (uiState.isTodosSelectionMode) {
                                                        onToggleTodoSelection(todo.id)
                                                    } else {
                                                        onEditTodo(todo)
                                                    }
                                                },
                                                onLongClick = { onToggleTodoSelection(todo.id) },
                                                onEdit = { onEditTodo(todo) },
                                                onToggleCompleted = { isCompleted ->
                                                    onToggleTodoCompleted(todo, isCompleted)
                                                },
                                                onTogglePinned = { isPinned ->
                                                    onToggleTodoPinned(todo, isPinned)
                                                },
                                                onToggleSubtask = { subtaskId, isDone ->
                                                    onToggleSubtask(todo.id, subtaskId, isDone)
                                                },
                                                onDelete = {
                                                    onDeleteTodo(todo)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Header adhesivo superior con efecto Spatial Pure Blur Ultra Thin
        DraftoStickyHeader(
            isScrolled = isScrolled,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onGloballyPositioned { headerHeightPx = it.size.height }
        ) {
            DraftoSearchableTopBar(
                title = stringResource(R.string.tab_notebook),
                isSearchActive = uiState.isSearchActive,
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onCloseSearch = onCloseSearch,
                placeholder = if (uiState.selectedSection == NotebookSection.NOTES)
                    stringResource(R.string.placeholder_search_notes)
                else
                    stringResource(R.string.placeholder_search_notebook),
                actions = {
                    DraftoTopBarIconButton(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = stringResource(R.string.action_search),
                        onClick = onToggleSearch
                    )
                    DraftoTopBarIconButton(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.tab_options_title_notebook),
                        onClick = { onShowOptionsSheet(true) }
                    )
                }
            )

            NotebookSectionSelectorRow(
                currentSection = uiState.selectedSection,
                onSectionChange = onSectionChange
            )
        }

        if (uiState.showOptionsSheet) {
            DraftoTabOptionsBottomSheet(
                title = stringResource(R.string.tab_options_title_notebook),
                isGridView = if (uiState.selectedSection == NotebookSection.NOTES) uiState.isGridView else null,
                onToggleGridView = if (uiState.selectedSection == NotebookSection.NOTES) onToggleGridView else null,
                onBackupClick = { onShowBackupSheet(true) },
                selectedFilter = uiState.filterOption,
                onFilterChange = onSetFilterOption,
                selectedSort = uiState.sortOption,
                onSortChange = onSetSortOption,
                onDismissRequest = { onShowOptionsSheet(false) }
            )
        }

        if (uiState.showAddTodoSheet) {
            DraftoAddTodoBottomSheet(
                todoToEdit = uiState.editingTodo,
                availableCollections = uiState.availableCollections,
                initialCollectionId = uiState.editingTodo?.collectionId,
                onDismissRequest = onDismissAddTodoSheet,
                onSaveTodo = { title, desc, pinned, due ->
                    onSaveTodo(title, desc, pinned, due, uiState.editingTodo?.collectionId, uiState.editingTodo?.subtasks ?: emptyList())
                },
                onSaveTodoList = onSaveTodo
            )
        }

        if (uiState.showBackupSheet) {
            DraftoNotebookBackupBottomSheet(
                onDismissRequest = { onShowBackupSheet(false) },
                onExportZip = onExportZip,
                onImportZip = onImportZip,
                onImportSingleNote = onImportSingleNote,
                onImportSingleTodos = onImportSingleTodos
            )
        }

        uiState.backupProgressState?.let { progressState ->
            if (progressState.isActive) {
                DraftoProgressDialog(
                    title = if (progressState.isExport) {
                        stringResource(R.string.dialog_exporting_notebook)
                    } else {
                        stringResource(R.string.dialog_importing_notebook)
                    },
                    statusMessage = progressState.statusText,
                    progress = progressState.progress,
                    isExport = progressState.isExport
                )
            }
        }
    }
}

/**
 * Fila modular para el selector de sección (Notes vs To-dos), ubicada en la cabecera fija superior.
 * Mantiene un diseño minimalista Nothing OS centrado utilizando exclusivamente [DraftoCapsuleSelector].
 */
@Composable
private fun NotebookSectionSelectorRow(
    currentSection: NotebookSection,
    onSectionChange: (NotebookSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val notesLabel = stringResource(R.string.section_notes)
    val todoLabel = stringResource(R.string.section_todo)

    val selectorOptions = remember(notesLabel, todoLabel) {
        listOf(
            DraftoCapsuleOption(
                key = NotebookSection.NOTES,
                label = notesLabel,
                icon = Icons.Rounded.Book
            ),
            DraftoCapsuleOption(
                key = NotebookSection.TODOS,
                label = todoLabel,
                icon = Icons.Rounded.TaskAlt
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = PaddingSmall),
        contentAlignment = Alignment.Center
    ) {
        DraftoCapsuleSelector(
            options = selectorOptions,
            selectedKey = currentSection,
            onOptionSelected = onSectionChange,
            height = BookmarkCapsuleHeight,
            spacing = BookmarkCapsuleSpacing
        )
    }
}

/**
 * Cabecera interactiva y colapsable para las secciones de tareas (Completed y Pending).
 *
 * Sigue la estética minimalista de Nothing OS con tipografía Poppins,
 * píldora con conteo numérico de elementos, texto de acción contextual ("Show"/"Hide")
 * y chevron con rotación fluida mediante animación de resorte Snappy.
 */
@Composable
private fun TodoSectionHeader(
    title: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = DraftoSprings.SnappyFloat,
        label = "todoSectionChevronRotation"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(DraftoShapePill)
            .clickable(onClick = onToggle)
            .padding(horizontal = PaddingExtraSmall, vertical = TodoSectionHeaderPaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaddingSmall)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Badge de conteo numérico Nothing OS sin borde
            Box(
                modifier = Modifier
                    .clip(DraftoShapePill)
                    .background(DraftoTheme.colors.cardSurface)
                    .padding(
                        horizontal = TodoSectionBadgePaddingHorizontal,
                        vertical = TodoSectionBadgePaddingVertical
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = DraftoTheme.colors.accent
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaddingExtraSmall)
        ) {
            Text(
                text = stringResource(if (isExpanded) R.string.todo_section_hide else R.string.todo_section_show),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium
                ),
                color = DraftoTheme.colors.textMuted
            )

            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier
                    .size(TodoSectionChevronSize)
                    .graphicsLayer { rotationZ = rotation },
                tint = DraftoTheme.colors.textMuted
            )
        }
    }
}

/**
 * TopBar encapsulada para la pestaña de Notebook (Notas y To-dos).
 * Incluye botón de respaldo en la nube / almacenamiento, cambio de vista (Lista / Bento) y botón de búsqueda.
 */
@Composable
fun NotebookTopBar(
    isGridView: Boolean = false,
    showGridViewToggle: Boolean = true,
    onToggleGridView: () -> Unit = {},
    onToggleSearch: () -> Unit = {},
    onOpenBackup: () -> Unit = {},
    modifier: Modifier = Modifier,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
        DraftoTopBarIconButton(
            imageVector = Icons.Rounded.Cloud,
            contentDescription = stringResource(R.string.action_backup_notebook),
            onClick = onOpenBackup
        )
        if (showGridViewToggle) {
            DraftoTopBarIconButton(
                imageVector = if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                contentDescription = stringResource(if (isGridView) R.string.view_mode_list else R.string.view_mode_grid),
                onClick = onToggleGridView
            )
        }
        DraftoTopBarIconButton(
            imageVector = Icons.Default.Search,
            contentDescription = stringResource(R.string.action_search),
            onClick = onToggleSearch
        )
    }
) {
    DraftoTabTopBar(
        title = stringResource(R.string.tab_notebook),
        modifier = modifier,
        actions = actions
    )
}

@Composable
fun NotesTopBar(
    isGridView: Boolean = false,
    showGridViewToggle: Boolean = true,
    onToggleGridView: () -> Unit = {},
    onToggleSearch: () -> Unit = {},
    onOpenBackup: () -> Unit = {},
    modifier: Modifier = Modifier,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
        DraftoTopBarIconButton(
            imageVector = Icons.Rounded.Cloud,
            contentDescription = stringResource(R.string.action_backup_notebook),
            onClick = onOpenBackup
        )
        if (showGridViewToggle) {
            DraftoTopBarIconButton(
                imageVector = if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                contentDescription = stringResource(if (isGridView) R.string.view_mode_list else R.string.view_mode_grid),
                onClick = onToggleGridView
            )
        }
        DraftoTopBarIconButton(
            imageVector = Icons.Default.Search,
            contentDescription = stringResource(R.string.action_search),
            onClick = onToggleSearch
        )
    }
) = NotebookTopBar(isGridView, showGridViewToggle, onToggleGridView, onToggleSearch, onOpenBackup, modifier, actions)
