package com.valdo.notasinteligentesvaldo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valdo.notasinteligentesvaldo.data.NoteDao
import com.valdo.notasinteligentesvaldo.models.Note
import com.valdo.notasinteligentesvaldo.models.Category
import com.valdo.notasinteligentesvaldo.models.NoteCategoryCrossRef
import com.valdo.notasinteligentesvaldo.models.NoteWithCategories
import com.valdo.notasinteligentesvaldo.models.CategoryWithNotes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.channels.BufferOverflow

// Documento externo temporal para visor (no se persiste)
data class ExternalDocument(val title: String, val content: String, val isMarkdown: Boolean)

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

    // --- Categorías y relaciones ---
    private val _allCategories = MutableStateFlow<List<Category>>(emptyList())
    val allCategories: StateFlow<List<Category>> = _allCategories

    private val _notesWithCategories = MutableStateFlow<List<NoteWithCategories>>(emptyList())
    val notesWithCategories: StateFlow<List<NoteWithCategories>> = _notesWithCategories

    // Añadido: estado para una categoría con sus notas (para usos específicos)
    private val _categoryWithNotes = MutableStateFlow<CategoryWithNotes?>(null)
    val categoryWithNotes: StateFlow<CategoryWithNotes?> = _categoryWithNotes.asStateFlow()

    // NUEVO: flujo dedicado para la nota actual con categorías (para la pantalla de detalle)
    private val _currentNoteWithCategories = MutableStateFlow<NoteWithCategories?>(null)
    val currentNoteWithCategories: StateFlow<NoteWithCategories?> = _currentNoteWithCategories.asStateFlow()

    // Evento: ID de nota importada externamente para navegación
    private val _importedNoteId = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val importedNoteId: SharedFlow<Int> = _importedNoteId

    // NUEVO: documento externo para visor efímero y evento de apertura
    private val _externalDoc = MutableStateFlow<ExternalDocument?>(null)
    val externalDoc: StateFlow<ExternalDocument?> = _externalDoc.asStateFlow()
    private val _openExternalViewer = MutableSharedFlow<Unit>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val openExternalViewer: SharedFlow<Unit> = _openExternalViewer

    // --- Inicialización: carga notas y favoritas al crear el ViewModel ---
    init {
        loadAllNotes()
        loadFavorites()
        loadAllCategories()
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

    /** Limpia el estado de la nota actual (para evitar que se muestre en la creación de una nueva nota). */
    fun clearCurrentNote() {
        _currentNote.value = null
    }

    // NUEVO: set/clear para documento externo
    fun setExternalDocument(title: String, content: String, asMarkdown: Boolean) {
        _externalDoc.value = ExternalDocument(
            title = title.ifBlank { "Nota sin título" },
            content = content,
            isMarkdown = asMarkdown
        )
        viewModelScope.launch { _openExternalViewer.emit(Unit) }
    }

    fun clearExternalDocument() { _externalDoc.value = null }

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

    /** Inserta una nueva nota y devuelve su ID. */
    suspend fun insertNoteAndGetId(note: Note): Long {
        return noteDao.insert(note.copy(
            timestamp = System.currentTimeMillis(),
            timestampInit = System.currentTimeMillis()
        ))
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
            // Primero limpia todas las relaciones para evitar estados inconsistentes
            noteDao.deleteAllNoteCategoryCrossRefsByNote(note.id)
            // Luego elimina la nota
            noteDao.delete(note)
            // Refresca estados
            loadAllNotes()
            loadFavorites()
            // Sincroniza la lista de notas con categorías (funciona tanto para vistas filtradas como no filtradas)
            _notesWithCategories.value = _notesWithCategories.value.filter { it.note.id != note.id }
            // Si la nota eliminada era la actual, límpiala
            if (_currentNote.value?.id == note.id) {
                _currentNote.value = null
            }
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

    /** Carga todas las categorías y las expone como estado observable. */
    fun loadAllCategories() {
        viewModelScope.launch {
            noteDao.getAllCategories().collect { cats ->
                _allCategories.value = cats
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
    suspend fun getCategories(): List<String> = _allCategories.value.map { it.name }

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

    // --- Categorías y relaciones ---
    /** Inserta una nueva categoría y actualiza la lista. */
    fun insertCategory(category: Category) {
        viewModelScope.launch {
            val name = category.name.trim()
            if (name.isEmpty()) return@launch
            val exists = _allCategories.value.any { it.name.equals(name, ignoreCase = true) }
            if (exists) return@launch
            noteDao.insertCategory(category.copy(name = name))
            loadAllCategories()
        }
    }

    /** Actualiza una categoría existente y refresca listas. */
    fun updateCategory(category: Category) {
        viewModelScope.launch {
            noteDao.updateCategory(category)
            loadAllCategories()
        }
    }

    /** Elimina una categoría y actualiza listas. */
    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            noteDao.deleteCategory(category)
            noteDao.deleteAllNoteCategoryCrossRefsByCategory(category.categoryId)
            loadAllCategories()
        }
    }

    /** Asocia una categoría a una nota. */
    fun addCategoryToNote(noteId: Int, categoryId: Int) {
        viewModelScope.launch {
            noteDao.insertNoteCategoryCrossRef(NoteCategoryCrossRef(noteId, categoryId))
        }
    }

    /** Desasocia una categoría de una nota. */
    fun removeCategoryFromNote(noteId: Int, categoryId: Int) {
        viewModelScope.launch {
            noteDao.deleteNoteCategoryCrossRef(NoteCategoryCrossRef(noteId, categoryId))
        }
    }

    /** Elimina todas las relaciones de categorías de una nota. */
    fun removeAllCategoriesFromNote(noteId: Int) {
        viewModelScope.launch {
            noteDao.deleteAllNoteCategoryCrossRefsByNote(noteId)
        }
    }

    /** Obtiene una nota con sus categorías y actualiza el StateFlow para la pantalla de detalle (sin tocar la lista global). */
    fun observeNoteWithCategories(noteId: Int) {
        viewModelScope.launch {
            noteDao.getNoteWithCategories(noteId).collect { n ->
                _currentNoteWithCategories.value = n
            }
        }
    }

    /** Obtiene una nota con sus categorías y actualiza la lista global (para pantallas de listas). */
    fun getNoteWithCategories(noteId: Int) {
        viewModelScope.launch {
            noteDao.getNoteWithCategories(noteId).collect { n ->
                // Actualiza la lista de notas con categorías, reemplazando o agregando la nota actual
                val actual = _notesWithCategories.value.toMutableList()
                val idx = actual.indexOfFirst { it.note.id == noteId }
                if (n != null) {
                    if (idx >= 0) actual[idx] = n else actual.add(n)
                } else {
                    if (idx >= 0) actual.removeAt(idx)
                }
                _notesWithCategories.value = actual
            }
        }
    }

    /** Obtiene notas por ID de categoría. */
    fun getNotesByCategoryId(categoryId: Int) {
        viewModelScope.launch {
            noteDao.getNotesByCategoryId(categoryId).collect { notes ->
                _notesWithCategories.value = notes
            }
        }
    }

    /** Obtiene una categoría con sus notas. */
    fun getCategoryWithNotes(categoryId: Int) {
        viewModelScope.launch {
            noteDao.getCategoryWithNotes(categoryId).collect { catWithNotes ->
                _categoryWithNotes.value = catWithNotes
            }
        }
    }

    /** Importa texto externo como nueva nota y emite su ID para navegar al detalle. */
    fun importExternalNote(title: String, content: String, asMarkdown: Boolean) {
        viewModelScope.launch {
            val computedTitle = if (asMarkdown) {
                // Para Markdown: usa el primer encabezado (# ...) como título si existe
                extractMarkdownTitle(content)
                    ?: title.takeIf { it.isNotBlank() && it != "Nota sin título" && !looksLikeRandomId(it) }
                    ?: "Nota importada"
            } else {
                // Para texto plano: conserva el título previo; si es inválido, usa "Nota importada"
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
            loadAllNotes()
            loadFavorites()
        }
    }

    // --- Helpers privados ---
    /** Extrae el primer encabezado Markdown (#, ##, ..., ######) como posible título. */
    private fun extractMarkdownTitle(text: String): String? {
        val regex = Regex("^\\s{0,3}#{1,6}\\s+(.+?)\\s*#*\\s*$", RegexOption.MULTILINE)
        val match = regex.find(text) ?: return null
        val raw = match.groupValues.getOrNull(1)?.trim() ?: return null
        return raw.takeIf { it.isNotBlank() }
    }

    /** Detecta títulos con pinta de ID aleatorio/UUID o nombres poco descriptivos. */
    private fun looksLikeRandomId(title: String): Boolean {
        val t = title.trim()
        // UUID estándar
        val uuid = Regex("^[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$")
        if (uuid.matches(t)) return true
        // Cadenas largas sin espacios con caracteres alfanuméricos/guiones/guión bajo (típico de nombres ofuscados)
        val longToken = Regex("^[A-Za-z0-9._-]{16,}$")
        if (longToken.matches(t)) return true
        return false
    }
}