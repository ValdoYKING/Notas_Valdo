package com.valdo.notasinteligentesvaldo.screens

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.valdo.notasinteligentesvaldo.components.NoteCard
import com.valdo.notasinteligentesvaldo.models.Note
import com.valdo.notasinteligentesvaldo.viewmodel.NoteViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.res.painterResource
import com.valdo.notasinteligentesvaldo.R

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

    // Estado de carga
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Determinar qué notas mostrar
    val notesToDisplay = when (filterType) {
        "favorites" -> favoriteNotesState
        else -> allNotesState
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Cargar datos al inicio y manejar estado de carga
    LaunchedEffect(Unit) {
        isLoading = true
        viewModel.loadAllNotes()
        viewModel.loadFavorites()
        // Simular tiempo mínimo de carga para mostrar el indicador
        kotlinx.coroutines.delay(500)
        isLoading = false
    }

    // Detectar cuando se termina de cargar después de un refresh
    LaunchedEffect(allNotesState, favoriteNotesState) {
        if (isRefreshing) {
            kotlinx.coroutines.delay(300) // Pequeño delay para mostrar que se refrescó
            isRefreshing = false
        }
    }

    val currentFilter = filterType

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
                        scope.launch { drawerState.close() }
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
                        scope.launch { drawerState.close() }
                        if (currentFilter != "favorites") {
                            navController.navigate("notes?filter=favorites")
                        }
                    },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Favoritas") }
                )
                // Item Categorías
                NavigationDrawerItem(
                    label = { Text("Categorías") },
                    selected = false,
                    onClick = {
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
                    onClick = { navController.navigate("addNote") },
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
                                isRefreshing = true
                                viewModel.loadAllNotes()
                                viewModel.loadFavorites()
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
            Box(
                modifier = Modifier
                    .padding(padding)
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
                        // Lista de notas con overlay de carga si se está refrescando
                        Box {
                            NotesGrid(
                                notes = notesToDisplay,
                                onNoteClick = { note -> navController.navigate("noteDetail/${note.id}") },
                                modifier = Modifier.fillMaxSize()
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
fun NotesGrid(notes: List<Note>, onNoteClick: (Note) -> Unit, modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current
    val columns = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 3 else 2

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp
    ) {
        items(notes, key = { it.id }) { note ->
            NoteCard(note = note, onNoteClick = onNoteClick)
        }
    }
}