package com.ixeken.drafto.ui.livenote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ixeken.drafto.data.notification.DraftoNotificationManager
import com.ixeken.drafto.domain.usecase.ManageLiveNoteUseCase
import com.ixeken.drafto.domain.usecase.ScheduleNotificationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsable de la lógica de negocio y gestión de estado para las notas rápidas en Dynamic Island y Notificaciones.
 */
@HiltViewModel
class LiveNoteViewModel @Inject constructor(
    private val manageLiveNoteUseCase: ManageLiveNoteUseCase,
    private val scheduleNotificationUseCase: ScheduleNotificationUseCase,
    private val draftoNotificationManager: DraftoNotificationManager
) : ViewModel() {

    private val _inputText = MutableStateFlow("")
    private val _isEditorExpanded = MutableStateFlow(false)
    private val _isPermissionGranted = MutableStateFlow(true)

    val uiState: StateFlow<LiveNoteUiState> = combine(
        manageLiveNoteUseCase.getActiveLiveNote(),
        _inputText,
        _isEditorExpanded,
        _isPermissionGranted
    ) { activeNote, inputText, isEditorExpanded, isPermissionGranted ->
        LiveNoteUiState(
            activeNote = activeNote,
            inputText = inputText,
            isEditorExpanded = isEditorExpanded,
            isPermissionGranted = isPermissionGranted
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LiveNoteUiState()
    )

    fun openQuickNoteEditor(initialText: String = "") {
        _inputText.value = initialText
        _isEditorExpanded.value = true
    }

    fun updateInputText(newText: String) {
        if (newText.length <= 64) {
            _inputText.value = newText
        }
    }

    fun closeEditor() {
        _isEditorExpanded.value = false
        _inputText.value = ""
    }

    fun saveLiveNote() {
        val textToSave = _inputText.value.trim()
        if (textToSave.isBlank()) return

        viewModelScope.launch {
            val savedNote = manageLiveNoteUseCase.createLiveNote(textToSave)
            draftoNotificationManager.showOrUpdateNotification(savedNote)
            scheduleNotificationUseCase.scheduleHourlyUpdate()
            closeEditor()
        }
    }

    fun completeLiveNote() {
        viewModelScope.launch {
            manageLiveNoteUseCase.completeLiveNote()
            draftoNotificationManager.cancelNotification()
            scheduleNotificationUseCase.cancelScheduledUpdate()
        }
    }

    fun extendLiveNoteTime() {
        viewModelScope.launch {
            manageLiveNoteUseCase.extendLiveNoteTime()
            scheduleNotificationUseCase.scheduleHourlyUpdate()
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        _isPermissionGranted.value = isGranted
    }
}

