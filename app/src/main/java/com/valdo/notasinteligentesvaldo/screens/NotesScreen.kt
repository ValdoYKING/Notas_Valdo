package com.valdo.notasinteligentesvaldo.screens

import android.content.res.Configuration
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.* // Asegúrate que todos los imports necesarios estén
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // Asegúrate que todos los iconos estén
import androidx.compose.material3.*
import androidx.compose.runtime.* // Asegúrate que todos los imports de runtime estén
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel // Si no lo pasas como argumento
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState // Importar para obtener ruta actual
import com.valdo.notasinteligentesvaldo.components.NoteCard
import com.valdo.notasinteligentesvaldo.models.Note
import com.valdo.notasinteligentesvaldo.viewmodel.NoteViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotesScreen(
    viewModel: NoteViewModel,
    navController: NavController,
    filterType: String, // NUEVO: Parámetro para saber qué mostrar
    // Quitamos los valores por defecto de onAddNote y onNoteClick si siempre los proveemos desde AppNavigation
    // onAddNote: () -> Unit,
    // onNoteClick: (Note) -> Unit
) {
    // Lógica para obtener las notas correctas según el filtro
    // Usamos remember(filterType) para que el StateFlow correcto sea elegido cuando filterType cambie
    val notesToDisplayState = remember(filterType) {
        when (filterType) {
            "favorites" -> viewModel.favoriteNotes
            else -> viewModel.allNotes // "all" y cualquier otro caso muestran todas las notas
        }
    }.collectAsState() // Colecciona el Flow elegido

    val notesToDisplay = notesToDisplayState.value // El valor actual de la lista a mostrar

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Cargar datos iniciales o cuando el filtro cambie
    LaunchedEffect(filterType) {
        when (filterType) {
            "favorites" -> viewModel.loadFavorites()
            else -> viewModel.loadAllNotes() // Carga todas por defecto
        }
    }

    // Para saber qué item del drawer está seleccionado
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    // Extraer el filtro de la ruta actual para la selección del drawer
    val currentFilter = navBackStackEntry?.arguments?.getString("filterType") ?: "all"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                // Item Inicio
                NavigationDrawerItem(
                    label = { Text("Inicio") },
                    // Seleccionado si la ruta empieza con "notes" Y el filtro es "all"
                    selected = currentRoute?.startsWith("notes") == true && currentFilter == "all",
                    onClick = {
                        scope.launch { drawerState.close() }
                        // Navega a la ruta 'notes' especificando el filtro 'all'
                        navController.navigate("notes?filter=all") {
                            popUpTo(navController.graph.startDestinationId) // Limpia stack hasta el inicio
                            launchSingleTop = true // Evita duplicados de la pantalla de inicio
                        }
                        // No es necesario llamar a viewModel.loadAllNotes() aquí,
                        // el LaunchedEffect se encargará al cambiar la ruta/filtro.
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") }
                )
                // Item Favoritas
                NavigationDrawerItem(
                    label = { Text("Favoritas") },
                    // Seleccionado si la ruta empieza con "notes" Y el filtro es "favorites"
                    selected = currentRoute?.startsWith("notes") == true && currentFilter == "favorites",
                    onClick = {
                        scope.launch { drawerState.close() }
                        // Navega a la ruta 'notes' especificando el filtro 'favorites'
                        navController.navigate("notes?filter=favorites") {
                            launchSingleTop = true // Evita duplicados si ya estás en favoritas
                            // Opcional: popUpTo como en Inicio si quieres limpiar el stack también
                            // popUpTo(navController.graph.startDestinationId)
                        }
                        // No es necesario llamar a viewModel.loadFavorites() aquí,
                        // el LaunchedEffect se encargará.
                    },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Favoritas") }
                )
                // Item Categorías (ejemplo, si lo implementas)
                NavigationDrawerItem(
                    label = { Text("Categorías") },
                    selected = currentRoute == "categories", // Asumiendo una ruta "categories" separada
                    onClick = {
                        scope.launch { drawerState.close() }
                        // navController.navigate("categories") // Navegar a la ruta de categorías
                    },
                    icon = { Icon(Icons.Default.Menu, contentDescription = "Categorías") } // Cambiar icono si es necesario
                )
                HorizontalDivider() // Usar HorizontalDivider de Material 3
                // Item Ajustes
                NavigationDrawerItem(
                    label = { Text("Ajustes") },
                    selected = currentRoute == "settings", // Asumiendo una ruta "settings" separada
                    onClick = {
                        scope.launch { drawerState.close() }
                        // navController.navigate("settings") // Navegar a la ruta de ajustes
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") }
                )
            }
        }
    ) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navController.navigate("addNote") }, // Navega a la pantalla de añadir nota
                    modifier = Modifier.defaultMinSize(minWidth = 150.dp, minHeight = 56.dp),
                    containerColor = MaterialTheme.colorScheme.primary // Color del FAB
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Nueva nota :)",
                            style = MaterialTheme.typography.labelLarge, // Usar labelLarge para FAB texto
                            color = MaterialTheme.colorScheme.onPrimary // Color del texto sobre el FAB
                        )
                    }
                }
            },
            topBar = {
                TopAppBar(
                    // MODIFICADO: Título cambia según el filtro
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
                                // MODIFICADO: Refrescar según el filtro actual
                                when (filterType) {
                                    "favorites" -> viewModel.loadFavorites()
                                    else -> viewModel.loadAllNotes()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                // Usa Crossfade para animar el cambio entre la lista vacía y la llena
                Crossfade(
                    targetState = notesToDisplay.isEmpty(), // El estado que determina qué mostrar
                    label = "NotesGridCrossfade", // Etiqueta para debugging de animación
                    animationSpec = tween(durationMillis = 300) // Duración de la animación (ajustable)
                ) { isEmpty ->
                    if (isEmpty) {
                        // El estado cuando la lista está vacía
                        val emptyMessage = when(filterType) {
                            "favorites" -> "Aún no tienes notas favoritas."
                            else -> "¡Crea tu primera nota!"
                        }
                        EmptyNotesMessage(message = emptyMessage)
                    } else {
                        // El estado cuando la lista NO está vacía
                        NotesGrid(
                            notes = notesToDisplay,
                            onNoteClick = { note -> navController.navigate("noteDetail/${note.id}") }
                        )
                    }
                }
            }
        }
    }
}

// MODIFICADO: EmptyNotesMessage para aceptar un mensaje
@Composable
private fun EmptyNotesMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), // Añadir padding para que no esté pegado a los bordes
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                // Cambiar icono según el contexto podría ser útil, pero mantenemos uno genérico
                imageVector = Icons.Default.Warning, // Icono alternativo
                contentDescription = null, // Descripción es proporcionada por el texto
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) // Color más suave
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = message, // Usa el mensaje pasado como argumento
                style = MaterialTheme.typography.bodyLarge, // Quizás un estilo más adecuado
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotesGrid(notes: List<Note>, onNoteClick: (Note) -> Unit) {
    val configuration = LocalConfiguration.current
    val columns = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 3 else 2

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp
    ) {
        items(notes, key = { it.id }) { note ->
            NoteCard(note = note, onNoteClick = onNoteClick) // Asegúrate que NoteCard esté bien definido
        }
    }
}