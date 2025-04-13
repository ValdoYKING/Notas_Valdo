package com.valdo.notasinteligentesvaldo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import com.valdo.notasinteligentesvaldo.screens.NoteDetailScreen
import com.valdo.notasinteligentesvaldo.screens.NoteFormScreen
import com.valdo.notasinteligentesvaldo.screens.NotesScreen
import com.valdo.notasinteligentesvaldo.viewmodel.NoteViewModel

@Composable
fun AppNavigation(
    viewModel: NoteViewModel, // Recibimos el ViewModel directamente
) {
    val navController = rememberNavController()

    NavHost(navController, startDestination = "notes") {
        composable("notes") {
            NotesScreen(
                viewModel = viewModel, // Pasamos el ViewModel inyectado
                navController = navController
            )
        }
        composable("addNote") {
            NoteFormScreen(
                onNoteSaved = { newNote ->
                    viewModel.insertNote(newNote)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("noteDetail/{noteId}") { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")?.toIntOrNull()
            if (noteId != null) {
                NoteDetailScreen(
                    noteId = noteId,
                    viewModel = viewModel, // Pasamos el ViewModel
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}