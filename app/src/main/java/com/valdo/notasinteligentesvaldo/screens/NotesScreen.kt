package com.valdo.notasinteligentesvaldo.screens

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.valdo.notasinteligentesvaldo.util.NoteActions
import coil.compose.AsyncImage
import com.valdo.notasinteligentesvaldo.ui.avatar.AvatarContainer
import com.valdo.notasinteligentesvaldo.ui.avatar.avatarStyleFromId
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun NotesScreen(
    viewModel: NoteViewModel,
    navController: NavController,
    filterType: String,
    onAddNote: (Int?) -> Unit
) {
    // Estados de notas
    val allNotesState by viewModel.allNotes.collectAsState()
    val favoriteNotesState by viewModel.favoriteNotes.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val context = LocalContext.current
    val firstName by UiPrefs.firstNameFlow(context).collectAsState(initial = "")
    val profileUri by UiPrefs.profileImageUriFlow(context).collectAsState(initial = "")
    val avatarStyleId by UiPrefs.avatarStyleFlow(context).collectAsState(initial = "circle")

    // Estado de carga
    var isLoading by remember { mutableStateOf(true) }
    var showCategoryManager by remember { mutableStateOf(false) }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }

    // NUEVO: Estado para compartir
    var shareTarget by remember { mutableStateOf<Note?>(null) }
    val shareSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // NUEVO: Estado para selección múltiple (Contextual Top Action Bar)
    var selectedNoteIds by remember { mutableStateOf(setOf<Int>()) }
    val isSelectionMode = selectedNoteIds.isNotEmpty()

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

    val gridState = rememberLazyStaggeredGridState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            ) {
                // Header del Menú
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val avatarStyle = avatarStyleFromId(avatarStyleId)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            AvatarContainer(
                                style = avatarStyle,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (profileUri.isNotBlank()) {
                                    AsyncImage(
                                        model = profileUri,
                                        contentDescription = "Avatar",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text("U", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Column {
                            Text(
                                text = "Notas Inteligentes",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val displayName = firstName.trim()
                            val subtitleText = if (displayName.isNotBlank()) displayName else "Organiza tu día"
                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(12.dp))

                // Sección Organización
                Text(
                    text = "ORGANIZACIÓN",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
                )

                val itemColors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val itemModifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .padding(horizontal = 12.dp)

                // Item Inicio
                NavigationDrawerItem(
                    label = { Text("Mis Notas") },
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
                    icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                    colors = itemColors,
                    modifier = itemModifier
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
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Favoritas") },
                    colors = itemColors,
                    modifier = itemModifier
                )
                // Item Categorías
                NavigationDrawerItem(
                    label = { Text("Categorías") },
                    selected = showCategoryManager,
                    onClick = {
                        showCategoryManager = true
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Menu, contentDescription = "Categorías") },
                    colors = itemColors,
                    modifier = itemModifier
                )
                // Item Bóveda
                NavigationDrawerItem(
                    label = { Text("Bóveda") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("vaultAuth")
                    },
                    icon = { Icon(Icons.Default.Lock, contentDescription = "Bóveda") },
                    colors = itemColors,
                    modifier = itemModifier
                )

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(8.dp))

                // Sección Sistema
                Text(
                    text = "SISTEMA",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
                )

                // Item Ajustes
                NavigationDrawerItem(
                    label = { Text("Ajustes") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("settings")
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                    colors = itemColors,
                    modifier = itemModifier
                )
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                val isScrollingDown = remember {
                    derivedStateOf { gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 40 }
                }.value

                if (!isScrollingDown) {
                    ExtendedFloatingActionButton(
                        onClick = { onAddNote(selectedCategoryId) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Nueva nota") }
                    )
                } else {
                    SmallFloatingActionButton(
                        onClick = { onAddNote(selectedCategoryId) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = ShapeDefaults.Medium
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Nueva nota")
                    }
                }
            },
            topBar = {
                if (isSelectionMode) {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        title = { Text("${selectedNoteIds.size} seleccionada(s)") },
                        navigationIcon = {
                            IconButton(onClick = { selectedNoteIds = emptySet() }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancelar selección")
                            }
                        },
                        actions = {
                            // Copiar
                            IconButton(onClick = {
                                val contentToCopy = filteredNotes.filter { selectedNoteIds.contains(it.id) }.joinToString("\n\n") { "${it.title}\n${it.content}" }
                                NoteActions.copyToClipboard(context, contentToCopy)
                                scope.launch { snackbarHostState.showSnackbar("Notas copiadas al portapapeles") }
                                selectedNoteIds = emptySet()
                            }) {
                                Icon(painter = painterResource(id = R.drawable.copy_24), contentDescription = "Copiar")
                            }
                            // Compartir
                            IconButton(onClick = {
                                val contentToShare = filteredNotes.filter { selectedNoteIds.contains(it.id) }.joinToString("\n\n") { "${it.title}\n${it.content}" }
                                NoteActions.shareNoteContent(context, "Notas seleccionadas", contentToShare)
                                selectedNoteIds = emptySet()
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Compartir")
                            }
                            // Eliminar
                            IconButton(onClick = {
                                scope.launch {
                                    filteredNotes.filter { selectedNoteIds.contains(it.id) }.forEach { note ->
                                        viewModel.deleteNote(note)
                                    }
                                    snackbarHostState.showSnackbar("Notas eliminadas")
                                    selectedNoteIds = emptySet()
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                } else {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        ),
                        title = {
                            val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
                            val greeting = when {
                                hour < 12 -> "Buenos días"
                                hour < 19 -> "Buenas tardes"
                                else -> "Buenas noches"
                            }
                            val displayName = firstName.trim()
                            val titleText = if (displayName.isNotBlank()) "$greeting, $displayName" else greeting
                            Column {
                                Text(titleText, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = "${filteredNotes.size} notas pendientes · Organiza tu día",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } }
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "Abrir menú")
                            }
                        },
                        actions = {
                            val avatarStyle = avatarStyleFromId(avatarStyleId)
                            IconButton(onClick = { navController.navigate("settings/profile") }) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                ) {
                                    AvatarContainer(
                                        style = avatarStyle,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        if (profileUri.isNotBlank()) {
                                            AsyncImage(
                                                model = profileUri,
                                                contentDescription = "Avatar",
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Text("U", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // Barra de categorías con surfaceContainer y botón [+] al final
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
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
                            containerColor = if (selectedCategoryId == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
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
                                containerColor = if (selectedCategoryId == category.categoryId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                                labelColor = if (selectedCategoryId == category.categoryId) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                    // Chip [+] para agregar categoría
                    AssistChip(
                        onClick = { showCategoryManager = true },
                        label = { Text("+") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    when {
                        isLoading -> {
                            Log.d("NOTES_DEBUG", "Mostrando loader en NotesScreen")
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
                                onAddNoteClick = { onAddNote(selectedCategoryId) },
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
                                },
                                gridState = gridState,
                                selectedNoteIds = selectedNoteIds,
                                isSelectionMode = isSelectionMode,
                                onSelectToggle = { note ->
                                    selectedNoteIds = if (selectedNoteIds.contains(note.id)) {
                                        selectedNoteIds - note.id
                                    } else {
                                        selectedNoteIds + note.id
                                    }
                                },
                                onLongPress = { note ->
                                    selectedNoteIds = if (selectedNoteIds.contains(note.id)) {
                                        selectedNoteIds - note.id
                                    } else {
                                        selectedNoteIds + note.id
                                    }
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
            onAddCategory = { name: String, emoji: String ->
                viewModel.insertCategory(Category(name = name, emoji = emoji, isSecret = false))
            },
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
    onShareFromCard: (Note) -> Unit,
    gridState: LazyStaggeredGridState,
    selectedNoteIds: Set<Int>,
    isSelectionMode: Boolean,
    onSelectToggle: (Note) -> Unit,
    onLongPress: (Note) -> Unit
) {
    val configuration = LocalConfiguration.current
    val columns = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 3 else 2

    // Conteo de items a mostrar (paginación local)
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
                onShareRequest = { n -> onShareFromCard(n) },
                isSelected = selectedNoteIds.contains(note.id),
                isSelectionMode = isSelectionMode,
                onSelectToggle = onSelectToggle,
                onLongPress = onLongPress
            )
        }
    }
}

@Composable
private fun EmptyNotesMessage(message: String, onAddNoteClick: () -> Unit, isFavorites: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val iconRes = if (isFavorites) R.drawable.sentiment_sad_24px else R.drawable.add_notes_24px
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { if (!isFavorites) onAddNoteClick() },
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tus notas aparecerán aquí organizadas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (!isFavorites) {
                Spacer(Modifier.height(24.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "¡Crea tu primera nota!",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Flecha hacia abajo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
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
    var newCategoryEmoji by remember { mutableStateOf("📝") }
    var editCategoryEmoji by remember { mutableStateOf("📝") }

    val scrollState = rememberScrollState()

    val emojiChoices = listOf(
        "📝","📚","💡","✅","📌","⭐","🔥","🏷️","🧠","🛒","🎯","🧹","💬","📅","🧪","📈","🛠️","🧭","✍️","🍽️","🏃","🛏️","🌱"
    )

    var showEmojiPickerForAdd by remember { mutableStateOf(false) }
    var showEmojiPickerForEdit by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onClose,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                "Gestión de Categorías",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp, max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Categorías existentes",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Column(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .verticalScroll(scrollState)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (categories.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay categorías creadas aún",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        categories.forEach { category ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = category.emoji,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(
                                        onClick = { onSelectCategory(category.categoryId) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Seleccionar", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(
                                        onClick = {
                                            categoryToEdit = category
                                            editCategoryName = category.name
                                            editCategoryEmoji = category.emoji
                                            showEditDialog = true
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(
                                        onClick = {
                                            categoryToDelete = category
                                            showDeleteDialog = true
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                Text(
                    text = "Nueva categoría",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AssistChip(
                        onClick = { showEmojiPickerForAdd = true },
                        label = { Text(newCategoryEmoji, fontSize = 18.sp) },
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { raw ->
                            val noNewlines = raw.replace("\n", " ").replace("\r", " ")
                            val collapsed = noNewlines.replace(Regex("\\s+"), " ")
                            newCategoryName = collapsed.trim().take(200)
                        },
                        label = { Text("Nombre") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
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
                    Button(
                        onClick = {
                            val name = newCategoryName.replace(Regex("\\s+"), " ").trim()
                            if (name.isNotEmpty()) {
                                onAddCategory(name.take(200), newCategoryEmoji)
                                newCategoryName = ""
                                newCategoryEmoji = "📝"
                            }
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Crear")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text("Cerrar") }
        }
    )

    // Picker de emoji para agregar (con campo de texto personalizado)
    if (showEmojiPickerForAdd) {
        AlertDialog(
            onDismissRequest = { showEmojiPickerForAdd = false },
            title = { Text("Elige o escribe un emoji") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    // Campo de texto para emoji personalizado
                    OutlinedTextField(
                        value = newCategoryEmoji,
                        onValueChange = { text ->
                            // Limitar a máximo 10 caracteres Unicode (soporta emojis compuestos)
                            // Los emojis pueden usar de 1 a 7+ caracteres Unicode
                            if (text.length <= 10) {
                                newCategoryEmoji = text
                            }
                        },
                        label = { Text("Emoji") },
                        placeholder = { Text("📝") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 24.sp, textAlign = TextAlign.Center)
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Sugerencias rápidas:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(8.dp))

                    // Emojis sugeridos
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
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Si está vacío, usar el default
                        if (newCategoryEmoji.isBlank()) {
                            newCategoryEmoji = "📝"
                        }
                        showEmojiPickerForAdd = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
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
            title = { Text("Elige o escribe un emoji") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    // Campo de texto para emoji personalizado
                    OutlinedTextField(
                        value = editCategoryEmoji,
                        onValueChange = { text ->
                            // Limitar a máximo 10 caracteres Unicode (soporta emojis compuestos)
                            // Los emojis pueden usar de 1 a 7+ caracteres Unicode
                            if (text.length <= 10) {
                                editCategoryEmoji = text
                            }
                        },
                        label = { Text("Emoji") },
                        placeholder = { Text("📝") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 24.sp, textAlign = TextAlign.Center)
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Sugerencias rápidas:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(8.dp))

                    // Emojis sugeridos
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
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Si está vacío, usar el default
                        if (editCategoryEmoji.isBlank()) {
                            editCategoryEmoji = "📝"
                        }
                        showEmojiPickerForEdit = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
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
