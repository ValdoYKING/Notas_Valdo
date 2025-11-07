package com.valdo.notasinteligentesvaldo.components

import android.os.SystemClock
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.valdo.notasinteligentesvaldo.models.Note
import com.valdo.notasinteligentesvaldo.util.NoteActions
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.isActive
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Tarjeta de nota optimizada para listas con soporte de Markdown (preview truncado).
 */
@Composable
fun NoteCard(
    note: Note,
    onNoteClick: (Note) -> Unit,
    onDoubleTapFavorite: (Note) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onShowMessage: (String) -> Unit,
    onShareRequest: (Note) -> Unit
) {
    // Precalcular formateador una vez por composición del item
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    // Precomputar valores derivados para evitar trabajo en recomposiciones
    val previewText = remember(note.content) {
        if (note.content.length <= 120) note.content else note.content.take(120) + "..."
    }
    val markdownPreview = remember(note.content) {
        if (note.content.length <= 200) note.content else note.content.take(200) + "..."
    }
    val editedAtText = remember(note.timestamp) {
        "Editado: ${dateFormatter.format(Date(note.timestamp))}"
    }

    // Estados de animación para doble toque
    var showHeart by remember { mutableStateOf(false) }
    val blurRadius by animateDpAsState(targetValue = if (showHeart) 10.dp else 0.dp, label = "blurAnim")
    val context = LocalContext.current

    // Debounce de clic para evitar aperturas múltiples por taps rápidos
    var lastOpenTime by remember { mutableLongStateOf(0L) }
    val clickDebounceMs = 600L

    // Estado para el overlay de acciones
    var showActions by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Estrellas animadas más pequeñas y numerosas
    data class Star(var x: Float, var y: Float, var vx: Float, var vy: Float, var size: Float)
    val stars = remember { mutableStateListOf<Star>() }
    val localDensity = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // Ajuste: densidad para escalar cantidad/velocidad de estrellas
    val densityScale = LocalDensity.current.density // ~1-4
    val baseCount = 70
    val starCount = ((baseCount * densityScale).coerceIn(60f, 120f)).toInt()
    val speedFactor = 1f / (3500f / densityScale) // velocidad relativa

    LaunchedEffect(showActions, starCount) {
        if (showActions && stars.isEmpty()) {
            stars.clear()
            // Inicializa estrellas
            repeat(starCount) {
                stars += Star(
                    x = (0..1000).random() / 1000f,
                    y = (0..1000).random() / 1000f,
                    vx = ((-4..4).random() * speedFactor / 1000f),
                    vy = ((-4..4).random() * speedFactor / 1000f),
                    size = with(localDensity) { ((1..3).random() * 0.35f).dp.toPx() } // más pequeños
                )
            }
        }
        // Animación lenta
        if (showActions) {
            // Vibración / haptic feedback al mostrarse
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            // Autodismiss tras 5s si no se eligió nada
            val startedAt = SystemClock.uptimeMillis()
            while (isActive && showActions) {
                stars.forEach { s ->
                    s.x += s.vx; s.y += s.vy
                    if (s.x < 0f) s.x = 1f; if (s.x > 1f) s.x = 0f
                    if (s.y < 0f) s.y = 1f; if (s.y > 1f) s.y = 0f
                }
                if (SystemClock.uptimeMillis() - startedAt > 5000L) {
                    showActions = false
                }
                kotlinx.coroutines.delay(40) // movimiento más lento
            }
        }
    }

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .pointerInput(note.id) {
                detectTapGestures(
                    onDoubleTap = {
                        // Animación visual: blur + corazón
                        showHeart = true
                        // Disparar callback para marcar como favorita si aún no lo es
                        if (!note.isFavorite) {
                            onDoubleTapFavorite(note)
                        }
                    },
                    onLongPress = {
                        // Mostrar acciones al mantener presionado
                        showActions = true
                    },
                    onTap = {
                        if (!showActions) {
                            val now = SystemClock.uptimeMillis()
                            if (now - lastOpenTime > clickDebounceMs) {
                                lastOpenTime = now
                                onNoteClick(note)
                            }
                        } else {
                            // Si está abierto el overlay y se hace tap fuera de botones, cerramos
                            showActions = false
                        }
                    }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        // Root Box SIN padding para que overlay cubra toda el área
        Box(Modifier.fillMaxWidth()) {
            val effectiveBlur = when {
                showActions -> 24.dp // blur más fuerte durante overlay
                showHeart -> blurRadius
                else -> 0.dp
            }
            // Contenido con padding interno
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .blur(effectiveBlur)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (note.title.isNotEmpty() && note.title != "Nota sin título") {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (note.isMarkdownEnabled) {
                    val isDark = isSystemInDarkTheme()
                    MarkdownText(
                        markdown = markdownPreview,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                } else {
                    Text(
                        text = previewText,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = editedAtText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.align(Alignment.End)
                )
            }

            // Corazón centrado con animación (similar a Instagram)
            androidx.compose.animation.AnimatedVisibility(
                visible = showHeart && !showActions,
                enter = fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.6f, animationSpec = tween(250)),
                exit = fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.8f, animationSpec = tween(200)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(96.dp)
                )
            }

            // Overlay acciones: ocupa todo el espacio, sin panel, solo íconos y estrellas
            androidx.compose.animation.AnimatedVisibility(
                visible = showActions,
                enter = fadeIn() + scaleIn(initialScale = 0.95f),
                exit = fadeOut() + scaleOut(targetScale = 0.95f),
                modifier = Modifier.matchParentSize()
            ) {
                val isDark = isSystemInDarkTheme()
                val scrimAlpha = if (isDark) 0.55f else 0.65f
                Box(Modifier.matchParentSize()) {
                    // Scrim + estrellas pequeñas
                    Box(Modifier.matchParentSize().blur(12.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = scrimAlpha)))
                    val starColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    Canvas(Modifier.matchParentSize()) {
                        stars.forEach { s ->
                            drawCircle(color = starColor, radius = s.size, center = Offset(s.x * size.width, s.y * size.height))
                        }
                    }

                    // Composable reutilizable para íconos con escala y haptic
                    @Composable
                    fun ActionIcon(
                        image: androidx.compose.ui.graphics.vector.ImageVector,
                        contentDescription: String,
                        tint: Color,
                        align: Alignment,
                        hapticType: HapticFeedbackType = HapticFeedbackType.TextHandleMove,
                        onClick: () -> Unit
                    ) {
                        var pressed by remember { mutableStateOf(false) }
                        val scale by animateFloatAsState(if (pressed) 0.85f else 1f, label = "iconScale")
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(hapticType)
                                onClick()
                            },
                            modifier = Modifier
                                .align(align)
                                .graphicsLayer(scaleX = scale, scaleY = scale)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            pressed = true
                                            tryAwaitRelease()
                                            pressed = false
                                        }
                                    )
                                }
                                .padding(4.dp)
                        ) {
                            Icon(image, contentDescription = contentDescription, tint = tint)
                        }
                    }

                    // Copiar (arriba izquierda)
                    ActionIcon(
                        image = Icons.Filled.ContentCopy,
                        contentDescription = "Copiar",
                        tint = MaterialTheme.colorScheme.onSurface,
                        align = Alignment.TopStart,
                        onClick = {
                            NoteActions.copyToClipboard(context, note.content)
                            onShowMessage("Nota copiada al portapapeles")
                            showActions = false
                        }
                    )
                    // Compartir ahora llama al callback y cierra overlay
                    ActionIcon(
                        image = Icons.Filled.Share,
                        contentDescription = "Compartir",
                        tint = MaterialTheme.colorScheme.onSurface,
                        align = Alignment.TopEnd,
                        onClick = {
                            showActions = false
                            onShareRequest(note)
                        }
                    )
                    // Eliminar (abajo centro) -> diálogo confirmación
                    ActionIcon(
                        image = Icons.Filled.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error,
                        align = Alignment.BottomCenter,
                        hapticType = HapticFeedbackType.LongPress,
                        onClick = { showDeleteConfirm = true }
                    )
                }
            }
        }
    }

    // Diálogo de confirmación de eliminación
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar nota") },
            text = { Text("¿Seguro que deseas eliminar esta nota?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteNote(note)
                    onShowMessage("Nota eliminada")
                    showDeleteConfirm = false
                    showActions = false
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            }
        )
    }

    // Temporizador para ocultar el corazón y quitar blur
    LaunchedEffect(showHeart) {
        if (showHeart) {
            kotlinx.coroutines.delay(600)
            showHeart = false
        }
    }
}
