package com.ixeken.drafto.ui.navigation

import android.app.Activity
import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import com.ixeken.drafto.ui.components.DraftoToastHost
import com.ixeken.drafto.ui.components.DraftoToastManager
import com.ixeken.drafto.ui.security.BiometricAuthenticator
import com.ixeken.drafto.ui.theme.ToastBottomOffsetWithNavBar
import com.ixeken.drafto.ui.theme.ToastBottomOffsetWithoutNavBar
import com.ixeken.drafto.ui.utils.DraftoSystemInteractions
import com.ixeken.drafto.ui.components.DraftoImportBottomSheet
import com.ixeken.drafto.ui.components.DraftoProgressDialog


import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.ui.graphics.Color
import com.ixeken.drafto.R
import com.ixeken.drafto.ui.components.DraftoSelectionAction
import com.ixeken.drafto.ui.components.DraftoSelectionFloatingDock
import com.ixeken.drafto.ui.theme.DraftoSunsetCoral
import com.ixeken.drafto.ui.theme.NavBarCollapsedHeight
import com.ixeken.drafto.ui.theme.PaddingNavBarBottom
import com.ixeken.drafto.ui.theme.PaddingNavBarTop
import com.ixeken.drafto.ui.theme.PaddingSmall
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.ixeken.drafto.MainActivity
import com.ixeken.drafto.domain.model.Note
import com.ixeken.drafto.navigation.NavRoutes
import com.ixeken.drafto.ui.editor.EditorScreen
import com.ixeken.drafto.ui.components.DraftoDynamicIsland
import com.ixeken.drafto.ui.components.DraftoLanguageBottomSheet
import com.ixeken.drafto.ui.components.DraftoNavBar
import com.ixeken.drafto.ui.components.DraftoShareTargetBottomSheet
import com.ixeken.drafto.ui.components.NavBarMode
import kotlinx.coroutines.flow.MutableStateFlow
import com.ixeken.drafto.ui.livenote.LiveNoteViewModel
import com.ixeken.drafto.ui.notes.NotebookSection
import com.ixeken.drafto.ui.notes.NotesScreen
import com.ixeken.drafto.ui.notes.NotesViewModel
import com.ixeken.drafto.ui.pins.PinsScreen
import com.ixeken.drafto.ui.bookmarks.BookmarksScreen
import com.ixeken.drafto.ui.bookmarks.BookmarksViewModel
import com.ixeken.drafto.ui.collections.CollectionDetailScreen
import com.ixeken.drafto.ui.collections.CollectionDetailViewModel
import com.ixeken.drafto.ui.collections.CollectionsScreen
import com.ixeken.drafto.ui.collections.CollectionsViewModel
import com.ixeken.drafto.ui.placeholders.TasksScreen
import com.ixeken.drafto.ui.settings.AboutDraftoScreen
import com.ixeken.drafto.ui.settings.AppearanceScreen
import com.ixeken.drafto.ui.components.DraftoMasterRestoreModeBottomSheet
import com.ixeken.drafto.ui.settings.DataStorageScreen
import com.ixeken.drafto.ui.settings.DataStorageViewModel
import com.ixeken.drafto.ui.settings.PermissionsBottomSheet
import com.ixeken.drafto.ui.settings.PrivacyInfoBottomSheet
import com.ixeken.drafto.ui.settings.SettingsScreen
import com.ixeken.drafto.ui.settings.SettingsSheetType
import com.ixeken.drafto.ui.settings.SettingsViewModel
import com.ixeken.drafto.ui.settings.ThemeSelectionBottomSheet
import com.ixeken.drafto.ui.theme.DraftoGlassMaterial
import com.ixeken.drafto.ui.theme.DraftoSprings
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.DraftoTransitions
import com.ixeken.drafto.ui.theme.LocalDraftoReducedMotion
import com.ixeken.drafto.ui.theme.findActivity
import com.ixeken.drafto.ui.theme.LocalDraftoBlurEnabled
import com.ixeken.drafto.ui.theme.LocalFetchWebMetadataEnabled
import com.ixeken.drafto.ui.theme.LocalHazeState
import dev.chrisbanes.haze.rememberHazeState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    notesViewModel: NotesViewModel = hiltViewModel(),
    liveNoteViewModel: LiveNoteViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    bookmarksViewModel: BookmarksViewModel = hiltViewModel(),
    collectionsViewModel: CollectionsViewModel = hiltViewModel(),
    dataStorageViewModel: DataStorageViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val liveNoteState by liveNoteViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val bookmarksState by bookmarksViewModel.uiState.collectAsState()
    val bookmarkBackupProgressState by bookmarksViewModel.backupProgressState.collectAsState()
    val notebookBackupProgressState by notesViewModel.backupProgressState.collectAsState()
    val dataStorageState by dataStorageViewModel.uiState.collectAsState()
    val collectionsState by collectionsViewModel.uiState.collectAsState()
    val notesState by notesViewModel.uiState.collectAsState()

    val hazeState = rememberHazeState()

    // Observar peticiones de edición desde la notificación
    val activity = LocalContext.current as? MainActivity
    val sharedPayload by (activity?.sharedPayload ?: MutableStateFlow(null)).collectAsState()

    LaunchedEffect(activity) {
        activity?.editLiveNoteRequest?.collect {
            val activeText = liveNoteState.activeNote?.text.orEmpty()
            liveNoteViewModel.openQuickNoteEditor(activeText)
        }
    }

    var showImportTypeSheet by remember { mutableStateOf(false) }

    val importBookmarksLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            bookmarksViewModel.onImportUriSelected(
                uri = uri,
                onSuccess = { imported, collections ->
                    if (imported == 0) {
                        DraftoToastManager.showInfo(context.getString(com.ixeken.drafto.core.R.string.toast_bookmarks_import_empty))
                    } else {
                        DraftoToastManager.showSuccess(
                            context.getString(com.ixeken.drafto.core.R.string.toast_bookmarks_import_success, imported, collections)
                        )
                        navController.navigate(NavRoutes.Saved) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }

    val importNotebookZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            notesViewModel.onImportNotebookZipSelected(uri)
        }
    }

    val importMasterBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            dataStorageViewModel.onRestoreUriSelected(uri)
        }
    }

    val importSingleNoteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            notesViewModel.onImportSingleNoteSelected(uri)
        }
    }

    val importSingleTodosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            notesViewModel.onImportSingleTodosSelected(uri)
        }
    }


    fun getTabIndex(destination: NavDestination?): Int = when {
        destination?.hasRoute(NavRoutes.Collections::class) == true -> 0
        destination?.hasRoute(NavRoutes.CollectionDetail::class) == true -> 0
        destination?.hasRoute(NavRoutes.Pins::class) == true -> 0
        destination?.hasRoute(NavRoutes.Notes::class) == true -> 1
        destination?.hasRoute(NavRoutes.Tasks::class) == true -> 1
        destination?.hasRoute(NavRoutes.Saved::class) == true -> 2
        destination?.hasRoute(NavRoutes.Settings::class) == true -> 3
        destination?.hasRoute(NavRoutes.Appearance::class) == true -> 3
        destination?.hasRoute(NavRoutes.AboutDrafto::class) == true -> 3
        destination?.hasRoute(NavRoutes.DataStorage::class) == true -> 3
        else -> 0
    }

    val selectedTabIndex = getTabIndex(currentDestination)

    LaunchedEffect(selectedTabIndex, settingsState.isPreferencesLoaded) {
        if (settingsState.isPreferencesLoaded && selectedTabIndex != settingsState.lastActiveTab) {
            settingsViewModel.setLastActiveTab(selectedTabIndex)
        }
    }

    val isNavBarVisible = currentDestination?.let { dest ->
        dest.hasRoute(NavRoutes.Collections::class) ||
        dest.hasRoute(NavRoutes.Pins::class) ||
        dest.hasRoute(NavRoutes.Notes::class) ||
        dest.hasRoute(NavRoutes.Tasks::class) ||
        dest.hasRoute(NavRoutes.Saved::class) ||
        dest.hasRoute(NavRoutes.Settings::class)
    } ?: true

    DraftoTheme(
        theme = settingsState.theme,
        fontSizeStep = settingsState.fontSizeStep,
        accentColor = settingsState.accentColor,
        isReducedMotion = settingsState.isReduceMotionEnabled
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalHazeState provides hazeState,
            LocalDraftoBlurEnabled provides settingsState.isGlobalBlurEnabled,
            LocalFetchWebMetadataEnabled provides settingsState.isFetchWebMetadataEnabled
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (!settingsState.isPreferencesLoaded) {
                    return@Box
                }

                val initialTab = rememberSaveable { settingsState.lastActiveTab }
                val startDestination: Any = remember(initialTab) {
                    when (initialTab) {
                        0 -> NavRoutes.Collections
                        1 -> NavRoutes.Notes
                        2 -> NavRoutes.Saved
                        3 -> NavRoutes.Settings
                        else -> NavRoutes.Collections
                    }
                }

                val isReducedMotion = LocalDraftoReducedMotion.current

                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = {
                        val isTargetDetail = targetState.destination.hasRoute(NavRoutes.Appearance::class) ||
                            targetState.destination.hasRoute(NavRoutes.AboutDrafto::class) ||
                            targetState.destination.hasRoute(NavRoutes.DataStorage::class) ||
                            targetState.destination.hasRoute(NavRoutes.Editor::class) ||
                            targetState.destination.hasRoute(NavRoutes.CollectionDetail::class)

                        if (isTargetDetail) {
                            DraftoTransitions.navDetailEnter(isReducedMotion)
                        } else {
                            val initialTab = getTabIndex(initialState.destination)
                            val targetTab = getTabIndex(targetState.destination)
                            val direction = if (targetTab >= initialTab) 1 else -1
                            DraftoTransitions.navTabEnter(direction, isReducedMotion)
                        }
                    },
                    exitTransition = {
                        val isTargetDetail = targetState.destination.hasRoute(NavRoutes.Appearance::class) ||
                            targetState.destination.hasRoute(NavRoutes.AboutDrafto::class) ||
                            targetState.destination.hasRoute(NavRoutes.DataStorage::class) ||
                            targetState.destination.hasRoute(NavRoutes.Editor::class) ||
                            targetState.destination.hasRoute(NavRoutes.CollectionDetail::class)

                        if (isTargetDetail) {
                            DraftoTransitions.navDetailExit(isReducedMotion)
                        } else {
                            val initialTab = getTabIndex(initialState.destination)
                            val targetTab = getTabIndex(targetState.destination)
                            val direction = if (targetTab >= initialTab) -1 else 1
                            DraftoTransitions.navTabExit(direction, isReducedMotion)
                        }
                    },
                    popEnterTransition = {
                        val isInitialDetail = initialState.destination.hasRoute(NavRoutes.Appearance::class) ||
                            initialState.destination.hasRoute(NavRoutes.AboutDrafto::class) ||
                            initialState.destination.hasRoute(NavRoutes.DataStorage::class) ||
                            initialState.destination.hasRoute(NavRoutes.Editor::class) ||
                            initialState.destination.hasRoute(NavRoutes.CollectionDetail::class)

                        if (isInitialDetail) {
                            DraftoTransitions.navDetailPopEnter(isReducedMotion)
                        } else {
                            val initialTab = getTabIndex(initialState.destination)
                            val targetTab = getTabIndex(targetState.destination)
                            val direction = if (targetTab >= initialTab) 1 else -1
                            DraftoTransitions.navTabEnter(direction, isReducedMotion)
                        }
                    },
                    popExitTransition = {
                        val isInitialDetail = initialState.destination.hasRoute(NavRoutes.Appearance::class) ||
                            initialState.destination.hasRoute(NavRoutes.AboutDrafto::class) ||
                            initialState.destination.hasRoute(NavRoutes.DataStorage::class) ||
                            initialState.destination.hasRoute(NavRoutes.Editor::class) ||
                            initialState.destination.hasRoute(NavRoutes.CollectionDetail::class)

                        if (isInitialDetail) {
                            DraftoTransitions.navDetailPopExit(isReducedMotion)
                        } else {
                            val initialTab = getTabIndex(initialState.destination)
                            val targetTab = getTabIndex(targetState.destination)
                            val direction = if (targetTab >= initialTab) -1 else 1
                            DraftoTransitions.navTabExit(direction, isReducedMotion)
                        }
                    }
                ) {

                        composable<NavRoutes.Settings> {
                            SettingsScreen(
                                uiState = settingsState,
                                onNavigateToAppearance = { navController.navigate(NavRoutes.Appearance) },
                                onNavigateToAboutDrafto = { navController.navigate(NavRoutes.AboutDrafto) },
                                onNavigateToDataStorage = { navController.navigate(NavRoutes.DataStorage) },
                                onToggleLockApp = { enabled ->
                                    val activity = context.findActivity()
                                    if (activity == null) {
                                        DraftoToastManager.showWarning(
                                            context.getString(com.ixeken.drafto.core.R.string.error_authentication_general)
                                        )
                                    } else if (enabled) {
                                        if (!BiometricAuthenticator.isDeviceSecure(activity)) {
                                            DraftoToastManager.showWarning(
                                                context.getString(com.ixeken.drafto.core.R.string.error_no_secure_lock)
                                            )
                                        } else {
                                            BiometricAuthenticator.authenticate(
                                                activity = activity,
                                                onSuccess = {
                                                    settingsViewModel.setLockAppEnabled(true)
                                                },
                                                onError = { error ->
                                                    DraftoToastManager.showWarning(error)
                                                }
                                            )
                                        }
                                    } else {
                                        if (!BiometricAuthenticator.isDeviceSecure(activity)) {
                                            settingsViewModel.setLockAppEnabled(false)
                                        } else {
                                            BiometricAuthenticator.authenticate(
                                                activity = activity,
                                                onSuccess = {
                                                    settingsViewModel.setLockAppEnabled(false)
                                                },
                                                onError = { error ->
                                                    DraftoToastManager.showWarning(error)
                                                }
                                            )
                                        }
                                    }
                                },
                                onToggleClipboardAutoDetect = { settingsViewModel.setClipboardAutoDetectEnabled(it) },
                                onToggleFetchWebMetadata = { settingsViewModel.setFetchWebMetadataEnabled(it) },
                                onOpenPermissionsBottomSheet = { settingsViewModel.openBottomSheet(SettingsSheetType.PERMISSIONS) },
                                onOpenPrivacyInfoBottomSheet = { settingsViewModel.openBottomSheet(SettingsSheetType.PRIVACY_INFO) },
                                onOpenThemeSelectionBottomSheet = { settingsViewModel.openBottomSheet(SettingsSheetType.THEME_SELECTION) },
                                onOpenLanguageSelectionBottomSheet = { settingsViewModel.openBottomSheet(SettingsSheetType.LANGUAGE_SELECTION) },
                                onSelectTheme = { settingsViewModel.setTheme(it) },
                                onDismissBottomSheet = { settingsViewModel.dismissBottomSheet() }
                            )
                        }

                        composable<NavRoutes.Appearance> {
                            AppearanceScreen(
                                uiState = settingsState,
                                onBack = { navController.popBackStack() },
                                onOpenThemeSelection = { settingsViewModel.openBottomSheet(SettingsSheetType.THEME_SELECTION) },
                                onToggleGlobalBlur = { settingsViewModel.setGlobalBlurEnabled(it) },
                                onToggleReduceMotion = { settingsViewModel.setReduceMotionEnabled(it) },
                                onSelectAccentColor = { settingsViewModel.setAccentColor(it) },
                                onChangeFontSizeStep = { settingsViewModel.setFontSizeStep(it) }
                            )
                        }

                        composable<NavRoutes.AboutDrafto> {
                            AboutDraftoScreen(
                                uiState = settingsState,
                                onBack = { navController.popBackStack() },
                                onToggleCheckUpdateOnStart = { settingsViewModel.setCheckUpdateOnStartEnabled(it) }
                            )
                        }

                        composable<NavRoutes.DataStorage> {
                            DataStorageScreen(
                                uiState = dataStorageState,
                                onBack = { navController.popBackStack() },
                                onClearCache = {
                                    dataStorageViewModel.clearCache(
                                        onSuccess = {
                                            DraftoToastManager.showSuccess(
                                                context.getString(com.ixeken.drafto.core.R.string.toast_cache_cleared)
                                            )
                                        }
                                    )
                                },
                                onExportMasterBackup = { uri ->
                                    dataStorageViewModel.exportMasterBackup(
                                        uri = uri,
                                        onSuccess = {
                                            DraftoToastManager.showSuccess(
                                                context.getString(com.ixeken.drafto.core.R.string.toast_master_backup_exported)
                                            )
                                        },
                                        onError = {
                                            DraftoToastManager.showWarning(
                                                context.getString(com.ixeken.drafto.core.R.string.toast_master_backup_export_error)
                                            )
                                        }
                                    )
                                },
                                onRestoreUriSelected = { uri ->
                                    dataStorageViewModel.onRestoreUriSelected(uri)
                                },
                                onRestoreMasterBackup = { uri, mode ->
                                    dataStorageViewModel.restoreMasterBackup(
                                        uri = uri,
                                        mode = mode,
                                        onSuccess = {
                                            DraftoToastManager.showSuccess(
                                                context.getString(com.ixeken.drafto.core.R.string.toast_master_backup_imported)
                                            )
                                        },
                                        onError = {
                                            DraftoToastManager.showWarning(
                                                context.getString(com.ixeken.drafto.core.R.string.toast_master_backup_import_error)
                                            )
                                        }
                                    )
                                },
                                onDismissRestoreModeSheet = { dataStorageViewModel.dismissRestoreModeSheet() },
                                onOpenWipeConfirmDialog = { dataStorageViewModel.showWipeConfirmDialog() },
                                onDismissWipeConfirmDialog = { dataStorageViewModel.dismissWipeConfirmDialog() },
                                onWipeAllData = {
                                    dataStorageViewModel.wipeAllData(
                                        onSuccess = {
                                            DraftoToastManager.showSuccess(
                                                context.getString(com.ixeken.drafto.core.R.string.toast_wipe_success)
                                            )
                                            navController.navigate(NavRoutes.Notes) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    inclusive = false
                                                }
                                                launchSingleTop = true
                                            }
                                        },
                                        onError = {
                                            DraftoToastManager.showWarning(
                                                context.getString(com.ixeken.drafto.core.R.string.toast_wipe_error)
                                            )
                                        }
                                    )
                                }
                            )
                        }

                        composable<NavRoutes.Notes> {
                            NotesScreen(
                                onNavigateToEditor = { noteId ->
                                    navController.navigate(NavRoutes.Editor(noteId))
                                },
                                viewModel = notesViewModel
                            )
                        }

                        composable<NavRoutes.Editor> {
                            EditorScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable<NavRoutes.Tasks> {
                            TasksScreen()
                        }

                        composable<NavRoutes.Saved> {
                            BookmarksScreen(viewModel = bookmarksViewModel)
                        }

                        composable<NavRoutes.Collections> {
                            CollectionsScreen(
                                uiState = collectionsState,
                                onOpenCollection = { collectionId ->
                                    navController.navigate(NavRoutes.CollectionDetail(collectionId))
                                },
                                onOpenCreateSheet = { collectionsViewModel.openCreateSheet() },
                                onOpenEditSheet = { collectionsViewModel.openEditSheet(it) },
                                onDismissSheet = { collectionsViewModel.dismissSheet() },
                                onSaveCollection = { name, colorHex, iconName ->
                                    collectionsViewModel.saveCollection(name, colorHex, iconName)
                                },
                                onRequestDelete = { collectionsViewModel.requestDeleteCollection(it) },
                                onDismissDeleteDialog = { collectionsViewModel.dismissDeleteDialog() },
                                onConfirmDelete = { collectionsViewModel.confirmDeleteCollection() },
                                onSearchQueryChange = { collectionsViewModel.setSearchQuery(it) },
                                onToggleSearch = { collectionsViewModel.toggleSearch(it) },
                                onToggleSelection = { collectionsViewModel.toggleSelection(it) },
                                onClearSelection = { collectionsViewModel.clearSelection() },
                                onConfirmDeleteSelected = { collectionsViewModel.deleteSelectedCollections() },
                                onDismissDeleteConfirmDialog = { collectionsViewModel.setShowDeleteConfirmDialog(false) },
                                onShowOptionsSheet = { collectionsViewModel.setShowOptionsSheet(it) },
                                onSetSortOption = { collectionsViewModel.setSortOption(it) }
                            )
                        }

                        composable<NavRoutes.CollectionDetail> { backStackEntry ->
                            val route = backStackEntry.toRoute<NavRoutes.CollectionDetail>()
                            val collectionDetailViewModel: CollectionDetailViewModel = hiltViewModel()
                            LaunchedEffect(route.collectionId) {
                                collectionDetailViewModel.setCollectionId(route.collectionId)
                            }
                            val detailState by collectionDetailViewModel.uiState.collectAsState()
                            CollectionDetailScreen(
                                uiState = detailState,
                                onBackClick = { navController.popBackStack() },
                                onSubTabSelected = { collectionDetailViewModel.setSubTab(it) },
                                onNoteClick = { noteId ->
                                    navController.navigate(NavRoutes.Editor(noteId))
                                },
                                onNewNoteClick = {
                                    navController.navigate(NavRoutes.Editor("new", collectionId = route.collectionId))
                                },
                                onToggleNotePinned = { collectionDetailViewModel.toggleNotePinned(it) },
                                onDeleteNote = { collectionDetailViewModel.deleteNote(it) },
                                onToggleTodoCompleted = { todo, isCompleted ->
                                    collectionDetailViewModel.toggleTodoCompleted(todo, isCompleted)
                                },
                                onToggleTodoPinned = { todo, isPinned ->
                                    collectionDetailViewModel.toggleTodoPinned(todo, isPinned)
                                },
                                onToggleSubtask = { todoId, subtaskId, isDone ->
                                    collectionDetailViewModel.toggleSubtask(todoId, subtaskId, isDone)
                                },
                                onEditTodo = { collectionDetailViewModel.openEditTodo(it) },
                                onDeleteTodo = { collectionDetailViewModel.deleteTodo(it) },
                                onOpenAddTodoSheet = { collectionDetailViewModel.openAddTodoSheet() },
                                onDismissAddTodoSheet = { collectionDetailViewModel.dismissAddTodoSheet() },
                                onSaveTodo = { title, description, isPinned, dueDate ->
                                    collectionDetailViewModel.saveTodo(title, description, isPinned, dueDate)
                                },
                                onSaveTodoList = { title, description, isPinned, dueDate, _, subtasks ->
                                    collectionDetailViewModel.saveTodo(title, description, isPinned, dueDate, subtasks)
                                },
                                onToggleBookmarkPin = { collectionDetailViewModel.toggleBookmarkPin(it) },
                                onDeleteBookmark = { collectionDetailViewModel.deleteBookmark(it) },
                                onOpenAddBookmarkSheet = { collectionDetailViewModel.openAddBookmarkSheet() },
                                onDismissAddBookmarkSheet = { collectionDetailViewModel.dismissAddBookmarkSheet() },
                                onSaveBookmark = { url, title, description ->
                                    collectionDetailViewModel.saveBookmark(url, title, description)
                                },
                                onFabClick = {
                                    collectionDetailViewModel.onFabClick(
                                        onNewNote = {
                                            navController.navigate(NavRoutes.Editor("new", collectionId = route.collectionId))
                                        }
                                    )
                                },
                                onSelectTodoFromChooser = { collectionDetailViewModel.selectTodoFromChooser() },
                                onSelectBookmarkFromChooser = { collectionDetailViewModel.selectBookmarkFromChooser() },
                                onOpenEditCollectionSheet = { collectionDetailViewModel.openEditCollectionSheet() },
                                onDismissEditCollectionSheet = { collectionDetailViewModel.dismissEditCollectionSheet() },
                                onOpenTypeChooserSheet = { collectionDetailViewModel.openTypeChooser() },
                                onDismissTypeChooserSheet = { collectionDetailViewModel.dismissTypeChooser() },
                                onUpdateCollection = { name, colorHex, iconName ->
                                    collectionDetailViewModel.updateCollection(name, colorHex, iconName)
                                },
                                onSearchQueryChange = { collectionDetailViewModel.setSearchQuery(it) },
                                onToggleSearch = { collectionDetailViewModel.toggleSearch(it) }
                            )
                        }

                        composable<NavRoutes.Pins> {
                            val notesState by notesViewModel.uiState.collectAsState()
                            var isPinsSearchActive by rememberSaveable { mutableStateOf(false) }
                            var pinsSearchQuery by rememberSaveable { mutableStateOf("") }
                            PinsScreen(
                                pinnedNotes = notesState.notes.filter { it.isPinned },
                                pinnedTodos = notesState.todos.filter { it.isPinned },
                                pinnedBookmarks = bookmarksState.bookmarks.filter { it.isPinned },
                                isGridView = settingsState.isPinsGridView,
                                isSearchActive = isPinsSearchActive,
                                searchQuery = pinsSearchQuery,
                                onToggleGridView = {
                                    settingsViewModel.setPinsGridView(!settingsState.isPinsGridView)
                                },
                                onToggleSearch = {
                                    isPinsSearchActive = !isPinsSearchActive
                                    if (!isPinsSearchActive) pinsSearchQuery = ""
                                },
                                onSearchQueryChange = { pinsSearchQuery = it },
                                onCloseSearch = {
                                    isPinsSearchActive = false
                                    pinsSearchQuery = ""
                                },
                                onNoteClick = { note ->
                                    navController.navigate(NavRoutes.Editor(note.id))
                                },
                                onToggleTodoCompleted = { todo, isCompleted ->
                                    notesViewModel.toggleTodoCompleted(todo, isCompleted)
                                },
                                onToggleSubtask = { todoId, subtaskId, isDone ->
                                    notesViewModel.toggleSubtask(todoId, subtaskId, isDone)
                                },
                                onTodoClick = { todo ->
                                    navController.navigate(NavRoutes.Notes) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                    notesViewModel.setSelectedSection(NotebookSection.TODOS)
                                    notesViewModel.startEditingTodo(todo)
                                },
                                onBookmarkClick = { bookmark ->
                                    DraftoSystemInteractions.openUrl(context, bookmark.url)
                                },
                                onAddNotificationNoteClick = {
                                    liveNoteViewModel.openQuickNoteEditor()
                                }
                            )
                        }
                    }

            // Barra de Navegación 100% FLOTANTE sobre la pantalla sincronizada con físicas Spring
            AnimatedVisibility(
                visible = isNavBarVisible,
                enter = DraftoTransitions.navBarEnter(isReducedMotion),
                exit = DraftoTransitions.navBarExit(isReducedMotion),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                val navBarMode = NavBarMode.MainTabs(selectedTab = selectedTabIndex)

                DraftoNavBar(
                    mode = navBarMode,
                    onTabSelected = { index ->
                        settingsViewModel.setLastActiveTab(index)
                        when (index) {
                            0 -> navController.navigate(NavRoutes.Collections) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                            1 -> navController.navigate(NavRoutes.Notes) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                            2 -> navController.navigate(NavRoutes.Saved) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                            3 -> navController.navigate(NavRoutes.Settings) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onNewTaskClick = {
                        navController.navigate(NavRoutes.Notes) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                        notesViewModel.setSelectedSection(NotebookSection.TODOS)
                        notesViewModel.setShowAddTodoSheet(true)
                    },
                    onNewNoteClick = {
                        navController.navigate(NavRoutes.Editor())
                    },
                    onNewSavedClick = {
                        navController.navigate(NavRoutes.Saved) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                        bookmarksViewModel.setShowAddBookmarkSheet(true)
                    },
                    onNotificationNoteClick = {
                        liveNoteViewModel.openQuickNoteEditor()
                    },
                    onImportClick = {
                        showImportTypeSheet = true
                    },
                    onPinClick = {},
                    onShareClick = {},
                    onDeleteClick = {},
                    onBackClick = {},
                    isBlurEnabled = settingsState.isGlobalBlurEnabled,
                    hazeState = hazeState,
                    glassMaterial = DraftoGlassMaterial.UltraThin
                )
            }

            // Cápsula Flotante Unificada de Selección (Desacoplada del NavBar)
            val context = LocalContext.current
            val isBookmarksSelection = bookmarksState.isSelectionMode
            val selectedBookmarksList = remember(bookmarksState.selectedBookmarkIds, bookmarksState.bookmarks) {
                bookmarksState.bookmarks.filter { it.id in bookmarksState.selectedBookmarkIds }
            }
            val allSelectedBookmarksPinned = remember(selectedBookmarksList) {
                selectedBookmarksList.isNotEmpty() && selectedBookmarksList.all { it.isPinned }
            }

            val bookmarkSelectionActions = remember(selectedBookmarksList, allSelectedBookmarksPinned, context) {
                listOf(
                    DraftoSelectionAction(
                        icon = Icons.Rounded.PushPin,
                        contentDescription = context.getString(
                            if (allSelectedBookmarksPinned) R.string.action_unpin else R.string.action_pin
                        ),
                        tint = if (allSelectedBookmarksPinned) DraftoSunsetCoral else Color.Unspecified,
                        onClick = {
                            val newPinState = !allSelectedBookmarksPinned
                            selectedBookmarksList.forEach { bm ->
                                bookmarksViewModel.togglePin(bm.id, newPinState)
                            }
                        }
                    ),
                    DraftoSelectionAction(
                        icon = Icons.Rounded.Share,
                        contentDescription = context.getString(R.string.action_share),
                        onClick = {
                            if (selectedBookmarksList.isNotEmpty()) {
                                val shareText = if (selectedBookmarksList.size == 1) {
                                    selectedBookmarksList.first().url
                                } else {
                                    selectedBookmarksList.joinToString("\n") { bm ->
                                        "${bm.title ?: bm.domain}: ${bm.url}"
                                    }
                                }
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, null))
                            }
                        }
                    ),
                    DraftoSelectionAction(
                        icon = Icons.Rounded.Delete,
                        contentDescription = context.getString(R.string.action_delete),
                        tint = DraftoSunsetCoral,
                        onClick = {
                            bookmarksViewModel.setShowDeleteSelectedConfirmDialog(true)
                        }
                    )
                )
            }

            val isCollectionsSelection = collectionsState.isSelectionMode
            val collectionSelectionActions = remember(context) {
                listOf(
                    DraftoSelectionAction(
                        icon = Icons.Rounded.Delete,
                        contentDescription = context.getString(R.string.action_delete),
                        tint = DraftoSunsetCoral,
                        onClick = {
                            collectionsViewModel.setShowDeleteConfirmDialog(true)
                        }
                    )
                )
            }

            val isNotesSelection = notesState.isNotesSelectionMode
            val selectedNotesList = remember(notesState.selectedNoteIds, notesState.filteredNotes) {
                notesState.filteredNotes.filter { it.id in notesState.selectedNoteIds }
            }
            val allSelectedNotesPinned = remember(selectedNotesList) {
                selectedNotesList.isNotEmpty() && selectedNotesList.all { it.isPinned }
            }

            val noteSelectionActions = remember(selectedNotesList, allSelectedNotesPinned, context) {
                listOf(
                    DraftoSelectionAction(
                        icon = Icons.Rounded.PushPin,
                        contentDescription = context.getString(
                            if (allSelectedNotesPinned) R.string.action_unpin else R.string.action_pin
                        ),
                        tint = if (allSelectedNotesPinned) DraftoSunsetCoral else Color.Unspecified,
                        onClick = {
                            notesViewModel.togglePinSelectedNotes(!allSelectedNotesPinned)
                        }
                    ),
                    DraftoSelectionAction(
                        icon = Icons.Rounded.Share,
                        contentDescription = context.getString(R.string.action_share),
                        onClick = {
                            if (selectedNotesList.isNotEmpty()) {
                                val shareText = selectedNotesList.joinToString("\n\n---\n\n") { n ->
                                    buildString {
                                        if (n.title.isNotBlank()) appendLine(n.title)
                                        append(n.content)
                                    }
                                }
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, null))
                            }
                        }
                    ),
                    DraftoSelectionAction(
                        icon = Icons.Rounded.Delete,
                        contentDescription = context.getString(R.string.action_delete),
                        tint = DraftoSunsetCoral,
                        onClick = {
                            notesViewModel.setShowDeleteSelectedNotesDialog(true)
                        }
                    )
                )
            }

            val isTodosSelection = notesState.isTodosSelectionMode
            val selectedTodosList = remember(notesState.selectedTodoIds, notesState.filteredTodos) {
                notesState.filteredTodos.filter { it.id in notesState.selectedTodoIds }
            }
            val allSelectedTodosCompleted = remember(selectedTodosList) {
                selectedTodosList.isNotEmpty() && selectedTodosList.all { it.isCompleted }
            }
            val allSelectedTodosPinned = remember(selectedTodosList) {
                selectedTodosList.isNotEmpty() && selectedTodosList.all { it.isPinned }
            }

            val todoSelectionActions = remember(selectedTodosList, allSelectedTodosCompleted, allSelectedTodosPinned, context) {
                listOf(
                    DraftoSelectionAction(
                        icon = Icons.Rounded.TaskAlt,
                        contentDescription = context.getString(com.ixeken.drafto.core.R.string.todo_filter_completed),
                        tint = if (allSelectedTodosCompleted) DraftoSunsetCoral else Color.Unspecified,
                        onClick = {
                            notesViewModel.toggleCompleteSelectedTodos(!allSelectedTodosCompleted)
                        }
                    ),
                    DraftoSelectionAction(
                        icon = Icons.Rounded.PushPin,
                        contentDescription = context.getString(
                            if (allSelectedTodosPinned) R.string.action_unpin else R.string.action_pin
                        ),
                        tint = if (allSelectedTodosPinned) DraftoSunsetCoral else Color.Unspecified,
                        onClick = {
                            notesViewModel.togglePinSelectedTodos(!allSelectedTodosPinned)
                        }
                    ),
                    DraftoSelectionAction(
                        icon = Icons.Rounded.Delete,
                        contentDescription = context.getString(R.string.action_delete),
                        tint = DraftoSunsetCoral,
                        onClick = {
                            notesViewModel.setShowDeleteSelectedTodosDialog(true)
                        }
                    )
                )
            }

            val isAnySelectionActive = isBookmarksSelection || isCollectionsSelection || isNotesSelection || isTodosSelection
            val activeSelectedCount = when {
                isBookmarksSelection -> bookmarksState.selectedBookmarkIds.size
                isCollectionsSelection -> collectionsState.selectedCollectionIds.size
                isNotesSelection -> notesState.selectedNoteIds.size
                isTodosSelection -> notesState.selectedTodoIds.size
                else -> 0
            }
            val activeSelectionActions = when {
                isBookmarksSelection -> bookmarkSelectionActions
                isCollectionsSelection -> collectionSelectionActions
                isNotesSelection -> noteSelectionActions
                isTodosSelection -> todoSelectionActions
                else -> emptyList()
            }
            val onClearActiveSelection: () -> Unit = {
                when {
                    isBookmarksSelection -> bookmarksViewModel.clearSelection()
                    isCollectionsSelection -> collectionsViewModel.clearSelection()
                    isNotesSelection -> notesViewModel.clearNoteSelection()
                    isTodosSelection -> notesViewModel.clearTodoSelection()
                }
            }

            DraftoSelectionFloatingDock(
                visible = isAnySelectionActive,
                selectedCount = activeSelectedCount,
                actions = activeSelectionActions,
                onClearSelection = onClearActiveSelection,
                hazeState = hazeState,
                isBlurEnabled = settingsState.isGlobalBlurEnabled,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                    .padding(bottom = NavBarCollapsedHeight + PaddingNavBarBottom + PaddingNavBarTop + PaddingSmall)
            )

            // La isla dinámica se dibuja en la parte superior sobre el cutout
            DraftoDynamicIsland(
                visible = liveNoteState.isEditorExpanded,
                quickNoteText = liveNoteState.inputText,
                onQuickNoteTextChange = { liveNoteViewModel.updateInputText(it) },
                onSaveQuickNote = { liveNoteViewModel.saveLiveNote() },
                onCancelQuickNote = { liveNoteViewModel.closeEditor() },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Modal Bottom Sheets globales de Settings (Renderizados sobre cualquier pantalla activa)
            when (settingsState.activeBottomSheet) {
                SettingsSheetType.PRIVACY_INFO -> {
                    PrivacyInfoBottomSheet(
                        onDismiss = { settingsViewModel.dismissBottomSheet() }
                    )
                }
                SettingsSheetType.PERMISSIONS -> {
                    PermissionsBottomSheet(
                        onDismiss = { settingsViewModel.dismissBottomSheet() }
                    )
                }
                SettingsSheetType.THEME_SELECTION -> {
                    ThemeSelectionBottomSheet(
                        selectedTheme = settingsState.theme,
                        onSelectTheme = { settingsViewModel.setTheme(it) },
                        onDismiss = { settingsViewModel.dismissBottomSheet() }
                    )
                }
                SettingsSheetType.LANGUAGE_SELECTION -> {
                    val context = LocalContext.current
                    val currentLocalesTag = remember(context) {
                        val localeManager = context.getSystemService(android.app.LocaleManager::class.java)
                        val locales = localeManager?.applicationLocales
                        if (locales != null && !locales.isEmpty) {
                            locales.toLanguageTags()
                        } else {
                            java.util.Locale.getDefault().toLanguageTag()
                        }
                    }
                    DraftoLanguageBottomSheet(
                        selectedLanguageTag = currentLocalesTag,
                        onSelectLanguage = { tag ->
                            val localeManager = context.getSystemService(android.app.LocaleManager::class.java)
                            localeManager?.applicationLocales = android.os.LocaleList.forLanguageTags(tag)
                        },
                        onDismiss = { settingsViewModel.dismissBottomSheet() }
                    )
                }
                SettingsSheetType.NONE -> {}
            }

            bookmarkBackupProgressState?.let { progressState ->
                if (progressState.isActive) {
                    DraftoProgressDialog(
                        title = if (progressState.isExport) androidx.compose.ui.res.stringResource(com.ixeken.drafto.core.R.string.progress_exporting_bookmarks)
                                else androidx.compose.ui.res.stringResource(com.ixeken.drafto.core.R.string.progress_importing_bookmarks),
                        statusMessage = progressState.statusText,
                        progress = progressState.progress,
                        isExport = progressState.isExport
                    )
                }
            }

            notebookBackupProgressState?.let { progressState ->
                if (progressState.isActive) {
                    DraftoProgressDialog(
                        title = if (progressState.isExport) androidx.compose.ui.res.stringResource(com.ixeken.drafto.core.R.string.dialog_exporting_notebook)
                                else androidx.compose.ui.res.stringResource(com.ixeken.drafto.core.R.string.dialog_importing_notebook),
                        statusMessage = progressState.statusText,
                        progress = progressState.progress,
                        isExport = progressState.isExport
                    )
                }
            }

            val isDataStorageRoute = currentDestination?.hasRoute(NavRoutes.DataStorage::class) == true

            dataStorageState.progressState?.let { progressState ->
                if (progressState.isActive && !isDataStorageRoute) {
                    DraftoProgressDialog(
                        title = if (progressState.isExport) androidx.compose.ui.res.stringResource(com.ixeken.drafto.core.R.string.dialog_exporting_master_backup)
                                else androidx.compose.ui.res.stringResource(com.ixeken.drafto.core.R.string.dialog_importing_master_backup),
                        statusMessage = progressState.statusText,
                        progress = progressState.progress,
                        isExport = progressState.isExport
                    )
                }
            }

            if (dataStorageState.showRestoreModeSheet && dataStorageState.selectedRestoreUri != null && !isDataStorageRoute) {
                DraftoMasterRestoreModeBottomSheet(
                    onDismissRequest = { dataStorageViewModel.dismissRestoreModeSheet() },
                    onSelectMode = { mode ->
                        val uri = dataStorageState.selectedRestoreUri
                        if (uri != null) {
                            dataStorageViewModel.restoreMasterBackup(
                                uri = uri,
                                mode = mode,
                                onSuccess = {
                                    DraftoToastManager.showSuccess(
                                        context.getString(com.ixeken.drafto.core.R.string.toast_master_backup_imported)
                                    )
                                },
                                onError = {
                                    DraftoToastManager.showWarning(
                                        context.getString(com.ixeken.drafto.core.R.string.toast_master_backup_import_error)
                                    )
                                }
                            )
                        }
                    }
                )
            }

            if (showImportTypeSheet) {
                DraftoImportBottomSheet(
                    onDismissRequest = { showImportTypeSheet = false },
                    onImportMasterBackup = {
                        importMasterBackupLauncher.launch(NotesViewModel.MIME_ZIP_TYPES)
                    },
                    onImportBookmarks = {
                        importBookmarksLauncher.launch(BookmarksViewModel.IMPORT_MIME_TYPES)
                    },
                    onImportNotebook = {
                        importNotebookZipLauncher.launch(NotesViewModel.MIME_ZIP_TYPES)
                    },
                    onImportSingleNote = {
                        importSingleNoteLauncher.launch(NotesViewModel.MIME_MARKDOWN_TYPES)
                    },
                    onImportSingleTodos = {
                        importSingleTodosLauncher.launch(NotesViewModel.MIME_MARKDOWN_TYPES)
                    }
                )
            }

            // Modal Universal de Compartir desde Android (ACTION_SEND)
            sharedPayload?.let { payload ->
                DraftoShareTargetBottomSheet(
                    payload = payload,
                    collections = collectionsState.collections.ifEmpty { bookmarksState.collections },
                    onSaveAsNote = { title, content, collectionId ->
                        notesViewModel.createNote(title, content, collectionId = collectionId)
                        activity?.clearSharedPayload()
                        DraftoToastManager.showSuccess(context.getString(com.ixeken.drafto.core.R.string.share_target_toast_note_saved))
                    },
                    onSaveAsBookmark = { url, title, collectionId ->
                        bookmarksViewModel.saveBookmark(url = url, collectionId = collectionId, customTitle = title)
                        activity?.clearSharedPayload()
                        DraftoToastManager.showSuccess(context.getString(com.ixeken.drafto.core.R.string.share_target_toast_bookmark_saved))
                    },
                    onDismiss = {
                        activity?.clearSharedPayload()
                    }
                )
            }

            // Anfitrión Global de Notificaciones Flotantes Nothing OS (DraftoFloatingToast)
            val targetToastOffset = if (isNavBarVisible) ToastBottomOffsetWithNavBar else ToastBottomOffsetWithoutNavBar
            val animatedToastOffset by animateDpAsState(
                targetValue = targetToastOffset,
                animationSpec = if (LocalDraftoReducedMotion.current) snap() else DraftoSprings.SnappyDp,
                label = "toast_bottom_offset"
            )

            DraftoToastHost(
                modifier = Modifier.align(Alignment.BottomCenter),
                bottomOffset = animatedToastOffset
            )
        }
    }
}
}
