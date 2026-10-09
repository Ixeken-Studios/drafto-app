package com.ixeken.drafto.ui.bookmarks

import android.content.Context
import android.net.Uri
import com.ixeken.drafto.ui.components.DraftoToastManager
import com.ixeken.drafto.core.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ixeken.drafto.data.backup.BookmarkBackupManager
import com.ixeken.drafto.data.backup.BookmarkExportFormat
import com.ixeken.drafto.data.local.datastore.SettingsDataStore
import com.ixeken.drafto.domain.model.Bookmark
import com.ixeken.drafto.domain.model.BookmarkCollection
import com.ixeken.drafto.domain.model.PlatformFilter
import com.ixeken.drafto.domain.model.TabFilterOption
import com.ixeken.drafto.domain.model.TabSortOption
import com.ixeken.drafto.domain.repository.BookmarkRepository
import com.ixeken.drafto.util.LinkMetadataExtractor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Gestor de estado para la pantalla de marcadores y colecciones.
 * Integra la persistencia de datos, la gestión de selecciones múltiples,
 * extracción de metadatos de enlaces y exportación/importación SAF.
 *
 * @property bookmarkRepository Repositorio de dominio puro para marcadores y colecciones.
 * @property bookmarkBackupManager Administrador para exportación/importación SAF en segundo plano.
 * @property settingsDataStore Almacenamiento persistente de preferencias de usuario.
 */
@HiltViewModel
class BookmarksViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val bookmarkRepository: BookmarkRepository,
    private val bookmarkBackupManager: BookmarkBackupManager,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _selectedCollectionId = MutableStateFlow<String?>(null)
    private val _selectedPlatform = MutableStateFlow(PlatformFilter.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _filterOption = MutableStateFlow(TabFilterOption.ALL)
    private val _sortOption = MutableStateFlow(TabSortOption.NEWEST)

    private val _backupProgressState = MutableStateFlow<BookmarkBackupProgressState?>(null)
    val backupProgressState: StateFlow<BookmarkBackupProgressState?> = _backupProgressState.asStateFlow()

    private val _uiState = MutableStateFlow(BookmarksUiState(isLoading = true))
    val uiState: StateFlow<BookmarksUiState> = _uiState.asStateFlow()

    private var detailCollectionJob: Job? = null
    private var lastCheckedClipboardUrl: String? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    private val baseBookmarksFlow = combine(
        _selectedCollectionId,
        _selectedPlatform
    ) { colId, platform -> colId to platform }
        .flatMapLatest { (colId, platform) ->
            when {
                colId != null -> bookmarkRepository.observeBookmarksByCollection(colId)
                platform != PlatformFilter.ALL -> bookmarkRepository.observeBookmarksByPlatform(platform)
                else -> bookmarkRepository.observeAllBookmarks()
            }
        }

    private val bookmarksFlow = combine(
        baseBookmarksFlow,
        _searchQuery,
        _filterOption,
        _sortOption
    ) { list, query, filter, sort ->
        var result = list

        if (filter == TabFilterOption.WITHOUT_COLLECTION) {
            result = result.filter { it.collectionId.isNullOrBlank() }
        }

        if (query.isNotBlank()) {
            val q = query.trim()
            result = result.filter {
                it.title?.contains(q, ignoreCase = true) == true ||
                    it.url.contains(q, ignoreCase = true) ||
                    it.description?.contains(q, ignoreCase = true) == true ||
                    it.domain.contains(q, ignoreCase = true)
            }
        }

        when (sort) {
            TabSortOption.NEWEST -> result.sortedByDescending { it.createdAt }
            TabSortOption.OLDEST -> result.sortedBy { it.createdAt }
            TabSortOption.ALPHABETICAL -> result.sortedBy { (it.title ?: it.domain).lowercase() }
        }
    }

    init {
        cleanupEmptyCollections()
        observeCollections()
        observeBookmarks()
        observeViewMode()
        observeClipboardAutoDetect()
        observeFetchWebMetadata()
    }

    private fun cleanupEmptyCollections() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                bookmarkRepository.deleteEmptyCollections()
            }
        }
    }

    private fun observeCollections() {
        viewModelScope.launch {
            bookmarkRepository.observeAllCollections()
                .catch { exception ->
                    _uiState.update { it.copy(error = exception.message) }
                }
                .collect { collections ->
                    _uiState.update { current ->
                        val currentSelected = current.selectedCollectionId
                        val stillExists = currentSelected == null || collections.any { it.id == currentSelected }
                        val newSelected = if (stillExists) currentSelected else null
                        if (!stillExists && _selectedCollectionId.value != null) {
                            _selectedCollectionId.value = null
                        }
                        current.copy(
                            collections = collections,
                            selectedCollectionId = newSelected
                        )
                    }
                }
        }
    }

    private fun observeBookmarks() {
        viewModelScope.launch {
            bookmarksFlow
                .catch { exception ->
                    _uiState.update { it.copy(error = exception.message, isLoading = false) }
                }
                .collect { bookmarks ->
                    _uiState.update { it.copy(bookmarks = bookmarks, isLoading = false) }
                }
        }
    }

    private fun observeViewMode() {
        viewModelScope.launch {
            settingsDataStore.bookmarksGridViewFlow.collect { isGrid ->
                _uiState.update { it.copy(isGridView = isGrid) }
            }
        }
    }

    private fun observeClipboardAutoDetect() {
        viewModelScope.launch {
            settingsDataStore.clipboardAutoDetectFlow.collect { isEnabled ->
                _uiState.update { it.copy(isClipboardAutoDetectEnabled = isEnabled) }
            }
        }
    }

    private fun observeFetchWebMetadata() {
        viewModelScope.launch {
            settingsDataStore.fetchWebMetadataFlow.collect { isEnabled ->
                _uiState.update { it.copy(isFetchWebMetadataEnabled = isEnabled) }
            }
        }
    }

    /**
     * Actualiza el termino de busqueda en tiempo real.
     */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * Alterna la visibilidad de la barra de busqueda animada.
     */
    fun toggleSearch(active: Boolean? = null) {
        _uiState.update { current ->
            val newActive = active ?: !current.isSearchActive
            val newQuery = if (!newActive) "" else current.searchQuery
            if (!newActive) {
                _searchQuery.value = ""
            }
            current.copy(isSearchActive = newActive, searchQuery = newQuery)
        }
    }

    /**
     * Cierra el buscador y limpia la consulta activa.
     */
    fun closeSearch() {
        toggleSearch(active = false)
    }

    /**
     * Alterna el modo de visualizacion entre cuadricula Bento y lista corrida persistiendo en DataStore.
     */
    fun toggleGridView() {
        viewModelScope.launch {
            settingsDataStore.setBookmarksGridView(!_uiState.value.isGridView)
        }
    }

    /**
     * Actualiza el criterio de filtrado por pertenencia a colección de forma reactiva.
     */
    fun setFilterOption(option: TabFilterOption) {
        _filterOption.value = option
        _uiState.update { it.copy(filterOption = option) }
    }

    /**
     * Actualiza el criterio de ordenación activo para marcadores.
     */
    fun setSortOption(option: TabSortOption) {
        _sortOption.value = option
        _uiState.update { it.copy(sortOption = option) }
    }

    /**
     * Controla la visibilidad del modal de opciones de la pestaña Bookmarks.
     */
    fun setShowOptionsSheet(show: Boolean) {
        _uiState.update { it.copy(showOptionsSheet = show) }
    }

    /**
     * Alterna entre el feed global de todos los marcadores y la exploracion por carpetas.
     */
    fun setViewMode(mode: BookmarkViewMode) {
        _uiState.update {
            it.copy(
                viewMode = mode,
                selectedCollectionId = if (mode == BookmarkViewMode.ALL) null else it.selectedCollectionId
            )
        }
        if (mode == BookmarkViewMode.ALL) {
            _selectedCollectionId.value = null
        }
    }

    /**
     * Aplica el filtro por coleccion tematica. Si [collectionId] es null, se muestran todos.
     */
    fun selectCollection(collectionId: String?) {
        _selectedCollectionId.value = collectionId
        _selectedPlatform.value = PlatformFilter.ALL
        _uiState.update {
            it.copy(
                selectedCollectionId = collectionId,
                selectedPlatform = PlatformFilter.ALL
            )
        }
    }

    /**
     * Aplica el filtro por plataforma web automatica (Instagram, YouTube, GitHub, X, Articulos).
     */
    fun selectPlatform(platform: PlatformFilter) {
        _selectedCollectionId.value = null
        _selectedPlatform.value = platform
        _uiState.update {
            it.copy(
                selectedCollectionId = null,
                selectedPlatform = platform
            )
        }
    }

    /**
     * Abre el Bottom Sheet con la ficha de detalle, previsualizacion y opciones del marcador.
     */
    fun openDetail(bookmark: Bookmark) {
        _uiState.update { it.copy(activeDetailBookmark = bookmark) }
        observeDetailCollections(bookmark.id)
    }

    /**
     * Cierra el modal de detalle del marcador y libera la observacion de colecciones asignadas.
     */
    fun closeDetail() {
        detailCollectionJob?.cancel()
        detailCollectionJob = null
        _uiState.update { it.copy(activeDetailBookmark = null, detailBookmarkCollectionIds = emptySet()) }
    }

    private fun observeDetailCollections(bookmarkId: String) {
        detailCollectionJob?.cancel()
        detailCollectionJob = viewModelScope.launch {
            bookmarkRepository.observeCollectionIdsForBookmark(bookmarkId)
                .catch { /* Fallback silencioso */ }
                .collect { ids ->
                    _uiState.update { it.copy(detailBookmarkCollectionIds = ids.toSet()) }
                }
        }
    }

    /**
     * Alterna el estado de fijado (pin) de un marcador en la base de datos.
     */
    fun togglePin(id: String, isPinned: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                bookmarkRepository.togglePin(id, isPinned)
            }.onFailure { exception ->
                _uiState.update { it.copy(error = exception.message) }
            }
        }
    }

    /**
     * Alterna la visibilidad de la imagen previa de un marcador en la base de datos y en el estado de detalle activo.
     */
    fun toggleBookmarkPreviewVisibility(id: String, isVisible: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                bookmarkRepository.updatePreviewVisibility(id, isVisible)
                _uiState.update { current ->
                    if (current.activeDetailBookmark?.id == id) {
                        current.copy(activeDetailBookmark = current.activeDetailBookmark.copy(isPreviewVisible = isVisible))
                    } else current
                }
            }.onFailure { exception ->
                _uiState.update { it.copy(error = exception.message) }
            }
        }
    }

    /**
     * Elimina definitivamente un marcador por su ID universal y cierra la hoja de detalle si coincide.
     */
    fun deleteBookmark(id: String) {
        if (_uiState.value.activeDetailBookmark?.id == id) {
            closeDetail()
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                bookmarkRepository.deleteBookmark(id)
            }.onFailure { exception ->
                _uiState.update { it.copy(error = exception.message) }
            }
        }
    }

    /**
     * Actualiza el titulo y la descripcion de un marcador existente tras ser editado en el Bottom Sheet.
     */
    fun updateBookmark(
        id: String,
        title: String,
        description: String,
        url: String? = null,
        collectionId: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val existing = bookmarkRepository.findBookmarkById(id) ?: return@launch
                val updatedUrl = url?.trim()?.takeIf { it.isNotBlank() } ?: existing.url
                if (!LinkMetadataExtractor.isValidUrl(updatedUrl)) return@launch
                val normalizedUrl = if (updatedUrl.startsWith("http://", ignoreCase = true) ||
                    updatedUrl.startsWith("https://", ignoreCase = true)
                ) {
                    updatedUrl
                } else {
                    "https://$updatedUrl"
                }
                val updated = existing.copy(
                    url = normalizedUrl,
                    domain = if (normalizedUrl != existing.url) LinkMetadataExtractor.extractDomain(normalizedUrl) else existing.domain,
                    title = title.trim().ifBlank { null },
                    description = description.trim().ifBlank { null }
                )
                bookmarkRepository.updateBookmark(updated)

                if (collectionId != null) {
                    val currentIds = _uiState.value.detailBookmarkCollectionIds
                    currentIds.forEach { colId ->
                        if (colId != collectionId) {
                            bookmarkRepository.removeBookmarkFromCollection(id, colId)
                        }
                    }
                    bookmarkRepository.addBookmarkToCollection(id, collectionId)
                } else {
                    val currentIds = _uiState.value.detailBookmarkCollectionIds
                    currentIds.forEach { colId ->
                        bookmarkRepository.removeBookmarkFromCollection(id, colId)
                    }
                }

                _uiState.update { current ->
                    if (current.activeDetailBookmark?.id == id) {
                        current.copy(activeDetailBookmark = updated)
                    } else current
                }
            }.onFailure { exception ->
                _uiState.update { it.copy(error = exception.message) }
            }
        }
    }

    /**
     * Asocia o desvincula un marcador de una coleccion tematica especifica.
     */
    fun toggleBookmarkCollection(bookmarkId: String, collectionId: String, isAssigned: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                if (isAssigned) {
                    bookmarkRepository.addBookmarkToCollection(bookmarkId, collectionId)
                } else {
                    bookmarkRepository.removeBookmarkFromCollection(bookmarkId, collectionId)
                }
            }.onFailure { exception ->
                _uiState.update { it.copy(error = exception.message) }
            }
        }
    }

    /**
     * Alterna la seleccion de un marcador en el modo multiseleccion.
     */
    fun toggleSelection(bookmarkId: String) {
        _uiState.update { state ->
            val newSelection = if (state.selectedBookmarkIds.contains(bookmarkId)) {
                state.selectedBookmarkIds - bookmarkId
            } else {
                state.selectedBookmarkIds + bookmarkId
            }
            state.copy(selectedBookmarkIds = newSelection)
        }
    }

    /**
     * Limpia la seleccion multiple activa y restaura el modo normal.
     */
    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedBookmarkIds = emptySet(),
                showDeleteSelectedConfirmDialog = false
            )
        }
    }

    /**
     * Controla la visibilidad del diálogo modal de confirmación para eliminar
     * los marcadores seleccionados en modo multiselección.
     */
    fun setShowDeleteSelectedConfirmDialog(show: Boolean) {
        _uiState.update { it.copy(showDeleteSelectedConfirmDialog = show) }
    }

    /**
     * Elimina en lote todos los marcadores seleccionados en la sesion actual.
     */
    fun deleteSelectedBookmarks() {
        val idsToDelete = _uiState.value.selectedBookmarkIds
        if (idsToDelete.isEmpty()) {
            _uiState.update { it.copy(showDeleteSelectedConfirmDialog = false) }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                idsToDelete.forEach { id ->
                    bookmarkRepository.deleteBookmark(id)
                }
                clearSelection()
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        error = exception.message,
                        showDeleteSelectedConfirmDialog = false
                    )
                }
            }
        }
    }

    /**
     * Controla la visibilidad del modal para crear colecciones personalizadas.
     */
    fun setShowCreateCollectionDialog(show: Boolean) {
        _uiState.update { it.copy(showCreateCollectionDialog = show) }
    }

    /**
     * Controla la visibilidad del modal para agregar manualmente nuevos marcadores.
     */
    fun setShowAddBookmarkSheet(show: Boolean) {
        _uiState.update { it.copy(showAddBookmarkSheet = show) }
    }

    /**
     * Crea y persiste una nueva coleccion tematica con color e icono personalizados.
     */
    fun createCollection(name: String, colorHex: String, iconName: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val collection = BookmarkCollection(
                    id = UUID.randomUUID().toString(),
                    name = trimmedName,
                    colorHex = colorHex,
                    iconName = iconName,
                    createdAt = System.currentTimeMillis()
                )
                bookmarkRepository.saveCollection(collection)
                _uiState.update { it.copy(showCreateCollectionDialog = false) }
            }.onFailure { exception ->
                _uiState.update { it.copy(error = exception.message) }
            }
        }
    }

    /**
     * Actualiza los datos (nombre, color, icono) de una coleccion existente.
     */
    fun updateCollection(collectionId: String, name: String, colorHex: String, iconName: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val existing = _uiState.value.collections.find { it.id == collectionId }
                val updated = BookmarkCollection(
                    id = collectionId,
                    name = trimmedName,
                    colorHex = colorHex,
                    iconName = iconName,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis()
                )
                bookmarkRepository.updateCollection(updated)
            }.onFailure { exception ->
                _uiState.update { it.copy(error = exception.message) }
            }
        }
    }

    /**
     * Elimina una coleccion tematica por su identificador unico.
     */
    fun deleteCollection(collectionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                bookmarkRepository.deleteCollection(collectionId)
                if (_selectedCollectionId.value == collectionId) {
                    selectCollection(null)
                }
            }.onFailure { exception ->
                _uiState.update { it.copy(error = exception.message) }
            }
        }
    }

    /**
     * Verifica si el texto detectado en el portapapeles corresponde a una URL web valida
     * que no este registrada previamente en la base de datos local ni haya sido procesada en esta sesion.
     */
    fun checkClipboard(url: String?) {
        if (!_uiState.value.isClipboardAutoDetectEnabled) return
        if (url.isNullOrBlank()) return
        val trimmed = url.trim()
        if (!LinkMetadataExtractor.isValidUrl(trimmed)) return

        val normalized = if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }

        if (normalized == lastCheckedClipboardUrl) return
        lastCheckedClipboardUrl = normalized

        viewModelScope.launch(Dispatchers.IO) {
            val existing = bookmarkRepository.findBookmarkByUrl(normalized)
            if (existing == null) {
                _uiState.update {
                    it.copy(
                        detectedClipboardUrl = normalized,
                        clipboardMetadata = null
                    )
                }
                val metadata = LinkMetadataExtractor.extract(
                    url = normalized,
                    allowNetwork = _uiState.value.isFetchWebMetadataEnabled
                )
                _uiState.update { current ->
                    if (current.detectedClipboardUrl == normalized) {
                        current.copy(clipboardMetadata = metadata)
                    } else current
                }
            }
        }
    }

    /**
     * Descarta el modal de confirmacion del portapapeles sin guardar el enlace.
     */
    fun dismissClipboard() {
        _uiState.update { it.copy(detectedClipboardUrl = null, clipboardMetadata = null) }
    }

    /**
     * Guarda el enlace detectado en el portapapeles, asocia opcionalmente una coleccion
     * e inicia la extraccion asincrona de metadatos si no se ha completado aun.
     */
    fun saveClipboardBookmark(collectionId: String? = null) {
        val url = _uiState.value.detectedClipboardUrl ?: return
        val meta = _uiState.value.clipboardMetadata
        _uiState.update { it.copy(detectedClipboardUrl = null, clipboardMetadata = null) }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val bookmarkId = UUID.randomUUID().toString()
                val domain = meta?.domain ?: LinkMetadataExtractor.extractDomain(url)
                val platform = meta?.platform ?: LinkMetadataExtractor.detectPlatform(url)
                val bookmark = Bookmark(
                    id = bookmarkId,
                    url = url,
                    title = meta?.title ?: domain,
                    description = meta?.description,
                    imageUrl = meta?.imageUrl,
                    domain = domain,
                    faviconUrl = meta?.faviconUrl,
                    createdAt = System.currentTimeMillis(),
                    isPinned = false,
                    platform = platform
                )
                bookmarkRepository.saveBookmark(bookmark)
                if (collectionId != null) {
                    bookmarkRepository.addBookmarkToCollection(bookmarkId, collectionId)
                }

                // Si los metadatos estaban pendientes de extraccion al confirmar, los obtenemos y actualizamos
                if (meta == null) {
                    val extracted = LinkMetadataExtractor.extract(
                        url = url,
                        allowNetwork = _uiState.value.isFetchWebMetadataEnabled
                    )
                    val enriched = bookmark.copy(
                        title = extracted.title ?: bookmark.title,
                        description = extracted.description ?: bookmark.description,
                        imageUrl = extracted.imageUrl ?: bookmark.imageUrl,
                        faviconUrl = extracted.faviconUrl ?: bookmark.faviconUrl,
                        platform = extracted.platform
                    )
                    bookmarkRepository.updateBookmark(enriched)
                }
            }.onFailure { exception ->
                _uiState.update { it.copy(error = exception.message) }
            }
        }
    }

    /**
     * Guarda directamente una URL creando el marcador de inmediato y extrayendo metadatos en segundo plano.
     */
    fun saveBookmark(
        url: String,
        collectionId: String? = null,
        customTitle: String? = null,
        customDescription: String? = null
    ) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return
        if (!LinkMetadataExtractor.isValidUrl(trimmed)) return
        val normalized = if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val bookmarkId = UUID.randomUUID().toString()
                val domain = LinkMetadataExtractor.extractDomain(normalized)
                val platform = LinkMetadataExtractor.detectPlatform(normalized)
                val initialTitle = customTitle?.trim()?.takeIf { it.isNotBlank() } ?: domain
                val initialDescription = customDescription?.trim()?.takeIf { it.isNotBlank() }
                val initialBookmark = Bookmark(
                    id = bookmarkId,
                    url = normalized,
                    title = initialTitle,
                    description = initialDescription,
                    domain = domain,
                    createdAt = System.currentTimeMillis(),
                    isPinned = false,
                    platform = platform
                )
                bookmarkRepository.saveBookmark(initialBookmark)
                if (collectionId != null) {
                    bookmarkRepository.addBookmarkToCollection(bookmarkId, collectionId)
                }
                val metadata = LinkMetadataExtractor.extract(
                    url = normalized,
                    allowNetwork = _uiState.value.isFetchWebMetadataEnabled
                )
                val enriched = initialBookmark.copy(
                    title = customTitle?.trim()?.takeIf { it.isNotBlank() } ?: metadata.title ?: initialBookmark.title,
                    description = customDescription?.trim()?.takeIf { it.isNotBlank() } ?: metadata.description,
                    imageUrl = metadata.imageUrl,
                    faviconUrl = metadata.faviconUrl,
                    platform = metadata.platform
                )
                bookmarkRepository.updateBookmark(enriched)
            }.onFailure { exception ->
                _uiState.update { it.copy(error = exception.message) }
            }
        }
    }

    /**
     * Controla la visibilidad del modal de respaldo y restauración de marcadores.
     */
    fun setBackupSheetVisible(show: Boolean) {
        _uiState.update { it.copy(showBackupSheet = show) }
    }

    /**
     * Orquesta la exportación asíncrona de marcadores (toda la biblioteca o colección activa)
     * hacia el almacenamiento seleccionado por el usuario mediante el Storage Access Framework (SAF).
     *
     * Despacha el trabajo sobre un hilo de fondo, actualizando el estado reactivo de progreso
     * para retroalimentar la interfaz sin provocar jank ni congelar la pantalla a 120 FPS.
     *
     * @param uri URI provisto por el contrato CreateDocument de SAF para escribir el archivo.
     * @param format Formato elegido (Netscape HTML o JSON nativo de Drafto).
     * @param collectionId ID opcional de la colección seleccionada para exportación acotada.
     * @param onSuccess Callback invocado al completar la escritura exitosa.
     * @param onError Callback invocado ante fallas de E/S o cancelación de permisos.
     */
    fun exportBookmarks(
        uri: Uri,
        format: BookmarkExportFormat,
        collectionId: String? = null,
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            val initialStatus = context.getString(com.ixeken.drafto.core.R.string.progress_exporting_bookmarks)
            val initialProgress = BookmarkBackupProgressState(
                isActive = true,
                isExport = true,
                progress = 0.1f,
                statusText = initialStatus
            )
            _backupProgressState.value = initialProgress
            _uiState.update { it.copy(backupProgressState = initialProgress) }

            try {
                val outputStream = context.contentResolver.openOutputStream(uri)
                    ?: throw IllegalStateException("No se pudo abrir el flujo de escritura para la exportación.")

                val result = bookmarkBackupManager.exportBookmarks(
                    outputStream = outputStream,
                    format = format,
                    collectionId = collectionId,
                    onProgress = { progress, statusMessage ->
                        val progressObj = BookmarkBackupProgressState(
                            isActive = true,
                            isExport = true,
                            progress = progress,
                            statusText = statusMessage
                        )
                        _backupProgressState.value = progressObj
                        _uiState.update { it.copy(backupProgressState = progressObj) }
                    }
                )
                kotlinx.coroutines.delay(350)
                _backupProgressState.value = null
                _uiState.update { it.copy(backupProgressState = null) }
                result.fold(
                    onSuccess = { onSuccess() },
                    onFailure = { onError(it) }
                )
            } catch (e: Exception) {
                _backupProgressState.value = null
                _uiState.update { it.copy(backupProgressState = null, error = e.message) }
                onError(e)
            }
        }
    }

    /**
     * Importa marcadores desde un archivo universal HTML (Netscape) o JSON de respaldo,
     * detectando automáticamente el formato, deserializando jerarquías de carpetas y persistiendo
     * en lote con resolución y deduplicación inteligente de enlaces existentes.
     *
     * @param uri URI del archivo seleccionado mediante el contrato OpenDocument de SAF.
     * @param onSuccess Callback con la cantidad de marcadores importados y colecciones creadas.
     * @param onError Callback invocado ante fallas de lectura o formato inválido.
     */
    fun importBookmarks(
        uri: Uri,
        onSuccess: (imported: Int, collections: Int) -> Unit = { _, _ -> },
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            val initialStatus = context.getString(com.ixeken.drafto.core.R.string.progress_importing_bookmarks)
            val initialProgress = BookmarkBackupProgressState(
                isActive = true,
                isExport = false,
                progress = 0.1f,
                statusText = initialStatus
            )
            _backupProgressState.value = initialProgress
            _uiState.update { it.copy(backupProgressState = initialProgress) }

            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("No se pudo abrir el archivo de marcadores seleccionado.")

                val result = bookmarkBackupManager.importBookmarks(
                    inputStream = inputStream,
                    onProgress = { progress, statusMessage ->
                        val progressObj = BookmarkBackupProgressState(
                            isActive = true,
                            isExport = false,
                            progress = progress,
                            statusText = statusMessage
                        )
                        _backupProgressState.value = progressObj
                        _uiState.update { it.copy(backupProgressState = progressObj) }
                    }
                )
                kotlinx.coroutines.delay(350)
                _backupProgressState.value = null
                _uiState.update { it.copy(backupProgressState = null) }
                result.fold(
                    onSuccess = { (imported, collections) ->
                        onSuccess(imported, collections)
                    },
                    onFailure = {
                        android.util.Log.e("BookmarksViewModel", "Error importing bookmarks: ${it.message}", it)
                        onError(it)
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("BookmarksViewModel", "Exception in importBookmarks: ${e.message}", e)
                _backupProgressState.value = null
                _uiState.update { it.copy(backupProgressState = null, error = e.message) }
                onError(e)
            }
        }
    }

    private var pendingExportCollectionId: String? = null

    /**
     * Prepara y retorna el nombre de archivo sugerido para la exportación SAF,
     * almacenando internamente el identificador de la colección opcional.
     */
    fun getExportFileName(format: BookmarkExportFormat, collectionId: String? = null): String {
        this.pendingExportCollectionId = collectionId
        val extension = if (format == BookmarkExportFormat.HTML) "html" else "json"
        return "Drafto_Bookmarks_${System.currentTimeMillis()}.$extension"
    }

    /**
     * Procesa la selección del URI devuelto por el contrato CreateDocument de SAF,
     * despachando la exportación y retroalimentando mediante Toast sin ensuciar la UI.
     */
    fun onExportUriSelected(
        uri: Uri,
        format: BookmarkExportFormat,
        onSuccess: () -> Unit = {
            DraftoToastManager.showSuccess(context.getString(R.string.toast_bookmarks_export_success))
        },
        onError: (Throwable) -> Unit = {
            DraftoToastManager.showWarning(context.getString(R.string.toast_bookmarks_export_error))
        }
    ) {
        exportBookmarks(
            uri = uri,
            format = format,
            collectionId = pendingExportCollectionId,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    /**
     * Procesa la selección del URI devuelto por el contrato OpenDocument de SAF,
     * deserializando los marcadores e informando el resultado mediante Toast.
     */
    fun onImportUriSelected(
        uri: Uri,
        onSuccess: (imported: Int, collections: Int) -> Unit = { imported, collections ->
            if (imported == 0) {
                DraftoToastManager.showInfo(context.getString(R.string.toast_bookmarks_import_empty))
            } else {
                DraftoToastManager.showSuccess(
                    context.getString(R.string.toast_bookmarks_import_success, imported, collections)
                )
            }
        },
        onError: (Throwable) -> Unit = {
            DraftoToastManager.showWarning(context.getString(R.string.toast_bookmarks_import_error))
        }
    ) {
        importBookmarks(
            uri = uri,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    companion object {
        val IMPORT_MIME_TYPES = arrayOf("text/html", "application/json", "text/plain", "*/*")
    }
}

