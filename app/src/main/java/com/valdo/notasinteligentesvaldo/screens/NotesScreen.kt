package com.valdo.notasinteligentesvaldo.screens

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.SearchBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.valdo.notasinteligentesvaldo.components.NoteCard
import com.valdo.notasinteligentesvaldo.models.Note
import com.valdo.notasinteligentesvaldo.viewmodel.NoteViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotesScreen(
    viewModel: NoteViewModel,
    navController: NavController,
    onAddNote: () -> Unit = { navController.navigate("addNote") },
    onNoteClick: (Note) -> Unit = { note -> navController.navigate("noteDetail/${note.id}") }
) {
    // Estados observables
    val notes by viewModel.allNotes.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    //Cargar notas al iniciar
    LaunchedEffect(Unit) {
        viewModel.loadAllNotes()
    }

//    LaunchedEffect(Unit) {
//        viewModel.getNotesWithNotifications().collect { notes ->
//            notes.forEach { note ->
//                scheduleNotification(note)
//            }
//        }
//    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                NavigationDrawerItem(
                    label = { Text("Inicio") },
                    selected = navController.currentDestination?.route == "notes",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("notes") { popUpTo("notes") }
                        viewModel.loadAllNotes()
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Favoritas") },
                    selected = navController.currentDestination?.route == "favorites",
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate("favorites")
                            viewModel.loadFavorites()
                        }
                    },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Categorías") },
                    selected = navController.currentDestination?.route == "categories",
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate("categories")
                            viewModel.loadFavorites()
                        }
                    },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) }
                )
                androidx.compose.material3.HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Ajustes") },
                    selected = navController.currentDestination?.route == "settings",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("settings")
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) }
                )

            }
        }
    ) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onAddNote,
                    modifier = Modifier.size(70.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar nota")
                }
            },
            topBar = {
                TopAppBar(
                    title = { Text("Mis Notas") },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } }
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Abrir menú")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.loadAllNotes() }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                        }
                    }
                )}
        )  { padding ->
            Box(modifier = Modifier.padding(padding)) {
                if (notes.isEmpty()) {
                    Text(
                        "¡Crea tu primera nota!",
                        modifier = Modifier.fillMaxSize().wrapContentSize(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    NotesGrid(notes = notes, onNoteClick = onNoteClick)
                }
            }
        }
    }
}

@Composable
private fun EmptyNotesMessage() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(),
        contentAlignment = Center
    ) {
        Column(horizontalAlignment = CenterHorizontally) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Notas vacías",
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "¡Crea tu primera nota!",
                style = MaterialTheme.typography.headlineSmall
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
            NoteCard(note, onNoteClick)
        }
    }
}