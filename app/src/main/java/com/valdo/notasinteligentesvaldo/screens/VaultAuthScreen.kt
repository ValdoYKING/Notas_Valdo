package com.valdo.notasinteligentesvaldo.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.valdo.notasinteligentesvaldo.auth.BiometricAuthHelper
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity

/**
 * Pantalla de autenticación para acceder a la Bóveda.
 * Muestra un mensaje y permite autenticarse con biometría o credenciales del dispositivo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultAuthScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var authenticationAttempted by remember { mutableStateOf(false) }

    // Verificar si el dispositivo tiene autenticación biométrica
    val canAuthenticate = remember { BiometricAuthHelper.canAuthenticate(context) }

    // Función para iniciar autenticación
    fun startAuthentication() {
        if (activity != null && canAuthenticate) {
            authenticationAttempted = true
            BiometricAuthHelper.authenticate(
                activity = activity,
                title = "Acceder a la Bóveda",
                subtitle = "Verifica tu identidad para continuar",
                onSuccess = {
                    // Navegar a la pantalla de la bóveda
                    navController.navigate("vault") {
                        popUpTo("vaultAuth") { inclusive = true }
                    }
                },
                onError = { error ->
                    errorMessage = error
                    showError = true
                },
                onFailed = {
                    errorMessage = "Autenticación fallida. Intenta de nuevo."
                    showError = true
                }
            )
        } else {
            errorMessage = "No se puede usar autenticación biométrica en este dispositivo"
            showError = true
        }
    }

    // Iniciar autenticación automáticamente al entrar
    LaunchedEffect(Unit) {
        if (!authenticationAttempted) {
            startAuthentication()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bóveda") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Bóveda",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Bóveda de Notas Secretas",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Tus notas más privadas están protegidas con autenticación biométrica",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (canAuthenticate) {
                    Button(
                        onClick = { startAuthentication() },
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Desbloquear Bóveda")
                    }
                } else {
                    Text(
                        text = "⚠️ Este dispositivo no tiene autenticación biométrica configurada",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (showError) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

