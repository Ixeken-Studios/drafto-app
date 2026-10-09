package com.ixeken.drafto.ui.editor

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.input.TextFieldValue
import com.ixeken.drafto.domain.model.DraftoCollection
import com.ixeken.drafto.domain.model.Note

/**
 * Estado inmutable de la pantalla de edición y previsualización de notas Markdown.
 *
 * Sigue las directrices de Compose Compiler Stability (/jetpack-compose-performance)
 * para Smart Skipping y animaciones a 120 FPS.
 */
@Immutable
data class EditorUiState(
    val initialNote: Note? = null,
    val title: TextFieldValue = TextFieldValue(""),
    val content: TextFieldValue = TextFieldValue(""),
    val isPinned: Boolean = false,
    val selectedCollectionId: String? = null,
    val isPreviewMode: Boolean = false,
    val showOptionsMenu: Boolean = false,
    val availableCollections: List<DraftoCollection> = emptyList(),
    val isLoading: Boolean = false
)
