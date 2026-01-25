package com.valdo.notasinteligentesvaldo.screens

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valdo.notasinteligentesvaldo.models.Category
import com.valdo.notasinteligentesvaldo.viewmodel.NoteViewModel
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.material3.RichText
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.text.TextRange
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.background
import com.valdo.notasinteligentesvaldo.R
// Android framework for sharing/copy and PDF
import android.widget.Toast
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ClipDescription
import android.os.Build
import android.os.PersistableBundle
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.Layout
import android.text.TextPaint
import android.graphics.Typeface
import java.io.FileOutputStream
import androidx.navigation.NavController
import androidx.compose.foundation.layout.FlowRow
import kotlin.math.min
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.valdo.notasinteligentesvaldo.notification.NotificationHelper
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.layout.positionInRoot

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteDetailScreen(
    noteId: Int,
    onBack: () -> Unit,
    viewModel: NoteViewModel = viewModel(),
    navController: NavController,
    @Suppress("UNUSED_PARAMETER") categoryId: Int = -1 // mantenido por compatibilidad de navegación
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    // El modo edición usa LazyColumn (un único scroll).

    val currentDate = remember {
        SimpleDateFormat("EEEE, d 'de' MMMM", Locale.getDefault()).format(Date())
    }
    val context = LocalContext.current

    // Formateador de fecha
    val dateFormatter = remember {
        SimpleDateFormat("EEEE, d 'de' MMMM 'a las' HH:mm", Locale.getDefault())
    }

    // IMPORTANTE: estos estados deben sobrevivir rotación
    var isEditMode by rememberSaveable(noteId) { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable(noteId) { mutableStateOf(false) }
    var showEditCategoriesDialog by rememberSaveable(noteId) { mutableStateOf(false) }
    var showReminderDialog by rememberSaveable(noteId) { mutableStateOf(false) }
    var didNavigateBack by rememberSaveable(noteId) { mutableStateOf(false) }

    // OBSERVAR SOLO ESTA NOTA POR ID, NO EL currentNote GLOBAL COMPARTIDO
    val currentNote by viewModel.getNoteById(noteId).collectAsState(initial = null)
    val noteWithCategoriesState by viewModel.currentNoteWithCategories.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()

    // Carga inicial de la nota
    LaunchedEffect(noteId) {
        viewModel.observeNoteWithCategories(noteId)
    }

    // Manejo del teclado y foco - CORRECCIÓN IMPORTANTE
    LaunchedEffect(isEditMode) {
        if (isEditMode) {
            // Sólo mostrar el teclado; el foco se decide en el siguiente efecto según título/contenido
            keyboardController?.show()
        } else {
            keyboardController?.hide()
        }
    }

    // --- Protección contra nota nula ---
    // Si la nota aún no está cargada, mostrar loader (no navegar atrás aquí)
    if (currentNote == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Saver para persistir TextFieldValue (texto + selección) en rotación
    val textFieldValueSaver: Saver<TextFieldValue, List<Any>> = remember {
        Saver(
            save = { listOf(it.text, it.selection.start, it.selection.end) },
            restore = {
                val text = it[0] as String
                val start = it[1] as Int
                val end = it[2] as Int
                TextFieldValue(text = text, selection = TextRange(start, end))
            }
        )
    }

    // NUEVO: estado local del editor con control de selección/cursor (persistente en rotación)
    var contentValue by rememberSaveable(noteId, stateSaver = textFieldValueSaver) {
        mutableStateOf(TextFieldValue(currentNote!!.content))
    }

    // Seguimiento del cursor (estilo Obsidian): medimos el rect del cursor en coordenadas de raíz
    // y hacemos scroll del contenedor para mantenerlo visible/cerca del centro.
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var viewportHeightPx by remember { mutableStateOf(0) }
    var contentBoxTopInRootPx by remember { mutableStateOf(0f) }
    var cursorRectInRoot by remember { mutableStateOf<Rect?>(null) }

    // Función para ajustar el scroll y centrar el cursor (aprox. al 45% del viewport)
    fun requestCenterCursor(listState: androidx.compose.foundation.lazy.LazyListState) {
        val rect = cursorRectInRoot ?: return
        if (viewportHeightPx <= 0) return

        val viewportTop = contentBoxTopInRootPx
        val cursorCenterY = (rect.top + rect.bottom) / 2f

        val targetY = viewportTop + viewportHeightPx * 0.45f
        val delta = cursorCenterY - targetY

        val thresholdPx = with(density) { 24.dp.toPx() }
        if (kotlin.math.abs(delta) < thresholdPx) return

        scope.launch {
            // Fallback compatible: llevamos el editor (item del contenido) a una posición visible.
            // Esto evita depender de APIs scrollBy/animateScrollBy que varían por versión.
            // Nota: nuestro LazyColumn en edición tiene: [0] titulo, [1] (opcional) markdown toolbar/preview, [último] editor.
            val lastIndex = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
            listState.animateScrollToItem(lastIndex)
        }
    }

    // Sincronizar cuando el contenido subyacente cambie externamente (solo si NO estamos editando)
    LaunchedEffect(currentNote!!.content, isEditMode) {
        if (!isEditMode && currentNote!!.content != contentValue.text) {
            val newSel = min(contentValue.selection.end, currentNote!!.content.length)
            contentValue = TextFieldValue(currentNote!!.content, TextRange(newSel))
        }
    }
    // Al entrar en edición, forzar cursor al final o al inicio si vacío
    LaunchedEffect(isEditMode) {
        if (isEditMode) {
            contentValue = contentValue.copy(selection = TextRange(contentValue.text.length))
            // Asegurar que el cursor sea llevado a la vista (sobre el teclado)
            delay(1)
        }
    }

    // NUEVO: Obtener las categorías de la nota
    val categories = noteWithCategoriesState?.categories ?: emptyList()

    // NUEVO: estado local para el título con selección (persistente en rotación)
    val titleFocusRequester = remember { FocusRequester() }
    var titleValue by rememberSaveable(noteId, stateSaver = textFieldValueSaver) {
        mutableStateOf(TextFieldValue(currentNote!!.title, TextRange(currentNote!!.title.length)))
    }

    // Sincronizar título cuando cambie externamente (solo si NO estamos editando)
    LaunchedEffect(currentNote!!.title, isEditMode) {
        if (!isEditMode && currentNote!!.title != titleValue.text) {
            val sel = min(titleValue.selection.end, currentNote!!.title.length)
            titleValue = TextFieldValue(currentNote!!.title, TextRange(sel))
        }
    }

    // Ajustar foco al entrar en modo edición: priorizar título si está vacío
    LaunchedEffect(isEditMode) {
        if (isEditMode) {
            delay(300)
            try {
                if (titleValue.text.isBlank()) {
                    titleFocusRequester.requestFocus()
                } else {
                    focusRequester.requestFocus()
                }
                keyboardController?.show()
            } catch (_: Exception) {
                keyboardController?.show()
            }
        } else {
            keyboardController?.hide()
        }
    }

    // --- Guardado robusto ---
    // Persiste los valores locales (titleValue/contentValue) en Room.
    // Evita depender de _currentNote del ViewModel (que aquí no está enlazado con getNoteById()).
    fun persistEditsAndSyncUi() {
        val base = currentNote ?: return
        val newTitle = titleValue.text.ifBlank { "Nota sin título" }
        val newContent = contentValue.text

        // Si no hubo cambios, no hagas I/O innecesario
        val isDirty = newTitle != base.title || newContent != base.content
        if (!isDirty) return

        viewModel.updateNote(
            base.copy(
                title = newTitle,
                content = newContent,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    // Acción reutilizable para entrar/salir de edición desde icono o doble tap
    val toggleEdit: () -> Unit = {
        if (isEditMode) {
            // Guardar al salir de edición
            persistEditsAndSyncUi()
        }
        isEditMode = !isEditMode
    }

    // Back: si el usuario está editando, guardar de inmediato para no perder cambios
    val handleBack: () -> Unit = handleBack@{
        if (didNavigateBack) return@handleBack
        if (isEditMode) {
            persistEditsAndSyncUi()
            isEditMode = false
        }
        didNavigateBack = true
        onBack()
    }

    // Hay recordatorio activo si notificationTime != null
    val hasActiveReminder = currentNote!!.notificationTime != null

    // En la AppBar, desactivar el icono de campana si ya hay recordatorio activo
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Título vacío para liberar espacio

                },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    // Menú overflow (más opciones) con Copiar / Compartir / Eliminar
                    var showOverflow by remember { mutableStateOf(false) }
                    var showShareMenu by remember { mutableStateOf(false) }

                    // --- Resto de acciones visibles ---
                    IconButton(onClick = { viewModel.toggleFavorite(currentNote!!.id) }) {
                        Icon(
                            imageVector = if (currentNote!!.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (currentNote!!.isFavorite) "Quitar de favoritas" else "Marcar como favorita",
                            tint = if (currentNote!!.isFavorite) MaterialTheme.colorScheme.error else LocalContentColor.current
                        )
                    }

                    // Toggle Markdown solo visible en edición
                    if (isEditMode) {
                        IconButton(onClick = {
                            val newValue = !currentNote!!.isMarkdownEnabled
                            // Persistir en Room (la UI observa getNoteById(noteId))
                            viewModel.toggleMarkdown(currentNote!!.id, newValue)
                            // Actualizar inmediatamente el modelo local (optimistic UI)
                            viewModel.updateNoteTemp(currentNote!!.copy(isMarkdownEnabled = newValue))
                        }) {
                            val markdownIcon = if (currentNote!!.isMarkdownEnabled) R.drawable.code_off_24px else R.drawable.code_24px
                            Icon(
                                painter = painterResource(id = markdownIcon),
                                contentDescription = "Formato Markdown",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Editar/Guardar
                    IconButton(onClick = toggleEdit) {
                        Icon(
                            painter = painterResource(id = if (isEditMode) R.drawable.done_outline else R.drawable.edit_note),
                            contentDescription = if (isEditMode) "Guardar" else "Editar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Editar categorías
                    IconButton(onClick = { showEditCategoriesDialog = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.bookmark_flag_24),
                            contentDescription = "Seleccione categoría"
                        )
                    }

                    // Icono de campana para recordatorios POR TIEMPO (1/5/10/15 min)
                    // FIX: cuando la notificación persistente está activa, no mostramos este botón;
                    // en su lugar mostramos el botón de desactivar (para evitar 2 iconos).
                    if (!currentNote!!.isNotificationPersistent) {
                        IconButton(
                            onClick = { showReminderDialog = true },
                            enabled = !hasActiveReminder
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.notification_24),
                                contentDescription = "Recordatorio",
                                tint = if (hasActiveReminder)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        // Icono para desactivar notificación persistente activa ("no seguir recordando")
                        IconButton(onClick = {
                            NotificationHelper.cancelReminder(context, currentNote!!.id)
                            viewModel.clearReminder(currentNote!!.id)
                            Toast.makeText(
                                context,
                                "La notificación persistente se ha desactivado para esta nota",
                                Toast.LENGTH_SHORT
                            ).show()
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.notification_audio_off_24),
                                contentDescription = "No seguir recordando",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Botón overflow al extremo superior derecho (a la derecha del icono de notificación)
                    Box {
                        IconButton(onClick = { showOverflow = true }) {
                            Icon(
                                painter = painterResource(id = R.drawable.more_vert_24),
                                contentDescription = "Más opciones"
                            )
                        }

                        // Menú principal (anclado al botón overflow)
                        DropdownMenu(
                            expanded = showOverflow,
                            onDismissRequest = {
                                showOverflow = false
                                showShareMenu = false
                            }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Copiar") },
                                onClick = {
                                    showOverflow = false
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                    copyToClipboard(context, currentNote!!.content)
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.copy_24),
                                        contentDescription = null
                                    )
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Compartir") },
                                onClick = {
                                    // Toggle: si ya está abierto, contraer; si no, expandir
                                    showShareMenu = !showShareMenu
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null
                                    )
                                }
                            )

                            // Submenú de compartir (mismos formatos que antes)
                            if (showShareMenu) {
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Texto (.txt)") },
                                    onClick = {
                                        showOverflow = false
                                        showShareMenu = false
                                        shareNote(context, currentNote!!, asMarkdown = false)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Markdown (.md)") },
                                    onClick = {
                                        showOverflow = false
                                        showShareMenu = false
                                        shareNote(context, currentNote!!, asMarkdown = true)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Contenido") },
                                    onClick = {
                                        showOverflow = false
                                        showShareMenu = false
                                        shareNoteContent(context, currentNote!!.content)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("PDF (.pdf)") },
                                    onClick = {
                                        showOverflow = false
                                        showShareMenu = false
                                        shareNoteAsPdf(context, currentNote!!.title, currentNote!!.content)
                                    }
                                )
                            }

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text("Eliminar") },
                                onClick = {
                                    showOverflow = false
                                    showShareMenu = false
                                    showDeleteDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                },
                // NUEVO: respetar la barra de estado
                windowInsets = WindowInsets.statusBars,
                modifier = Modifier.statusBarsPadding()
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        // Bottom bar eliminada para ganar espacio y evitar solapamientos con el IME
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Mostrar la fecha pequeña y suave en la parte superior del contenido
            Text(
                text = currentDate,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                ),
                modifier = Modifier
                    .padding(start = 16.dp, top = 8.dp)
                    .fillMaxWidth(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Mensaje de estado de recordatorio
            if (hasActiveReminder || currentNote!!.isNotificationPersistent) {
                val reminderText = when {
                    currentNote!!.isNotificationPersistent ->
                        "Notificación persistente activa para esta nota. Toca el icono de pausa para dejar de recordarla."
                    else -> "Recordatorio activo configurado para esta nota."
                }
                Text(
                    text = reminderText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .padding(start = 16.dp, top = 4.dp)
                        .fillMaxWidth()
                )
            }

            // NUEVO: Mostrar todas las categorías con emojis, envolviendo en varias líneas si es necesario
            if (categories.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        AssistChip(
                            onClick = { /* sin acción */ },
                            label = { Text(cat.emoji + " " + cat.name) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            if (isEditMode) {
                // MODO EDICIÓN
                val listState = rememberLazyListState()
                val isDark = isSystemInDarkTheme()

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        // FIX IME: no dupliques insets. Scaffold ya aplica safeDrawing (incl. IME en Compose).
                        .onGloballyPositioned { coords ->
                            viewportHeightPx = coords.size.height
                            contentBoxTopInRootPx = coords.positionInRoot().y
                        },
                    // Un pequeño margen para que el final no quede pegado (sin sumar padding extra del IME)
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        BasicTextField(
                            value = titleValue,
                            onValueChange = { titleValue = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .focusRequester(titleFocusRequester),
                            textStyle = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(if (isDark) Color.White else MaterialTheme.colorScheme.onSurface),
                            decorationBox = { inner ->
                                Box {
                                    if (titleValue.text.isEmpty()) {
                                        Text(
                                            "Título (opcional)",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                            )
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }

                    if (currentNote!!.isMarkdownEnabled) {
                        item {
                            var preview by remember { mutableStateOf(false) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
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
                                RichText(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    Markdown(contentValue.text)
                                }
                            } else {
                                // Editor cuando NO está en preview
                                NoteDetailContentEditor(
                                    contentValue = contentValue,
                                    onContentValueChange = { contentValue = it },
                                    focusRequester = focusRequester,
                                    isDark = isDark,
                                    requestCenterCursor = { requestCenterCursor(listState) }
                                )
                            }
                        }
                    } else {
                        item {
                            // Editor normal (no-markdown)
                            NoteDetailContentEditor(
                                contentValue = contentValue,
                                onContentValueChange = { contentValue = it },
                                focusRequester = focusRequester,
                                isDark = isDark,
                                // Ocupar el alto restante del LazyColumn para que se vea el contenido completo
                                modifier = Modifier.fillParentMaxHeight(),
                                requestCenterCursor = { requestCenterCursor(listState) }
                            )
                        }
                    }
                }
            }

            // MODO LECTURA (fuera del if isEditMode)
            if (!isEditMode) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState)
                        .pointerInput(currentNote!!.id) {
                            detectTapGestures(onDoubleTap = { toggleEdit() })
                        }
                ) {
                    if (currentNote!!.title.isNotEmpty() && currentNote!!.title != "Nota sin título") {
                        Text(
                            text = currentNote!!.title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    if (currentNote!!.isMarkdownEnabled) {
                        Text(
                            text = "Vista en formato Markdown. La selección/copia directa no está soportada por Compose.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        RichText(modifier = Modifier.fillMaxWidth()) { Markdown(currentNote!!.content) }
                    } else {
                        SelectionContainer {
                            Text(
                                text = currentNote!!.content,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = 28.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Pie de nota con metadatos
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Creada: ${dateFormatter.format(Date(currentNote!!.timestampInit))}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                        Text(
                            text = "Modificada: ${dateFormatter.format(Date(currentNote!!.timestamp))}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                        if (currentNote!!.isMarkdownEnabled) {
                            Text(
                                text = "Formato: Markdown",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                            )
                        }
                        if (currentNote!!.isFavorite) {
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
                                viewModel.deleteNote(currentNote!!)
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
                        noteWithCategoriesState?.note?.id?.let { nid: Int ->
                            viewModel.viewModelScope.launch {
                                viewModel.removeAllCategoriesFromNote(nid)
                                delay(100)
                                selectedCatIds.forEach { catId -> viewModel.addCategoryToNote(nid, catId) }
                                delay(100)
                                viewModel.observeNoteWithCategories(nid)
                            }
                        }
                        showEditCategoriesDialog = false
                    }
                )
            }

            // Diálogo de configuración de recordatorio (solo POR TIEMPO)
            if (showReminderDialog) {
                AlertDialog(
                    onDismissRequest = { showReminderDialog = false },
                    title = { Text("Recordatorio") },
                    text = {
                        Column {
                            Text("Activar una notificación persistente para esta nota:")
                            Spacer(modifier = Modifier.height(8.dp))
                            AssistChip(
                                onClick = {
                                    // Marcar la nota como con notificación persistente activa
                                    viewModel.scheduleQuickReminder(currentNote!!.id, 0, true)
                                    // Mostrar la notificación persistente de inmediato (sin AlarmManager)
                                    NotificationHelper.showReminder(context, currentNote!!, true)
                                    showReminderDialog = false
                                    Toast.makeText(
                                        context,
                                        "Notificación persistente activada para esta nota",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                label = { Text("Activar notificación persistente ahora") }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "La notificación persistente se mostrará de inmediato y permanecerá visible hasta que la desactives desde la nota.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showReminderDialog = false }) {
                            Text("Cerrar")
                        }
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
        title = { Text("Seleccione categoría") },
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
                        Text("${category.emoji} ${category.name}")
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

@Composable
private fun NoteDetailContentEditor(
    contentValue: TextFieldValue,
    onContentValueChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    requestCenterCursor: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            // Fondo camuflado con el tema (mantiene el 'recuadro' pero sin contraste feo)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.02f))
            .padding(8.dp)
    ) {
        BasicTextField(
            value = contentValue,
            onValueChange = onContentValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .focusRequester(focusRequester)
                // Mantén un mínimo pero permite crecer; evita 'una sola línea'
                .defaultMinSize(minHeight = 240.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 18.sp,
                lineHeight = 28.sp,
                color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
            ),
            singleLine = false,
            minLines = 10,
            maxLines = Int.MAX_VALUE,
            cursorBrush = SolidColor(if (isDark) Color.White else MaterialTheme.colorScheme.onSurface),
            decorationBox = { inner ->
                if (contentValue.text.isEmpty()) {
                    Text(
                        "Comienza a escribir tu nota aquí...",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            lineHeight = 28.sp,
                            fontSize = 18.sp
                        )
                    )
                }
                inner()
            }
        )
    }

    LaunchedEffect(contentValue.text, contentValue.selection) {
        delay(16)
        requestCenterCursor()
    }
}
