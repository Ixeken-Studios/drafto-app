package com.ixeken.drafto.domain.usecase

import com.ixeken.drafto.domain.repository.NoteRepository

class DeleteNoteUseCase(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(id: String) {
        repository.deleteNote(id)
    }
}

