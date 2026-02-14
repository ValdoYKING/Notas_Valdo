package com.valdo.notasinteligentesvaldo.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.valdo.notasinteligentesvaldo.data.UiPrefs
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import com.valdo.notasinteligentesvaldo.ui.avatar.AvatarContainer
import com.valdo.notasinteligentesvaldo.ui.avatar.avatarStyleFromId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController
) {
    val context = LocalContext.current

    val firstName by UiPrefs.firstNameFlow(context).collectAsState(initial = "")
    val lastName by UiPrefs.lastNameFlow(context).collectAsState(initial = "")
    val themeMode by UiPrefs.themeModeFlow(context).collectAsState(initial = "system")
    val startAction by UiPrefs.startActionFlow(context).collectAsState(initial = "notes")
    val profileUri by UiPrefs.profileImageUriFlow(context).collectAsState(initial = "")
    val avatarStyleId by UiPrefs.avatarStyleFlow(context).collectAsState(initial = "circle")

    val fullName = (firstName.trim() + " " + lastName.trim()).trim()
    val initials = ((firstName.firstOrNull()?.toString() ?: "") + (lastName.firstOrNull()?.toString() ?: "")).uppercase().ifBlank { "U" }

    val themeLabel = when (themeMode) {
        "light" -> "Claro"
        "dark" -> "Oscuro"
        "dark_plus" -> "Oscuro+ (OLED)"
        else -> "Predeterminado del sistema"
    }

    val startActionLabel = if (startAction == "quick_note") "Nota rápida" else "Mostrar notas"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
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
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Apartado de Perfil
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.navigate("settings/profile") }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("settings/profile") }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val style = avatarStyleFromId(avatarStyleId)

                    // Avatar placeholder
                    AvatarContainer(
                        style = style,
                        modifier = Modifier.size(56.dp)
                    ) {
                        if (profileUri.isNotBlank()) {
                            AsyncImage(
                                model = profileUri,
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(initials, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tu perfil", style = MaterialTheme.typography.titleMedium)
                        val subtitle = if (fullName.isBlank()) "Toca aquí para registrarte" else fullName
                        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Apartado de Tema
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.navigate("settings/theme") }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("settings/theme") }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tema", style = MaterialTheme.typography.titleMedium)
                        Text(themeLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Apartado Acción al abrir
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.navigate("settings/start_action") }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("settings/start_action") }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Acción al abrir la app", style = MaterialTheme.typography.titleMedium)
                        Text(startActionLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
