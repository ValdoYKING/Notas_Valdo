package com.valdo.notasinteligentesvaldo.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.valdo.notasinteligentesvaldo.data.UiPrefs
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentMode by UiPrefs.themeModeFlow(context).collectAsState(initial = "system")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tema") },
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
            val options = listOf(
                "system" to "Predeterminado del sistema",
                "light" to "Claro",
                "dark" to "Oscuro",
                "dark_plus" to "Oscuro+ (OLED)"
            )

            options.forEach { (value, label) ->
                ListItem(
                    headlineContent = { Text(label) },
                    trailingContent = {
                        RadioButton(
                            selected = currentMode == value,
                            onClick = { scope.launch { UiPrefs.setThemeMode(context, value) } }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { scope.launch { UiPrefs.setThemeMode(context, value) } }
                )
                HorizontalDivider()
            }
        }
    }
}
