package com.valdo.notasinteligentesvaldo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.valdo.notasinteligentesvaldo.data.DatabaseBuilder
import com.valdo.notasinteligentesvaldo.models.Note
import com.valdo.notasinteligentesvaldo.navigation.AppNavigation
import com.valdo.notasinteligentesvaldo.ui.theme.NotasInteligentesValdoTheme
import com.valdo.notasinteligentesvaldo.viewmodel.NoteViewModel
import com.valdo.notasinteligentesvaldo.viewmodel.NoteViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch


import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import com.valdo.notasinteligentesvaldo.data.NoteDao
import com.valdo.notasinteligentesvaldo.ui.theme.NotasInteligentesValdoTheme

class MainActivity : ComponentActivity() {
    private val noteViewModel: NoteViewModel by viewModels {
        NoteViewModelFactory(DatabaseBuilder.getInstance(this).noteDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NotasInteligentesValdoTheme {
                AppNavigation(
                    viewModel = noteViewModel // Pasamos el ViewModel creado
                )
            }
        }
    }
}