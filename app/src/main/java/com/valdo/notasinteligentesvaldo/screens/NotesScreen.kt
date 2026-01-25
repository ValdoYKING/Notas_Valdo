package com.valdo.notasinteligentesvaldo.screens

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.valdo.notasinteligentesvaldo.components.NoteCard
import com.valdo.notasinteligentesvaldo.data.UiPrefs
import com.valdo.notasinteligentesvaldo.models.Category
import com.valdo.notasinteligentesvaldo.models.Note
import com.valdo.notasinteligentesvaldo.viewmodel.NoteViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.res.painterResource
import com.valdo.notasinteligentesvaldo.R
import kotlinx.coroutines.flow.first
import kotlin.math.min
// Agrego FlowRow y anotación experimental
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.valdo.notasinteligentesvaldo.util.NoteActions

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun NotesScreen(
    viewModel: NoteViewModel,
    navController: NavController,
    filterType: String,
    onAddNote: () -> Unit
) {
    // Estados de notas
    val allNotesState by viewModel.allNotes.collectAsState()
    val favoriteNotesState by viewModel.favoriteNotes.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()

    // Estado de carga
    var isLoading by remember { mutableStateOf(true) }
    var showCategoryManager by remember { mutableStateOf(false) }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }

    // NUEVO: Estado para compartir
    var shareTarget by remember { mutableStateOf<Note?>(null) }
    val shareSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val context = LocalContext.current

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Helper para navegar a detalle con launchSingleTop y evitar múltiples instancias
    fun navigateToNoteDetail(noteId: Int, categoryId: Int?) {
        val cat = categoryId ?: -1
        navController.navigate("noteDetail/$noteId?categoryId=$cat") {
            launchSingleTop = true
        }
    }

    // Cargar preferencia de categoría seleccionada al iniciar
    LaunchedEffect(Unit) {
        val savedCat = UiPrefs.selectedCategoryIdFlow(context).first()
        if (savedCat != null) {
            selectedCategoryId = savedCat
        }
    }

    // Determinar qué notas mostrar
    val notesToDisplay = when (filterType) {
        "favorites" -> favoriteNotesState
        else -> allNotesState
    }
    // NUEVO: alias legible del filtro actual para el Drawer
    val currentFilter = filterType

    val notesByCategory = viewModel.notesWithCategories.collectAsState().value

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Detectar cuando hay datos por primera vez y bajar el loader inicial
    LaunchedEffect(allNotesState, favoriteNotesState) {
        if (isLoading) isLoading = false
    }

    // Filtrado por categoría (debe ir antes del Scaffold)
    val filteredNotes by remember(selectedCategoryId, notesToDisplay, notesByCategory) {
        derivedStateOf {
            if (selectedCategoryId != null) {
                notesByCategory.filter { it.categories.any { c -> c.categoryId == selectedCategoryId } }.map { it.note }
            } else notesToDisplay
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                // Item Inicio
                NavigationDrawerItem(
                    label = { Text("Inicio") },
                    selected = currentFilter == "all",
                    onClick = {
                        scope.launch {
                            UiPrefs.setFilterType(context, "all")
                            drawerState.close()
                        }
                        if (currentFilter != "all") {
                            navController.navigate("notes?filter=all")
                        }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") }
                )
                // Item Favoritas
                NavigationDrawerItem(
                    label = { Text("Favoritas") },
                    selected = currentFilter == "favorites",
                    onClick = {
                        scope.launch {
                            UiPrefs.setFilterType(context, "favorites")
                            drawerState.close()
                        }
                        if (currentFilter != "favorites") {
                            navController.navigate("notes?filter=favorites")
                        }
                    },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Favoritas") }
                )
                // Item Categorías
                NavigationDrawerItem(
                    label = { Text("Categorías") },
                    selected = showCategoryManager,
                    onClick = {
                        showCategoryManager = true
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Menu, contentDescription = "Categorías") }
                )
                HorizontalDivider()
                // Item Ajustes
                NavigationDrawerItem(
                    label = { Text("Ajustes") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        // Navegar a pantalla de ajustes
                        navController.navigate("settings")
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onAddNote, // CAMBIO: Llama a onAddNote para limpiar el estado antes de navegar
                    modifier = Modifier.defaultMinSize(minWidth = 150.dp, minHeight = 56.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Nueva nota :)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            },
            topBar = {
                TopAppBar(
                    title = { Text(if (filterType == "favorites") "Notas Favoritas" else "Mis Notas") },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } }
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Abrir menú")
                        }
                    },
                    actions = {
                        // (Eliminado) Botón de refrescar
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // Barra de categorías: siempre una sola fila desplazable (portrait y landscape)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        // Un poco más arriba: menos padding vertical y menos top
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {
                            selectedCategoryId = null
                            scope.launch { UiPrefs.setSelectedCategoryId(context, null) }
                        },
                        label = { Text("Todas") },
                        leadingIcon = {
                            val iconTint = if (selectedCategoryId == null)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurface

                            Icon(
                                painter = painterResource(id = R.drawable.bookmark_flag_24),
                                contentDescription = null,
                                tint = iconTint
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selectedCategoryId == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = if (selectedCategoryId == null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    )
                    allCategories.forEach { category ->
                        AssistChip(
                            onClick = {
                                selectedCategoryId = category.categoryId
                                scope.launch { UiPrefs.setSelectedCategoryId(context, category.categoryId) }
                            },
                            label = { Text(category.emoji + " " + category.name) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (selectedCategoryId == category.categoryId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = if (selectedCategoryId == category.categoryId) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    when {
                        isLoading -> {
                            Log.d("NOTES_DEBUG", "Mostrando loader en NotesScreen")
                            // Indicador de carga inicial
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 4.dp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Cargando notas...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                        // Cambiado: usar filteredNotes para detectar vacío con filtro activo
                        filteredNotes.isEmpty() -> {
                            val emptyMessage = when {
                                filterType == "favorites" -> "Aún no tienes notas favoritas."
                                selectedCategoryId != null -> "No hay notas en esta categoría."
                                else -> "¡Crea tu primera nota!"
                            }
                            Log.d(
                                "NOTES_DEBUG",
                                "Mostrando EmptyNotesMessage; filterType=$filterType selectedCategoryId=$selectedCategoryId"
                            )
                            EmptyNotesMessage(
                                message = emptyMessage,
                                onAddNoteClick = onAddNote,
                                isFavorites = filterType == "favorites"
                            )
                        }
                        else -> {
                            Log.d(
                                "NOTES_DEBUG",
                                "Mostrando NotesGrid; count=${filteredNotes.size}"
                            )
                            NotesGrid(
                                notes = filteredNotes,
                                onNoteClick = { note ->
                                    if (selectedCategoryId != null) {
                                        navigateToNoteDetail(note.id, selectedCategoryId)
                                    } else {
                                        navigateToNoteDetail(note.id, -1)
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                                viewModel = viewModel,
                                snackbarHostState = snackbarHostState,
                                onShareFromCard = { note ->
                                    shareTarget = note
                                    scope.launch { shareSheetState.show() }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCategoryManager) {
        CategoryManagerDialog(
            categories = allCategories,
            onClose = { showCategoryManager = false },
            onSelectCategory = { catId: Int ->
                selectedCategoryId = catId
                showCategoryManager = false
            },
            onAddCategory = { name: String, emoji: String -> viewModel.insertCategory(Category(name = name, emoji = emoji)) },
            onEditCategory = { category: Category -> viewModel.updateCategory(category) },
            onDeleteCategory = { category: Category -> viewModel.deleteCategory(category) }
        )
    }

    // Hoja de compartir para opciones de compartir (Texto, Archivo, PDF, Cancelar)
    if (shareTarget != null) {
        ModalBottomSheet(
            onDismissRequest = { shareTarget = null },
            sheetState = shareSheetState
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Compartir nota", style = MaterialTheme.typography.titleMedium)
                val note = shareTarget!!
                Button(onClick = {
                    NoteActions.shareNoteContent(context, note.title, note.content)
                    shareTarget = null
                    scope.launch { snackbarHostState.showSnackbar("Compartida como texto") }
                }, modifier = Modifier.fillMaxWidth()) { Text("Texto plano") }
                Button(onClick = {
                    NoteActions.shareNoteFile(context, note.title, note.content, note.isMarkdownEnabled)
                    shareTarget = null
                    scope.launch { snackbarHostState.showSnackbar("Compartida como archivo") }
                }, modifier = Modifier.fillMaxWidth()) { Text("Archivo") }
                Button(onClick = {
                    NoteActions.shareNoteAsPdf(context, note.title, note.content)
                    shareTarget = null
                    scope.launch { snackbarHostState.showSnackbar("Compartida como PDF") }
                }, modifier = Modifier.fillMaxWidth()) { Text("PDF") }
                OutlinedButton(onClick = { shareTarget = null }, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotesGrid(
    notes: List<Note>,
    onNoteClick: (Note) -> Unit,
    modifier: Modifier = Modifier,
    pageSize: Int = 20,
    prefetchDistance: Int = 10,
    viewModel: NoteViewModel,
    snackbarHostState: SnackbarHostState,
    onShareFromCard: (Note) -> Unit
) {
    val configuration = LocalConfiguration.current
    val columns = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 3 else 2

    // Estado del grid y conteo de items a mostrar (paginación local)
    val gridState = rememberLazyStaggeredGridState()
    var itemsToShow by remember(notes) { mutableStateOf(min(pageSize, notes.size)) }

    // Calcular si debemos cargar más cuando el usuario se acerca al final de lo ya mostrado
    val shouldLoadMore by remember(notes, itemsToShow) {
        derivedStateOf {
            val visible = gridState.layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) return@derivedStateOf false
            val lastVisible = visible.maxOf { it.index }
            lastVisible >= itemsToShow - prefetchDistance && itemsToShow < notes.size
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            itemsToShow = min(itemsToShow + pageSize, notes.size)
        }
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp
    ) {
        items(
            items = notes.take(itemsToShow),
            key = { it.id },
            contentType = { _ -> "note" }
        ) { note ->
            NoteCard(
                note = note,
                onNoteClick = onNoteClick,
                onDoubleTapFavorite = { n ->
                    viewModel.toggleFavorite(n.id)
                    // Mostrar snackbar de confirmación
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        snackbarHostState.showSnackbar("Nota marcada como favorita")
                    }
                },
                onDeleteNote = { n -> viewModel.deleteNote(n) },
                onShowMessage = { msg ->
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        snackbarHostState.showSnackbar(msg)
                    }
                },
                onShareRequest = { n -> onShareFromCard(n) }
            )
        }
    }
}

@Composable
private fun EmptyNotesMessage(message: String, onAddNoteClick: () -> Unit, isFavorites: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val iconRes = if (isFavorites) R.drawable.sentiment_sad_24px else R.drawable.add_notes_24px
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clickable { if (!isFavorites) onAddNoteClick() },
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun CategoryManagerDialog(
    categories: List<Category>,
    onClose: () -> Unit,
    onSelectCategory: (Int) -> Unit,
    onAddCategory: (String, String) -> Unit,
    onEditCategory: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    var newCategoryName by remember { mutableStateOf("") }
    var editCategoryName by remember { mutableStateOf("") }
    // Estados para emojis
    var newCategoryEmoji by remember { mutableStateOf("📝") }
    var editCategoryEmoji by remember { mutableStateOf("📝") }

    val scrollState = rememberScrollState()

    // Lista de emojis sugeridos
    val emojiChoices = listOf(
        "📝","📚","💡","✅","📌","⭐","🔥","🏷️","🧠","🛒","🎯","🧹","💬","📅","🧪","📈","🛠️","🧭","✍️","🍽️","🏃","🛏️","🌱"
    )

    // Diálogo para agregar/editar emoji
    var showEmojiPickerForAdd by remember { mutableStateOf(false) }
    var showEmojiPickerForEdit by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Categorías") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 420.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .verticalScroll(scrollState)
                        .fillMaxWidth()
                ) {
                    categories.forEach { category ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(category.emoji + " " + category.name, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onSelectCategory(category.categoryId) }) {
                                Icon(Icons.Default.Check, contentDescription = "Seleccionar")
                            }
                            IconButton(onClick = {
                                categoryToEdit = category
                                editCategoryName = category.name
                                editCategoryEmoji = category.emoji
                                showEditDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar")
                            }
                            IconButton(onClick = {
                                categoryToDelete = category
                                showDeleteDialog = true
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                // Fila fija para agregar nueva categoría con emoji
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { raw ->
                            val noNewlines = raw.replace("\n", " ").replace("\r", " ")
                            val collapsed = noNewlines.replace(Regex("\\s+"), " ")
                            newCategoryName = collapsed.trim().take(200)
                        },
                        label = { Text("Nueva categoría") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val name = newCategoryName.replace(Regex("\\s+"), " ").trim()
                                if (name.isNotEmpty()) {
                                    onAddCategory(name.take(200), newCategoryEmoji)
                                    newCategoryName = ""
                                    newCategoryEmoji = "📝"
                                }
                            }
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    AssistChip(onClick = { showEmojiPickerForAdd = true }, label = { Text(newCategoryEmoji) })
                    IconButton(onClick = {
                        val name = newCategoryName.replace(Regex("\\s+"), " ").trim()
                        if (name.isNotEmpty()) {
                            onAddCategory(name.take(200), newCategoryEmoji)
                            newCategoryName = ""
                            newCategoryEmoji = "📝"
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text("Cerrar") }
        }
    )

    // Picker de emoji para agregar
    if (showEmojiPickerForAdd) {
        AlertDialog(
            onDismissRequest = { showEmojiPickerForAdd = false },
            title = { Text("Elige un emoji") },
            text = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    emojiChoices.forEach { e ->
                        Text(
                            text = e,
                            fontSize = 24.sp,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable {
                                    newCategoryEmoji = e
                                    showEmojiPickerForAdd = false
                                },
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEmojiPickerForAdd = false }) { Text("Cancelar") }
            }
        )
    }

    // Diálogo de edición de categoría con emoji
    if (showEditDialog && categoryToEdit != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar categoría") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editCategoryName,
                        onValueChange = { editCategoryName = it.replace("\n", " ").replace("\r", " ").replace(Regex("\\s+"), " ").trim().take(200) },
                        label = { Text("Nombre de la categoría") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Emoji:")
                        Spacer(Modifier.width(8.dp))
                        AssistChip(onClick = { showEmojiPickerForEdit = true }, label = { Text(editCategoryEmoji) })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    categoryToEdit?.let {
                        val clean = editCategoryName.replace(Regex("\\s+"), " ").trim()
                        if (clean.isNotBlank()) {
                            onEditCategory(it.copy(name = clean, emoji = editCategoryEmoji))
                            showEditDialog = false
                        }
                    }
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showEmojiPickerForEdit) {
        AlertDialog(
            onDismissRequest = { showEmojiPickerForEdit = false },
            title = { Text("Elige un emoji") },
            text = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    emojiChoices.forEach { e ->
                        Text(
                            text = e,
                            fontSize = 24.sp,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable {
                                    editCategoryEmoji = e
                                    showEmojiPickerForEdit = false
                                },
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEmojiPickerForEdit = false }) { Text("Cancelar") }
            }
        )
    }

    // Diálogo de confirmación de eliminación (sin cambios)
    if (showDeleteDialog && categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar categoría") },
            text = { Text("¿Estás seguro de que deseas eliminar la categoría '${categoryToDelete?.name}'?") },
            confirmButton = {
                TextButton(onClick = {
                    categoryToDelete?.let {
                        onDeleteCategory(it)
                        showDeleteDialog = false
                    }
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }
}
