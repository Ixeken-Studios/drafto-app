package com.ixeken.drafto.data.repository

import com.ixeken.drafto.data.local.dao.NoteDao
import com.ixeken.drafto.data.mapper.toDomain
import com.ixeken.drafto.data.mapper.toEntity
import com.ixeken.drafto.domain.model.Note
import com.ixeken.drafto.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepositoryImpl(
    private val noteDao: NoteDao
) : NoteRepository {

    override fun getAllNotes(): Flow<List<Note>> {
        return noteDao.getAllNotes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAllNotesSync(): List<Note> {
        return noteDao.getAllNotesSync().map { it.toDomain() }
    }

    override suspend fun getNoteById(id: String): Note? {
        return noteDao.getNoteById(id)?.toDomain()
    }

    override suspend fun saveNote(note: Note) {
        noteDao.insertNote(note.toEntity())
    }

    override suspend fun deleteNote(id: String) {
        noteDao.deleteNoteById(id)
    }

    override suspend fun saveAllNotes(notes: List<Note>) {
        noteDao.insertNotes(notes.map { it.toEntity() })
    }

    override fun getNotesByCollection(collectionId: String): Flow<List<Note>> {
        return noteDao.getNotesByCollection(collectionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getNotesByCollectionSync(collectionId: String): List<Note> {
        return noteDao.getNotesByCollectionSync(collectionId).map { it.toDomain() }
    }
}

