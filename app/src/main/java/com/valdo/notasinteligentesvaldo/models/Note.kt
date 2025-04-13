package com.valdo.notasinteligentesvaldo.models

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val category: String? = null,  // Categoría (opcional)
    val isMarkdownEnabled: Boolean = false  // Si soporta Markdown (default: false)
)