package com.ixeken.drafto.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ixeken.drafto.core.R
import com.ixeken.drafto.ui.theme.DraftoTheme

/**
 * Insignia circular canónica de elemento fijado (Pin Badge) para notas, tareas y marcadores.
 *
 * Diseñada para unificar los badges circulares repetidos en cuadrículas y listas, garantizando
 * contraste perfecto entre el color de acento del tema y el glifo mediante [DraftoTheme.colors.onAccent].
 *
 * @param modifier Modificador Compose opcional.
 * @param size Diámetro total del contenedor circular.
 * @param iconSize Tamaño del glifo interior de la chincheta.
 * @param containerColor Color de fondo del badge circular (por defecto el acento del tema).
 * @param iconTint Tinte del ícono (por defecto el contraste [DraftoTheme.colors.onAccent]).
 */
@NonRestartableComposable
@Composable
fun DraftoPinBadge(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    iconSize: Dp = 11.dp,
    containerColor: Color = DraftoTheme.colors.accent,
    iconTint: Color = DraftoTheme.colors.onAccent
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.PushPin,
            contentDescription = stringResource(R.string.badge_pinned),
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}
