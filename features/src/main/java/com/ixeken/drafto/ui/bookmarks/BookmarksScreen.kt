package com.ixeken.drafto.ui.bookmarks

import android.content.Context
import com.ixeken.drafto.ui.utils.DraftoSystemInteractions
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import com.ixeken.drafto.domain.model.TabFilterOption
import com.ixeken.drafto.domain.model.TabSortOption
import com.ixeken.drafto.ui.components.DraftoTabOptionsBottomSheet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ixeken.drafto.core.R
import com.ixeken.drafto.data.backup.BookmarkExportFormat
import com.ixeken.drafto.domain.model.Bookmark
import com.ixeken.drafto.domain.model.BookmarkCollection
import com.ixeken.drafto.domain.model.PlatformFilter
import com.ixeken.drafto.ui.components.DraftoAddBookmarkBottomSheet
import com.ixeken.drafto.ui.components.DraftoBookmarkBackupBottomSheet
import com.ixeken.drafto.ui.components.DraftoBookmarkCard
import com.ixeken.drafto.ui.components.DraftoBookmarkDetailBottomSheet
import com.ixeken.drafto.ui.components.DraftoCollectionFormBottomSheet
import com.ixeken.drafto.ui.components.DraftoConfirmationDialog
import com.ixeken.drafto.ui.components.DraftoCreateCollectionBottomSheet
import com.ixeken.drafto.ui.components.DraftoEmptyState
import com.ixeken.drafto.ui.components.DraftoFolderCard
import com.ixeken.drafto.ui.components.DraftoProgressDialog
import com.ixeken.drafto.ui.theme.DraftoShapePill
import com.ixeken.drafto.ui.components.DraftoSearchableTopBar
import com.ixeken.drafto.ui.components.DraftoStickyHeader
import com.ixeken.drafto.ui.components.DraftoTopBarIconButton
import com.ixeken.drafto.ui.theme.BookmarkFallbackTopPadding
import com.ixeken.drafto.ui.theme.BookmarkTabSwitchHeight
import com.ixeken.drafto.ui.theme.BookmarkTabSwitchIconSize
import com.ixeken.drafto.ui.theme.BookmarkTabSwitchInnerPadding
import com.ixeken.drafto.ui.theme.BookmarkTabSwitchPaddingVertical
import com.ixeken.drafto.ui.theme.BookmarkTabSwitchSpacing
import com.ixeken.drafto.ui.theme.DraftoShapeScreenBottom
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.LocalHazeState
import com.ixeken.drafto.ui.theme.PaddingListBottomContent
import com.ixeken.drafto.ui.theme.PaddingMedium
import com.ixeken.drafto.ui.theme.PaddingScreenHorizontal
import com.ixeken.drafto.ui.theme.PaddingSmall
import dev.chrisbanes.haze.hazeSource

/**
 * Pantalla principal de marcadores (Bookmarks) de Drafto.
 *
 * Conecta el estado reactivo proveniente de [BookmarksViewModel] con los componentes
 * de presentacion optimizados para 120 FPS sin recomposiciones accidentales.
 *
 * @param modifier Modificador Compose opcional.
 * @param viewModel Instancia inyectada por Hilt de [BookmarksViewModel].
 */
@Composable
fun BookmarksScreen(
    modifier: Modifier = Modifier,
    viewModel: BookmarksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Observador de ciclo de vida para detectar enlaces en el portapapeles al reanudar la app
    val isAutoDetectEnabled = uiState.isClipboardAutoDetectEnabled
    DisposableEffect(lifecycleOwner, isAutoDetectEnabled) {
        if (!isAutoDetectEnabled) {
            return@DisposableEffect onDispose { }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val clipText = DraftoSystemInteractions.getPrimaryClipText(context, onlyIfChanged = true)
                viewModel.checkClipboard(clipText)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Intercepción del botón Atrás del sistema para limpiar selección o cerrar búsqueda
    androidx.activity.compose.BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.clearSelection()
    }
    androidx.activity.compose.BackHandler(enabled = uiState.isSearchActive && !uiState.isSelectionMode) {
        viewModel.toggleSearch()
    }

    val onOpenUrl: (String) -> Unit = { url -> DraftoSystemInteractions.openUrl(context, url) }
    val onCopyUrl: (String) -> Unit = { url -> DraftoSystemInteractions.copyToClipboard(context, url) }
    val onShareUrl: (String) -> Unit = { url -> DraftoSystemInteractions.shareText(context, url) }

    val exportHtmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
    ) { uri ->
        if (uri != null) viewModel.onExportUriSelected(uri, BookmarkExportFormat.HTML)
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.onExportUriSelected(uri, BookmarkExportFormat.JSON)
    }

    val importBookmarksLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.onImportUriSelected(uri)
    }

    BookmarksScreenContent(
        uiState = uiState,
        onSetViewMode = viewModel::setViewMode,
        onToggleGridView = viewModel::toggleGridView,
        onToggleSearch = viewModel::toggleSearch,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onCloseSearch = viewModel::closeSearch,
        onSelectCollection = viewModel::selectCollection,
        onSelectPlatform = viewModel::selectPlatform,
        onShowCreateCollection = viewModel::setShowCreateCollectionDialog,
        onCreateCollection = viewModel::createCollection,
        onUpdateCollection = viewModel::updateCollection,
        onDeleteCollection = viewModel::deleteCollection,
        onOpenDetail = viewModel::openDetail,
        onCloseDetail = viewModel::closeDetail,
        onTogglePin = viewModel::togglePin,
        onDeleteBookmark = viewModel::deleteBookmark,
        onUpdateBookmark = viewModel::updateBookmark,
        onToggleBookmarkCollection = viewModel::toggleBookmarkCollection,
        onTogglePreviewVisibility = viewModel::toggleBookmarkPreviewVisibility,
        onToggleSelection = viewModel::toggleSelection,
        onClearSelection = viewModel::clearSelection,
        onShowDeleteSelectedConfirmDialog = viewModel::setShowDeleteSelectedConfirmDialog,
        onDeleteSelectedBookmarks = viewModel::deleteSelectedBookmarks,
        onDismissClipboard = viewModel::dismissClipboard,
        onSaveClipboardBookmark = viewModel::saveClipboardBookmark,
        onShowAddBookmark = viewModel::setShowAddBookmarkSheet,
        onSaveBookmark = { url, title, desc, colId ->
            viewModel.saveBookmark(
                url = url,
                collectionId = colId,
                customTitle = title,
                customDescription = desc
            )
        },
        onOpenUrl = onOpenUrl,
        onCopyUrl = onCopyUrl,
        onShareUrl = onShareUrl,
        onShowBackupSheet = viewModel::setBackupSheetVisible,
        onExportHtml = { colId ->
            exportHtmlLauncher.launch(viewModel.getExportFileName(BookmarkExportFormat.HTML, colId))
        },
        onExportJson = { colId ->
            exportJsonLauncher.launch(viewModel.getExportFileName(BookmarkExportFormat.JSON, colId))
        },
        onImportBookmarks = {
            importBookmarksLauncher.launch(BookmarksViewModel.IMPORT_MIME_TYPES)
        },
        onShowOptionsSheet = viewModel::setShowOptionsSheet,
        onSetFilterOption = viewModel::setFilterOption,
        onSetSortOption = viewModel::setSortOption,
        modifier = modifier
    )
}

/**
 * Contenido sin estado (stateless) de la pantalla de marcadores.
 *
 * Facilita pruebas visuales, aislamiento de fases y previsualizaciones de Compose.
 */
@Composable
fun BookmarksScreenContent(
    uiState: BookmarksUiState,
    onSetViewMode: (BookmarkViewMode) -> Unit,
    onToggleGridView: () -> Unit,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    onSelectCollection: (String?) -> Unit,
    onSelectPlatform: (PlatformFilter) -> Unit,
    onShowCreateCollection: (Boolean) -> Unit,
    onCreateCollection: (String, String, String) -> Unit,
    onUpdateCollection: (String, String, String, String) -> Unit,
    onDeleteCollection: (String) -> Unit,
    onOpenDetail: (Bookmark) -> Unit,
    onCloseDetail: () -> Unit,
    onTogglePin: (String, Boolean) -> Unit,
    onDeleteBookmark: (String) -> Unit,
    onUpdateBookmark: (String, String, String, String?, String?) -> Unit,
    onToggleBookmarkCollection: (String, String, Boolean) -> Unit,
    onTogglePreviewVisibility: (String, Boolean) -> Unit = { _, _ -> },
    onToggleSelection: (String) -> Unit,
    onClearSelection: () -> Unit,
    onShowDeleteSelectedConfirmDialog: (Boolean) -> Unit,
    onDeleteSelectedBookmarks: () -> Unit,
    onDismissClipboard: () -> Unit,
    onSaveClipboardBookmark: (String?) -> Unit,
    onShowAddBookmark: (Boolean) -> Unit,
    onSaveBookmark: (String, String?, String?, String?) -> Unit,
    onOpenUrl: (String) -> Unit,
    onCopyUrl: (String) -> Unit,
    onShareUrl: (String) -> Unit,
    onShowBackupSheet: (Boolean) -> Unit,
    onExportHtml: (String?) -> Unit,
    onExportJson: (String?) -> Unit,
    onImportBookmarks: () -> Unit,
    onShowOptionsSheet: (Boolean) -> Unit,
    onSetFilterOption: (TabFilterOption) -> Unit,
    onSetSortOption: (TabSortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val gridState = rememberLazyStaggeredGridState()
    val folderGridState = rememberLazyGridState()

    val activeCollection = remember(uiState.collections, uiState.selectedCollectionId) {
        uiState.collections.find { it.id == uiState.selectedCollectionId }
    }

    // Manejo de navegación atrás: Si está en drill-down de carpeta vuelve a la grilla; si está en carpetas vuelve a Todos
    BackHandler(enabled = uiState.viewMode == BookmarkViewMode.FOLDERS) {
        if (uiState.selectedCollectionId != null) {
            onSelectCollection(null)
        } else {
            onSetViewMode(BookmarkViewMode.ALL)
        }
    }

    val isScrolled by remember(uiState.isGridView, uiState.isSearchActive, uiState.viewMode) {
        derivedStateOf {
            uiState.isSearchActive || when {
                uiState.viewMode == BookmarkViewMode.FOLDERS && uiState.selectedCollectionId == null -> {
                    folderGridState.firstVisibleItemIndex > 0 || folderGridState.firstVisibleItemScrollOffset > 0
                }
                uiState.isGridView -> {
                    gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0
                }
                else -> {
                    listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
                }
            }
        }
    }

    var headerHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val headerHeightDp = with(density) { headerHeightPx.toDp() }
    val safeTopPadding = if (headerHeightDp > 0.dp) headerHeightDp + PaddingSmall else BookmarkFallbackTopPadding

    val hazeState = LocalHazeState.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(DraftoShapeScreenBottom)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier)
        ) {
            when {
                uiState.bookmarks.isEmpty() -> {
                    val emptyTitle = if (uiState.searchQuery.isNotBlank()) {
                        stringResource(R.string.empty_search_title)
                    } else {
                        stringResource(R.string.bookmark_empty_title)
                    }
                    val emptyDesc = if (uiState.searchQuery.isNotBlank()) {
                        stringResource(R.string.empty_search_description)
                    } else {
                        stringResource(R.string.bookmark_empty_desc)
                    }
                    val emptyIcon = if (uiState.searchQuery.isNotBlank()) {
                        Icons.Rounded.Search
                    } else {
                        Icons.Rounded.Bookmark
                    }

                    DraftoEmptyState(
                        icon = emptyIcon,
                        iconTint = DraftoTheme.colors.accent,
                        title = emptyTitle,
                        description = emptyDesc,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = safeTopPadding)
                    )
                }
                uiState.isGridView -> {
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
                            items = uiState.bookmarks,
                            key = { it.id },
                            contentType = { "bookmark_grid" }
                        ) { bookmark ->
                            DraftoBookmarkCard(
                                bookmark = bookmark,
                                isGridView = true,
                                isSelected = uiState.selectedBookmarkIds.contains(bookmark.id),
                                isSelectionMode = uiState.isSelectionMode,
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        onToggleSelection(bookmark.id)
                                    } else {
                                        onOpenUrl(bookmark.url)
                                    }
                                },
                                onInfoClick = { onOpenDetail(bookmark) },
                                onLongClick = { onToggleSelection(bookmark.id) },
                                onTogglePin = { onTogglePin(bookmark.id, !bookmark.isPinned) }
                            )
                        }
                    }
                }
                else -> {
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
                            contentType = { "bookmark" }
                        ) { bookmark ->
                            DraftoBookmarkCard(
                                bookmark = bookmark,
                                isGridView = false,
                                isSelected = uiState.selectedBookmarkIds.contains(bookmark.id),
                                isSelectionMode = uiState.isSelectionMode,
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        onToggleSelection(bookmark.id)
                                    } else {
                                        onOpenUrl(bookmark.url)
                                    }
                                },
                                onInfoClick = { onOpenDetail(bookmark) },
                                onLongClick = { onToggleSelection(bookmark.id) },
                                onTogglePin = { onTogglePin(bookmark.id, !bookmark.isPinned) }
                            )
                        }
                    }
                }
            }
        }

        // Encabezado superior adhesivo con Spatial Pure Blur (Haze Ultra Thin)
        DraftoStickyHeader(
            isScrolled = isScrolled,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onGloballyPositioned { coordinates ->
                    headerHeightPx = coordinates.size.height
                }
        ) {
            DraftoSearchableTopBar(
                title = stringResource(R.string.tab_bookmarks),
                isSearchActive = uiState.isSearchActive,
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onCloseSearch = onCloseSearch,
                actions = {
                    if (!uiState.isSelectionMode) {
                        DraftoTopBarIconButton(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = stringResource(R.string.action_search),
                            onClick = onToggleSearch
                        )
                        DraftoTopBarIconButton(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.tab_options_title_bookmarks),
                            onClick = { onShowOptionsSheet(true) }
                        )
                    }
                }
            )
        }

        // Modales y Hojas Inferiores
        if (uiState.showOptionsSheet) {
            DraftoTabOptionsBottomSheet(
                title = stringResource(R.string.tab_options_title_bookmarks),
                isGridView = uiState.isGridView,
                onToggleGridView = onToggleGridView,
                onBackupClick = { onShowBackupSheet(true) },
                selectedFilter = uiState.filterOption,
                onFilterChange = onSetFilterOption,
                selectedSort = uiState.sortOption,
                onSortChange = onSetSortOption,
                onDismissRequest = { onShowOptionsSheet(false) }
            )
        }

        if (uiState.activeDetailBookmark != null) {
            DraftoBookmarkDetailBottomSheet(
                bookmark = uiState.activeDetailBookmark,
                allCollections = uiState.collections,
                assignedCollectionIds = uiState.detailBookmarkCollectionIds,
                onDismiss = onCloseDetail,
                onOpenInBrowser = { url -> onOpenUrl(url) },
                onCopyUrl = { url -> onCopyUrl(url) },
                onShareUrl = { url -> onShareUrl(url) },
                onDeleteBookmark = { id -> onDeleteBookmark(id) },
                onTogglePreviewVisibility = { isVisible ->
                    onTogglePreviewVisibility(uiState.activeDetailBookmark.id, isVisible)
                },
                onToggleCollection = { colId, isAssigned ->
                    onToggleBookmarkCollection(uiState.activeDetailBookmark.id, colId, isAssigned)
                },
                onUpdateBookmark = { id, title, desc, url, colId -> onUpdateBookmark(id, title, desc, url, colId) }
            )
        }

        if (uiState.detectedClipboardUrl != null) {
            DraftoAddBookmarkBottomSheet(
                collections = uiState.collections,
                prefillUrl = uiState.detectedClipboardUrl,
                prefillTitle = uiState.clipboardMetadata?.title,
                prefillDescription = uiState.clipboardMetadata?.description,
                initialCollectionId = uiState.selectedCollectionId,
                onDismiss = onDismissClipboard,
                onSave = { url, title, desc, colId ->
                    onSaveBookmark(url, title, desc, colId)
                    onDismissClipboard()
                }
            )
        }

        if (uiState.showCreateCollectionDialog) {
            DraftoCreateCollectionBottomSheet(
                onDismiss = { onShowCreateCollection(false) },
                onCreateCollection = { name, colorHex, iconName ->
                    onCreateCollection(name, colorHex, iconName)
                }
            )
        }

        if (uiState.showDeleteSelectedConfirmDialog) {
            val count = uiState.selectedBookmarkIds.size
            val title = if (count == 1) {
                stringResource(R.string.dialog_delete_bookmark_title)
            } else {
                stringResource(R.string.dialog_delete_selected_bookmarks_title)
            }
            val message = if (count == 1) {
                stringResource(R.string.dialog_delete_bookmark_message)
            } else {
                stringResource(R.string.dialog_delete_selected_bookmarks_message, count)
            }
            DraftoConfirmationDialog(
                title = title,
                message = message,
                icon = Icons.Rounded.Delete,
                confirmText = stringResource(R.string.action_delete),
                cancelText = stringResource(R.string.action_cancel),
                isDestructive = true,
                onConfirm = {
                    onShowDeleteSelectedConfirmDialog(false)
                    onDeleteSelectedBookmarks()
                },
                onDismiss = { onShowDeleteSelectedConfirmDialog(false) }
            )
        }

        if (uiState.showAddBookmarkSheet) {
            DraftoAddBookmarkBottomSheet(
                collections = uiState.collections,
                initialCollectionId = uiState.selectedCollectionId,
                onDismiss = { onShowAddBookmark(false) },
                onSave = { url, title, desc, colId ->
                    onSaveBookmark(url, title, desc, colId)
                }
            )
        }

        if (uiState.showBackupSheet) {
            DraftoBookmarkBackupBottomSheet(
                onDismissRequest = { onShowBackupSheet(false) },
                onExportHtml = onExportHtml,
                onExportJson = onExportJson,
                onImportBookmarks = onImportBookmarks,
                totalBookmarksCount = uiState.bookmarks.size,
                collections = uiState.collections,
                activeCollectionId = uiState.selectedCollectionId
            )
        }

        uiState.backupProgressState?.let { progressState ->
            if (progressState.isActive) {
                DraftoProgressDialog(
                    title = if (progressState.isExport) stringResource(R.string.progress_exporting_bookmarks)
                            else stringResource(R.string.progress_importing_bookmarks),
                    statusMessage = progressState.statusText,
                    progress = progressState.progress,
                    isExport = progressState.isExport
                )
            }
        }
    }
}
