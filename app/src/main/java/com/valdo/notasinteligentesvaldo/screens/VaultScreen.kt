package com.valdo.notasinteligentesvaldo.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.valdo.notasinteligentesvaldo.R
import com.valdo.notasinteligentesvaldo.data.UiPrefs
import com.valdo.notasinteligentesvaldo.models.Category
import com.valdo.notasinteligentesvaldo.models.Note
import com.valdo.notasinteligentesvaldo.viewmodel.NoteViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla de la Bóveda - Muestra notas secretas con la misma UI que NotesScreen
 * pero solo para notas con isSecret = true.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: NoteViewModel,
    navController: NavController,
    filterType: String = "all"
) {
    val scope = rememberCoroutineScope()

    // Estados de notas secretas
    val allSecretNotesState by viewModel.secretNotes.collectAsState()
    val favoriteSecretNotesState by viewModel.favoriteSecretNotes.collectAsState()
    val notesByCategory = viewModel.secretNotesWithCategories.collectAsState().value
    val allCategories by viewModel.allCategories.collectAsState()

    // Estados locales
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var showCategoryManager by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Determinar qué notas mostrar
    val notesToDisplay = when (filterType) {
        "favorites" -> favoriteSecretNotesState
        else -> allSecretNotesState
    }

    // Filtrado por categoría
    val filteredNotes by remember(selectedCategoryId, notesToDisplay, notesByCategory) {
        derivedStateOf {
            if (selectedCategoryId != null) {
                notesByCategory.filter { it.categories.any { c -> c.categoryId == selectedCategoryId } }.map { it.note }
            } else notesToDisplay
        }
    }

    // Helper para navegar a detalle
    fun navigateToNoteDetail(noteId: Int, categoryId: Int?) {
        val cat = categoryId ?: -1
        navController.navigate("noteDetail/$noteId?categoryId=$cat") {
            launchSingleTop = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Bóveda 🔐") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Botón para gestionar categorías
                    IconButton(onClick = { showCategoryManager = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Categorías")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Navegar a crear nota secreta
                    navController.navigate("addSecretNote")
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar nota secreta")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Barra de categorías
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {
                        selectedCategoryId = null
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
                        containerColor = if (selectedCategoryId == null)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = if (selectedCategoryId == null)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                )

                allCategories.forEach { category ->
                    val isSelected = selectedCategoryId == category.categoryId
                    AssistChip(
                        onClick = { selectedCategoryId = category.categoryId },
                        label = { Text("${category.emoji} ${category.name}") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = if (isSelected)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            // Grid de notas
            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyVaultMessage(
                        message = when {
                            filterType == "favorites" -> "No hay notas favoritas en la bóveda"
                            selectedCategoryId != null -> "No hay notas secretas en esta categoría"
                            else -> "No hay notas en la bóveda.\nToca + para agregar una nota secreta."
                        },
                        onAddNoteClick = {
                            navController.navigate("addSecretNote")
                        }
                    )
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        VaultNoteCard(
                            note = note,
                            onClick = {
                                navigateToNoteDetail(
                                    note.id,
                                    selectedCategoryId
                                )
                            },
                            onToggleFavorite = {
                                scope.launch {
                                    viewModel.toggleFavorite(note.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Diálogo de gestión de categorías
    if (showCategoryManager) {
        CategoryManagerDialog(
            categories = allCategories,
            onClose = { showCategoryManager = false },
            onSelectCategory = { catId ->
                selectedCategoryId = catId
                showCategoryManager = false
            },
            onAddCategory = { name, emoji ->
                scope.launch {
                    viewModel.insertCategory(Category(name = name, emoji = emoji))
                }
            },
            onEditCategory = { category ->
                scope.launch {
                    viewModel.updateCategory(category)
                }
            },
            onDeleteCategory = { category ->
                scope.launch {
                    viewModel.deleteCategory(category)
                    snackbarHostState.showSnackbar("Categoría eliminada")
                }
            }
        )
    }
}

@Composable
fun VaultNoteCard(
    note: Note,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (note.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (note.isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                        tint = if (note.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = note.content,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Fecha
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            Text(
                text = dateFormat.format(Date(note.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyVaultMessage(
    message: String,
    onAddNoteClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .clickable { onAddNoteClick() },
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

