package com.valdo.notasinteligentesvaldo.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.valdo.notasinteligentesvaldo.data.UiPrefs
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartActionSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val startAction by UiPrefs.startActionFlow(context).collectAsState(initial = "notes")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Acción al abrir") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Elige qué quieres ver primero al abrir la app", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            val options = listOf(
                "notes" to "Mostrar mis notas",
                "quick_note" to "Abrir una nota nueva rápida"
            )
            options.forEach { (value, label) ->
                ListItem(
                    headlineContent = { Text(label) },
                    trailingContent = {
                        RadioButton(selected = startAction == value, onClick = { scope.launch { UiPrefs.setStartAction(context, value) } })
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { scope.launch { UiPrefs.setStartAction(context, value) } }
                )
                HorizontalDivider()
            }
        }
    }
}

