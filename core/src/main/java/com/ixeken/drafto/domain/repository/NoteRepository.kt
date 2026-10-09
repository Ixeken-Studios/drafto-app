package com.ixeken.drafto.domain.repository

import com.ixeken.drafto.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getAllNotes(): Flow<List<Note>>
    suspend fun getAllNotesSync(): List<Note>
    suspend fun getNoteById(id: String): Note?
    suspend fun saveNote(note: Note)
    suspend fun deleteNote(id: String)
    suspend fun saveAllNotes(notes: List<Note>)
    fun getNotesByCollection(collectionId: String): Flow<List<Note>>
    suspend fun getNotesByCollectionSync(collectionId: String): List<Note>
}

