package com.ixeken.drafto.ui.bookmarks

import androidx.compose.runtime.Immutable
import com.ixeken.drafto.domain.model.Bookmark
import com.ixeken.drafto.domain.model.BookmarkCollection
import com.ixeken.drafto.domain.model.PlatformFilter
import com.ixeken.drafto.domain.model.TabFilterOption
import com.ixeken.drafto.domain.model.TabSortOption
import com.ixeken.drafto.util.ExtractedMetadata

/**
 * Estado inmutable que describe de manera exhaustiva la interfaz de usuario de Bookmarks.
 *
 * Marcado con [Immutable] para que el compilador de Compose aplique smart-skipping estricto,
 * evitando recomposiciones innecesarias en cascada durante el scroll a 120 FPS.
 *
 * @property bookmarks Lista de marcadores filtrados y listos para ser mostrados en la cuadricula o lista.
 * @property collections Colecciones tematicas creadas por el usuario con sus conteos actualizados.
 * @property selectedCollectionId Identificador de la coleccion filtrada actualmente, o null si no hay filtro de coleccion.
 * @property selectedPlatform Filtro de plataforma web seleccionado ([PlatformFilter.ALL] cuando no hay filtro de red).
 * @property searchQuery Texto de busqueda ingresado por el usuario en la barra superior.
 * @property isSearchActive Bandera que controla la expansion y visibilidad animada del buscador.
 * @property isGridView Alterna entre el diseno Bento en cuadricula de 2 columnas o lista corrida vertical.
 * @property selectedBookmarkIds Conjunto de identificadores de marcadores seleccionados en modo multiseleccion.
 * @property activeDetailBookmark Marcador actualmente enfocado en el Bottom Sheet de detalles y edicion.
 * @property detailBookmarkCollectionIds Conjunto de identificadores de colecciones a las que pertenece el marcador en detalle.
 * @property detectedClipboardUrl Enlace web detectado en el portapapeles del sistema para sugerir guardado inmediato.
 * @property clipboardMetadata Metadatos enriquecidos de OpenGraph extraidos asincronamente para el enlace del portapapeles.
 * @property showCreateCollectionDialog Controla el despliegue del Bottom Sheet para crear una nueva coleccion tematica.
 * @property showDeleteSelectedConfirmDialog Controla la visibilidad del diálogo de confirmación para eliminar marcadores seleccionados.
 * @property isLoading Indica si hay un proceso de carga o actualizacion en curso.
 * @property error Mensaje de error legible ante cualquier falla en operaciones asincronas.
 */
enum class BookmarkViewMode {
    ALL,
    FOLDERS
}

@Immutable
data class BookmarksUiState(
    val bookmarks: List<Bookmark> = emptyList(),
    val collections: List<BookmarkCollection> = emptyList(),
    val selectedCollectionId: String? = null,
    val viewMode: BookmarkViewMode = BookmarkViewMode.ALL,
    val selectedPlatform: PlatformFilter = PlatformFilter.ALL,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isGridView: Boolean = false,
    val filterOption: TabFilterOption = TabFilterOption.ALL,
    val sortOption: TabSortOption = TabSortOption.NEWEST,
    val showOptionsSheet: Boolean = false,
    val selectedBookmarkIds: Set<String> = emptySet(),
    val activeDetailBookmark: Bookmark? = null,
    val detailBookmarkCollectionIds: Set<String> = emptySet(),
    val detectedClipboardUrl: String? = null,
    val clipboardMetadata: ExtractedMetadata? = null,
    val isClipboardAutoDetectEnabled: Boolean = false,
    val isFetchWebMetadataEnabled: Boolean = true,
    val showCreateCollectionDialog: Boolean = false,
    val showDeleteSelectedConfirmDialog: Boolean = false,
    val showAddBookmarkSheet: Boolean = false,
    val showBackupSheet: Boolean = false,
    val backupProgressState: BookmarkBackupProgressState? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    /**
     * Determina si la pantalla esta operando en modo de seleccion multiple.
     */
    val isSelectionMode: Boolean
        get() = selectedBookmarkIds.isNotEmpty()
}

/**
 * Estado inmutable que describe el progreso granular de una operación de respaldo
 * o restauración de marcadores (exportación HTML/JSON o importación).
 *
 * @property isActive Indica si la tarea de backup o importación está en curso.
 * @property isExport true si la tarea es de exportación, false si es importación.
 * @property progress Avance normalizado de 0.0f a 1.0f (o nulo para progreso indeterminado).
 * @property statusText Mensaje descriptivo del paso actual para retroalimentar al usuario.
 */
@Immutable
data class BookmarkBackupProgressState(
    val isActive: Boolean = false,
    val isExport: Boolean = true,
    val progress: Float = 0f,
    val statusText: String = ""
)
