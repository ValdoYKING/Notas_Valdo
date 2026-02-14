package com.valdo.notasinteligentesvaldo.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.valdo.notasinteligentesvaldo.data.UiPrefs
import com.valdo.notasinteligentesvaldo.ui.avatar.AvatarContainer
import com.valdo.notasinteligentesvaldo.ui.avatar.AvatarStyle
import com.valdo.notasinteligentesvaldo.ui.avatar.avatarStyleFromId
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Colores para el recortador (Activity AppCompat) derivados del tema actual.
    // Esto asegura que en modo oscuro no se vea una pantalla blanca y que los iconos/botones tengan contraste.
    val colorScheme = MaterialTheme.colorScheme
    val cropperToolbarColor = colorScheme.surface.toArgb()
    val cropperToolbarContentColor = colorScheme.onSurface.toArgb()
    val cropperBackgroundColor = colorScheme.background.toArgb()

    val firstName by UiPrefs.firstNameFlow(context).collectAsState(initial = "")
    val lastName by UiPrefs.lastNameFlow(context).collectAsState(initial = "")
    val birthDate by UiPrefs.birthDateFlow(context).collectAsState(initial = "")
    val profileUriString by UiPrefs.profileImageUriFlow(context).collectAsState(initial = "")
    val avatarStyleId by UiPrefs.avatarStyleFlow(context).collectAsState(initial = "circle")

    var firstNameState by remember { mutableStateOf("") }
    var lastNameState by remember { mutableStateOf("") }
    var birthDateState by remember { mutableStateOf("") }
    var profileUri by remember { mutableStateOf<Uri?>(null) }

    var showAvatarSheet by remember { mutableStateOf(false) }
    var showPhotoViewer by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var selectedStyle by remember { mutableStateOf(avatarStyleFromId(avatarStyleId)) }

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
    LaunchedEffect(avatarStyleId) {
        selectedStyle = avatarStyleFromId(avatarStyleId)
    }

    // 3) Recorte (retorna URI)
    val cropImageLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        val uri = result.uriContent
        if (uri != null) profileUri = uri
    }

    fun safeLaunchCrop(uri: Uri) {
        try {
            cropImageLauncher.launch(
                CropImageContractOptions(
                    uri,
                    CropImageOptions(
                        aspectRatioX = 1,
                        aspectRatioY = 1,
                        fixAspectRatio = true,
                        activityTitle = "Editar foto",
                        // Toolbar con colores del tema actual
                        toolbarColor = cropperToolbarColor,
                        toolbarBackButtonColor = cropperToolbarContentColor,
                        // Botón de confirmar
                        cropMenuCropButtonTitle = "Listo",
                        // Evita el blanco alrededor (fondo de la Activity de recorte)
                        backgroundColor = cropperBackgroundColor
                    )
                )
            )
        } catch (_: Throwable) {
            // Si el cropper no puede abrir la uri, evitamos crash
        }
    }

    // 2) Cámara (genera un URI temporal y luego recorta)
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (ok && uri != null) safeLaunchCrop(uri)
    }

    fun launchCamera() {
        try {
            val values = android.content.ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "profile_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                pendingCameraUri = uri
                cameraLauncher.launch(uri)
            }
        } catch (_: ActivityNotFoundException) {
            // sin app de cámara
        } catch (_: SecurityException) {
            // sin permisos/denegado
        } catch (_: Throwable) {
            // cualquier otro fallo: no crashear
        }
    }

    // Permiso de cámara (runtime)
    var pendingCameraAfterPermission by remember { mutableStateOf(false) }
    val requestCameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && pendingCameraAfterPermission) {
            pendingCameraAfterPermission = false
            launchCamera()
        } else {
            pendingCameraAfterPermission = false
        }
    }

    fun ensureCameraAndLaunch() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            launchCamera()
        } else {
            pendingCameraAfterPermission = true
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    // 1) Selección desde galería (SAF)
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
                // Algunas URIs no permiten permiso persistente; igual intentamos recortar.
            }
            safeLaunchCrop(uri)
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
                UiPrefs.setAvatarStyle(context, selectedStyle.id)
            }
        }
    }

    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    LaunchedEffect(datePickerState.selectedDateMillis) {
        val millis = datePickerState.selectedDateMillis
        if (millis != null) {
            val localDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
            birthDateState = localDate.format(formatter)
        }
    }

    if (showPhotoViewer && profileUri != null) {
        AlertDialog(
            onDismissRequest = { showPhotoViewer = false },
            confirmButton = {
                TextButton(onClick = { showPhotoViewer = false }) { Text("Cerrar") }
            },
            title = { Text("Foto de perfil") },
            text = {
                AsyncImage(
                    model = profileUri,
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 360.dp)
                )
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar foto de perfil") },
            text = { Text("¿Seguro que quieres eliminar tu foto de perfil?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        profileUri = null
                        showDeleteConfirm = false
                    }
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            }
        )
    }

    if (showAvatarSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAvatarSheet = false }
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Foto de perfil", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))

                ListItem(
                    headlineContent = { Text("Ver foto de perfil") },
                    leadingContent = { Icon(Icons.Default.Visibility, contentDescription = null) },
                    supportingContent = { Text(if (profileUri != null) "Abrir previsualización" else "Aún no tienes una foto") },
                    modifier = Modifier.clickable {
                        showAvatarSheet = false
                        if (profileUri != null) showPhotoViewer = true
                    }
                )

                ListItem(
                    headlineContent = { Text("Tomar nueva foto de perfil") },
                    leadingContent = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showAvatarSheet = false
                        ensureCameraAndLaunch()
                    }
                )

                ListItem(
                    headlineContent = { Text("Seleccionar foto de perfil") },
                    leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showAvatarSheet = false
                        imagePicker.launch(arrayOf("image/*"))
                    }
                )

                if (profileUri != null) {
                    ListItem(
                        headlineContent = { Text("Eliminar foto de perfil") },
                        leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable {
                            showAvatarSheet = false
                            showDeleteConfirm = true
                        }
                    )
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                Text("Diseño de avatar", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))

                AvatarStyle.entries.forEach { style ->
                    ListItem(
                        headlineContent = { Text(style.label) },
                        leadingContent = { Icon(Icons.Default.Style, contentDescription = null) },
                        trailingContent = {
                            RadioButton(
                                selected = selectedStyle == style,
                                onClick = { selectedStyle = style }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStyle = style }
                    )
                }

                Spacer(Modifier.height(8.dp))
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
                            UiPrefs.setAvatarStyle(context, selectedStyle.id)
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
            // Avatar
            AvatarContainer(
                style = selectedStyle,
                modifier = Modifier
                    .size(96.dp)
                    .clickable { showAvatarSheet = true }
            ) {
                if (profileUri != null) {
                    AsyncImage(
                        model = profileUri,
                        contentDescription = "Imagen de perfil",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val initials = ((firstNameState.firstOrNull()?.toString() ?: "") + (lastNameState.firstOrNull()?.toString() ?: "")).uppercase().ifBlank { "U" }
                    Text(initials, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
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
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.DateRange, contentDescription = "Elegir fecha")
                    }
                }
            )

            if (showDatePicker) {
                DatePickerModal(
                    state = datePickerState,
                    onDismiss = { showDatePicker = false },
                    onConfirm = { showDatePicker = false }
                )
            }

            Spacer(Modifier.height(8.dp))
            Text("Los cambios se guardarán automáticamente al regresar.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    state: DatePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    ) {
        DatePicker(state = state)
    }
}
