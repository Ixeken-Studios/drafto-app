package com.ixeken.drafto.domain.usecase

import com.ixeken.drafto.domain.model.Note
import com.ixeken.drafto.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow

class GetNotesUseCase(
    private val repository: NoteRepository
) {
    operator fun invoke(): Flow<List<Note>> {
        return repository.getAllNotes()
    }
}

