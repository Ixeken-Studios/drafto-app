package com.ixeken.drafto.ui.pins

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.ixeken.drafto.ui.components.DraftoCapsuleOption
import com.ixeken.drafto.ui.components.DraftoEmptyState
import com.ixeken.drafto.ui.components.DraftoPinBadge
import com.ixeken.drafto.ui.components.DraftoScrollableCapsuleSelector
import com.ixeken.drafto.ui.components.DraftoSearchableTopBar
import com.ixeken.drafto.ui.components.DraftoStickyScaffold
import com.ixeken.drafto.ui.components.DraftoTabTopBar
import com.ixeken.drafto.ui.components.DraftoTopBarIconButton
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.ixeken.drafto.core.R
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextDecoration
import com.ixeken.drafto.domain.model.Bookmark
import com.ixeken.drafto.domain.model.Note
import com.ixeken.drafto.domain.model.TodoItem
import com.ixeken.drafto.ui.components.DraftoBookmarkCard
import com.ixeken.drafto.ui.components.DraftoTodoItemCard
import com.ixeken.drafto.ui.components.formatTodoDueDate
import com.ixeken.drafto.ui.theme.HeightTopBar
import com.ixeken.drafto.ui.theme.PaddingScreenHorizontal
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.PoppinsFontFamily
import com.ixeken.drafto.ui.theme.DraftoShapeScreenBottom
import com.ixeken.drafto.ui.theme.DraftoShapeCard
import com.ixeken.drafto.ui.theme.DraftoShapePill
import com.ixeken.drafto.ui.theme.DraftoSprings
import com.ixeken.drafto.ui.theme.BookmarkCapsuleSpacing
import com.ixeken.drafto.ui.theme.BookmarkFallbackTopPadding
import com.ixeken.drafto.ui.theme.PaddingListBottomContent
import com.ixeken.drafto.ui.theme.PaddingMedium
import com.ixeken.drafto.ui.theme.PaddingSmall
import com.ixeken.drafto.ui.theme.PaddingLarge
import com.ixeken.drafto.ui.theme.PaddingExtraSmall
import com.ixeken.drafto.ui.theme.PinboardFilterCapsuleHeight
import com.ixeken.drafto.ui.theme.PinboardGridCardHeight
import com.ixeken.drafto.ui.theme.PinboardPinBadgeSize
import com.ixeken.drafto.ui.theme.PinboardPinBadgeSizeSmall
import com.ixeken.drafto.ui.theme.PinboardPinIconSize
import com.ixeken.drafto.ui.theme.PinboardPinIconSizeSmall
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Filtros de categoría para el tablero de accesos directos Pinboard.
 */
enum class PinboardFilter {
    ALL,
    NOTES,
    TODOS,
    BOOKMARKS
}

/**
 * Jerarquía sellada inmutable para el reciclaje heterogéneo a 120 FPS de elementos en Pinboard.
 */
sealed interface PinboardItem {
    val timestamp: Long

    data class NoteItem(val note: Note) : PinboardItem {
        override val timestamp: Long get() = note.updatedAt
    }
    data class TaskItem(val todo: TodoItem) : PinboardItem {
        override val timestamp: Long get() = todo.createdAt
    }
    data class BookmarkItem(val bookmark: Bookmark) : PinboardItem {
        override val timestamp: Long get() = bookmark.createdAt
    }
}

typealias PinnedItem = PinboardItem

@Composable
fun PinsScreen(
    pinnedNotes: List<Note>,
    pinnedTodos: List<TodoItem> = emptyList(),
    pinnedBookmarks: List<Bookmark> = emptyList(),
    isGridView: Boolean,
    isSearchActive: Boolean,
    searchQuery: String,
    onToggleGridView: () -> Unit = {},
    onToggleSearch: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    onNoteClick: (Note) -> Unit,
    onToggleTodoCompleted: (TodoItem, Boolean) -> Unit = { _, _ -> },
    onToggleSubtask: (todoId: String, subtaskId: String, isDone: Boolean) -> Unit = { _, _, _ -> },
    onTodoClick: (TodoItem) -> Unit = {},
    onBookmarkClick: (Bookmark) -> Unit = {},
    onAddNotificationNoteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = isSearchActive) {
        onCloseSearch()
    }

    var selectedFilter by remember { mutableStateOf(PinboardFilter.ALL) }

    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val isScrolled by remember(isGridView, isSearchActive) {
        derivedStateOf {
            isSearchActive || if (isGridView) {
                gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0
            } else {
                listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
            }
        }
    }

    val allPinnedItems = remember(pinnedNotes, pinnedTodos, pinnedBookmarks) {
        val notes = pinnedNotes.map { PinboardItem.NoteItem(it) }
        val todos = pinnedTodos.map { PinboardItem.TaskItem(it) }
        val bookmarks = pinnedBookmarks.map { PinboardItem.BookmarkItem(it) }
        (notes + todos + bookmarks).sortedByDescending { it.timestamp }
    }

    val counts = remember(allPinnedItems) {
        mapOf(
            PinboardFilter.ALL to allPinnedItems.size,
            PinboardFilter.NOTES to allPinnedItems.count { it is PinboardItem.NoteItem },
            PinboardFilter.TODOS to allPinnedItems.count { it is PinboardItem.TaskItem },
            PinboardFilter.BOOKMARKS to allPinnedItems.count { it is PinboardItem.BookmarkItem }
        )
    }

    val filteredItems = remember(allPinnedItems, selectedFilter, searchQuery) {
        val byType = when (selectedFilter) {
            PinboardFilter.ALL -> allPinnedItems
            PinboardFilter.NOTES -> allPinnedItems.filterIsInstance<PinboardItem.NoteItem>()
            PinboardFilter.TODOS -> allPinnedItems.filterIsInstance<PinboardItem.TaskItem>()
            PinboardFilter.BOOKMARKS -> allPinnedItems.filterIsInstance<PinboardItem.BookmarkItem>()
        }
        if (searchQuery.isBlank()) {
            byType
        } else {
            val q = searchQuery.trim()
            byType.filter { item ->
                when (item) {
                    is PinboardItem.NoteItem -> item.note.title.contains(q, ignoreCase = true) || item.note.content.contains(q, ignoreCase = true)
                    is PinboardItem.TaskItem -> item.todo.title.contains(q, ignoreCase = true) || item.todo.description.contains(q, ignoreCase = true)
                    is PinboardItem.BookmarkItem -> item.bookmark.title?.contains(q, ignoreCase = true) == true ||
                            item.bookmark.url.contains(q, ignoreCase = true) ||
                            item.bookmark.domain.contains(q, ignoreCase = true)
                }
            }
        }
    }

    DraftoStickyScaffold(
        isScrolled = isScrolled,
        header = {
            DraftoSearchableTopBar(
                title = stringResource(R.string.tab_pins),
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onCloseSearch = onCloseSearch,
                placeholder = stringResource(R.string.placeholder_search_pinboard),
                actions = {
                    DraftoTopBarIconButton(
                        imageVector = if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                        contentDescription = stringResource(if (isGridView) R.string.view_mode_list else R.string.view_mode_grid),
                        onClick = onToggleGridView
                    )
                    DraftoTopBarIconButton(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = stringResource(R.string.action_search),
                        onClick = onToggleSearch
                    )
                }
            )

            // Barra de filtros por categoría
            PinboardFilterSelectorRow(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                counts = counts
            )
        },
        fallbackTopPadding = BookmarkFallbackTopPadding,
        modifier = modifier.clip(DraftoShapeScreenBottom)
    ) { safeTopPadding ->
            if (filteredItems.isEmpty()) {
                val title = if (searchQuery.isNotBlank()) stringResource(R.string.empty_search_title) else stringResource(R.string.empty_pins_title)
                val description = if (searchQuery.isNotBlank()) stringResource(R.string.empty_search_description) else stringResource(R.string.empty_pins_description)
                val icon = if (searchQuery.isNotBlank()) Icons.Default.Search else Icons.Rounded.PushPin

                DraftoEmptyState(
                    icon = icon,
                    iconTint = DraftoTheme.colors.accent,
                    title = title,
                    description = description,
                    modifier = Modifier.padding(top = safeTopPadding)
                )
            } else if (isGridView) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = PaddingScreenHorizontal, top = safeTopPadding, end = PaddingScreenHorizontal, bottom = PaddingListBottomContent),
                    horizontalArrangement = Arrangement.spacedBy(PaddingMedium),
                    verticalArrangement = Arrangement.spacedBy(PaddingMedium),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = filteredItems,
                        key = { item ->
                            when (item) {
                                is PinboardItem.NoteItem -> "note_${item.note.id}"
                                is PinboardItem.TaskItem -> "todo_${item.todo.id}"
                                is PinboardItem.BookmarkItem -> "bm_${item.bookmark.id}"
                            }
                        },
                        contentType = { item ->
                            when (item) {
                                is PinboardItem.NoteItem -> 0
                                is PinboardItem.TaskItem -> 1
                                is PinboardItem.BookmarkItem -> 2
                            }
                        }
                    ) { item ->
                        when (item) {
                            is PinboardItem.NoteItem -> {
                                PinnedNoteGridCard(note = item.note, onClick = { onNoteClick(item.note) })
                            }
                            is PinboardItem.TaskItem -> {
                                PinnedTodoGridCard(
                                    todo = item.todo,
                                    onToggleCompleted = { onToggleTodoCompleted(item.todo, it) },
                                    onClick = { onTodoClick(item.todo) }
                                )
                            }
                            is PinboardItem.BookmarkItem -> {
                                DraftoBookmarkCard(
                                    bookmark = item.bookmark,
                                    isGridView = true,
                                    onClick = { onBookmarkClick(item.bookmark) }
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = PaddingScreenHorizontal, top = safeTopPadding, end = PaddingScreenHorizontal, bottom = PaddingListBottomContent),
                    verticalArrangement = Arrangement.spacedBy(PaddingSmall),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = filteredItems,
                        key = { item ->
                            when (item) {
                                is PinboardItem.NoteItem -> "note_${item.note.id}"
                                is PinboardItem.TaskItem -> "todo_${item.todo.id}"
                                is PinboardItem.BookmarkItem -> "bm_${item.bookmark.id}"
                            }
                        },
                        contentType = { item ->
                            when (item) {
                                is PinboardItem.NoteItem -> 0
                                is PinboardItem.TaskItem -> 1
                                is PinboardItem.BookmarkItem -> 2
                            }
                        }
                    ) { item ->
                        when (item) {
                            is PinboardItem.NoteItem -> {
                                PinnedNoteListItem(note = item.note, onClick = { onNoteClick(item.note) })
                            }
                            is PinboardItem.TaskItem -> {
                                DraftoTodoItemCard(
                                    todo = item.todo,
                                    onClick = { onTodoClick(item.todo) },
                                    onEdit = { onTodoClick(item.todo) },
                                    onToggleCompleted = { onToggleTodoCompleted(item.todo, it) },
                                    onTogglePinned = {},
                                    onToggleSubtask = { subtaskId, isDone ->
                                        onToggleSubtask(item.todo.id, subtaskId, isDone)
                                    },
                                    onDelete = {}
                                )
                            }
                            is PinboardItem.BookmarkItem -> {
                                DraftoBookmarkCard(
                                    bookmark = item.bookmark,
                                    isGridView = false,
                                    onClick = { onBookmarkClick(item.bookmark) }
                                )
                            }
                        }
                    }
                }
        }
    }
}

/**
 * Selector horizontal por cápsulas de categoría para el tablero Pinboard con físicas elásticas Snappy.
 */
@Composable
private fun PinboardFilterSelectorRow(
    selectedFilter: PinboardFilter,
    onFilterSelected: (PinboardFilter) -> Unit,
    counts: Map<PinboardFilter, Int>,
    modifier: Modifier = Modifier
) {
    val allLabel = stringResource(R.string.pinboard_filter_all)
    val notesLabel = stringResource(R.string.pinboard_filter_notes)
    val todosLabel = stringResource(R.string.pinboard_filter_todos)
    val bookmarksLabel = stringResource(R.string.pinboard_filter_bookmarks)

    val capsuleOptions = remember(counts, allLabel, notesLabel, todosLabel, bookmarksLabel) {
        listOf(
            DraftoCapsuleOption(
                key = PinboardFilter.ALL,
                label = (counts[PinboardFilter.ALL] ?: 0).let { if (it > 0) "$allLabel $it" else allLabel },
                icon = Icons.Rounded.PushPin
            ),
            DraftoCapsuleOption(
                key = PinboardFilter.NOTES,
                label = (counts[PinboardFilter.NOTES] ?: 0).let { if (it > 0) "$notesLabel $it" else notesLabel },
                icon = Icons.Rounded.Description
            ),
            DraftoCapsuleOption(
                key = PinboardFilter.TODOS,
                label = (counts[PinboardFilter.TODOS] ?: 0).let { if (it > 0) "$todosLabel $it" else todosLabel },
                icon = Icons.Rounded.TaskAlt
            ),
            DraftoCapsuleOption(
                key = PinboardFilter.BOOKMARKS,
                label = (counts[PinboardFilter.BOOKMARKS] ?: 0).let { if (it > 0) "$bookmarksLabel $it" else bookmarksLabel },
                icon = Icons.Rounded.Bookmark
            )
        )
    }

    DraftoScrollableCapsuleSelector(
        options = capsuleOptions,
        selectedKey = selectedFilter,
        onOptionSelected = onFilterSelected,
        contentPadding = PaddingValues(horizontal = PaddingScreenHorizontal, vertical = PaddingSmall),
        modifier = modifier.fillMaxWidth()
    )
}


@Composable
private fun PinnedNoteListItem(note: Note, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = DraftoShapeCard,
        modifier = Modifier
            .fillMaxWidth()
            .clip(DraftoShapeCard)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(PaddingLarge)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val untitledText = stringResource(R.string.untitled_note)
                Text(
                    text = stringResource(R.string.badge_note, note.title.ifBlank { untitledText }),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                DraftoPinBadge(
                    size = PinboardPinBadgeSizeSmall,
                    iconSize = PinboardPinIconSizeSmall
                )
            }
            Spacer(modifier = Modifier.height(PaddingExtraSmall))
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                color = DraftoTheme.colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PinnedNoteGridCard(note: Note, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = DraftoShapeCard,
        modifier = Modifier
            .fillMaxWidth()
            .height(PinboardGridCardHeight)
            .clip(DraftoShapeCard)
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(PaddingMedium)) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = note.title.ifBlank { stringResource(R.string.untitled_note) },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DraftoTheme.colors.textSecondary,
                    maxLines = 4
                )
                Text(
                    text = formatDate(note.updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = DraftoTheme.colors.textMuted
                )
            }
            DraftoPinBadge(
                modifier = Modifier.align(Alignment.TopEnd),
                size = PinboardPinBadgeSize,
                iconSize = PinboardPinIconSize
            )
        }
    }
}

@Composable
private fun PinnedTodoGridCard(
    todo: TodoItem,
    onToggleCompleted: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val contentAlpha by animateFloatAsState(
        targetValue = if (todo.isCompleted) 0.5f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "todoContentAlpha"
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = DraftoShapeCard,
        modifier = Modifier
            .fillMaxWidth()
            .height(PinboardGridCardHeight)
            .clip(DraftoShapeCard)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingMedium)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = contentAlpha },
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header: Checkbox + Title
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = PinboardPinBadgeSize + PaddingExtraSmall)
                ) {
                    val checkboxBgColor by animateColorAsState(
                        targetValue = if (todo.isCompleted) DraftoTheme.colors.emerald else Color.Transparent,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "todoGridCheckboxBg"
                    )
                    val checkboxBorderColor by animateColorAsState(
                        targetValue = if (todo.isCompleted) DraftoTheme.colors.emerald else DraftoTheme.colors.textMuted.copy(alpha = 0.45f),
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "todoGridCheckboxBorder"
                    )

                    Box(
                        modifier = Modifier
                            .size(TodoCheckboxSize)
                            .clip(CircleShape)
                            .background(checkboxBgColor)
                            .border(1.5.dp, checkboxBorderColor, CircleShape)
                            .clickable { onToggleCompleted(!todo.isCompleted) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (todo.isCompleted) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(PinboardPinIconSize)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(PaddingSmall))

                    Text(
                        text = todo.title.ifBlank { stringResource(R.string.untitled_note) },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Description / Subtasks summary
                if (todo.hasSubtasks) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(PaddingMicro)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.todo_progress_summary, todo.completedSubtasksCount, todo.totalSubtasksCount),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = if (todo.isAllCompleted) DraftoTheme.colors.emerald else DraftoTheme.colors.textMuted
                            )
                        }

                        val isReducedMotion = LocalDraftoReducedMotion.current
                        val animatedProgress by animateFloatAsState(
                            targetValue = todo.progressPercentage,
                            animationSpec = DraftoTransitions.progressSpec(isReducedMotion),
                            label = "pinnedTodoProgress"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(TodoProgressBarHeight)
                                .clip(DraftoShapePill)
                                .background(DraftoTheme.colors.divider)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(TodoProgressBarHeight)
                                    .graphicsLayer {
                                        scaleX = animatedProgress
                                        transformOrigin = TransformOrigin(0f, 0f)
                                    }
                                    .clip(DraftoShapePill)
                                    .background(DraftoTheme.colors.emerald)
                            )
                        }
                    }
                } else if (todo.description.isNotBlank()) {
                    Text(
                        text = todo.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = DraftoTheme.colors.textSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Spacer(modifier = Modifier.height(PaddingExtraSmall))
                }

                // Due Date or Creation Date
                val todayText = stringResource(R.string.date_today)
                val yesterdayText = stringResource(R.string.date_yesterday)
                val dueDate = todo.dueDate
                val dateLabel = if (dueDate != null) {
                    formatTodoDueDate(dueDate, todayText, yesterdayText)
                } else {
                    formatDate(todo.createdAt)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PaddingExtraSmall)
                ) {
                    if (dueDate != null) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = if (dueDate < System.currentTimeMillis() && !todo.isCompleted) {
                                DraftoTheme.colors.sunsetCoral
                            } else {
                                DraftoTheme.colors.textMuted
                            },
                            modifier = Modifier.size(PinboardPinIconSizeSmall)
                        )
                    }
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (dueDate != null && dueDate < System.currentTimeMillis() && !todo.isCompleted) {
                            DraftoTheme.colors.sunsetCoral
                        } else {
                            DraftoTheme.colors.textMuted
                        }
                    )
                }
            }

            // Pin Badge
            DraftoPinBadge(
                modifier = Modifier.align(Alignment.TopEnd),
                size = PinboardPinBadgeSize,
                iconSize = PinboardPinIconSize
            )
        }
    }
}

private val PinDateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

private fun formatDate(timestamp: Long): String {
    return synchronized(PinDateFormat) {
        PinDateFormat.format(Date(timestamp))
    }
}

/**
 * TopBar encapsulada para la pestaña de Pins.
 * Incluye botón de cambio de vista (Lista / Bento) y botón de búsqueda.
 */
@Composable
fun PinsTopBar(
    isGridView: Boolean = false,
    onToggleGridView: () -> Unit = {},
    onToggleSearch: () -> Unit = {},
    modifier: Modifier = Modifier,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
        DraftoTopBarIconButton(
            imageVector = if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
            contentDescription = stringResource(if (isGridView) R.string.view_mode_list else R.string.view_mode_grid),
            onClick = onToggleGridView
        )
        DraftoTopBarIconButton(
            imageVector = Icons.Default.Search,
            contentDescription = stringResource(R.string.action_search),
            onClick = onToggleSearch
        )
    }
) {
    DraftoTabTopBar(
        title = stringResource(R.string.tab_pins),
        modifier = modifier,
        actions = actions
    )
}

/**
 * Alias público y canónico para la pantalla Pinboard.
 */
@Composable
fun PinboardScreen(
    pinnedNotes: List<Note>,
    pinnedTodos: List<TodoItem> = emptyList(),
    pinnedBookmarks: List<Bookmark> = emptyList(),
    isGridView: Boolean,
    isSearchActive: Boolean,
    searchQuery: String,
    onToggleGridView: () -> Unit = {},
    onToggleSearch: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    onNoteClick: (Note) -> Unit,
    onToggleTodoCompleted: (TodoItem, Boolean) -> Unit = { _, _ -> },
    onTodoClick: (TodoItem) -> Unit = {},
    onBookmarkClick: (Bookmark) -> Unit = {},
    onAddNotificationNoteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) = PinsScreen(
    pinnedNotes = pinnedNotes,
    pinnedTodos = pinnedTodos,
    pinnedBookmarks = pinnedBookmarks,
    isGridView = isGridView,
    isSearchActive = isSearchActive,
    searchQuery = searchQuery,
    onToggleGridView = onToggleGridView,
    onToggleSearch = onToggleSearch,
    onSearchQueryChange = onSearchQueryChange,
    onCloseSearch = onCloseSearch,
    onNoteClick = onNoteClick,
    onToggleTodoCompleted = onToggleTodoCompleted,
    onTodoClick = onTodoClick,
    onBookmarkClick = onBookmarkClick,
    onAddNotificationNoteClick = onAddNotificationNoteClick,
    modifier = modifier
)

/**
 * Alias público y canónico para la barra superior de Pinboard.
 */
@Composable
fun PinboardTopBar(
    isGridView: Boolean = false,
    onToggleGridView: () -> Unit = {},
    onToggleSearch: () -> Unit = {},
    modifier: Modifier = Modifier,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
        DraftoTopBarIconButton(
            imageVector = if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
            contentDescription = stringResource(if (isGridView) R.string.view_mode_list else R.string.view_mode_grid),
            onClick = onToggleGridView
        )
        DraftoTopBarIconButton(
            imageVector = Icons.Default.Search,
            contentDescription = stringResource(R.string.action_search),
            onClick = onToggleSearch
        )
    }
) = PinsTopBar(
    isGridView = isGridView,
    onToggleGridView = onToggleGridView,
    onToggleSearch = onToggleSearch,
    modifier = modifier,
    actions = actions
)


