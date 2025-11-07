package com.valdo.notasinteligentesvaldo.components

import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.valdo.notasinteligentesvaldo.models.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import dev.jeziellago.compose.markdowntext.MarkdownText

/**
 * Tarjeta de nota optimizada para listas con soporte de Markdown (preview truncado).
 */
@Composable
fun NoteCard(
    note: Note,
    onNoteClick: (Note) -> Unit
) {
    // Precalcular formateador una vez por composición del item
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    // Precomputar valores derivados para evitar trabajo en recomposiciones
    val previewText = remember(note.content) {
        if (note.content.length <= 120) note.content else note.content.take(120) + "..."
    }
    val markdownPreview = remember(note.content) {
        if (note.content.length <= 200) note.content else note.content.take(200) + "..."
    }
    val editedAtText = remember(note.timestamp) {
        "Editado: ${dateFormatter.format(Date(note.timestamp))}"
    }

    // Debounce de clic para evitar aperturas múltiples
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val clickDebounceMs = 600L

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable {
                val now = SystemClock.uptimeMillis()
                if (now - lastClickTime > clickDebounceMs) {
                    lastClickTime = now
                    onNoteClick(note)
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (note.title.isNotEmpty() && note.title != "Nota sin título") {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (note.isMarkdownEnabled) {
                val isDark = isSystemInDarkTheme()
                MarkdownText(
                    markdown = markdownPreview,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            } else {
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = editedAtText,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                ),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
