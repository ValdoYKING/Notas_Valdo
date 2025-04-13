package com.valdo.notasinteligentesvaldo.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.valdo.notasinteligentesvaldo.models.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    // Operaciones básicas
    @Insert
    suspend fun insert(note: Note)

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    // Obtener notas (versiones Flow para observación reactiva)
    @Query("SELECT * FROM notes WHERE id = :noteId")
    fun getNoteById(noteId: Int): Flow<Note?>

    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE category = :category ORDER BY timestamp DESC")
    fun getNotesByCategory(category: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE location IS NOT NULL ORDER BY timestamp DESC")
    fun getNotesWithLocation(): Flow<List<Note>>

    // Consultas puntuales (sin Flow)
    @Query("SELECT DISTINCT category FROM notes WHERE category IS NOT NULL")
    suspend fun getAllCategories(): List<String>

    @Query("SELECT * FROM notes WHERE title LIKE :query OR content LIKE :query ORDER BY timestamp DESC")
    suspend fun searchNotes(query: String): List<Note>

    @Query("SELECT * FROM notes WHERE notificationTime IS NOT NULL AND notificationTime > 0")
    suspend fun getNotesWithNotifications(): List<Note>

    // Operaciones de modificación directa
    @Query("UPDATE notes SET isFavorite = NOT isFavorite WHERE id = :noteId")
    suspend fun toggleFavorite(noteId: Int)

    @Query("UPDATE notes SET isMarkdownEnabled = :enabled WHERE id = :noteId")
    suspend fun setMarkdownEnabled(noteId: Int, enabled: Boolean)
}