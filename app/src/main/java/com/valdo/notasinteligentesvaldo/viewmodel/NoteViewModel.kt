package com.valdo.notasinteligentesvaldo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valdo.notasinteligentesvaldo.data.NoteDao
import com.valdo.notasinteligentesvaldo.models.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NoteViewModel(private val noteDao: NoteDao) : ViewModel() {

    // Estados observables
    private val _allNotes = MutableStateFlow<List<Note>>(emptyList())
    val allNotes: StateFlow<List<Note>> = _allNotes

    private val _favoriteNotes = MutableStateFlow<List<Note>>(emptyList())
    val favoriteNotes: StateFlow<List<Note>> = _favoriteNotes

    private val _currentNote = MutableStateFlow<Note?>(null)
    val currentNote: StateFlow<Note?> = _currentNote.asStateFlow()

    // Carga inicial
    init {
        loadAllNotes()
        loadFavorites()
    }

    // Carga y sincronización de la nota actual
    fun loadNote(noteId: Int) {
        viewModelScope.launch {
            noteDao.getNoteById(noteId).collect { note ->
                _currentNote.value = note
            }
        }
    }

    // Actualización temporal optimizada
    fun updateCurrentNote(updater: (Note) -> Note) {
        _currentNote.value?.let { current ->
            _currentNote.value = updater(current)
        }
    }

    // Guardado persistente mejorado
    fun saveCurrentNote() {
        viewModelScope.launch {
            _currentNote.value?.let { current ->
                val updatedNote = current.copy(timestamp = System.currentTimeMillis())
                noteDao.update(updatedNote)
                // Actualiza todos los estados relacionados
                loadAllNotes()
                loadFavorites()
            }
        }
    }


    // Métodos para NoteDetailScreen
    fun getNoteById(noteId: Int): Flow<Note?> {
        return noteDao.getNoteById(noteId)
    }

    fun setCurrentNote(note: Note) {
        _currentNote.value = note
    }

    fun updateNoteTemp(note: Note) {
        _currentNote.value = note
    }


    // Operaciones CRUD
    fun insertNote(note: Note) {
        viewModelScope.launch {
            noteDao.insert(note.copy(
                timestamp = System.currentTimeMillis(),
                timestampInit = System.currentTimeMillis()
            ))
            loadAllNotes()
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            noteDao.update(note.copy(timestamp = System.currentTimeMillis()))
            loadAllNotes()
            loadFavorites()
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteDao.delete(note)
            loadAllNotes()
            loadFavorites()
        }
    }

    // Operaciones especiales
    fun toggleFavorite(noteId: Int) {
        viewModelScope.launch {
            noteDao.toggleFavorite(noteId)
            loadAllNotes()
            loadFavorites()
        }
    }

    fun toggleMarkdown(noteId: Int, enabled: Boolean) {
        viewModelScope.launch {
            noteDao.setMarkdownEnabled(noteId, enabled)
            loadAllNotes()
        }
    }

    // Carga de datos
    fun loadAllNotes() {
        viewModelScope.launch {
            noteDao.getAllNotes().collect { notes ->
                _allNotes.value = notes
            }
        }
    }

    fun loadFavorites() {
        viewModelScope.launch {
            noteDao.getFavoriteNotes().collect { favorites ->
                _favoriteNotes.value = favorites
            }
        }
    }

    // Búsqueda
    fun searchNotes(query: String): StateFlow<List<Note>> {
        val results = MutableStateFlow<List<Note>>(emptyList())
        viewModelScope.launch {
            results.value = noteDao.searchNotes("%$query%")
        }
        return results
    }

    // Categorías
    suspend fun getCategories(): List<String> {
        return noteDao.getAllCategories()
    }

    // Notificaciones
    fun scheduleNotification(noteId: Int, timeInMinutes: Long) {
        viewModelScope.launch {
            noteDao.getNoteById(noteId).collect { note ->
                note?.let {
                    noteDao.update(it.copy(
                        notificationTime = timeInMinutes,
                        timestamp = System.currentTimeMillis()
                    ))
                }
            }
        }
    }
}