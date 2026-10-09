package com.ixeken.drafto.ui.editor

import android.content.Context
import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ixeken.drafto.data.backup.NotebookBackupManager
import com.ixeken.drafto.domain.model.Note
import com.ixeken.drafto.domain.repository.CollectionRepository
import com.ixeken.drafto.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel que orquesta la pantalla del editor de notas Markdown.
 *
 * Responsabilidades:
 * - Aisla la logica de dirty-checking y auto-guardado en Dispatchers.IO.
 * - Desacopla la manipulacion de texto y formato Markdown de la UI.
 * - Centraliza las operaciones de persistencia, exportacion y borrado.
 */
@HiltViewModel
class EditorViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val collectionRepository: CollectionRepository,
    private val notebookBackupManager: NotebookBackupManager,
    @param:ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val routeNoteId: String = savedStateHandle.get<String>("noteId") ?: "new"
    private val routeCollectionId: String? = savedStateHandle.get<String>("collectionId")

    private val _uiState = MutableStateFlow(
        EditorUiState(
            selectedCollectionId = routeCollectionId,
            isLoading = routeNoteId != "new"
        )
    )
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        loadCollections()
        if (routeNoteId != "new") {
            loadNote(routeNoteId)
        }
    }

    fun initialize(noteId: String?, initialCollectionId: String?) {
        val targetId = noteId ?: routeNoteId
        if (targetId != "new" && _uiState.value.initialNote?.id != targetId) {
            loadNote(targetId)
        }
        if (initialCollectionId != null && _uiState.value.selectedCollectionId == null) {
            _uiState.update { it.copy(selectedCollectionId = initialCollectionId) }
        }
    }

    private fun loadCollections() {
        viewModelScope.launch(Dispatchers.IO) {
            collectionRepository.getCollections().collect { collections ->
                _uiState.update { it.copy(availableCollections = collections) }
            }
        }
    }

    private fun loadNote(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val note = noteRepository.getNoteById(id)
            withContext(Dispatchers.Main) {
                if (note != null) {
                    _uiState.update {
                        it.copy(
                            initialNote = note,
                            title = TextFieldValue(note.title),
                            content = TextFieldValue(note.content),
                            isPinned = note.isPinned,
                            selectedCollectionId = note.collectionId ?: routeCollectionId,
                            isPreviewMode = true,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onTitleChange(newTitle: TextFieldValue) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun onContentChange(newContent: TextFieldValue) {
        _uiState.update { it.copy(content = newContent) }
    }

    fun togglePin() {
        _uiState.update { it.copy(isPinned = !it.isPinned) }
    }

    fun selectCollection(collectionId: String?) {
        _uiState.update { it.copy(selectedCollectionId = collectionId) }
    }

    fun togglePreviewMode() {
        _uiState.update { it.copy(isPreviewMode = !it.isPreviewMode) }
    }

    fun toggleOptionsMenu() {
        _uiState.update { it.copy(showOptionsMenu = !it.showOptionsMenu) }
    }

    fun dismissOptionsMenu() {
        _uiState.update { it.copy(showOptionsMenu = false) }
    }

    fun applyMarkdownFormat(prefix: String, suffix: String = prefix) {
        val content = _uiState.value.content
        val sel = content.selection
        val curText = content.text
        val newTfv = if (sel.collapsed) {
            val newText = curText.substring(0, sel.start) + prefix + suffix + curText.substring(sel.start)
            TextFieldValue(text = newText, selection = TextRange(sel.start + prefix.length))
        } else {
            val selected = curText.substring(sel.min, sel.max)
            val newText = curText.substring(0, sel.min) + prefix + selected + suffix + curText.substring(sel.max)
            TextFieldValue(
                text = newText,
                selection = TextRange(sel.min + prefix.length, sel.min + prefix.length + selected.length)
            )
        }
        _uiState.update { it.copy(content = newTfv) }
    }

    fun applyLineMarkdownFormat(prefix: String) {
        val content = _uiState.value.content
        val sel = content.selection
        val curText = content.text
        val newTfv = if (sel.collapsed) {
            val newText = curText.substring(0, sel.start) + prefix + curText.substring(sel.start)
            TextFieldValue(text = newText, selection = TextRange(sel.start + prefix.length))
        } else {
            val selected = curText.substring(sel.min, sel.max)
            val newText = curText.substring(0, sel.min) + prefix + selected + curText.substring(sel.max)
            TextFieldValue(
                text = newText,
                selection = TextRange(sel.min + prefix.length, sel.min + prefix.length + selected.length)
            )
        }
        _uiState.update { it.copy(content = newTfv) }
    }

    /**
     * Auto-guarda la nota si se detectan cambios con respecto al estado inicial o si es una nota nueva con contenido.
     */
    fun saveIfNeeded(defaultTitle: String = "Untitled note", onSaved: () -> Unit = {}) {
        val state = _uiState.value
        val currentTitle = state.title.text.trim()
        val currentContent = state.content.text.trim()
        val initial = state.initialNote

        val hasChanges = if (initial != null) {
            currentTitle != initial.title ||
            currentContent != initial.content ||
            state.isPinned != initial.isPinned ||
            state.selectedCollectionId != initial.collectionId
        } else {
            currentTitle.isNotBlank() || currentContent.isNotBlank() || state.selectedCollectionId != null
        }

        if (!hasChanges) {
            onSaved()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val finalTitle = currentTitle.ifBlank { defaultTitle }
            val noteToSave = Note(
                id = initial?.id ?: UUID.randomUUID().toString(),
                title = finalTitle,
                content = currentContent,
                createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isPinned = state.isPinned,
                collectionId = state.selectedCollectionId,
                tags = initial?.tags ?: emptyList()
            )
            noteRepository.saveNote(noteToSave)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(initialNote = noteToSave) }
                onSaved()
            }
        }
    }

    fun deleteNote(onDeleted: () -> Unit) {
        val noteId = _uiState.value.initialNote?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            noteRepository.deleteNote(noteId)
            withContext(Dispatchers.Main) {
                onDeleted()
            }
        }
    }

    fun exportMarkdown(uri: Uri, defaultTitle: String, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        val state = _uiState.value
        val currentNote = Note(
            id = state.initialNote?.id ?: UUID.randomUUID().toString(),
            title = state.title.text.trim().ifBlank { defaultTitle },
            content = state.content.text.trim(),
            createdAt = state.initialNote?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isPinned = state.isPinned,
            collectionId = state.selectedCollectionId,
            tags = state.initialNote?.tags ?: emptyList()
        )
        viewModelScope.launch {
            try {
                val outputStream = withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)
                } ?: throw IllegalStateException("No se pudo abrir el archivo para exportar la nota.")

                val result = notebookBackupManager.exportSingleNote(currentNote, outputStream)
                result.fold(
                    onSuccess = { onSuccess() },
                    onFailure = { onError(it) }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                onError(e)
            }
        }
    }
}
