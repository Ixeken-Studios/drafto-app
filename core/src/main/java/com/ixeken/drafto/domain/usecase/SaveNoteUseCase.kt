package com.ixeken.drafto.domain.usecase

import com.ixeken.drafto.domain.model.Note
import com.ixeken.drafto.domain.repository.NoteRepository

class SaveNoteUseCase(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: Note) {
        repository.saveNote(note)
    }
}

