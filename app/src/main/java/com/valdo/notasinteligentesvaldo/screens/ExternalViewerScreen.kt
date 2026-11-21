package com.valdo.notasinteligentesvaldo.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.valdo.notasinteligentesvaldo.viewmodel.NoteViewModel
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.material3.RichText

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
                    if (doc != null) {
                        Button(
                            onClick = {
                                viewModel.importExternalNote(
                                    title = doc!!.title,
                                    content = doc!!.content,
                                    asMarkdown = doc!!.isMarkdown
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Guardar nota")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (doc == null) {
            LaunchedEffect(Unit) { onBack() }
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("No hay documento para mostrar")
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Insignia de formato
            AssistChip(
                onClick = { },
                label = {
                    Text(if (doc!!.isMarkdown) "📝 Formato Markdown (.md)" else "📄 Texto Plano (.txt)")
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            SelectionContainer {
                if (doc!!.isMarkdown) {
                    val markdownColor = MaterialTheme.colorScheme.onSurface
                    CompositionLocalProvider(LocalContentColor provides markdownColor) {
                        ProvideTextStyle(
                            MaterialTheme.typography.bodyLarge.copy(
                                color = markdownColor,
                                lineHeight = 26.sp
                            )
                        ) {
                            RichText(modifier = Modifier.fillMaxWidth()) {
                                Markdown(doc!!.content)
                            }
                        }
                    }
                } else {
                    Text(
                        text = doc!!.content,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 28.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            }
        }
    }
}
