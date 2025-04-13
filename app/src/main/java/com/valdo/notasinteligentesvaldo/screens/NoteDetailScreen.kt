package com.valdo.notasinteligentesvaldo.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valdo.notasinteligentesvaldo.models.Note
import com.valdo.notasinteligentesvaldo.viewmodel.NoteViewModel
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun NoteDetailScreen(
    noteId: Int,
    onBack: () -> Unit,
    viewModel: NoteViewModel = viewModel()
) {
    val note by viewModel.currentNote.collectAsState()
    var isEditMode by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()
    val currentDate = remember {
        SimpleDateFormat("EEEE, d 'de' MMMM", Locale.getDefault()).format(Date())
    }

    // Formateador de fecha
    val dateFormatter = remember {
        SimpleDateFormat("EEEE, d 'de' MMMM 'a las' HH:mm", Locale.getDefault())
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    // Carga inicial de la nota
    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
    }

    // Manejo del teclado y foco - CORRECCIÓN IMPORTANTE
    LaunchedEffect(isEditMode) {
        if (isEditMode) {
            delay(300) // Pequeño delay para permitir la composición
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
            } catch (e: Exception) {
                // Maneja cualquier error de focus
                keyboardController?.show()
            }
        } else {
            keyboardController?.hide()
        }
    }

    if (note == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                //title = { Text(if (isEditMode) "Editando nota" else dateFormatter.format(Date(note!!.timestamp))) },
                title = {
                    if (isEditMode) {
                        Text("Editando nota")
                    } else {
                        Text(
                            text = "Hoy es $currentDate",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    // Botón para cambiar entre Markdown/Texto normal
                    // Botón de eliminar (visible siempre)
                    // Botón de eliminar
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar nota",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    if (isEditMode) {
                        IconButton(
                            onClick = {
                                viewModel.updateCurrentNote { current ->
                                    current.copy(isMarkdownEnabled = !current.isMarkdownEnabled)
                                }
                            }
                        ) {
                            Icon(
                                if (note!!.isMarkdownEnabled) Icons.Default.Star else Icons.Default.Clear,
                                contentDescription = "Formato",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Botón de edición/guardado
                    IconButton(
                        onClick = {
                            if (isEditMode) {
                                viewModel.saveCurrentNote()
                            }
                            isEditMode = !isEditMode
                        }
                    ) {
                        Icon(
                            if (isEditMode) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = if (isEditMode) "Guardar" else "Editar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isEditMode) {
                // MODO EDICIÓN
                BasicTextField(
                    value = note?.title ?: "",
                    onValueChange = { newTitle ->
                        viewModel.updateCurrentNote { it.copy(title = newTitle) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    decorationBox = { innerTextField ->
                        Box {
                            if (note?.title.isNullOrEmpty()) {
                                Text(
                                    "Título (opcional)",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Divider(modifier = Modifier.padding(horizontal = 16.dp))

                // Editor de contenido (Markdown o texto normal)
                if (note!!.isMarkdownEnabled) {
                    MarkdownEditor(
                        content = note?.content ?: "",
                        onContentChange = { newContent ->
                            viewModel.updateCurrentNote { it.copy(content = newContent) }
                        }
                    )
                } else {
                    BasicTextField(
                        value = note?.content ?: "",
                        onValueChange = { newContent ->
                            viewModel.updateCurrentNote { it.copy(content = newContent) }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .focusRequester(focusRequester),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            } else {
                // MODO LECTURA
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState)
                ) {
                    note?.let { currentNote ->
                        if (currentNote.title.isNotEmpty() && currentNote.title != "Nota sin título") {
                            Text(
                                text = currentNote.title,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        if (currentNote.isMarkdownEnabled) {
                            MarkdownText(
                                markdown = currentNote.content,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        } else {
                            Text(
                                text = currentNote.content,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = 28.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Pie de nota con metadatos
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Creada: ${dateFormatter.format(Date(currentNote.timestampInit))}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                            Text(
                                text = "Modificada: ${dateFormatter.format(Date(currentNote.timestamp))}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                            if (currentNote.isMarkdownEnabled) {
                                Text(
                                    text = "Formato: Markdown",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Diálogo de confirmación
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Eliminar nota") },
                    text = { Text("¿Estás seguro de que quieres eliminar esta nota permanentemente?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                note?.let {
                                    viewModel.deleteNote(it)
                                    onBack()
                                }
                                showDeleteDialog = false
                            }
                        ) {
                            Text("Eliminar", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteDialog = false }
                        ) {
                            Text("Cancelar")
                        }
                    }
                )
            }

        }
    }
}