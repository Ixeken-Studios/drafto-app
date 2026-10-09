package com.ixeken.drafto.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ixeken.drafto.domain.model.LiveNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "live_note_preferences")

/**
 * Gestor de almacenamiento liviano respaldado por Jetpack DataStore Preferences.
 * Se implementa DataStore en lugar de SQLite/Room porque solo necesitamos guardar un único registro activo de forma atómica.
 */
@Singleton
class LiveNoteDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val ID = stringPreferencesKey("live_note_id")
        val TEXT = stringPreferencesKey("live_note_text")
        val EXPIRATION = longPreferencesKey("live_note_expiration")
        val IS_EXPIRED = booleanPreferencesKey("live_note_is_expired")
    }

    /**
     * Emite la nota activa persistida o null en caso de no contar con un texto guardado.
     */
    val liveNoteFlow: Flow<LiveNote?> = context.dataStore.data.map { preferences ->
        val text = preferences[PreferencesKeys.TEXT] ?: return@map null
        val id = preferences[PreferencesKeys.ID] ?: "live_note_active"
        val expiration = preferences[PreferencesKeys.EXPIRATION] ?: 0L
        val isExpired = preferences[PreferencesKeys.IS_EXPIRED] ?: false

        LiveNote(
            id = id,
            text = text,
            expirationTimestampMillis = expiration,
            isExpired = isExpired
        )
    }

    /**
     * Guarda atómicamente los datos de la nueva nota en DataStore.
     */
    suspend fun saveLiveNote(liveNote: LiveNote) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ID] = liveNote.id
            preferences[PreferencesKeys.TEXT] = liveNote.text
            preferences[PreferencesKeys.EXPIRATION] = liveNote.expirationTimestampMillis
            preferences[PreferencesKeys.IS_EXPIRED] = liveNote.isExpired
        }
    }

    /**
     * Actualiza solo el timestamp de expiración manteniendo el texto intacto.
     */
    suspend fun updateExpiration(newExpirationMillis: Long, isExpired: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.EXPIRATION] = newExpirationMillis
            preferences[PreferencesKeys.IS_EXPIRED] = isExpired
        }
    }

    /**
     * Limpia completamente todos los campos guardados en el DataStore.
     */
    suspend fun clearLiveNote() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

