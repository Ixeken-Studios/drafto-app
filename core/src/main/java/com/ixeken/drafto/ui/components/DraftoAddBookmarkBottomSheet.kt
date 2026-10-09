package com.ixeken.drafto.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ixeken.drafto.domain.model.BookmarkCollection

/**
 * Modal Bottom Sheet para agregar manualmente un nuevo enlace o marcador a Drafto.
 * Delega en [DraftoBookmarkFormBottomSheet] implementando el nuevo lenguaje visual editorial Nothing OS.
 */
@Composable
fun DraftoAddBookmarkBottomSheet(
    collections: List<BookmarkCollection> = emptyList(),
    prefillUrl: String = "",
    prefillTitle: String? = null,
    prefillDescription: String? = null,
    initialCollectionId: String? = null,
    onDismiss: () -> Unit,
    onSave: (url: String, title: String?, description: String?, collectionId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    DraftoBookmarkFormBottomSheet(
        isEditMode = false,
        initialBookmark = null,
        prefillUrl = prefillUrl,
        prefillTitle = prefillTitle,
        prefillDescription = prefillDescription,
        collections = collections,
        initialCollectionId = initialCollectionId,
        onDismiss = onDismiss,
        onSave = onSave,
        modifier = modifier
    )
}

/**
 * Sobrecarga retrocompatible sin parámetro de descripción.
 */
@Composable
fun DraftoAddBookmarkBottomSheet(
    collections: List<BookmarkCollection> = emptyList(),
    prefillUrl: String = "",
    initialCollectionId: String? = null,
    onDismiss: () -> Unit,
    onSave: (url: String, title: String?, collectionId: String?) -> Unit
) {
    DraftoBookmarkFormBottomSheet(
        isEditMode = false,
        initialBookmark = null,
        collections = collections,
        initialCollectionId = initialCollectionId,
        onDismiss = onDismiss,
        onSave = { url, title, _, colId -> onSave(url, title, colId) }
    )
}
