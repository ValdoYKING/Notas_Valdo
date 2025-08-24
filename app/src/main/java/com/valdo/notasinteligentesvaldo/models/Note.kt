package com.valdo.notasinteligentesvaldo.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import androidx.room.Index

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,  // Título de la nota (obligatorio)
    val content: String,  // Contenido (soporta Markdown si está activo)
    var timestamp: Long = System.currentTimeMillis(),  // Última modificación
    var timestampInit: Long = System.currentTimeMillis(),  // Fecha creación (valor por defecto: ahora)
    val location: String? = null,  // Formato: "lat,long" o null si no aplica
    val notificationTime: Long? = null,  // Tiempo en minutos (null si no hay notificación)
    val isFavorite: Boolean = false,  // Favorito (default: false)
    val isMarkdownEnabled: Boolean = false  // Si soporta Markdown (default: false)
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val categoryId: Int = 0,
    val name: String
)

@Entity(
    primaryKeys = ["noteId", "categoryId"],
    indices = [Index(value = ["noteId"]), Index(value = ["categoryId"])]
)
data class NoteCategoryCrossRef(
    val noteId: Int,
    val categoryId: Int
)

// Relación Nota con Categorías
data class NoteWithCategories(
    @Embedded val note: Note,
    @Relation(
        parentColumn = "id",
        entityColumn = "categoryId",
        associateBy = Junction(
            value = NoteCategoryCrossRef::class,
            parentColumn = "noteId",
            entityColumn = "categoryId"
        )
    )
    val categories: List<Category>
)

data class CategoryWithNotes(
    @Embedded val category: Category,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id",
        associateBy = Junction(
            value = NoteCategoryCrossRef::class,
            parentColumn = "categoryId",
            entityColumn = "noteId"
        )
    )
    val notes: List<Note>
)
