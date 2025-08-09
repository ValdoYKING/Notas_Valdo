package com.valdo.notasinteligentesvaldo.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.valdo.notasinteligentesvaldo.models.Note
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Componente visual que muestra una tarjeta/resumen de una nota.
 *
 * - Muestra el título, contenido (con o sin Markdown), y la fecha de edición.
 * - Permite clic para navegar al detalle de la nota.
 *
 * @param note Nota a mostrar.
 * @param onNoteClick Acción al hacer clic en la tarjeta.
 */
@Composable
fun NoteCard(note: Note, onNoteClick: (Note) -> Unit) {
    val dateFormatter = remember {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    }

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable { onNoteClick(note) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(), // Eliminar .clickable aquí
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Mostrar título solo si existe
            if (!note.title.isNullOrEmpty() && note.title != "Nota sin título") {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Contenido con soporte Markdown condicional
            if (note.isMarkdownEnabled) {
                val isDark = isSystemInDarkTheme()
                MarkdownText(
                    markdown = note.content.takeIf { it.length <= 120 }
                        ?: "${note.content.take(120)}...",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            } else {
                Text(
                    text = note.content.takeIf { it.length <= 120 }
                        ?: "${note.content.take(120)}...",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Fecha de modificación
            Text(
                text = "Editado: ${dateFormatter.format(Date(note.timestamp))}",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                ),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}