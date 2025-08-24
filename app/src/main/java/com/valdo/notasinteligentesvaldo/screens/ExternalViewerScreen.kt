package com.valdo.notasinteligentesvaldo.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.valdo.notasinteligentesvaldo.R
import com.valdo.notasinteligentesvaldo.viewmodel.NoteViewModel
import dev.jeziellago.compose.markdowntext.MarkdownText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalViewerScreen(
    viewModel: NoteViewModel,
    onBack: () -> Unit
) {
    val doc by viewModel.externalDoc.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = doc?.title ?: "Documento",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Acción opcional: Guardar como nota (no automático)
                    if (doc != null) {
                        IconButton(onClick = {
                            viewModel.importExternalNote(
                                title = doc!!.title,
                                content = doc!!.content,
                                asMarkdown = doc!!.isMarkdown
                            )
                            // No limpiar aquí; navegación al detalle se hace vía importedNoteId
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.done_outline),
                                contentDescription = "Guardar como nota"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (doc == null) {
            Box(Modifier.fillMaxSize().padding(padding)) {}
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            if (doc!!.isMarkdown) {
                val isDark = isSystemInDarkTheme()
                MarkdownText(
                    markdown = doc!!.content,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                )
            } else {
                SelectionContainer {
                    Text(
                        text = doc!!.content,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

