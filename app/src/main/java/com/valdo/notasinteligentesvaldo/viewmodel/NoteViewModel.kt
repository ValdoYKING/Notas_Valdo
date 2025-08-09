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

/**
 * ViewModel principal para la gestión de notas.
 *
 * - Expone estados observables para la UI (todas las notas, favoritas, nota actual).
 * - Maneja operaciones CRUD y sincronización con Room.
 * - Permite búsquedas, favoritos, markdown y notificaciones.
 *
 * @param noteDao DAO de acceso a datos de Room.
 */
class NoteViewModel(private val noteDao: NoteDao) : ViewModel() {

    // --- Estados observables para la UI ---
    private val _allNotes = MutableStateFlow<List<Note>>(emptyList())
    val allNotes: StateFlow<List<Note>> = _allNotes

    private val _favoriteNotes = MutableStateFlow<List<Note>>(emptyList())
    val favoriteNotes: StateFlow<List<Note>> = _favoriteNotes

    private val _currentNote = MutableStateFlow<Note?>(null)
    val currentNote: StateFlow<Note?> = _currentNote.asStateFlow()

    // --- Inicialización: carga notas y favoritas al crear el ViewModel ---
    init {
        loadAllNotes()
        loadFavorites()
    }

    /**
     * Carga una nota específica y la expone como estado observable.
     * @param noteId ID de la nota a cargar.
     */
    fun loadNote(noteId: Int) {
        viewModelScope.launch {
            noteDao.getNoteById(noteId).collect { note ->
                _currentNote.value = note
            }
        }
    }

    /**
     * Actualiza temporalmente la nota actual en memoria (no persiste en base de datos).
     * Útil para edición en tiempo real.
     */
    fun updateCurrentNote(updater: (Note) -> Note) {
        _currentNote.value?.let { current ->
            _currentNote.value = updater(current)
        }
    }

    /**
     * Guarda la nota actual en la base de datos (actualiza timestamp).
     * Sincroniza los estados de la UI tras guardar.
     */
    fun saveCurrentNote() {
        viewModelScope.launch {
            _currentNote.value?.let { current ->
                val updatedNote = current.copy(timestamp = System.currentTimeMillis())
                noteDao.update(updatedNote)
                // Refresca listas tras guardar
                loadAllNotes()
                loadFavorites()
            }
        }
    }

    // --- Métodos auxiliares para pantallas ---
    fun getNoteById(noteId: Int): Flow<Note?> = noteDao.getNoteById(noteId)
    fun setCurrentNote(note: Note) { _currentNote.value = note }
    fun updateNoteTemp(note: Note) { _currentNote.value = note }

    // --- Operaciones CRUD ---
    /** Inserta una nueva nota y actualiza la lista. */
    fun insertNote(note: Note) {
        viewModelScope.launch {
            noteDao.insert(note.copy(
                timestamp = System.currentTimeMillis(),
                timestampInit = System.currentTimeMillis()
            ))
            loadAllNotes()
        }
    }

    /** Actualiza una nota existente y refresca listas. */
    fun updateNote(note: Note) {
        viewModelScope.launch {
            noteDao.update(note.copy(timestamp = System.currentTimeMillis()))
            loadAllNotes()
            loadFavorites()
        }
    }

    /** Elimina una nota y actualiza listas. */
    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteDao.delete(note)
            loadAllNotes()
            loadFavorites()
        }
    }

    // --- Operaciones especiales ---
    /** Cambia el estado de favorito de una nota. */
    fun toggleFavorite(noteId: Int) {
        viewModelScope.launch {
            noteDao.toggleFavorite(noteId)
            loadAllNotes()
            loadFavorites()
        }
    }

    /** Activa o desactiva el soporte Markdown en una nota. */
    fun toggleMarkdown(noteId: Int, enabled: Boolean) {
        viewModelScope.launch {
            noteDao.setMarkdownEnabled(noteId, enabled)
            loadAllNotes()
        }
    }

    // --- Carga de datos reactiva ---
    /** Carga todas las notas y las expone como estado observable. */
    fun loadAllNotes() {
        viewModelScope.launch {
            noteDao.getAllNotes().collect { notes ->
                _allNotes.value = notes
            }
        }
    }

    /** Carga las notas favoritas y las expone como estado observable. */
    fun loadFavorites() {
        viewModelScope.launch {
            noteDao.getFavoriteNotes().collect { favorites ->
                _favoriteNotes.value = favorites
            }
        }
    }

    // --- Búsqueda reactiva ---
    /**
     * Busca notas por texto en título o contenido.
     * @param query Texto a buscar (usa LIKE en SQL).
     * @return StateFlow con los resultados.
     */
    fun searchNotes(query: String): StateFlow<List<Note>> {
        val results = MutableStateFlow<List<Note>>(emptyList())
        viewModelScope.launch {
            results.value = noteDao.searchNotes("%$query%")
        }
        return results
    }

    // --- Categorías ---
    /** Obtiene todas las categorías distintas usadas en notas. */
    suspend fun getCategories(): List<String> = noteDao.getAllCategories()

    // --- Notificaciones ---
    /**
     * Programa una notificación para una nota (actualiza notificationTime).
     * @param noteId ID de la nota.
     * @param timeInMinutes Minutos para la notificación.
     */
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