package com.ixeken.drafto.ui.collections

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import com.ixeken.drafto.core.R
import com.ixeken.drafto.domain.model.Bookmark
import com.ixeken.drafto.domain.model.DraftoCollection
import com.ixeken.drafto.domain.model.Note
import com.ixeken.drafto.domain.model.TodoItem
import com.ixeken.drafto.ui.components.DraftoAddTodoBottomSheet
import com.ixeken.drafto.ui.components.DraftoBookmarkCard
import com.ixeken.drafto.ui.components.DraftoBookmarkDetailBottomSheet
import com.ixeken.drafto.ui.components.DraftoBookmarkFormBottomSheet
import com.ixeken.drafto.ui.components.DraftoCapsuleOption
import com.ixeken.drafto.ui.components.DraftoCapsuleSelector
import com.ixeken.drafto.ui.components.DraftoCollectionFormBottomSheet
import com.ixeken.drafto.ui.components.DraftoCollectionTypeChooserBottomSheet
import com.ixeken.drafto.ui.components.DraftoConfirmationDialog
import com.ixeken.drafto.ui.components.DraftoEmptyState
import com.ixeken.drafto.ui.components.DraftoFloatingActionCapsule
import com.ixeken.drafto.ui.components.DraftoNoteCard
import com.ixeken.drafto.ui.components.DraftoSearchableTopBar
import com.ixeken.drafto.ui.components.DraftoStickyHeader
import com.ixeken.drafto.ui.components.DraftoTodoItemCard
import com.ixeken.drafto.ui.components.DraftoTopBarIconButton
import com.ixeken.drafto.ui.components.parseCollectionColor
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.LocalHazeState
import com.ixeken.drafto.ui.theme.PaddingListBottomContent
import com.ixeken.drafto.ui.theme.PaddingMedium
import com.ixeken.drafto.ui.theme.PaddingNavBarBottom
import com.ixeken.drafto.ui.theme.PaddingScreenHorizontal
import com.ixeken.drafto.ui.theme.PaddingSmall
import com.ixeken.drafto.ui.utils.DraftoSystemInteractions
import dev.chrisbanes.haze.hazeSource

/**
 * Pantalla detallada de una Colección Global unificada.
 *
 * Despliega notas, tareas y marcadores asociados a la colección mediante sub-pestañas
 * de diseño Nothing OS (`DraftoCapsuleSelector`), ofreciendo un botón flotante (+) contextual
 * pre-asignado a la carpeta actual.
 */
@Composable
fun CollectionDetailScreen(
    uiState: CollectionDetailUiState,
    onBackClick: () -> Unit,
    onSubTabSelected: (CollectionSubTab) -> Unit,
    onNoteClick: (noteId: String) -> Unit,
    onNewNoteClick: () -> Unit,
    onToggleNotePinned: (Note) -> Unit,
    onDeleteNote: (String) -> Unit,
    onToggleTodoCompleted: (TodoItem, Boolean) -> Unit,
    onToggleTodoPinned: (TodoItem, Boolean) -> Unit,
    onToggleSubtask: (todoId: String, subtaskId: String, isDone: Boolean) -> Unit = { _, _, _ -> },
    onEditTodo: (TodoItem) -> Unit,
    onDeleteTodo: (TodoItem) -> Unit,
    onOpenAddTodoSheet: () -> Unit,
    onDismissAddTodoSheet: () -> Unit,
    onSaveTodo: (title: String, description: String, isPinned: Boolean, dueDate: Long?) -> Unit,
    onSaveTodoList: ((title: String, description: String, isPinned: Boolean, dueDate: Long?, collectionId: String?, subtasks: List<com.ixeken.drafto.domain.model.TodoSubtask>) -> Unit)? = null,
    onToggleBookmarkPin: (Bookmark) -> Unit,
    onDeleteBookmark: (String) -> Unit,
    onBookmarkClick: ((Bookmark) -> Unit)? = null,
    onOpenAddBookmarkSheet: () -> Unit = {},
    onDismissAddBookmarkSheet: () -> Unit = {},
    onSaveBookmark: (url: String, title: String?, description: String?) -> Unit = { _, _, _ -> },
    onFabClick: () -> Unit = {},
    onSelectTodoFromChooser: () -> Unit = onOpenAddTodoSheet,
    onSelectBookmarkFromChooser: () -> Unit = onOpenAddBookmarkSheet,
    onOpenTypeChooserSheet: () -> Unit = {},
    onDismissTypeChooserSheet: () -> Unit = {},
    onOpenEditCollectionSheet: () -> Unit,
    onDismissEditCollectionSheet: () -> Unit,
    onUpdateCollection: (name: String, colorHex: String, iconName: String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: (Boolean?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val handleBookmarkClick: (Bookmark) -> Unit = remember(onBookmarkClick, context) {
        onBookmarkClick ?: { bookmark ->
            DraftoSystemInteractions.openUrl(context, bookmark.url)
            Unit
        }
    }
    var todoPendingDelete by remember { mutableStateOf<TodoItem?>(null) }
    var notePendingDelete by remember { mutableStateOf<Note?>(null) }
    var pendingExportNote by remember { mutableStateOf<Note?>(null) }
    var selectedBookmarkForDetail by remember { mutableStateOf<Bookmark?>(null) }
    BackHandler(enabled = uiState.isSearchActive) {
        onToggleSearch(false)
    }

    val exportSingleNoteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null) {
            val note = pendingExportNote
            if (note != null) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        val content = "# ${note.title}\n\n${note.content}"
                        outputStream.write(content.toByteArray(Charsets.UTF_8))
                    }
                } catch (_: Exception) {
                }
            }
        }
        pendingExportNote = null
    }

    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    var isFabVisible by remember { mutableStateOf(true) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -12f) {
                    isFabVisible = false
                } else if (available.y > 12f) {
                    isFabVisible = true
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(uiState.effectiveSubTab) {
        isFabVisible = true
    }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
            isFabVisible = true
        }
    }

    var headerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val safeTopPadding = remember(headerHeightPx, density) {
        with(density) { headerHeightPx.toDp() } + PaddingSmall
    }

    val collection = uiState.collection
    val isDarkTheme = DraftoTheme.colors.isDark
    val collectionColor = remember(collection?.colorHex, isDarkTheme) {
        if (collection != null) parseCollectionColor(collection.colorHex, isDark = isDarkTheme) else Color.Gray
    }

    val subTabOptions = remember(uiState.activeTabs, uiState.notes.size, uiState.todos.size, uiState.bookmarks.size) {
        uiState.activeTabs.mapNotNull { tab ->
            when (tab) {
                CollectionSubTab.NOTES -> DraftoCapsuleOption(
                    key = CollectionSubTab.NOTES,
                    label = "Notas (${uiState.notes.size})",
                    icon = Icons.Rounded.Book
                )
                CollectionSubTab.TODOS -> DraftoCapsuleOption(
                    key = CollectionSubTab.TODOS,
                    label = "To-dos (${uiState.todos.size})",
                    icon = Icons.Rounded.TaskAlt
                )
                CollectionSubTab.BOOKMARKS -> DraftoCapsuleOption(
                    key = CollectionSubTab.BOOKMARKS,
                    label = "Marcadores (${uiState.bookmarks.size})",
                    icon = Icons.Rounded.Bookmark
                )
            }
        }
    }

    val hazeState = LocalHazeState.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(nestedScrollConnection)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier)
        ) {
            // Contenido de la sub-pestaña seleccionada o estado vacío global de la colección
        if (uiState.isCollectionEmpty) {
            DraftoEmptyState(
                icon = Icons.Rounded.Folder,
                iconTint = collectionColor,
                title = stringResource(R.string.collection_empty_items_title),
                description = stringResource(R.string.collection_empty_items_desc),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = safeTopPadding)
            )
        } else {
            when (uiState.effectiveSubTab) {
            CollectionSubTab.NOTES -> {
                if (uiState.notes.isEmpty()) {
                    DraftoEmptyState(
                        icon = Icons.Rounded.Book,
                        iconTint = collectionColor,
                        title = stringResource(R.string.collection_empty_items_title),
                        description = stringResource(R.string.collection_empty_items_desc),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = safeTopPadding)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            start = PaddingScreenHorizontal,
                            top = safeTopPadding,
                            end = PaddingScreenHorizontal,
                            bottom = PaddingListBottomContent
                        ),
                        verticalArrangement = Arrangement.spacedBy(PaddingMedium),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.notes,
                            key = { it.id },
                            contentType = { "collection_note" }
                        ) { note ->
                            DraftoNoteCard(
                                note = note,
                                collectionName = uiState.collection?.name,
                                onClick = { onNoteClick(note.id) },
                                onTogglePinned = { onToggleNotePinned(note) },
                                onDelete = { notePendingDelete = note },
                                onExport = {
                                    pendingExportNote = note
                                    val sanitizedTitle = note.title.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "note" }
                                    exportSingleNoteLauncher.launch("$sanitizedTitle.md")
                                }
                            )
                        }
                    }
                }
            }
            CollectionSubTab.TODOS -> {
                if (uiState.todos.isEmpty()) {
                    DraftoEmptyState(
                        icon = Icons.Rounded.TaskAlt,
                        iconTint = DraftoTheme.colors.accent,
                        title = stringResource(R.string.todo_empty_title),
                        description = stringResource(R.string.collection_empty_items_desc),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = safeTopPadding)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            start = PaddingScreenHorizontal,
                            top = safeTopPadding,
                            end = PaddingScreenHorizontal,
                            bottom = PaddingListBottomContent
                        ),
                        verticalArrangement = Arrangement.spacedBy(PaddingMedium),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.todos,
                            key = { it.id },
                            contentType = { "collection_todo" }
                        ) { todo ->
                            DraftoTodoItemCard(
                                todo = todo,
                                collectionName = uiState.collection?.name,
                                onToggleCompleted = { onToggleTodoCompleted(todo, !todo.isCompleted) },
                                onTogglePinned = { onToggleTodoPinned(todo, !todo.isPinned) },
                                onToggleSubtask = { subtaskId, isDone ->
                                    onToggleSubtask(todo.id, subtaskId, isDone)
                                },
                                onClick = { onEditTodo(todo) },
                                onEdit = { onEditTodo(todo) },
                                onDelete = { todoPendingDelete = todo }
                            )
                        }
                    }
                }
            }
            CollectionSubTab.BOOKMARKS -> {
                if (uiState.bookmarks.isEmpty()) {
                    DraftoEmptyState(
                        icon = Icons.Rounded.Bookmark,
                        iconTint = DraftoTheme.colors.accent,
                        title = stringResource(R.string.bookmark_empty_title),
                        description = stringResource(R.string.collection_empty_items_desc),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = safeTopPadding)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            start = PaddingScreenHorizontal,
                            top = safeTopPadding,
                            end = PaddingScreenHorizontal,
                            bottom = PaddingListBottomContent
                        ),
                        verticalArrangement = Arrangement.spacedBy(PaddingMedium),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.bookmarks,
                            key = { it.id },
                            contentType = { "collection_bookmark" }
                        ) { bookmark ->
                            DraftoBookmarkCard(
                                bookmark = bookmark,
                                onClick = { handleBookmarkClick(bookmark) },
                                onInfoClick = { selectedBookmarkForDetail = bookmark },
                                onTogglePin = { onToggleBookmarkPin(bookmark) }
                            )
                        }
                    }
                }
            }
        }
    }
        }

        // Encabezado Adhesivo Superior con Selector de Sub-pestañas
        DraftoStickyHeader(
            isScrolled = isScrolled,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onGloballyPositioned { coordinates ->
                    headerHeightPx = coordinates.size.height
                }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DraftoSearchableTopBar(
                    title = collection?.name ?: "",
                    isSearchActive = uiState.isSearchActive,
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    onCloseSearch = { onToggleSearch(false) },
                    navigationIcon = {
                        DraftoTopBarIconButton(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            onClick = onBackClick
                        )
                    },
                    actions = {
                        DraftoTopBarIconButton(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.action_edit_collection),
                            onClick = onOpenEditCollectionSheet
                        )
                        DraftoTopBarIconButton(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = stringResource(R.string.action_search),
                            onClick = { onToggleSearch(true) }
                        )
                    }
                )

                // Sub-pestañas Nothing OS con contadores dinámicos (solo si hay 2 o más tipos)
                if (uiState.isCapsuleSelectorVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PaddingScreenHorizontal, vertical = PaddingSmall),
                        contentAlignment = Alignment.Center
                    ) {
                        DraftoCapsuleSelector(
                            options = subTabOptions,
                            selectedKey = uiState.effectiveSubTab,
                            onOptionSelected = onSubTabSelected
                        )
                    }
                }
            }
        }

        // Botón Flotante Contextual Nothing OS 5 con Scroll-Aware Quick-Return
        val fabText = when (uiState.fabAction) {
            CollectionFabAction.CREATE_NOTE -> stringResource(R.string.action_new_note)
            CollectionFabAction.CREATE_TODO -> stringResource(R.string.action_new_todo)
            CollectionFabAction.CREATE_BOOKMARK -> stringResource(R.string.action_new_saved)
            CollectionFabAction.OPEN_TYPE_CHOOSER -> stringResource(R.string.action_add)
        }

        DraftoFloatingActionCapsule(
            text = fabText,
            onClick = onFabClick,
            visible = isFabVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(
                    WindowInsets.navigationBars
                        .union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                )
                .padding(bottom = PaddingNavBarBottom)
        )
    }

    // Modal para Editar Metadatos de la Colección
    if (uiState.isEditCollectionSheetVisible && collection != null) {
        DraftoCollectionFormBottomSheet(
            initialCollection = collection,
            onDismiss = onDismissEditCollectionSheet,
            onSaveCollection = onUpdateCollection
        )
    }

    // Modal para Crear / Editar Tarea dentro de esta Colección
    if (uiState.isAddTodoSheetVisible) {
        DraftoAddTodoBottomSheet(
            onDismissRequest = onDismissAddTodoSheet,
            onSaveTodo = onSaveTodo,
            onSaveTodoList = onSaveTodoList,
            todoToEdit = uiState.editingTodo,
            initialCollectionId = collection?.id
        )
    }

    // Modal para Crear Marcador dentro de esta Colección
    if (uiState.isAddBookmarkSheetVisible) {
        DraftoBookmarkFormBottomSheet(
            isEditMode = false,
            initialCollectionId = collection?.id,
            onDismiss = onDismissAddBookmarkSheet,
            onSave = { url, title, description, _ ->
                onSaveBookmark(url, title, description)
            }
        )
    }

    // Modal Nothing OS para elegir tipo de contenido en colección vacía
    if (uiState.isTypeChooserSheetVisible) {
        DraftoCollectionTypeChooserBottomSheet(
            onDismissRequest = onDismissTypeChooserSheet,
            onSelectNote = onNewNoteClick,
            onSelectTodo = onSelectTodoFromChooser,
            onSelectBookmark = onSelectBookmarkFromChooser
        )
    }

    selectedBookmarkForDetail?.let { bookmark ->
        DraftoBookmarkDetailBottomSheet(
            bookmark = bookmark,
            onDismiss = { selectedBookmarkForDetail = null },
            onOpenInBrowser = { url ->
                selectedBookmarkForDetail = null
                DraftoSystemInteractions.openUrl(context, url)
            },
            onCopyUrl = { url ->
                DraftoSystemInteractions.copyToClipboard(context, url)
            },
            onShareUrl = { url ->
                DraftoSystemInteractions.shareText(context, url)
            },
            onDeleteBookmark = { id ->
                onDeleteBookmark(id)
                selectedBookmarkForDetail = null
            }
        )
    }

    if (notePendingDelete != null) {
        DraftoConfirmationDialog(
            title = stringResource(R.string.dialog_delete_note_title),
            message = stringResource(R.string.dialog_delete_note_message),
            icon = Icons.Rounded.Delete,
            confirmText = stringResource(R.string.action_delete),
            cancelText = stringResource(R.string.action_cancel),
            isDestructive = true,
            onConfirm = {
                notePendingDelete?.let { onDeleteNote(it.id) }
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
                todoPendingDelete?.let { onDeleteTodo(it) }
                todoPendingDelete = null
            },
            onDismiss = { todoPendingDelete = null }
        )
    }
}
