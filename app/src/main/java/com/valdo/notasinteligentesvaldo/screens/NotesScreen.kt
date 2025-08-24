package com.valdo.notasinteligentesvaldo.screens

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import com.valdo.notasinteligentesvaldo.models.NoteWithCategories
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    var isRefreshing by remember { mutableStateOf(false) }
    var showCategoryManager by remember { mutableStateOf(false) }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }

    val context = LocalContext.current

    // Cargar preferencia de categoría seleccionada al iniciar
    LaunchedEffect(Unit) {
        val savedCat = UiPrefs.selectedCategoryIdFlow(context).first()
        if (savedCat != null) {
            selectedCategoryId = savedCat
            viewModel.getNotesByCategoryId(savedCat)
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
    val scope = rememberCoroutineScope()

    // Cargar datos al inicio y manejar estado de carga
    // Eliminado: no relanzar colecciones ni usar delays artificiales; el ViewModel ya colecta en init
    // La pantalla se considerará "cargando" hasta la primera emisión de cualquiera de las listas

    // Detectar cuando hay datos por primera vez y bajar el loader inicial
    LaunchedEffect(allNotesState, favoriteNotesState) {
        if (isLoading) isLoading = false
        if (isRefreshing) {
            // Pequeño delay opcional para feedback visual al usuario
            kotlinx.coroutines.delay(150)
            isRefreshing = false
        }
    }

    // Filtrado por categoría (debe ir antes del Scaffold)
    val filteredNotes by remember(selectedCategoryId, notesToDisplay, notesByCategory) {
        derivedStateOf {
            if (selectedCategoryId != null) {
                notesByCategory.filter { it.categories.any { c -> c.categoryId == selectedCategoryId } }.map { it.note }
            } else notesToDisplay
        }
    }

    // Precalcular categorías por nota para evitar búsquedas repetidas O(n) en cada item
    val categoriesByNoteId by remember(notesByCategory) {
        derivedStateOf {
            notesByCategory.associate { it.note.id to it.categories }
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
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
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
                        IconButton(
                            onClick = {
                                // Evitar relanzar colecciones en el ViewModel; solo mostrar feedback
                                if (!isRefreshing) isRefreshing = true
                            }
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier
                .padding(padding)
                .fillMaxSize()
            ) {
                // Filtro visual de categorías SIEMPRE ARRIBA
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {
                            selectedCategoryId = null
                            viewModel.loadAllNotes()
                            scope.launch { UiPrefs.setSelectedCategoryId(context, null) }
                        },
                        label = { Text("Todas") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selectedCategoryId == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = if (selectedCategoryId == null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    )
                    allCategories.forEach { category ->
                        AssistChip(
                            onClick = {
                                selectedCategoryId = category.categoryId
                                viewModel.getNotesByCategoryId(category.categoryId)
                                scope.launch { UiPrefs.setSelectedCategoryId(context, category.categoryId) }
                            },
                            label = { Text(category.name) },
                            leadingIcon = { Icon(painterResource(id = R.drawable.category_24px), contentDescription = null) },
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
                        notesToDisplay.isEmpty() -> {
                            // Mensaje cuando no hay notas
                            val emptyMessage = if (filterType == "favorites") {
                                "Aún no tienes notas favoritas."
                            } else {
                                "¡Crea tu primera nota!"
                            }
                            EmptyNotesMessage(
                                message = emptyMessage,
                                onAddNoteClick = onAddNote,
                                isFavorites = filterType == "favorites"
                            )
                        }
                        else -> {
                            // Lista de notas with overlay de carga si se está refrescando
                            Box {
                                NotesGrid(
                                    notes = filteredNotes,
                                    onNoteClick = { note ->
                                        if (selectedCategoryId != null) {
                                            navController.navigate("noteDetail/${note.id}?categoryId=${selectedCategoryId}")
                                        } else {
                                            navController.navigate("noteDetail/${note.id}?categoryId=-1")
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                    notesWithCategories = notesByCategory,
                                    categoriesByNoteId = categoriesByNoteId
                                )

                                // Overlay de carga transparente durante refresh
                                if (isRefreshing) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
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
                viewModel.getNotesByCategoryId(catId)
                showCategoryManager = false
            },
            onAddCategory = { name: String -> viewModel.insertCategory(Category(name = name)) },
            onEditCategory = { category: Category -> viewModel.updateCategory(category) },
            onDeleteCategory = { category: Category -> viewModel.deleteCategory(category) }
        )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotesGrid(
    notes: List<Note>,
    onNoteClick: (Note) -> Unit,
    modifier: Modifier = Modifier,
    notesWithCategories: List<NoteWithCategories> = emptyList(),
    categoriesByNoteId: Map<Int, List<Category>> = emptyMap()
) {
    val configuration = LocalConfiguration.current
    val columns = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 3 else 2

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp
    ) {
        items(
            items = notes,
            key = { it.id },
            contentType = { _ -> "note" }
        ) { note ->
            val categories = categoriesByNoteId[note.id] ?: emptyList()
            NoteCard(note = note, categories = categories, onNoteClick = onNoteClick)
        }
    }
}

@Composable
fun CategoryManagerDialog(
    categories: List<Category>,
    onClose: () -> Unit,
    onSelectCategory: (Int) -> Unit,
    onAddCategory: (String) -> Unit,
    onEditCategory: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    var newCategoryName by remember { mutableStateOf("") }
    var editCategoryName by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Categorías") },
        text = {
            // Limitar altura para que el campo de nueva categoría sea visible; lista desplazable arriba
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 420.dp)
            ) {
                // Lista desplazable de categorías
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
                            Text(category.name, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onSelectCategory(category.categoryId) }) {
                                Icon(Icons.Default.Check, contentDescription = "Seleccionar")
                            }
                            IconButton(onClick = {
                                categoryToEdit = category
                                editCategoryName = category.name
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
                // Fila fija para agregar nueva categoría
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Nueva categoría") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        if (newCategoryName.isNotBlank()) {
                            onAddCategory(newCategoryName)
                            newCategoryName = ""
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

    // Diálogo de edición de categoría
    if (showEditDialog && categoryToEdit != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar categoría") },
            text = {
                OutlinedTextField(
                    value = editCategoryName,
                    onValueChange = { editCategoryName = it },
                    label = { Text("Nombre de la categoría") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    categoryToEdit?.let {
                        if (editCategoryName.isNotBlank()) {
                            onEditCategory(it.copy(name = editCategoryName))
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

    // Diálogo de confirmación de eliminación
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
