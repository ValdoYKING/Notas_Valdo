package com.valdo.notasinteligentesvaldo.components

import android.os.SystemClock
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.valdo.notasinteligentesvaldo.models.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.valdo.notasinteligentesvaldo.ui.theme.LocalOledDarkMode
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tarjeta de nota optimizada con animación de doble toque estilo Instagram (resorte + partículas de mini corazones).
 */
@Composable
fun NoteCard(
    note: Note,
    onNoteClick: (Note) -> Unit,
    onDoubleTapFavorite: (Note) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onShowMessage: (String) -> Unit,
    onShareRequest: (Note) -> Unit,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onSelectToggle: (Note) -> Unit = {},
    onLongPress: (Note) -> Unit = {}
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    val previewText = remember(note.content) {
        if (note.content.length <= 120) note.content else note.content.take(120) + "..."
    }
    val markdownPlainPreview = remember(note.content) {
        note.content
            .replace(Regex("`{1,3}.*?`"), "")
            .replace(Regex("[*_~`>#-]"), "")
            .replace(Regex("!\\[[^]]*]\\([^)]*\\)"), "")
            .replace(Regex("\\[[^]]+]\\([^)]*\\)"), "")
            .take(200) + if (note.content.length > 200) "..." else ""
    }
    val editedAtText = remember(note.timestamp) {
        "Editado: ${dateFormatter.format(Date(note.timestamp))}"
    }

    var showHeart by remember { mutableStateOf(false) }
    
    // Partículas flotantes (mini corazones) para el doble toque
    data class MiniHeart(val id: Int, val startX: Float, val startY: Float, val targetX: Float, val targetY: Float)
    val miniHearts = remember { mutableStateListOf<MiniHeart>() }

    var heartScale by remember { mutableStateOf(0.4f) }
    var heartAlpha by remember { mutableStateOf(0f) }
    var heartRotation by remember { mutableStateOf(-20f) }

    var lastOpenTime by remember { mutableLongStateOf(0L) }
    val clickDebounceMs = 600L

    val haptic = LocalHapticFeedback.current
    val oledMode = LocalOledDarkMode.current
    val cardShape = MaterialTheme.shapes.medium
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        oledMode -> Color.White.copy(alpha = 0.2f)
        else -> outlineColor
    }
    val borderWidth = if (isSelected) 2.dp else if (oledMode) 1.25.dp else 1.dp

    Card(
        shape = cardShape,
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .border(borderWidth, borderColor, cardShape)
            .pointerInput(note.id, isSelectionMode) {
                detectTapGestures(
                    onDoubleTap = {
                        if (!isSelectionMode) {
                            showHeart = true
                            if (!note.isFavorite) {
                                onDoubleTapFavorite(note)
                            }
                        }
                    },
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress(note)
                    },
                    onTap = {
                        val now = SystemClock.uptimeMillis()
                        if (now - lastOpenTime > clickDebounceMs) {
                            lastOpenTime = now
                            if (isSelectionMode) {
                                onSelectToggle(note)
                            } else {
                                onNoteClick(note)
                            }
                        }
                    }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else if (oledMode) {
                Color.Black
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        )
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (note.title.isNotEmpty() && note.title != "Nota sin título") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelectionMode) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onSelectToggle(note) },
                                modifier = Modifier.size(24.dp)
                            )
                        } else if (note.isFavorite) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Favorita",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else if (isSelectionMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onSelectToggle(note) },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                if (note.isMarkdownEnabled) {
                    Text(
                        text = markdownPlainPreview,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
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

            // Corazón animado con física de resorte (Spring) + mini corazones flotantes
            if (showHeart) {
                val scaleAnim by animateFloatAsState(
                    targetValue = heartScale,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "heartScale"
                )
                val alphaAnim by animateFloatAsState(
                    targetValue = heartAlpha,
                    animationSpec = tween(200),
                    label = "heartAlpha"
                )
                val rotAnim by animateFloatAsState(
                    targetValue = heartRotation,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "heartRot"
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer(
                            scaleX = scaleAnim,
                            scaleY = scaleAnim,
                            rotationZ = rotAnim,
                            alpha = alphaAnim
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(96.dp)
                    )

                    // Mini corazones flotantes
                    miniHearts.forEach { mh ->
                        val animProgress by animateFloatAsState(
                            targetValue = if (heartAlpha > 0f) 1f else 0f,
                            animationSpec = tween(500),
                            label = "miniHeart"
                        )
                        val currX = mh.startX + (mh.targetX * animProgress)
                        val currY = mh.startY + (mh.targetY * animProgress)
                        val currAlpha = 1f - animProgress

                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = currAlpha),
                            modifier = Modifier
                                .size(24.dp)
                                .offset(x = currX.dp, y = currY.dp)
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(showHeart) {
        if (showHeart) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            miniHearts.clear()
            repeat(4) { i ->
                val angle = (i * 90f) + (-20..20).random()
                val dist = (35..75).random().toFloat()
                val tx = cos(Math.toRadians(angle.toDouble())).toFloat() * dist
                val ty = sin(Math.toRadians(angle.toDouble())).toFloat() * dist - 35f
                miniHearts.add(MiniHeart(i, 0f, 0f, tx, ty))
            }
            heartScale = 1.15f
            heartAlpha = 1f
            heartRotation = 0f
            delay(550)
            heartScale = 0.7f
            heartAlpha = 0f
            delay(150)
            showHeart = false
            miniHearts.clear()
        }
    }
}
