package com.ixeken.drafto.ui.livenote

import androidx.compose.runtime.Immutable
import com.ixeken.drafto.domain.model.LiveNote

/**
 * Estado inmutable de la interfaz de usuario para la edición en Dynamic Island y visualización de la nota en vivo.
 */
@Immutable
data class LiveNoteUiState(
    val activeNote: LiveNote? = null,
    val inputText: String = "",
    val maxCharacterLimit: Int = 64,
    val isEditorExpanded: Boolean = false,
    val isPermissionGranted: Boolean = true
) {
    /**
     * El botón de guardar estará activo si el texto ingresado no es en blanco y no supera el límite.
     */
    val isSaveEnabled: Boolean
        get() = inputText.isNotBlank() && inputText.length <= maxCharacterLimit
}

