package com.valdo.notasinteligentesvaldo.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.RewriteQueriesToDropUnusedColumns
import com.valdo.notasinteligentesvaldo.models.Category
import com.valdo.notasinteligentesvaldo.models.Note
import com.valdo.notasinteligentesvaldo.models.NoteCategoryCrossRef
import com.valdo.notasinteligentesvaldo.models.NoteWithCategories
import com.valdo.notasinteligentesvaldo.models.CategoryWithNotes
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    // --- NOTAS ---
    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("SELECT * FROM notes WHERE id = :noteId")
    fun getNoteById(noteId: Int): Flow<Note?>

    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE location IS NOT NULL ORDER BY timestamp DESC")
    fun getNotesWithLocation(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE title LIKE :query OR content LIKE :query ORDER BY timestamp DESC")
    suspend fun searchNotes(query: String): List<Note>

    @Query("SELECT * FROM notes WHERE notificationTime IS NOT NULL AND notificationTime > 0")
    suspend fun getNotesWithNotifications(): List<Note>

    @Query("UPDATE notes SET isFavorite = NOT isFavorite WHERE id = :noteId")
    suspend fun toggleFavorite(noteId: Int)

    @Query("UPDATE notes SET isMarkdownEnabled = :enabled WHERE id = :noteId")
    suspend fun setMarkdownEnabled(noteId: Int, enabled: Boolean)

    // --- CATEGORÍAS ---
    @Insert
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Transaction
    @Query("SELECT * FROM categories WHERE categoryId = :categoryId")
    fun getCategoryWithNotes(categoryId: Int): Flow<CategoryWithNotes>

    // --- RELACIÓN NOTA-CATEGORÍA ---
    @Insert
    suspend fun insertNoteCategoryCrossRef(crossRef: NoteCategoryCrossRef)

    @Delete
    suspend fun deleteNoteCategoryCrossRef(crossRef: NoteCategoryCrossRef)

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :noteId")
    fun getNoteWithCategories(noteId: Int): Flow<NoteWithCategories?>

    @Transaction
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotesWithCategories(): Flow<List<NoteWithCategories>>

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        """
        SELECT notes.*
        FROM notes
        INNER JOIN NoteCategoryCrossRef
            ON notes.id = NoteCategoryCrossRef.noteId
        WHERE NoteCategoryCrossRef.categoryId = :categoryId
        ORDER BY notes.timestamp DESC
        """
    )
    fun getNotesByCategoryId(categoryId: Int): Flow<List<NoteWithCategories>>

    // Eliminar todas las relaciones de una categoría (al eliminarla)
    @Query("DELETE FROM NoteCategoryCrossRef WHERE categoryId = :categoryId")
    suspend fun deleteAllNoteCategoryCrossRefsByCategory(categoryId: Int)

    // Eliminar todas las relaciones de una nota (al eliminarla)
    @Query("DELETE FROM NoteCategoryCrossRef WHERE noteId = :noteId")
    suspend fun deleteAllNoteCategoryCrossRefsByNote(noteId: Int)

    @Query("UPDATE notes SET notificationTime = NULL, isNotificationPersistent = 0 WHERE id = :noteId")
    suspend fun clearReminderState(noteId: Int)
}
