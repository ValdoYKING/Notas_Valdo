package com.valdo.notasinteligentesvaldo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.valdo.notasinteligentesvaldo.data.DatabaseBuilder
import com.valdo.notasinteligentesvaldo.navigation.AppNavigation
import com.valdo.notasinteligentesvaldo.ui.theme.NotasInteligentesValdoTheme
import com.valdo.notasinteligentesvaldo.viewmodel.NoteViewModel
import com.valdo.notasinteligentesvaldo.viewmodel.NoteViewModelFactory
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

/**
 * Actividad principal de la aplicación de notas inteligentes.
 *
 * - Inicializa el ViewModel de notas usando una fábrica personalizada.
 * - Configura el tema y la navegación principal de la app.
 * - Es el punto de entrada de la UI con Jetpack Compose.
 */
class MainActivity : ComponentActivity() {
    // ViewModel de notas, creado usando la fábrica y la base de datos Room
    private val noteViewModel: NoteViewModel by viewModels {
        NoteViewModelFactory(DatabaseBuilder.getInstance(this).noteDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Aplica el tema personalizado de la app
            NotasInteligentesValdoTheme {
                // Surface define el fondo principal de la app
                Surface(color = MaterialTheme.colorScheme.background) {
                    // Inicia la navegación principal, pasando el ViewModel global
                    AppNavigation(
                        viewModel = noteViewModel // ViewModel compartido en toda la app
                    )
                }
            }
        }
    }
}