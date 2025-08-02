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
// NUEVO: Importar iconos de corazón
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
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
// NUEVO: Importar Color si quieres un color específico como Rojo
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import com.valdo.notasinteligentesvaldo.R
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

    // --- Protección contra nota nula ---
    // Guardamos la nota actual en una variable local para asegurar que no sea nula
    // dentro del Scaffold, ya que la comprobación inicial ya se hizo.
    val currentNote = note

    if (currentNote == null) { // MODIFICADO: Usar la variable local
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
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
                    // --- NUEVO: Botón Favorito ---
                    IconButton(
                        onClick = {
                            // Llama a la función del ViewModel para cambiar el estado de favorito
                            viewModel.toggleFavorite(currentNote.id)
                        }
                    ) {
                        Icon(
                            // Cambia el icono según si la nota es favorita o no
                            imageVector = if (currentNote.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (currentNote.isFavorite) "Quitar de favoritas" else "Marcar como favorita",
                            // Opcional: Cambia el color si es favorita
                            tint = if (currentNote.isFavorite) MaterialTheme.colorScheme.error else LocalContentColor.current // Puedes usar Color.Red o primary
                        )
                    }
                    // --- FIN NUEVO ---

                    // Botón de eliminar
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        // modifier = Modifier.padding(end = 8.dp) // Quitar padding si quieres que estén más juntos
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar nota",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    // Botón de formato (solo en modo edición)
                    if (isEditMode) {
                        IconButton(
                            onClick = {
                                viewModel.updateCurrentNote { current ->
                                    current.copy(isMarkdownEnabled = !current.isMarkdownEnabled)
                                }
                            }
                        ) {
                            val markdownIcon = if (currentNote.isMarkdownEnabled) R.drawable.code_off_24px else R.drawable.code_24px
                            Icon(
                                painter = painterResource(id = markdownIcon),
                                contentDescription = "Formato Markdown",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Botón de edición/guardado
                    IconButton(
                        onClick = {
                            if (isEditMode) {
                                viewModel.saveCurrentNote() // Guarda la nota actual (incluyendo estado de favorito si cambió)
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
                    value = currentNote.title, // MODIFICADO: usar currentNote
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
                            if (currentNote.title.isEmpty()) { // MODIFICADO: usar currentNote
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
                if (currentNote.isMarkdownEnabled) { // MODIFICADO: usar currentNote
                    var preview by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { preview = !preview }) {
                            val previewIcon = if (preview) R.drawable.visibility_off_24px else R.drawable.visibility_24px
                            Icon(
                                painter = painterResource(id = previewIcon),
                                contentDescription = "Vista previa"
                            )
                        }
                    }
                    if (preview) {
                        MarkdownText(
                            markdown = currentNote.content,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    } else {
                        BasicTextField(
                            value = currentNote.content, // MODIFICADO: usar currentNote
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
                    BasicTextField(
                        value = currentNote.content, // MODIFICADO: usar currentNote
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
                    // Ya no necesitamos el let porque usamos currentNote que sabemos no es null aquí
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
                        // NUEVO: Mostrar si es favorita en los metadatos
                        if (currentNote.isFavorite) {
                            Text(
                                text = "Favorita ⭐", // Puedes usar un emoji o texto
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f) // Usar un color distintivo
                                )
                            )
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
                                // No necesitamos el let aquí porque currentNote no es null
                                viewModel.deleteNote(currentNote)
                                onBack() // Vuelve atrás después de eliminar
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
