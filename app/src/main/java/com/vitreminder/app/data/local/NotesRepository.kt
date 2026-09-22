package com.vitreminder.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vitreminder.app.data.model.ClassNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.notesDataStore: DataStore<Preferences> by preferencesDataStore(name = "vit_notes_prefs")

class NotesRepository(private val context: Context) {
    private val gson = Gson()
    private val notesKey = stringPreferencesKey("notes_list_json")

    val notesFlow: Flow<List<ClassNote>> = context.notesDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val json = preferences[notesKey]
            if (json.isNullOrBlank()) {
                emptyList()
            } else {
                try {
                    val type = object : TypeToken<List<ClassNote>>() {}.type
                    gson.fromJson<List<ClassNote>>(json, type) ?: emptyList()
                } catch (e: Exception) {
                    android.util.Log.e("NotesRepository", "Failed to deserialize notes: ${e.message}", e)
                    emptyList()
                }
            }
        }

    suspend fun getAllNotes(): List<ClassNote> {
        return try {
            notesFlow.first()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addNote(note: ClassNote) {
        val current = getAllNotes().toMutableList()
        current.add(0, note) // Put newest at top
        saveNotesList(current)
    }

    suspend fun toggleNoteCompletion(noteId: String) {
        val current = getAllNotes().map {
            if (it.id == noteId) it.copy(isCompleted = !it.isCompleted) else it
        }
        saveNotesList(current)
    }

    suspend fun deleteNote(noteId: String) {
        val current = getAllNotes().filterNot { it.id == noteId }
        saveNotesList(current)
    }

    suspend fun clearCompleted() {
        val current = getAllNotes().filterNot { it.isCompleted }
        saveNotesList(current)
    }

    private suspend fun saveNotesList(notes: List<ClassNote>) {
        val json = gson.toJson(notes)
        context.notesDataStore.edit { preferences ->
            preferences[notesKey] = json
        }
    }
}
