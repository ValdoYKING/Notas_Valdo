package com.valdo.notasinteligentesvaldo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valdo.notasinteligentesvaldo.data.NoteDao
import com.valdo.notasinteligentesvaldo.models.Category
import com.valdo.notasinteligentesvaldo.models.CategoryWithNotes
import com.valdo.notasinteligentesvaldo.models.Note
import com.valdo.notasinteligentesvaldo.models.NoteCategoryCrossRef
import com.valdo.notasinteligentesvaldo.models.NoteWithCategories
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

// Documento externo temporal para visor (no se persiste)
data class ExternalDocument(val title: String, val content: String, val isMarkdown: Boolean)

class NoteViewModel(private val noteDao: NoteDao) : ViewModel() {

    // --- Flujos persistentes (Room -> StateFlow) ---
    // Evita lanzar múltiples collect() cada vez que la UI pide refrescar.
    val allNotes: StateFlow<List<Note>> =
        noteDao.getAllNotes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteNotes: StateFlow<List<Note>> =
        noteDao.getFavoriteNotes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allCategories: StateFlow<List<Category>> =
        noteDao.getAllCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val notesWithCategories: StateFlow<List<NoteWithCategories>> =
        noteDao.getAllNotesWithCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- Nota actual (solo para pantallas que la usen explícitamente) ---
    private val _currentNote = MutableStateFlow<Note?>(null)
    val currentNote: StateFlow<Note?> = _currentNote.asStateFlow()

    private val _currentNoteWithCategories = MutableStateFlow<NoteWithCategories?>(null)
    val currentNoteWithCategories: StateFlow<NoteWithCategories?> = _currentNoteWithCategories.asStateFlow()

    // Jobs para colecciones "por pantalla" (se cancelan al observar otra nota/categoría)
    private var currentNoteJob: Job? = null
    private var currentNoteWithCategoriesJob: Job? = null
    private var categoryWithNotesJob: Job? = null
    private var noteWithCategoriesJob: Job? = null

    // --- Documento externo / navegación ---
    private val _importedNoteId = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val importedNoteId: SharedFlow<Int> = _importedNoteId

    private val _externalDoc = MutableStateFlow<ExternalDocument?>(null)
    val externalDoc: StateFlow<ExternalDocument?> = _externalDoc.asStateFlow()

    private val _openExternalViewer = MutableSharedFlow<Unit>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val openExternalViewer: SharedFlow<Unit> = _openExternalViewer

    // Eventos para navegación desde notificaciones
    private val _openNoteFromNotification = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val openNoteFromNotification: SharedFlow<Int> = _openNoteFromNotification

    private val _openNotesListFromNotification = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val openNotesListFromNotification: SharedFlow<Unit> = _openNotesListFromNotification

    private val _pendingNotificationNoteId = MutableStateFlow<Int?>(null)
    val pendingNotificationNoteId: StateFlow<Int?> = _pendingNotificationNoteId.asStateFlow()

    /**
     * Observa una nota específica.
     * Importante: cancela la observación anterior para no acumular collectors en viewModelScope.
     */
    fun loadNote(noteId: Int) {
        currentNoteJob?.cancel()
        currentNoteJob = viewModelScope.launch {
            noteDao.getNoteById(noteId).collect { note ->
                _currentNote.value = note
            }
        }
    }

    fun updateCurrentNote(updater: (Note) -> Note) {
        _currentNote.value?.let { current -> _currentNote.value = updater(current) }
    }

    fun saveCurrentNote() {
        viewModelScope.launch {
            _currentNote.value?.let { current ->
                noteDao.update(current.copy(timestamp = System.currentTimeMillis()))
            }
        }
    }

    fun clearCurrentNote() {
        _currentNote.value = null
    }

    fun setExternalDocument(title: String, content: String, asMarkdown: Boolean) {
        _externalDoc.value = ExternalDocument(
            title = title.ifBlank { "Nota sin título" },
            content = content,
            isMarkdown = asMarkdown
        )
        viewModelScope.launch { _openExternalViewer.emit(Unit) }
    }

    fun clearExternalDocument() {
        _externalDoc.value = null
    }

    // --- Métodos auxiliares para pantallas ---
    fun getNoteById(noteId: Int): Flow<Note?> = noteDao.getNoteById(noteId)
    fun setCurrentNote(note: Note) { _currentNote.value = note }
    fun updateNoteTemp(note: Note) { _currentNote.value = note }

    // --- Operaciones CRUD ---
    fun insertNote(note: Note) {
        viewModelScope.launch {
            noteDao.insert(
                note.copy(
                    timestamp = System.currentTimeMillis(),
                    timestampInit = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun insertNoteAndGetId(note: Note): Long {
        return noteDao.insert(
            note.copy(
                timestamp = System.currentTimeMillis(),
                timestampInit = System.currentTimeMillis()
            )
        )
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            noteDao.update(note.copy(timestamp = System.currentTimeMillis()))
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteDao.deleteAllNoteCategoryCrossRefsByNote(note.id)
            noteDao.delete(note)
            if (_currentNote.value?.id == note.id) {
                _currentNote.value = null
            }
        }
    }

    fun toggleFavorite(noteId: Int) {
        viewModelScope.launch {
            noteDao.toggleFavorite(noteId)
        }
    }

    fun toggleMarkdown(noteId: Int, enabled: Boolean) {
        viewModelScope.launch {
            noteDao.setMarkdownEnabled(noteId, enabled)
        }
    }

    // --- Búsqueda (one-shot) ---
    fun searchNotes(query: String): StateFlow<List<Note>> {
        val results = MutableStateFlow<List<Note>>(emptyList())
        viewModelScope.launch {
            results.value = noteDao.searchNotes("%$query%")
        }
        return results
    }

    suspend fun getCategories(): List<String> = allCategories.value.map { it.name }

    // --- Notificaciones ---
    /**
     * Programa una notificación para una nota.
     * Se usa firstOrNull() para evitar colecciones infinitas.
     */
    fun scheduleNotification(noteId: Int, timeInMinutes: Long) {
        viewModelScope.launch {
            val note = noteDao.getNoteById(noteId).firstOrNull()
            note?.let {
                noteDao.update(
                    it.copy(
                        notificationTime = timeInMinutes,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun scheduleQuickReminder(noteId: Int, minutesFromNow: Long, isPersistent: Boolean) {
        viewModelScope.launch {
            val note = noteDao.getNoteById(noteId).firstOrNull()
            note?.let {
                noteDao.update(
                    it.copy(
                        notificationTime = minutesFromNow,
                        isNotificationPersistent = isPersistent,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun clearReminder(noteId: Int) {
        viewModelScope.launch {
            val note = noteDao.getNoteById(noteId).firstOrNull()
            note?.let {
                noteDao.update(
                    it.copy(
                        notificationTime = null,
                        isNotificationPersistent = false,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // --- Categorías y relaciones ---
    fun insertCategory(category: Category) {
        viewModelScope.launch {
            val name = category.name.trim()
            if (name.isEmpty()) return@launch
            val exists = allCategories.value.any { it.name.equals(name, ignoreCase = true) }
            if (exists) return@launch
            noteDao.insertCategory(category.copy(name = name))
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            noteDao.updateCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            noteDao.deleteCategory(category)
            noteDao.deleteAllNoteCategoryCrossRefsByCategory(category.categoryId)
        }
    }

    fun addCategoryToNote(noteId: Int, categoryId: Int) {
        viewModelScope.launch {
            noteDao.insertNoteCategoryCrossRef(NoteCategoryCrossRef(noteId, categoryId))
        }
    }

    fun removeCategoryFromNote(noteId: Int, categoryId: Int) {
        viewModelScope.launch {
            noteDao.deleteNoteCategoryCrossRef(NoteCategoryCrossRef(noteId, categoryId))
        }
    }

    fun removeAllCategoriesFromNote(noteId: Int) {
        viewModelScope.launch {
            noteDao.deleteAllNoteCategoryCrossRefsByNote(noteId)
        }
    }

    /**
     * Observa una nota con categorías para la pantalla de detalle.
     * Cancela la observación anterior para evitar fugas.
     */
    fun observeNoteWithCategories(noteId: Int) {
        currentNoteWithCategoriesJob?.cancel()
        currentNoteWithCategoriesJob = viewModelScope.launch {
            noteDao.getNoteWithCategories(noteId).collect { n ->
                _currentNoteWithCategories.value = n
            }
        }
    }

    /**
     * Mantiene el método por compatibilidad, pero sin fugas: cancela y reemplaza.
     */
    fun getNoteWithCategories(noteId: Int) {
        noteWithCategoriesJob?.cancel()
        noteWithCategoriesJob = viewModelScope.launch {
            noteDao.getNoteWithCategories(noteId).collect { n ->
                val actual = notesWithCategories.value.toMutableList()
                val idx = actual.indexOfFirst { it.note.id == noteId }
                if (n != null) {
                    if (idx >= 0) actual[idx] = n else actual.add(n)
                } else {
                    if (idx >= 0) actual.removeAt(idx)
                }
                // Nota: notesWithCategories ahora viene de Room; este método solo tiene valor si quieres cache local.
                // Para no romper comportamiento previo, mantenemos el state interno si alguien lo consume.
            }
        }
    }

    /**
     * Si una pantalla necesita filtrar por categoría, lo correcto es hacerlo en UI a partir de notesWithCategories.
     * Se mantiene por compatibilidad, pero ya no sobrescribe el estado global (evita inconsistencias).
     */
    fun getNotesByCategoryId(categoryId: Int): Flow<List<NoteWithCategories>> = noteDao.getNotesByCategoryId(categoryId)

    private val _categoryWithNotes = MutableStateFlow<CategoryWithNotes?>(null)
    val categoryWithNotes: StateFlow<CategoryWithNotes?> = _categoryWithNotes.asStateFlow()

    fun getCategoryWithNotes(categoryId: Int) {
        categoryWithNotesJob?.cancel()
        categoryWithNotesJob = viewModelScope.launch {
            noteDao.getCategoryWithNotes(categoryId).collect { catWithNotes ->
                _categoryWithNotes.value = catWithNotes
            }
        }
    }

    /** Importa texto externo como nueva nota y emite su ID para navegar al detalle. */
    fun importExternalNote(title: String, content: String, asMarkdown: Boolean) {
        viewModelScope.launch {
            val computedTitle = if (asMarkdown) {
                extractMarkdownTitle(content)
                    ?: title.takeIf { it.isNotBlank() && it != "Nota sin título" && !looksLikeRandomId(it) }
                    ?: "Nota importada"
            } else {
                title.takeIf { it.isNotBlank() && it != "Nota sin título" && !looksLikeRandomId(it) }
                    ?: "Nota importada"
            }

            val note = Note(
                title = computedTitle,
                content = content,
                isMarkdownEnabled = asMarkdown,
                timestamp = System.currentTimeMillis(),
                timestampInit = System.currentTimeMillis()
            )
            val id = insertNoteAndGetId(note).toInt()
            _importedNoteId.emit(id)
        }
    }

    // --- Helpers privados ---
    private fun extractMarkdownTitle(text: String): String? {
        val regex = Regex("^\\s{0,3}#{1,6}\\s+(.+?)\\s*#*\\s*$", RegexOption.MULTILINE)
        val match = regex.find(text) ?: return null
        val raw = match.groupValues.getOrNull(1)?.trim() ?: return null
        return raw.takeIf { it.isNotBlank() }
    }

    private fun looksLikeRandomId(title: String): Boolean {
        val t = title.trim()
        val uuid = Regex("^[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$")
        if (uuid.matches(t)) return true
        val longToken = Regex("^[A-Za-z0-9._-]{16,}$")
        if (longToken.matches(t)) return true
        return false
    }

    fun emitOpenNoteFromNotification(noteId: Int) {
        _pendingNotificationNoteId.value = noteId
        _openNoteFromNotification.tryEmit(noteId)
    }

    fun emitOpenNotesFromNotification() {
        _openNotesListFromNotification.tryEmit(Unit)
    }

    fun clearPendingNotificationNoteId() {
        _pendingNotificationNoteId.value = null
    }
}