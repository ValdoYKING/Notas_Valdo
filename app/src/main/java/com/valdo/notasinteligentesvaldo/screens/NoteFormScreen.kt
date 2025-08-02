package com.valdo.notasinteligentesvaldo.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.valdo.notasinteligentesvaldo.models.Note
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.text.SimpleDateFormat
import java.util.*
import com.valdo.notasinteligentesvaldo.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun NoteFormScreen(
    onNoteSaved: (Note) -> Unit,
    onBack: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isMarkdownEnabled by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val currentDate = remember {
        SimpleDateFormat("EEEE, d 'de' MMMM", Locale.getDefault()).format(Date())
    }
    val titleAlpha by animateFloatAsState(
        targetValue = if (title.isBlank()) 0.5f else 1f,
        animationSpec = tween(durationMillis = 300)
    )


    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    // Botón de toggle Markdown
                    IconButton(
                        onClick = { isMarkdownEnabled = !isMarkdownEnabled },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        val markdownIcon = if (isMarkdownEnabled) R.drawable.code_off_24px else R.drawable.code_24px
                        Icon(
                            painter = painterResource(id = markdownIcon),
                            contentDescription = "Markdown",
                            tint = if (isMarkdownEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Botón de guardar
                    IconButton(
                        onClick = {
                            val newNote = Note(
                                title = title.ifEmpty { "Nota sin título" },
                                content = content,
                                timestamp = System.currentTimeMillis(),
                                timestampInit = System.currentTimeMillis(),
                                isMarkdownEnabled = isMarkdownEnabled
                            )
                            onNoteSaved(newNote)
                            // Eliminado: onBack() aquí para evitar doble navegación
                        },
                        enabled = content.isNotBlank() || title.isNotBlank()
                    ) {
                        Icon(Icons.Default.Check, "Guardar")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = currentDate,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        // Título siempre visible (pero transparente cuando está vacío)
                        Text(
                            text = title.ifEmpty { " " }, // Espacio para mantener altura
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = if (title.isBlank()) Color.Transparent
                                else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Campo de título
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (title.isEmpty()) {
                            Text(
                                text = title.ifEmpty { "Añade un título..." },
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = titleAlpha),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            // Área de contenido con soporte Markdown
            if (isMarkdownEnabled) {
                MarkdownEditor(content, onContentChange = { content = it })
            } else {
                BasicTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp,
                        lineHeight = 28.sp,
                        color = MaterialTheme.colorScheme.onSurface // Color legible en modo oscuro
                    ),
                    decorationBox = { innerTextField ->
                        if (content.isEmpty()) {
                            Text(
                                "Comienza a escribir tu nota aquí...",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
}

@Composable
fun MarkdownEditor(
    content: String,
    onContentChange: (String) -> Unit
) {
    var preview by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Barra de herramientas Markdown (corregida)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { onContentChange("$content **texto** ") }) {
                Text("B", fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { onContentChange("$content *texto* ") }) {
                Text("I", fontStyle = FontStyle.Italic)
            }
            IconButton(onClick = { onContentChange("$content [texto](url) ") }) {
                Text("Link")
            }
            IconButton(onClick = { preview = !preview }) {
                val previewIcon = if (preview) R.drawable.visibility_off_24px else R.drawable.visibility_24px
                Icon(
                    painter = painterResource(id = previewIcon),
                    contentDescription = "Vista previa"
                )
            }
        }

        if (preview) {
            // Vista previa del Markdown
            MarkdownPreview(content)
        } else {
            // Editor de Markdown
            BasicTextField(
                value = content,
                onValueChange = onContentChange,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                )
            )
        }
    }
}

@Composable
fun MarkdownPreview(content: String) {
    // Usamos una librería como compose-markdown para renderizar
    MarkdownText(
        markdown = content,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    )
}