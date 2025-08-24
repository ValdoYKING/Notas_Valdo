package com.valdo.notasinteligentesvaldo.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import com.valdo.notasinteligentesvaldo.R
import com.valdo.notasinteligentesvaldo.models.Note
import com.valdo.notasinteligentesvaldo.models.Category
import com.valdo.notasinteligentesvaldo.viewmodel.NoteViewModel
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NoteFormScreen(
    onNoteSaved: (Note, Set<Int>) -> Unit,
    onBack: () -> Unit,
    viewModel: NoteViewModel = viewModel()
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var textFieldValue by remember { mutableStateOf(TextFieldValue()) }
    var isMarkdownEnabled by remember { mutableStateOf(false) }
    val allCategories by viewModel.allCategories.collectAsState()
    var selectedCategories by remember { mutableStateOf(setOf<Int>()) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    // NUEVO: diálogo de selección de categorías
    var showCategorySelector by remember { mutableStateOf(false) }
    // Variables de error y edición
    var categoryError by remember { mutableStateOf("") }
    val editingNote = viewModel.currentNote.collectAsState().value
    // Sincroniza el contenido cuando cambias de modo
    LaunchedEffect(isMarkdownEnabled) {
        if (!isMarkdownEnabled) {
            // Al salir de markdown, actualiza el textFieldValue con el contenido
            textFieldValue = TextFieldValue(content)
        } else {
            // Al entrar a markdown, actualiza el contenido con el texto plano
            content = textFieldValue.text
        }
    }
    val focusRequester = remember { FocusRequester() }
    val currentDate = remember {
        SimpleDateFormat("EEEE, d 'de' MMMM", Locale.getDefault()).format(Date())
    }
    val titleAlpha by animateFloatAsState(
        targetValue = if (title.isBlank()) 0.5f else 1f,
        animationSpec = tween(durationMillis = 300)
    )


    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    // Botón de agregar categoría
                    IconButton(onClick = { showAddCategoryDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar categoría")
                    }
                    // Botón de toggle Markdown
                    IconButton(
                        onClick = { isMarkdownEnabled = !isMarkdownEnabled },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        val markdownIcon = if (isMarkdownEnabled) R.drawable.code_off_24px else R.drawable.code_24px
                        Icon(
                            painter = painterResource(id = markdownIcon),
                            contentDescription = "Markdown",
                            tint = if (isMarkdownEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Botón de guardar (icono actualizado)
                    IconButton(
                        onClick = {
                            val noteToSave = editingNote?.copy(
                                id = 0, // Siempre id = 0 para nuevas notas
                                title = title.ifEmpty { "Nota sin título" },
                                content = content,
                                isMarkdownEnabled = isMarkdownEnabled
                            ) ?: Note(
                                title = title.ifEmpty { "Nota sin título" },
                                content = content,
                                timestamp = System.currentTimeMillis(),
                                timestampInit = System.currentTimeMillis(),
                                isMarkdownEnabled = isMarkdownEnabled
                            )
                            onNoteSaved(noteToSave, selectedCategories)
                        },
                        enabled = content.isNotBlank() || title.isNotBlank()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.done_outline),
                            contentDescription = "Guardar"
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = currentDate,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        // Título siempre visible (pero transparente cuando está vacío)
                        Text(
                            text = title.ifEmpty { " " }, // Espacio para mantener altura
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = if (title.isBlank()) Color.Transparent
                                else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
            // Campo de título
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    color = if (isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                cursorBrush = SolidColor(
                    if (isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.onSurface
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (title.isEmpty()) {
                            Text(
                                text = title.ifEmpty { "Añade un título..." },
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = if (isSystemInDarkTheme()) Color(0xFFEFEFEF) else MaterialTheme.colorScheme.onSurface.copy(alpha = titleAlpha),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // Selector de categorías con botón y diálogo estable
            Button(
                onClick = { showCategorySelector = true },
                modifier = Modifier
                    .padding(start = 16.dp, top = 8.dp)
            ) {
                Text("Seleccionar categorías")
            }

            // Mostrar hashtags de categorías seleccionadas
            if (selectedCategories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allCategories.filter { selectedCategories.contains(it.categoryId) }.forEach { category ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = Color(0xFF6650a4), // Purple40
                            tonalElevation = 2.dp
                        ) {
                            Text(
                                text = "#${category.name}",
                                color = Color(0xFFD0BCFF), // Purple80
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Área de contenido con soporte Markdown
            if (isMarkdownEnabled) {
                MarkdownEditor(content, onContentChange = { content = it })
            } else {
                val scrollState = rememberScrollState()
                val bringIntoViewRequester = remember { BringIntoViewRequester() }
                var cursorRect by remember { mutableStateOf<Rect?>(null) }
                val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
                var bringIntoViewTrigger by remember { mutableStateOf(0) }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(bringIntoViewRequester)
                    ) {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = {
                                textFieldValue = it
                                content = it.text
                                bringIntoViewTrigger++
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                lineHeight = 28.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = false,
                            maxLines = Int.MAX_VALUE,
                            cursorBrush = SolidColor(if (isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.onSurface),
                            decorationBox = { innerTextField ->
                                if (textFieldValue.text.isEmpty()) {
                                    Text(
                                        "Comienza a escribir tu nota aquí...",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                                innerTextField()
                            },
                            onTextLayout = { textLayoutResult ->
                                val cursorOffset = textFieldValue.selection.end
                                cursorRect = textLayoutResult.getCursorRect(cursorOffset)
                            }
                        )
                    }
                }
                LaunchedEffect(bringIntoViewTrigger, imeBottom) {
                    if (imeBottom > 0 && cursorRect != null) {
                        delay(1)
                        bringIntoViewRequester.bringIntoView(cursorRect!!)
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }

        // Diálogo para agregar nueva categoría con feedback de error
        if (showAddCategoryDialog) {
            AlertDialog(
                onDismissRequest = { showAddCategoryDialog = false; categoryError = "" },
                title = { Text("Nueva categoría") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it; categoryError = "" },
                            label = { Text("Nombre de la categoría") },
                            isError = categoryError.isNotEmpty()
                        )
                        if (categoryError.isNotEmpty()) {
                            Text(categoryError, color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val name = newCategoryName.trim()
                            if (name.isBlank()) {
                                categoryError = "El nombre no puede estar vacío."
                            } else if (allCategories.any { it.name.equals(name, true) }) {
                                categoryError = "La categoría ya existe."
                            } else {
                                viewModel.insertCategory(Category(name = name))
                                newCategoryName = ""
                                showAddCategoryDialog = false
                                categoryError = ""
                            }
                        }
                    ) { Text("Agregar") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCategoryDialog = false; categoryError = "" }) { Text("Cancelar") }
                }
            )
        }

        // NUEVO: Diálogo para seleccionar categorías
        if (showCategorySelector) {
            CategorySelectionDialog(
                allCategories = allCategories,
                initiallySelected = selectedCategories,
                onDismiss = { showCategorySelector = false },
                onConfirm = { newSelection ->
                    selectedCategories = newSelection
                    showCategorySelector = false
                }
            )
        }
    }

    // CORRECCIÓN DEFINITIVA: Limpiar campos inmediatamente al entrar y cuando editingNote cambie
    LaunchedEffect(Unit) {
        // Forzar limpieza inicial al entrar al formulario
        title = ""
        content = ""
        textFieldValue = TextFieldValue("")
        isMarkdownEnabled = false
        selectedCategories = emptySet()
    }

    LaunchedEffect(editingNote) {
        if (editingNote == null) {
            // Nueva nota: asegurar limpieza completa
            title = ""
            content = ""
            textFieldValue = TextFieldValue("")
            isMarkdownEnabled = false
            selectedCategories = emptySet()
        } else {
            // Editar nota existente: cargar datos
            title = editingNote.title
            content = editingNote.content
            textFieldValue = TextFieldValue(editingNote.content)
            isMarkdownEnabled = editingNote.isMarkdownEnabled
            // Cargar categorías asociadas
            viewModel.getNoteWithCategories(editingNote.id)
            val noteWithCats = viewModel.notesWithCategories.value.firstOrNull { n -> n.note.id == editingNote.id }
            selectedCategories = noteWithCats?.categories?.map { c -> c.categoryId }?.toSet() ?: emptySet()
        }
    }
}

@Composable
fun MarkdownEditor(
    content: String,
    onContentChange: (String) -> Unit
) {
    var preview by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    // NUEVO: estados para seguir el cursor y evitar que el teclado lo tape
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var cursorRect by remember { mutableStateOf<Rect?>(null) }
    var bringIntoViewTrigger by remember { mutableStateOf(0) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Barra de herramientas Markdown
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { onContentChange("$content **texto** ") }) {
                Text("B", fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { onContentChange("$content *texto* ") }) {
                Text("I", fontStyle = FontStyle.Italic)
            }
            IconButton(onClick = { onContentChange("$content [texto](url) ") }) {
                Text("Link")
            }
            IconButton(onClick = { preview = !preview }) {
                val previewIcon = if (preview) R.drawable.visibility_off_24px else R.drawable.visibility_24px
                Icon(
                    painter = painterResource(id = previewIcon),
                    contentDescription = "Vista previa"
                )
            }
        }

        if (preview) {
            // Vista previa del Markdown
            MarkdownPreview(content)
        } else {
            // Editor de Markdown con desplazamiento y seguimiento del caret
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                BasicTextField(
                    value = content,
                    onValueChange = {
                        onContentChange(it)
                        bringIntoViewTrigger++
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(bringIntoViewRequester),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        color = if (isDark) Color.White else Color.Unspecified
                    ),
                    cursorBrush = SolidColor(if (isDark) Color.White else Color.Black),
                    onTextLayout = { layout ->
                        val end = content.length
                        cursorRect = layout.getCursorRect(end)
                    }
                )
            }

            // Desplazar para mantener visible el cursor por encima del teclado
            LaunchedEffect(bringIntoViewTrigger, imeBottom) {
                if (imeBottom > 0 && cursorRect != null) {
                    delay(1)
                    bringIntoViewRequester.bringIntoView(cursorRect!!)
                }
            }
        }
    }
}

@Composable
fun MarkdownPreview(content: String) {
    val isDark = isSystemInDarkTheme()
    MarkdownText(
        markdown = content,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun CategorySelectionDialog(
    allCategories: List<Category>,
    initiallySelected: Set<Int>,
    onDismiss: () -> Unit,
    onConfirm: (Set<Int>) -> Unit
) {
    var tempSelected by remember { mutableStateOf(initiallySelected) }
    val scrollState = rememberScrollState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar categorías") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 420.dp)
                    .verticalScroll(scrollState)
            ) {
                if (allCategories.isEmpty()) {
                    Text("No hay categorías. Usa el + para crear una.")
                } else {
                    allCategories.forEach { category ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    tempSelected = if (tempSelected.contains(category.categoryId))
                                        tempSelected - category.categoryId else tempSelected + category.categoryId
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = tempSelected.contains(category.categoryId),
                                onCheckedChange = { checked ->
                                    tempSelected = if (checked)
                                        tempSelected + category.categoryId else tempSelected - category.categoryId
                                }
                            )
                            Text(category.name)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(tempSelected) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
