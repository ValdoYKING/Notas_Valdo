package com.valdo.notasinteligentesvaldo.ui.avatar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Estilos simples para el contenedor del avatar.
 *
 * Nota: Si más adelante quieres “copiar” estilos de una imagen (pasted_image_1),
 * lo ideal es mapearlos aquí (shape + borde + fondos) y persistir el id.
 */
enum class AvatarStyle(val id: String, val label: String) {
    Circle("circle", "Círculo"),
    Rounded("rounded", "Redondeado"),
    Squircle("squircle", "Suave"),
    CutCorner("cut", "Esquinas cortadas"),
}

fun avatarStyleFromId(raw: String?): AvatarStyle =
    AvatarStyle.entries.firstOrNull { it.id == raw } ?: AvatarStyle.Circle

@Composable
fun avatarShape(style: AvatarStyle): Shape = when (style) {
    AvatarStyle.Circle -> CircleShape
    AvatarStyle.Rounded -> RoundedCornerShape(16.dp)
    AvatarStyle.Squircle -> RoundedCornerShape(28.dp)
    AvatarStyle.CutCorner -> CutCornerShape(14.dp)
}

@Composable
fun AvatarContainer(
    style: AvatarStyle,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = avatarShape(style)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(border, shape),
        content = {
            Box(Modifier.fillMaxSize(), content = content)
        }
    )
}

