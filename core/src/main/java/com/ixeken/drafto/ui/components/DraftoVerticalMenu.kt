package com.ixeken.drafto.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.ixeken.drafto.ui.theme.DraftoGlassMaterial
import com.ixeken.drafto.ui.theme.DraftoShapeVerticalMenu
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.LocalDraftoBlurEnabled
import com.ixeken.drafto.ui.theme.LocalHazeState
import com.ixeken.drafto.ui.theme.VerticalMenuElevation
import com.ixeken.drafto.ui.theme.VerticalMenuItemIconSize
import com.ixeken.drafto.ui.theme.VerticalMenuItemPaddingHorizontal
import com.ixeken.drafto.ui.theme.VerticalMenuItemPaddingVertical
import com.ixeken.drafto.ui.theme.VerticalMenuMinWidth
import com.ixeken.drafto.ui.theme.VerticalMenuQuickActionButtonSize
import com.ixeken.drafto.ui.theme.VerticalMenuQuickActionIconSize
import com.ixeken.drafto.ui.theme.VerticalMenuQuickActionSpacing
import com.ixeken.drafto.ui.theme.toHazeStyle
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur

/**
 * Menú contextual / DropdownMenu Nothing OS 5 unificado para todo el ecosistema de Drafto.
 *
 * Características de diseño:
 * - Fila opcional superior [quickActions] con botones circulares y etiquetas descriptivas abajo.
 * - Lista de opciones con texto alineado al inicio e iconos monocromáticos a la derecha.
 * - Elevación física perimetral suave de 10.dp con luz ambiental uniforme (sin cortes direccionales) y borde sutil de 1.dp.
 * - Desenfoque espacial Pure Blur Ultra Thin utilizando Haze 2.0 y compatibilidad total en Light/Dark/AMOLED.
 * - Esquinas curvadas Nothing de 24.dp.
 *
 * @param expanded Indica si el menú está visible.
 * @param onDismissRequest Callback invocado al descartar el menú.
 * @param modifier Modificador de Compose.
 * @param isBlurEnabled Indica si el efecto blur Haze está activo en la sesión.
 * @param offset Desplazamiento opcional respecto al elemento ancla.
 * @param properties Propiedades del Popup nativo.
 * @param quickActions Fila opcional superior de botones circulares de acción rápida.
 * @param content Contenido interno del menú (elementos [DraftoDropdownMenuItem]).
 */
@Composable
fun DraftoVerticalMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    isBlurEnabled: Boolean = LocalDraftoBlurEnabled.current,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    properties: PopupProperties = PopupProperties(focusable = true),
    quickActions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val hazeState = LocalHazeState.current
    val surfaceColor = if (isBlurEnabled && hazeState != null) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    } else {
        DraftoTheme.colors.cardSurface
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = offset,
        properties = properties,
        shape = DraftoShapeVerticalMenu,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = null,
        modifier = modifier
            .widthIn(min = VerticalMenuMinWidth)
            .shadow(
                elevation = VerticalMenuElevation,
                shape = DraftoShapeVerticalMenu,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.16f),
                spotColor = Color.Transparent
            )
            .clip(DraftoShapeVerticalMenu)
            .then(
                if (isBlurEnabled && hazeState != null) {
                    Modifier.hazeBlur(
                        input = HazeInput.Sources(hazeState),
                        style = DraftoGlassMaterial.Default.toHazeStyle()
                    )
                } else Modifier
            )
            .background(surfaceColor)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f),
                shape = DraftoShapeVerticalMenu
            )
    ) {
        if (quickActions != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                quickActions()
            }
            DraftoMenuDivider()
        }
        content()
    }
}

/**
 * Botón circular de acción rápida para la fila superior del menú contextual Nothing OS 5.
 *
 * @param icon Vector del icono a mostrar.
 * @param contentDescription Descripción accesible.
 * @param onClick Acción ejecutada al presionar.
 * @param modifier Modificador Compose opcional.
 * @param isActive Si la acción está en estado activo (por ejemplo, elemento fijado).
 * @param activeTint Tinte del icono cuando está activo.
 * @param inactiveTint Tinte del icono en estado normal.
 * @param enabled Si el botón está interactuable.
 */
@Composable
fun RowScope.DraftoMenuQuickActionButton(
    icon: ImageVector,
    label: String = "",
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = if (label.isNotEmpty()) label else null,
    isActive: Boolean = false,
    activeTint: Color = DraftoTheme.colors.onAccent,
    inactiveTint: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true
) {
    val backgroundColor = if (isActive) {
        DraftoTheme.colors.accent
    } else {
        DraftoTheme.colors.primaryContainer
    }
    val iconColor = if (isActive) {
        DraftoTheme.colors.onAccent
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val labelColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier
            .weight(1f)
            .padding(horizontal = 2.dp)
            .clip(DraftoShapeVerticalMenu)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(VerticalMenuQuickActionButtonSize)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconColor,
                modifier = Modifier.size(VerticalMenuQuickActionIconSize)
            )
        }
        if (label.isNotEmpty()) {
            Spacer(modifier = Modifier.height(VerticalMenuQuickActionSpacing))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = labelColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Elemento de opción estandarizado para los menús contextuales de Drafto.
 *
 * Sigue la ergonomía Nothing OS con el texto descriptivo a la izquierda (start)
 * y el icono monocromático en el extremo derecho (trailingIcon), permitiendo
 * opcionalmente iconos a la izquierda para compatibilidad retroactiva.
 *
 * @param text Texto de la opción.
 * @param onClick Acción al presionar la opción.
 * @param modifier Modificador opcional.
 * @param trailingIcon Icono canónico a la derecha.
 * @param leadingIcon Icono alternativo a la izquierda para retrocompatibilidad.
 * @param isDestructive Si es una acción destructiva (aplica color sunsetCoral).
 * @param enabled Si el elemento está habilitado.
 */
@Composable
fun DraftoDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    isDestructive: Boolean = false,
    enabled: Boolean = true
) {
    val contentColor = if (isDestructive) {
        DraftoTheme.colors.sunsetCoral
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isDestructive) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = contentColor
            )
        },
        onClick = onClick,
        modifier = modifier,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        colors = MenuDefaults.itemColors(
            textColor = contentColor,
            leadingIconColor = contentColor,
            trailingIconColor = contentColor,
            disabledTextColor = contentColor.copy(alpha = 0.38f),
            disabledLeadingIconColor = contentColor.copy(alpha = 0.38f),
            disabledTrailingIconColor = contentColor.copy(alpha = 0.38f)
        ),
        contentPadding = PaddingValues(
            horizontal = VerticalMenuItemPaddingHorizontal,
            vertical = VerticalMenuItemPaddingVertical
        )
    )
}

/**
 * Divisor sutil y elegante para agrupar secciones dentro de un menú vertical.
 */
@Composable
fun DraftoMenuDivider(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp),
        thickness = 0.8.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    )
}


