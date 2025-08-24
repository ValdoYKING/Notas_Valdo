package com.valdo.notasinteligentesvaldo.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.valdo.notasinteligentesvaldo.screens.NoteDetailScreen
import com.valdo.notasinteligentesvaldo.screens.NoteFormScreen
import com.valdo.notasinteligentesvaldo.screens.NotesScreen
import com.valdo.notasinteligentesvaldo.viewmodel.NoteViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.valdo.notasinteligentesvaldo.screens.ExternalViewerScreen
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.valdo.notasinteligentesvaldo.data.UiPrefs
import kotlinx.coroutines.flow.first

// Define duraciones de animación
private const val NAV_ANIM_DURATION = 300 // Reducido para menos parpadeo

@Composable
fun AppNavigation(
    viewModel: NoteViewModel,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Restaurar última ruta si existe (sustituye el inicio por defecto)
    LaunchedEffect(Unit) {
        val saved = UiPrefs.lastRouteFlow(context).first()
        if (!saved.isNullOrBlank() && saved != "notes?filter=all") {
            navController.navigate(saved) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // Guardar la ruta actual cada vez que cambie el destino
    LaunchedEffect(Unit) {
        navController.currentBackStackEntryFlow.collectLatest { entry ->
            val pattern = entry.destination.route ?: return@collectLatest
            val route = when {
                pattern.startsWith("noteDetail/") || pattern.startsWith("noteDetail") -> {
                    val id = entry.arguments?.getInt("noteId") ?: return@collectLatest
                    val cat = entry.arguments?.getInt("categoryId") ?: -1
                    "noteDetail/$id?categoryId=$cat"
                }
                pattern.startsWith("notes") -> {
                    val filter = entry.arguments?.getString("filterType") ?: "all"
                    "notes?filter=$filter"
                }
                else -> pattern
            }
            scope.launch { UiPrefs.setLastRoute(context, route) }
        }
    }

    // NUEVO: Navegar al detalle cuando se importe una nota externamente
    LaunchedEffect(Unit) {
        viewModel.importedNoteId.collectLatest { id ->
            navController.navigate("noteDetail/$id?categoryId=-1")
        }
    }
    // NUEVO: Navegar al visor efímero cuando llegue un documento externo
    LaunchedEffect(Unit) {
        viewModel.openExternalViewer.collectLatest {
            navController.navigate("viewer")
        }
    }

    NavHost(
        navController = navController,
        startDestination = "notes?filter=all"
    ) {

        // --- Pantalla de Notas (Lista) ---
        composable(
            route = "notes?filter={filterType}",
            arguments = listOf(navArgument("filterType") {
                type = NavType.StringType
                defaultValue = "all"
                nullable = false
            }),
            // Eliminar todas las transiciones para evitar parpadeo
            enterTransition = { null },
            exitTransition = { null },
            popEnterTransition = { null },
            popExitTransition = { null }
        ) { backStackEntry ->
            val filterType = backStackEntry.arguments?.getString("filterType") ?: "all"
            NotesScreen(
                viewModel = viewModel,
                navController = navController,
                filterType = filterType,
                onAddNote = {
                    viewModel.clearCurrentNote() // Limpia la nota actual antes de crear una nueva
                    navController.navigate("addNote")
                }
            )
        }

        // --- Pantalla de Añadir/Editar Nota (Formulario) ---
        composable(
            route = "addNote",
            enterTransition = {
                fadeIn(animationSpec = tween(NAV_ANIM_DURATION))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(NAV_ANIM_DURATION))
            }
        ) {
            NoteFormScreen(
                onNoteSaved = { newNote, selectedCategories ->
                    viewModel.viewModelScope.launch {
                        val noteId = viewModel.insertNoteAndGetId(newNote).toInt()
                        selectedCategories.forEach { catId ->
                            viewModel.addCategoryToNote(noteId, catId)
                        }
                        viewModel.loadAllNotes()
                        viewModel.loadAllCategories()
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }

        // --- Pantalla de Detalle de Nota ---
        composable(
            route = "noteDetail/{noteId}?categoryId={categoryId}",
            arguments = listOf(
                navArgument("noteId") { type = NavType.IntType },
                navArgument("categoryId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            ),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(NAV_ANIM_DURATION)
                ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION))
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(NAV_ANIM_DURATION)
                ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION))
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(NAV_ANIM_DURATION)
                ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION))
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(NAV_ANIM_DURATION)
                ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION))
            }
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getInt("noteId")
            val categoryId = backStackEntry.arguments?.getInt("categoryId") ?: -1
            if (noteId != null) {
                NoteDetailScreen(
                    noteId = noteId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    navController = navController,
                    categoryId = categoryId
                )
            } else {
                navController.popBackStack()
            }
        }

        // NUEVO: pantalla de visor externo (no persiste)
        composable(
            route = "viewer",
            enterTransition = { fadeIn(animationSpec = tween(NAV_ANIM_DURATION)) },
            exitTransition = { fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) }
        ) {
            ExternalViewerScreen(
                viewModel = viewModel,
                onBack = {
                    viewModel.clearExternalDocument()
                    navController.popBackStack()
                }
            )
        }
    }
}