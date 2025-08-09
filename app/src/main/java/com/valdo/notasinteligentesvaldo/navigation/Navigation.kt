package com.valdo.notasinteligentesvaldo.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
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

// Define duraciones de animación
private const val NAV_ANIM_DURATION = 300 // Reducido para menos parpadeo

@Composable
fun AppNavigation(
    viewModel: NoteViewModel,
) {
    val navController = rememberNavController()

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
                onAddNote = { navController.navigate("addNote") }
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
                onNoteSaved = { newNote ->
                    viewModel.insertNote(newNote)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        // --- Pantalla de Detalle de Nota ---
        composable(
            route = "noteDetail/{noteId}",
            arguments = listOf(navArgument("noteId") {
                type = NavType.IntType
            }),
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
    }
}