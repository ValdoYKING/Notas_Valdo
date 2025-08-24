package com.valdo.notasinteligentesvaldo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.valdo.notasinteligentesvaldo.data.DatabaseBuilder
import com.valdo.notasinteligentesvaldo.navigation.AppNavigation
import com.valdo.notasinteligentesvaldo.ui.theme.NotasInteligentesValdoTheme
import com.valdo.notasinteligentesvaldo.viewmodel.NoteViewModel
import com.valdo.notasinteligentesvaldo.viewmodel.NoteViewModelFactory
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.core.content.IntentCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Actividad principal de la aplicación de notas inteligentes.
 *
 * - Inicializa el ViewModel de notas usando una fábrica personalizada.
 * - Configura el tema y la navegación principal de la app.
 * - Es el punto de entrada de la UI con Jetpack Compose.
 */
class MainActivity : ComponentActivity() {
    // ViewModel de notas, creado usando la fábrica y la base de datos Room
    private val noteViewModel: NoteViewModel by viewModels {
        NoteViewModelFactory(DatabaseBuilder.getInstance(this).noteDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Instalar SplashScreen nativo (debe ir al inicio de onCreate)
        val splash = installSplashScreen()
        // Animación de salida: desvanecer y reducir el icono
        splash.setOnExitAnimationListener { provider ->
            val iconView = provider.iconView
            iconView.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(250L)
                .withEndAction { provider.remove() }
                .start()
        }

        super.onCreate(savedInstanceState)

        // Procesar intent entrante (abrir .txt/.md o compartir texto)
        processIncomingIntent(intent)

        setContent {
            // Aplica el tema personalizado de la app
            NotasInteligentesValdoTheme {
                // Surface define el fondo principal de la app
                Surface(color = MaterialTheme.colorScheme.background) {
                    // Inicia la navegación principal, pasando el ViewModel global
                    AppNavigation(
                        viewModel = noteViewModel // ViewModel compartido en toda la app
                    )
                }
            }
        }
    }

    // Cambiar a Intent no nulo para coincidir con la firma de Activity
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Procesar nuevos intents mientras la app está en foreground
        processIncomingIntent(intent)
    }

    private fun processIncomingIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data ?: return
                val type = intent.type
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val content = readTextFromUri(uri)
                        val nameGuess = guessFileName(uri)
                        val asMarkdown = type?.equals("text/markdown", true) == true || nameGuess.endsWith(".md", true) || looksLikeMarkdown(content)
                        val title = nameGuess.removeSuffix(".md").removeSuffix(".txt").ifBlank { guessTitleFromText(content) }
                        withContext(Dispatchers.Main) {
                            noteViewModel.setExternalDocument(title, content, asMarkdown)
                        }
                    } catch (_: Exception) { /* ignore */ }
                }
            }
            Intent.ACTION_SEND -> {
                val type = intent.type ?: ""
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: ""
                if (!sharedText.isNullOrBlank()) {
                    val title = subject.ifBlank { guessTitleFromText(sharedText) }
                    val asMarkdown = type.equals("text/markdown", ignoreCase = true) || looksLikeMarkdown(sharedText)
                    noteViewModel.setExternalDocument(title, sharedText, asMarkdown)
                } else {
                    val streamUri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    if (streamUri != null) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val content = readTextFromUri(streamUri)
                                val nameGuess = guessFileName(streamUri)
                                val asMarkdown = type.equals("text/markdown", ignoreCase = true) || nameGuess.endsWith(".md", true) || looksLikeMarkdown(content)
                                val title = nameGuess.removeSuffix(".md").removeSuffix(".txt").ifBlank { guessTitleFromText(content) }
                                withContext(Dispatchers.Main) {
                                    noteViewModel.setExternalDocument(title, content, asMarkdown)
                                }
                            } catch (_: Exception) { /* ignore */ }
                        }
                    }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val type = intent.type ?: ""
                val list = IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                val first = list?.firstOrNull()
                if (first != null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val content = readTextFromUri(first)
                            val nameGuess = guessFileName(first)
                            val asMarkdown = type.equals("text/markdown", ignoreCase = true) || nameGuess.endsWith(".md", true) || looksLikeMarkdown(content)
                            val title = nameGuess.removeSuffix(".md").removeSuffix(".txt").ifBlank { guessTitleFromText(content) }
                            withContext(Dispatchers.Main) {
                                noteViewModel.setExternalDocument(title, content, asMarkdown)
                            }
                        } catch (_: Exception) { /* ignore */ }
                    }
                } else {
                    val texts = intent.getStringArrayListExtra(Intent.EXTRA_TEXT)
                    val t = texts?.firstOrNull()
                    if (!t.isNullOrBlank()) {
                        val title = guessTitleFromText(t)
                        val asMarkdown = type.equals("text/markdown", ignoreCase = true) || looksLikeMarkdown(t)
                        noteViewModel.setExternalDocument(title, t, asMarkdown)
                    }
                }
            }
        }
    }


    private fun readTextFromUri(uri: Uri): String {
        contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input)).use { br ->
                val sb = StringBuilder()
                var line: String?
                while (true) {
                    line = br.readLine() ?: break
                    sb.append(line).append('\n')
                }
                return sb.toString()
            }
        }
        return ""
    }

    private fun guessFileName(uri: Uri): String {
        // Usa el último segmento de ruta como nombre estimado
        val raw = uri.lastPathSegment ?: "nota"
        return raw.substringAfterLast('/')
    }

    private fun guessTitleFromText(text: String): String {
        // Primer línea no vacía como título, limitado
        val first = text.lineSequence().firstOrNull { it.isNotBlank() }?.trim() ?: "Nota sin título"
        return first.take(60)
    }

    private fun looksLikeMarkdown(text: String): Boolean {
        // Heurística simple: encabezados, listas, enlaces
        val mdPatterns = listOf(
            Regex("^#\\s+", RegexOption.MULTILINE),
            Regex("^[-*+]\\s+", RegexOption.MULTILINE),
            Regex("\\[[^]]+]\\([^ )]+\\)"),
            Regex("```[a-zA-Z0-9_-]*")
        )
        return mdPatterns.any { it.containsMatchIn(text) }
    }
}