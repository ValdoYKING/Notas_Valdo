package com.valdo.notasinteligentesvaldo.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.valdo.notasinteligentesvaldo.data.UiPrefs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val firstName by UiPrefs.firstNameFlow(context).collectAsState(initial = "")
    val lastName by UiPrefs.lastNameFlow(context).collectAsState(initial = "")
    val birthDate by UiPrefs.birthDateFlow(context).collectAsState(initial = "")
    val profileUriString by UiPrefs.profileImageUriFlow(context).collectAsState(initial = "")

    var firstNameState by remember { mutableStateOf("") }
    var lastNameState by remember { mutableStateOf("") }
    var birthDateState by remember { mutableStateOf("") }
    var profileUri by remember { mutableStateOf<Uri?>(null) }

    // Flags para saber si el usuario ya tocó/edito los campos (evitar sobreescribir)
    var editedFirstName by remember { mutableStateOf(false) }
    var editedLastName by remember { mutableStateOf(false) }
    var editedBirthDate by remember { mutableStateOf(false) }

    LaunchedEffect(firstName) {
        if (!editedFirstName && firstName.isNotBlank()) firstNameState = firstName
    }
    LaunchedEffect(lastName) {
        if (!editedLastName && lastName.isNotBlank()) lastNameState = lastName
    }
    LaunchedEffect(birthDate) {
        if (!editedBirthDate && birthDate.isNotBlank()) birthDateState = birthDate
    }
    LaunchedEffect(profileUriString) {
        if (profileUri == null && profileUriString.isNotBlank()) profileUri = Uri.parse(profileUriString)
    }

    // Picker de imagen
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            profileUri = uri
        }
    }

    // Guardado automático al salir
    DisposableEffect(Unit) {
        onDispose {
            scope.launch {
                UiPrefs.setFirstName(context, firstNameState)
                UiPrefs.setLastName(context, lastNameState)
                UiPrefs.setBirthDate(context, birthDateState)
                UiPrefs.setProfileImageUri(context, profileUri?.toString().orEmpty())
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tu perfil") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            UiPrefs.setFirstName(context, firstNameState)
                            UiPrefs.setLastName(context, lastNameState)
                            UiPrefs.setBirthDate(context, birthDateState)
                            UiPrefs.setProfileImageUri(context, profileUri?.toString().orEmpty())
                        }
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Guardar")
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar + botón editar
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { imagePicker.launch(arrayOf("image/*")) },
                contentAlignment = Alignment.Center
            ) {
                val initials = ((firstNameState.firstOrNull()?.toString() ?: "") + (lastNameState.firstOrNull()?.toString() ?: "")).uppercase().ifBlank { "U" }
                Text(initials, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(
                value = firstNameState,
                onValueChange = { firstNameState = it; editedFirstName = true },
                label = { Text("Nombre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = lastNameState,
                onValueChange = { lastNameState = it; editedLastName = true },
                label = { Text("Apellido") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = birthDateState,
                onValueChange = { raw ->
                    editedBirthDate = true
                    val clean = raw.filter { it.isDigit() || it == '-' }.take(10)
                    val auto = buildString {
                        clean.forEachIndexed { i, c ->
                            append(c)
                            if ((i == 3 || i == 5) && i != clean.lastIndex) append('-')
                        }
                    }.take(10)
                    birthDateState = auto
                },
                placeholder = { Text("YYYY-MM-DD") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Text("Los cambios se guardarán automáticamente al regresar.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
