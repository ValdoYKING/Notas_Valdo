package com.valdo.notasinteligentesvaldo.navigation

import androidx.compose.animation.AnimatedContentTransitionScope // Importar para slideInto/slideOutOf
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.valdo.notasinteligentesvaldo.screens.NoteDetailScreen
import com.valdo.notasinteligentesvaldo.screens.NoteFormScreen
import com.valdo.notasinteligentesvaldo.screens.NotesScreen
import com.valdo.notasinteligentesvaldo.viewmodel.NoteViewModel

// Define duraciones de animación (opcional, para consistencia)
private const val NAV_ANIM_DURATION = 400 // milisegundos

@Composable
fun AppNavigation(
    viewModel: NoteViewModel,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "notes", // Ruta inicial
        // Aplicar transiciones por defecto si se desea (pueden ser anuladas por composable)
        // enterTransition = { fadeIn(animationSpec = tween(NAV_ANIM_DURATION)) },
        // exitTransition = { fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) }
    ) {

        // --- Pantalla de Notas (Lista) ---
        composable(
            route = "notes?filter={filterType}",
            arguments = listOf(navArgument("filterType") {
                type = NavType.StringType
                defaultValue = "all"
                nullable = false
            }),
            // Transiciones para ENTRAR a NotesScreen desde otra pantalla (ej: Detail)
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right, // Entra desde la derecha
                    animationSpec = tween(NAV_ANIM_DURATION)
                ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION))
            },
            // Transiciones para SALIR de NotesScreen hacia otra pantalla (ej: Detail)
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left, // Sale hacia la izquierda
                    animationSpec = tween(NAV_ANIM_DURATION)
                ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION))
            },
            // Transiciones cuando se vuelve a NotesScreen usando popBackStack (ej: desde Detail)
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(NAV_ANIM_DURATION)
                ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION))
            },
            // Transiciones cuando NotesScreen es eliminada del stack al volver atrás
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(NAV_ANIM_DURATION)
                ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION))
            }
            // NOTA: Las transiciones entre 'notes?filter=all' y 'notes?filter=favorites'
            // usualmente no usan estas animaciones porque no es un cambio de destino completo
            // en el backstack, sino una actualización. El Crossfade dentro de NotesScreen ayuda aquí.
        ) { backStackEntry ->
            val filterType = backStackEntry.arguments?.getString("filterType") ?: "all"
            NotesScreen(
                viewModel = viewModel,
                navController = navController,
                filterType = filterType
            )
        }

        // --- Pantalla de Añadir/Editar Nota (Formulario) ---
        composable(
            route = "addNote",
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left, // Entra desde la izquierda
                    animationSpec = tween(NAV_ANIM_DURATION)
                ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION))
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right, // Sale hacia la derecha
                    animationSpec = tween(NAV_ANIM_DURATION)
                ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION))
            },
            popEnterTransition = { // Al volver de una pantalla superior (si la hubiera)
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(NAV_ANIM_DURATION)
                ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION))
            },
            popExitTransition = { // Al presionar back desde esta pantalla
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(NAV_ANIM_DURATION)
                ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION))
            }
        ) {
            NoteFormScreen(
                onNoteSaved = { newNote ->
                    viewModel.insertNote(newNote)
                    navController.popBackStack() // Vuelve a la lista después de guardar
                },
                onBack = { navController.popBackStack() }
            )
        }

        // --- Pantalla de Detalle de Nota ---
        composable(
            route = "noteDetail/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.IntType }),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left, // Entra desde la izquierda
                    animationSpec = tween(NAV_ANIM_DURATION)
                ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION))
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right, // Sale hacia la derecha
                    animationSpec = tween(NAV_ANIM_DURATION)
                ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION))
            },
            popEnterTransition = { // Al volver de una pantalla superior (si la hubiera)
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(NAV_ANIM_DURATION)
                ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION))
            },
            popExitTransition = { // Al presionar back desde esta pantalla
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(NAV_ANIM_DURATION)
                ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION))
            }
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getInt("noteId")
            if (noteId != null) {
                NoteDetailScreen(
                    noteId = noteId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            } else {
                navController.popBackStack()
            }
        }

        // Añade otras rutas (categories, settings) con sus transiciones si es necesario
    }
}