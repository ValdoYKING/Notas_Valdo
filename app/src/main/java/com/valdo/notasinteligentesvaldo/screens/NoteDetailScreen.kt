package com.valdo.notasinteligentesvaldo.screens

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valdo.notasinteligentesvaldo.models.Category
import com.valdo.notasinteligentesvaldo.viewmodel.NoteViewModel
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.navigation.NavController
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.valdo.notasinteligentesvaldo.R
import android.widget.Toast
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ClipDescription
import android.os.Build
import android.os.PersistableBundle
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.graphics.Typeface
import android.graphics.Color as AndroidColor
import java.io.FileOutputStream
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextLayoutResult
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun NoteDetailScreen(
    noteId: Int,
    onBack: () -> Unit,
    viewModel: NoteViewModel = viewModel(),
    navController: NavController,
    categoryId: Int = -1 // <-- Nuevo parámetro
) {
    val note by viewModel.currentNote.collectAsState()
    var isEditMode by remember { mutableStateOf(false) }
    // NUEVO: bandera para evitar doble navegación al eliminar
    var didNavigateBack by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()
    // NUEVO: gestionar foco para evitar pegados accidentales tras copiar
    val focusManager = LocalFocusManager.current
    // NUEVO: utilidades para mantener el cursor visible sobre el teclado en modo edición
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var cursorRect by remember { mutableStateOf<Rect?>(null) }
    var bringIntoViewTrigger by remember { mutableStateOf(0) }
    val editScrollState = rememberScrollState()
    // Reemplazo: mantener el layout de texto en un state explícito
    val textLayoutResultState = remember { mutableStateOf<TextLayoutResult?>(null) }
    val currentDate = remember {
        SimpleDateFormat("EEEE, d 'de' MMMM", Locale.getDefault()).format(Date())
    }
    val context = LocalContext.current

    // Formateador de fecha
    val dateFormatter = remember {
        SimpleDateFormat("EEEE, d 'de' MMMM 'a las' HH:mm", Locale.getDefault())
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditCategoriesDialog by remember { mutableStateOf(false) }

    // Carga inicial de la nota
    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
        // Usar flujo dedicado para el detalle
        viewModel.observeNoteWithCategories(noteId)
    }

    // Manejo del teclado y foco - CORRECCIÓN IMPORTANTE
    LaunchedEffect(isEditMode) {
        if (isEditMode) {
            delay(300) // Pequeño delay para permitir la composición
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
            } catch (_: Exception) {
                keyboardController?.show()
            }
        } else {
            keyboardController?.hide()
        }
    }

    // --- Protección contra nota nula ---
    val currentNote = note
    // Usar flujo dedicado de la nota con categorías para el detalle
    val noteWithCategoriesState by viewModel.currentNoteWithCategories.collectAsState()

    // Si la nota aún no está cargada, mostrar loader (no navegar atrás aquí)
    if (currentNote == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Center) {
            CircularProgressIndicator()
        }
        return
    }

    // NUEVO: estado local del editor con control de selección/cursor
    var contentValue by remember(noteId) {
        mutableStateOf(
            TextFieldValue(
                text = currentNote.content,
                selection = TextRange(currentNote.content.length)
            )
        )
    }
    // Sincronizar cuando el contenido subyacente cambie externamente
    LaunchedEffect(currentNote.content) {
        if (currentNote.content != contentValue.text) {
            val newSel = min(contentValue.selection.end, currentNote.content.length)
            contentValue = TextFieldValue(currentNote.content, TextRange(newSel))
        }
    }
    // Al entrar en edición, forzar cursor al final o al inicio si vacío
    LaunchedEffect(isEditMode) {
        if (isEditMode) {
            contentValue = contentValue.copy(selection = TextRange(contentValue.text.length))
            // Asegurar que el cursor sea llevado a la vista (sobre el teclado)
            delay(1)
            bringIntoViewTrigger++
        }
    }

    // NUEVO: Obtener las categorías de la nota
    val allCategories by viewModel.allCategories.collectAsState()

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    // Sin acciones aquí para no truncar el título
                }
            )
        },
        // NUEVO: barra inferior para acciones clave en modo edición
        bottomBar = {
            if (isEditMode) {
                BottomAppBar(actions = {
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = {
                        // Toggle Markdown sin salir de edición
                        viewModel.updateCurrentNote { current ->
                            current.copy(isMarkdownEnabled = !current.isMarkdownEnabled)
                        }
                    }) {
                        val markdownIcon = if (currentNote.isMarkdownEnabled) R.drawable.code_off_24px else R.drawable.code_24px
                        Icon(painter = painterResource(id = markdownIcon), contentDescription = "Formato Markdown", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = {
                        // Guardar sin salir de edición
                        viewModel.saveCurrentNote()
                    }) {
                        Icon(painter = painterResource(id = R.drawable.done_outline), contentDescription = "Guardar", tint = MaterialTheme.colorScheme.primary)
                    }
                })
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            // NUEVO: barra de acciones bajo el encabezado, alineada a la derecha
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Empuja el resto de acciones al extremo derecho
                Spacer(modifier = Modifier.weight(1f))

                // Copiar (limpiar foco/teclado ANTES de copiar)
                IconButton(onClick = {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                    copyToClipboard(context, currentNote.content)
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.copy_24),
                        contentDescription = "Copiar contenido"
                    )
                }
                // Compartir
                var anchorShare by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { anchorShare = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir")
                    }
                    DropdownMenu(expanded = anchorShare, onDismissRequest = { anchorShare = false }) {
                        DropdownMenuItem(
                            text = { Text("Texto (.txt)") },
                            onClick = {
                                anchorShare = false
                                shareNote(context, currentNote, asMarkdown = false)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Markdown (.md)") },
                            onClick = {
                                anchorShare = false
                                shareNote(context, currentNote, asMarkdown = true)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Contenido") },
                            onClick = {
                                anchorShare = false
                                shareNoteContent(context, currentNote.content)
                            }
                        )
                        // NUEVO: Compartir como PDF
                        DropdownMenuItem(
                            text = { Text("PDF (.pdf)") },
                            onClick = {
                                anchorShare = false
                                shareNoteAsPdf(context, currentNote.title, currentNote.content)
                            }
                        )
                    }
                }
                // Favorito
                IconButton(onClick = { viewModel.toggleFavorite(currentNote.id) }) {
                    Icon(
                        imageVector = if (currentNote.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (currentNote.isFavorite) "Quitar de favoritas" else "Marcar como favorita",
                        tint = if (currentNote.isFavorite) MaterialTheme.colorScheme.error else LocalContentColor.current
                    )
                }
                // Borrar
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar nota", tint = MaterialTheme.colorScheme.error)
                }
                // Markdown (solo en edición) - mantenido pero principal está en BottomAppBar
                if (isEditMode) {
                    IconButton(onClick = {
                        viewModel.updateCurrentNote { current -> current.copy(isMarkdownEnabled = !current.isMarkdownEnabled) }
                    }) {
                        val markdownIcon = if (currentNote.isMarkdownEnabled) R.drawable.code_off_24px else R.drawable.code_24px
                        Icon(painter = painterResource(id = markdownIcon), contentDescription = "Formato Markdown", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                // Editar/Guardar (toggle de modo)
                IconButton(onClick = {
                    if (isEditMode) {
                        viewModel.saveCurrentNote()
                    }
                    isEditMode = !isEditMode
                }) {
                    Icon(
                        painter = painterResource(id = if (isEditMode) R.drawable.done_outline else R.drawable.edit_note),
                        contentDescription = if (isEditMode) "Guardar" else "Editar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                // Categorías (icono personalizado)
                IconButton(onClick = { showEditCategoriesDialog = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.format_list_bullete),
                        contentDescription = "Editar categorías"
                    )
                }
            }

            if (isEditMode) {
                // MODO EDICIÓN
                BasicTextField(
                    value = currentNote.title,
                    onValueChange = { newTitle ->
                        viewModel.updateCurrentNote { it.copy(title = newTitle) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(if (isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.onSurface),
                    decorationBox = { innerTextField ->
                        Box {
                            if (currentNote.title.isEmpty()) {
                                Text(
                                    "Título (opcional)",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // Editor de contenido (Markdown o texto normal)
                if (currentNote.isMarkdownEnabled) {
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
                        val isDark = isSystemInDarkTheme()
                        MarkdownText(
                            markdown = contentValue.text,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    } else {
                        val isDark = isSystemInDarkTheme()
                        // NUEVO: caja envolvente que detecta taps y coloca el cursor
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .pointerInput(contentValue.text, editScrollState.value) {
                                    detectTapGestures { pos ->
                                        val yAdj = pos.y + editScrollState.value
                                        val layout = textLayoutResultState.value
                                        val newOffset = if (contentValue.text.isBlank()) 0 else layout?.getOffsetForPosition(Offset(pos.x, yAdj)) ?: contentValue.text.length
                                        contentValue = contentValue.copy(selection = TextRange(newOffset))
                                        bringIntoViewTrigger++
                                        focusRequester.requestFocus()
                                    }
                                }
                        ) {
                            BasicTextField(
                                value = contentValue,
                                onValueChange = { newValue ->
                                    contentValue = newValue
                                    viewModel.updateCurrentNote { it.copy(content = newValue.text) }
                                    bringIntoViewTrigger++
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .focusRequester(focusRequester)
                                    .bringIntoViewRequester(bringIntoViewRequester)
                                    .verticalScroll(editScrollState),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 18.sp,
                                    color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(if (isDark) Color.White else MaterialTheme.colorScheme.onSurface),
                                onTextLayout = { layout ->
                                    textLayoutResultState.value = layout
                                    val selEnd = min(contentValue.selection.end, contentValue.text.length)
                                    cursorRect = layout.getCursorRect(selEnd)
                                }
                            )
                        }
                    }
                } else {
                    // NUEVO: caja envolvente que detecta taps y coloca el cursor
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .pointerInput(contentValue.text, editScrollState.value) {
                                detectTapGestures { pos ->
                                    val yAdj = pos.y + editScrollState.value
                                    val layout = textLayoutResultState.value
                                    val newOffset = if (contentValue.text.isBlank()) 0 else layout?.getOffsetForPosition(Offset(pos.x, yAdj)) ?: contentValue.text.length
                                    contentValue = contentValue.copy(selection = TextRange(newOffset))
                                    bringIntoViewTrigger++
                                    focusRequester.requestFocus()
                                }
                            }
                    ) {
                        BasicTextField(
                            value = contentValue,
                            onValueChange = { newValue ->
                                contentValue = newValue
                                viewModel.updateCurrentNote { it.copy(content = newValue.text) }
                                bringIntoViewTrigger++
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .focusRequester(focusRequester)
                                .bringIntoViewRequester(bringIntoViewRequester)
                                .verticalScroll(editScrollState),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(if (isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.onSurface),
                            onTextLayout = { layout ->
                                textLayoutResultState.value = layout
                                val selEnd = min(contentValue.selection.end, contentValue.text.length)
                                cursorRect = layout.getCursorRect(selEnd)
                            }
                        )
                    }
                }

                // NUEVO: seguir el cursor automáticamente cuando cambie la selección o el texto
                LaunchedEffect(bringIntoViewTrigger, contentValue.selection) {
                    if (cursorRect != null) {
                        // pequeño delay para evitar saltos durante composición
                        delay(1)
                        bringIntoViewRequester.bringIntoView(cursorRect!!)
                    }
                }
            } else {
                // MODO LECTURA
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState)
                ) {
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
                        val isDark = isSystemInDarkTheme()
                        Text(
                            text = "Vista en formato Markdown. La selección/copia directa no está soportada por Compose.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        MarkdownText(
                            markdown = currentNote.content,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    } else {
                        SelectionContainer {
                            Text(
                                text = currentNote.content,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = 28.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    // Visualización de categorías asociadas
                    val categories = noteWithCategoriesState?.categories ?: emptyList()
                    if (categories.isNotEmpty()) {
                        Row(
                            modifier = Modifier.padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { category ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(category.name) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                )
                            }
                        }
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
                        if (currentNote.isFavorite) {
                            Text(
                                text = "Favorita ⭐",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
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
                                viewModel.deleteNote(currentNote)
                                if (!didNavigateBack) {
                                    didNavigateBack = true
                                    val popped = navController.popBackStack()
                                    if (!popped) {
                                        navController.navigate("notes?filter=all") {
                                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
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

            // Diálogo para editar categorías
            if (showEditCategoriesDialog) {
                EditCategoriesDialog(
                    allCategories = allCategories,
                    selectedCategories = noteWithCategoriesState?.categories?.map { it.categoryId }?.toSet() ?: emptySet(),
                    onDismiss = { showEditCategoriesDialog = false },
                    onSave = { selectedCatIds ->
                        noteWithCategoriesState?.note?.id?.let { noteId ->
                            viewModel.viewModelScope.launch {
                                viewModel.removeAllCategoriesFromNote(noteId)
                                delay(100)
                                selectedCatIds.forEach { catId ->
                                    viewModel.addCategoryToNote(noteId, catId)
                                }
                                delay(100)
                                viewModel.observeNoteWithCategories(noteId)
                            }
                        }
                        showEditCategoriesDialog = false
                    }
                )
            }

        }
    }
}

@Composable
fun EditCategoriesDialog(
    allCategories: List<Category>,
    selectedCategories: Set<Int>,
    onDismiss: () -> Unit,
    onSave: (Set<Int>) -> Unit
) {
    var tempSelected by remember { mutableStateOf(selectedCategories) }
    val scrollState = rememberScrollState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar categorías") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 420.dp)
                    .verticalScroll(scrollState)
            ) {
                allCategories.forEach { category ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                tempSelected = if (tempSelected.contains(category.categoryId))
                                    tempSelected - category.categoryId
                                else
                                    tempSelected + category.categoryId
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = tempSelected.contains(category.categoryId),
                            onCheckedChange = {
                                tempSelected = if (it)
                                    tempSelected + category.categoryId
                                else
                                    tempSelected - category.categoryId
                            }
                        )
                        Text(category.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(tempSelected) }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// NUEVO: función utilitaria para compartir la nota como archivo (.txt o .md)
private fun shareNote(context: Context, note: com.valdo.notasinteligentesvaldo.models.Note, asMarkdown: Boolean) {
    val safeTitle = note.title.takeIf { it.isNotBlank() && it != "Nota sin título" } ?: "Nota"

    // Construir cuerpo según formato
    val body = if (asMarkdown) {
        val heading = if (safeTitle.isNotEmpty()) "# $safeTitle\n\n" else ""
        (heading + note.content).trim()
    } else {
        val heading = if (safeTitle.isNotEmpty()) "$safeTitle\n\n" else ""
        (heading + note.content).trim()
    }

    // Crear archivo temporal en cache con la extensión correcta
    val ext = if (asMarkdown) ".md" else ".txt"
    val fileName = buildString { append(safeTitle.lowercase()) }
        .replace("[^a-z0-9._-]".toRegex(), "_")
        .take(40)
        .ifBlank { "nota" } + ext

    val outFile = File(context.cacheDir, fileName)
    outFile.writeText(body, Charsets.UTF_8)

    val uri = FileProvider.getUriForFile(
        context,
        context.packageName + ".fileprovider",
        outFile
    )

    val mime = if (asMarkdown) "text/markdown" else "text/plain"
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_SUBJECT, safeTitle)
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, body)
        type = mime
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Compartir nota"))
}

// NUEVO: función utilitaria para compartir solo el contenido como texto plano
private fun shareNoteContent(context: Context, content: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, content)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Compartir contenido"))
}

// NUEVO: función utilitaria para copiar contenido al portapapeles (marcado sensible en Android 13+)
private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val label = context.getString(R.string.app_name)
    val clip = ClipData.newPlainText(label, text).also {
        if (Build.VERSION.SDK_INT >= 33) {
            val extras = PersistableBundle()
            extras.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            it.description.extras = extras
        }
    }
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Contenido copiado", Toast.LENGTH_SHORT).show()
}

// NUEVO: generar y compartir PDF desde título y contenido
private fun shareNoteAsPdf(context: Context, title: String, content: String) {
    try {
        val file = createPdfFile(context, title, content)
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, if (title.isNotBlank()) title else "Nota")
            type = "application/pdf"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Compartir PDF"))
    } catch (_: Exception) {
        Toast.makeText(context, "No se pudo crear el PDF", Toast.LENGTH_SHORT).show()
    }
}

private fun createPdfFile(context: Context, title: String, content: String): File {
    val document = PdfDocument()

    // Tamaño A4 en puntos (1/72 de pulgada): 595 x 842
    val pageWidth = 595
    val pageHeight = 842
    val margin = 40
    val contentWidth = pageWidth - margin * 2

    val titlePaint = TextPaint().apply {
        isAntiAlias = true
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = AndroidColor.BLACK
    }
    val bodyPaint = TextPaint().apply {
        isAntiAlias = true
        textSize = 12f
        color = AndroidColor.BLACK
    }

    // Layouts
    val safeTitle = if (title.isNotBlank() && title != "Nota sin título") title else "Nota"
    val titleLayout = StaticLayout.Builder.obtain(safeTitle, 0, safeTitle.length, titlePaint, contentWidth)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setIncludePad(false)
        .build()

    val bodyLayout = StaticLayout.Builder.obtain(content, 0, content.length, bodyPaint, contentWidth)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(0f, 1.2f)
        .setIncludePad(false)
        .build()

    var bodyDrawTopPx = 0 // desplazamiento vertical consumido del cuerpo
    var isFirstPage = true
    var pageIndex = 1

    while (true) {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Título en la primera página
        var y = margin
        if (isFirstPage) {
            canvas.save()
            canvas.translate(margin.toFloat(), y.toFloat())
            titleLayout.draw(canvas)
            canvas.restore()
            y += titleLayout.height + 16 // espacio después del título
        }

        // Altura disponible para el cuerpo en esta página
        val availableHeight = pageHeight - y - margin

        // Dibujar el cuerpo con recorte/paginación
        canvas.save()
        // Trasladar a la esquina superior izquierda del contenido
        canvas.translate(margin.toFloat(), y.toFloat() - bodyDrawTopPx)
        // Limitar el dibujo al área de contenido visible en esta página
        canvas.clipRect(0, bodyDrawTopPx, contentWidth, bodyDrawTopPx + availableHeight)
        bodyLayout.draw(canvas)
        canvas.restore()

        document.finishPage(page)

        // ¿Quedan más líneas por dibujar?
        if (bodyDrawTopPx + availableHeight >= bodyLayout.height) {
            break
        } else {
            bodyDrawTopPx += availableHeight
            isFirstPage = false
            pageIndex += 1
        }
    }

    val outFileName = (safeTitle.lowercase().replace("[^a-z0-9._-]".toRegex(), "_").take(40).ifBlank { "nota" }) + ".pdf"
    val outFile = File(context.cacheDir, outFileName)
    FileOutputStream(outFile).use { fos ->
        document.writeTo(fos)
    }
    document.close()
    return outFile
}
