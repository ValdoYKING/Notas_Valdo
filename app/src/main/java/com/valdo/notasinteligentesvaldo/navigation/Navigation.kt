package com.valdo.notasinteligentesvaldo.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptionsBuilder
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
import com.valdo.notasinteligentesvaldo.screens.SettingsScreen
import com.valdo.notasinteligentesvaldo.screens.ProfileSettingsScreen
import com.valdo.notasinteligentesvaldo.screens.ThemeSettingsScreen
import com.valdo.notasinteligentesvaldo.screens.StartActionSettingsScreen
import com.valdo.notasinteligentesvaldo.screens.VaultAuthScreen
import com.valdo.notasinteligentesvaldo.screens.VaultScreen
import kotlinx.coroutines.flow.firstOrNull

// Define duraciones de animación
private const val NAV_ANIM_DURATION = 300 // Reducido para menos parpadeo

@Composable
fun AppNavigation(
    viewModel: NoteViewModel,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun navigateSafe(route: String, builder: NavOptionsBuilder.() -> Unit = {}) {
        navController.navigate(route) {
            launchSingleTop = true
            restoreState = true
            builder()
        }
    }

    // NUEVO: conjunto de rutas seguras permitidas para restaurar
    val allowedRestoreRoutes = setOf(
        "notes?filter=all",
        "notes?filter=favorites",
        "addNote",
        "addNote?defaultCategoryId=-1",
        "settings",
        "settings/profile",
        "settings/theme",
        "settings/start_action",
        "vaultAuth",
        "vault?filter=all",
        "vault?filter=favorites",
        "vault",
        "addSecretNote"
    )

    // Si hay una nota pendiente de abrir desde notificación, priorizarla al arrancar
    LaunchedEffect(Unit) {
        val pendingId = viewModel.pendingNotificationNoteId.value
        if (pendingId != null && pendingId > 0) {
            android.util.Log.d("NAV_DEBUG", "Abriendo nota desde notificación pendiente: $pendingId")
            navigateSafe("noteDetail/$pendingId?categoryId=-1") {
                popUpTo(navController.graph.startDestinationId) { inclusive = false }
            }
            viewModel.clearPendingNotificationNoteId()
            return@LaunchedEffect
        }

        val saved = UiPrefs.lastRouteFlow(context).first()
        val isVaultUnlocked = viewModel.isVaultUnlocked.value

        var targetRoute: String? = null
        if (!saved.isNullOrBlank()) {
            if (saved.startsWith("vault") || saved == "addSecretNote") {
                targetRoute = if (isVaultUnlocked) saved else "vaultAuth"
            } else if (saved.startsWith("noteDetail/")) {
                // Si es detalle de nota, verificar si es secreta
                val noteId = saved.substringAfter("noteDetail/").substringBefore("?").toIntOrNull()
                if (noteId != null) {
                    val note = viewModel.allNotes.value.find { it.id == noteId }
                        ?: viewModel.secretNotes.value.find { it.id == noteId }
                    if (note?.isSecret == true && !isVaultUnlocked) {
                        targetRoute = "vaultAuth"
                    } else if (note != null) {
                        targetRoute = saved
                    }
                }
            } else if (saved in allowedRestoreRoutes || saved.startsWith("addNote?defaultCategoryId=")) {
                targetRoute = saved
            }
        }

        if (!targetRoute.isNullOrBlank() && targetRoute != "notes?filter=all") {
            android.util.Log.d("NAV_DEBUG", "Restaurando ruta guardada: $targetRoute")
            navigateSafe(targetRoute) {
                popUpTo(navController.graph.startDestinationId) { inclusive = false }
            }
        } else {
            val action = UiPrefs.startActionFlow(context).firstOrNull() ?: "notes"
            if (action == "quick_note") {
                android.util.Log.d("NAV_DEBUG", "StartAction=quick_note -> navegando a addNote")
                viewModel.clearCurrentNote()
                navigateSafe("addNote")
            } else {
                android.util.Log.d("NAV_DEBUG", "Sin ruta guardada válida, usando startDestination")
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
                pattern.startsWith("addNote") -> {
                    val defaultCategoryId = entry.arguments?.getInt("defaultCategoryId") ?: -1
                    "addNote?defaultCategoryId=$defaultCategoryId"
                }
                else -> pattern
            }
            if (route == "viewer") return@collectLatest
            android.util.Log.d("NAV_DEBUG", "Ruta actual: $route")
            scope.launch { UiPrefs.setLastRoute(context, route) }
        }
    }

    // NUEVO: Navegar al detalle cuando se importe una nota externamente
    LaunchedEffect(Unit) {
        viewModel.importedNoteId.collectLatest { id ->
            navigateSafe("noteDetail/$id?categoryId=-1")
        }
    }
    // NUEVO: Navegar al visor efímero cuando llegue un documento externo
    LaunchedEffect(Unit) {
        viewModel.openExternalViewer.collectLatest {
            navigateSafe("viewer")
        }
    }

    // Navegar al detalle de nota cuando llega un evento desde notificación
    LaunchedEffect(Unit) {
        viewModel.openNoteFromNotification.collectLatest { id ->
            navigateSafe("noteDetail/$id?categoryId=-1") {
                popUpTo(navController.graph.startDestinationId) { inclusive = false }
            }
        }
    }

    // Navegar a la lista desde notificación genérica
    LaunchedEffect(Unit) {
        viewModel.openNotesListFromNotification.collectLatest {
            navigateSafe("notes?filter=all") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
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
                onAddNote = { defaultCategoryId ->
                    viewModel.clearCurrentNote() // Limpia la nota actual antes de crear una nueva
                    val cat = defaultCategoryId ?: -1
                    navigateSafe("addNote?defaultCategoryId=$cat")
                }
            )
        }

        // --- Pantalla de Añadir/Editar Nota (Formulario) ---
        composable(
            route = "addNote?defaultCategoryId={defaultCategoryId}",
            arguments = listOf(
                navArgument("defaultCategoryId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            ),
            enterTransition = {
                fadeIn(animationSpec = tween(NAV_ANIM_DURATION))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(NAV_ANIM_DURATION))
            }
        ) { backStackEntry ->
            val defaultCategoryId = backStackEntry.arguments
                ?.getInt("defaultCategoryId")
                ?.takeIf { it >= 0 }
            NoteFormScreen(
                onNoteSaved = { newNote, selectedCategories ->
                    viewModel.viewModelScope.launch {
                        val noteId = viewModel.insertNoteAndGetId(newNote).toInt()
                        selectedCategories.forEach { catId ->
                            viewModel.addCategoryToNote(noteId, catId)
                        }
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() },
                viewModel = viewModel,
                defaultCategoryId = defaultCategoryId
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

        // NUEVO: Pantalla de Ajustes
        composable(
            route = "settings",
            enterTransition = { fadeIn(animationSpec = tween(NAV_ANIM_DURATION)) },
            exitTransition = { fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) }
        ) {
            SettingsScreen(navController = navController)
        }
        // NUEVO: Subpantalla perfil
        composable(
            route = "settings/profile",
            enterTransition = { fadeIn(animationSpec = tween(NAV_ANIM_DURATION)) },
            exitTransition = { fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) }
        ) {
            ProfileSettingsScreen(navController = navController)
        }
        // NUEVO: Subpantalla tema
        composable(
            route = "settings/theme",
            enterTransition = { fadeIn(animationSpec = tween(NAV_ANIM_DURATION)) },
            exitTransition = { fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) }
        ) {
            ThemeSettingsScreen(navController = navController)
        }
        // NUEVO: Subpantalla acción inicial
        composable(
            route = "settings/start_action",
            enterTransition = { fadeIn(animationSpec = tween(NAV_ANIM_DURATION)) },
            exitTransition = { fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) }
        ) {
            StartActionSettingsScreen(navController = navController)
        }

        // NUEVO: Pantalla de autenticación de la Bóveda
        composable(
            route = "vaultAuth",
            enterTransition = { fadeIn(animationSpec = tween(NAV_ANIM_DURATION)) },
            exitTransition = { fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) }
        ) {
            VaultAuthScreen(
                viewModel = viewModel,
                navController = navController
            )
        }

        // NUEVO: Pantalla de la Bóveda (notas secretas)
        composable(
            route = "vault?filter={filterType}",
            arguments = listOf(navArgument("filterType") {
                type = NavType.StringType
                defaultValue = "all"
                nullable = false
            }),
            enterTransition = { fadeIn(animationSpec = tween(NAV_ANIM_DURATION)) },
            exitTransition = { fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) }
        ) { backStackEntry ->
            val filterType = backStackEntry.arguments?.getString("filterType") ?: "all"
            VaultScreen(
                viewModel = viewModel,
                navController = navController,
                filterType = filterType
            )
        }

        // Ruta alternativa simplificada para la bóveda
        composable(
            route = "vault",
            enterTransition = { fadeIn(animationSpec = tween(NAV_ANIM_DURATION)) },
            exitTransition = { fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) }
        ) {
            VaultScreen(
                viewModel = viewModel,
                navController = navController,
                filterType = "all"
            )
        }

        // NUEVO: Agregar nota secreta
        composable(
            route = "addSecretNote",
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(NAV_ANIM_DURATION)
                ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION))
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(NAV_ANIM_DURATION)
                ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION))
            }
        ) {
            NoteFormScreen(
                onNoteSaved = { newNote, selectedCategories ->
                    viewModel.viewModelScope.launch {
                        // Crear nota con isSecret = true
                        val secretNote = newNote.copy(isSecret = true)
                        val noteId = viewModel.insertNoteAndGetId(secretNote).toInt()
                        selectedCategories.forEach { catId ->
                            viewModel.addCategoryToNote(noteId, catId)
                        }
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() },
                viewModel = viewModel,
                isVaultMode = true // NUEVO: Indica que debe usar categorías secretas
            )
        }
    }
}