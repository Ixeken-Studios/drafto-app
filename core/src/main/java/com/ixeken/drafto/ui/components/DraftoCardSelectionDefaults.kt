package com.ixeken.drafto.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ixeken.drafto.ui.theme.BookmarkCardElevation
import com.ixeken.drafto.ui.theme.BookmarkSelectionIndicatorSize
import com.ixeken.drafto.ui.theme.BorderWidthSelectedCard
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.PaddingLarge

/**
 * Fuente unica de verdad y clase modular para el estilo visual de seleccion de tarjetas
 * (Marcadores, Notas, To-dos y Colecciones) segun el estandar Nothing OS 5.
 *
 * Centraliza:
 * - Color de superficie opaco compuesto (elimina filtraciones de sombras nativas de Android).
 * - Borde perimetral en acento tematico.
 * - Anulacion de elevacion de sombra en seleccion para tarjetas planas y nitidas.
 * - Indicador visual con checkmark circular unificado.
 */
object DraftoCardSelectionDefaults {

    /**
     * Calcula el color de fondo solido de la tarjeta.
     * En estado seleccionado mezcla el color de acento sobre la superficie base para
     * garantizar opacidad al 100% y erradicar cualquier artefacto de dos colores.
     */
    @Composable
    fun surfaceColor(
        isSelected: Boolean,
        baseSurface: Color = MaterialTheme.colorScheme.surface,
        accentTintAlpha: Float = 0.14f
    ): Color = if (isSelected) {
        DraftoTheme.colors.accent.copy(alpha = accentTintAlpha).compositeOver(baseSurface)
    } else {
        baseSurface
    }

    /**
     * Borde Nothing OS para tarjetas seleccionadas.
     */
    @Composable
    fun border(
        isSelected: Boolean,
        borderWidth: Dp = BorderWidthSelectedCard,
        borderAlpha: Float = 0.60f
    ): BorderStroke? = if (isSelected) {
        BorderStroke(borderWidth, DraftoTheme.colors.accent.copy(alpha = borderAlpha))
    } else {
        null
    }

    /**
     * Elevacion de sombra de la tarjeta.
     * Se aplana a 0.dp cuando esta seleccionada para evitar halos de sombra desfasados.
     */
    fun shadowElevation(
        isSelected: Boolean,
        normalElevation: Dp = BookmarkCardElevation
    ): Dp = if (isSelected) 0.dp else normalElevation
}

/**
 * Indicador circular canonico de seleccion con checkmark (Nothing OS 5).
 * Reutilizable en todas las tarjetas de contenido de la aplicacion.
 */
@Composable
fun DraftoSelectionCheckmarkIndicator(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(BookmarkSelectionIndicatorSize)
            .clip(CircleShape)
            .background(if (isSelected) DraftoTheme.colors.accent else DraftoTheme.colors.divider),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = DraftoTheme.colors.onAccent,
                modifier = Modifier.size(PaddingLarge)
            )
        }
    }
}
