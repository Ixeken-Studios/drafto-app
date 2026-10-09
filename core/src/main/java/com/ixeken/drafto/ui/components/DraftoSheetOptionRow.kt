package com.ixeken.drafto.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.FrauncesFontFamily
import com.ixeken.drafto.ui.theme.PaddingExtraSmall
import com.ixeken.drafto.ui.theme.PaddingMicro
import com.ixeken.drafto.ui.theme.PaddingSmall
import com.ixeken.drafto.ui.theme.PoppinsFontFamily
import com.ixeken.drafto.ui.theme.SettingsItemIconSize
import com.ixeken.drafto.ui.theme.SheetOptionIconSize
import com.ixeken.drafto.ui.theme.SheetOptionItemPaddingHorizontal
import com.ixeken.drafto.ui.theme.SheetOptionItemPaddingVertical

/**
 * Fila de opción interactiva y unificada para Bottom Sheets (Editorial Nothing OS).
 *
 * Diseñada para agruparse dentro de [DraftoSheetCardGroup] en hojas de respaldo, importación,
 * selectores de tipo de colección y opciones de configuración.
 *
 * Características visuales:
 * - Ícono inicial alineado verticalmente con el título.
 * - Título tipográfico en [FrauncesFontFamily] semi-negrita para consistencia editorial.
 * - Subtítulo descriptivo en [PoppinsFontFamily] regular con tono secundario legible.
 * - Ranura final para acciones contextuales opcionales (radio, interruptor o flecha).
 * - Separador horizontal sutil integrado condicionalmente ([showDivider]).
 *
 * @param title Título principal de la opción.
 * @param onClick Acción ejecutada al pulsar la fila.
 * @param modifier Modificador Compose opcional.
 * @param subtitle Descripción secundaria o ruta de formato opcional.
 * @param icon Ícono vectorial opcional a la izquierda.
 * @param iconTint Color de tinte para el ícono (por defecto [MaterialTheme.colorScheme.onSurface]).
 * @param enabled Determina si la opción responde a interacciones táctiles.
 * @param showDivider Si es true, dibuja una línea divisoria sutil en la parte inferior.
 * @param trailingContent Contenido composable opcional en el extremo derecho.
 */
@Composable
fun DraftoSheetOptionRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    showDivider: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = SheetOptionItemPaddingHorizontal,
                    vertical = SheetOptionItemPaddingVertical
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) iconTint else iconTint.copy(alpha = 0.38f),
                        modifier = Modifier.size(SheetOptionIconSize)
                    )
                }
                Spacer(modifier = Modifier.width(SheetOptionItemPaddingHorizontal))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )

                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(PaddingExtraSmall))
                    Text(
                        text = subtitle,
                        fontFamily = PoppinsFontFamily,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) DraftoTheme.colors.textSecondary else DraftoTheme.colors.textSecondary.copy(alpha = 0.38f)
                    )
                }
            }

            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(PaddingSmall))
                trailingContent()
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SheetOptionItemPaddingHorizontal),
                thickness = 0.8.dp,
                color = DraftoTheme.colors.divider.copy(alpha = 0.08f)
            )
        }
    }
}

/**
 * Fila estática para presentación de políticas, términos o información legal en Bottom Sheets.
 *
 * Se diseñó específicamente para bloques de lectura multilínea donde no se requiere respuesta táctil
 * y el icono debe permanecer alineado a la parte superior junto a la primera línea del título.
 *
 * @param title Título principal del apartado informativo.
 * @param description Texto explicativo detallado.
 * @param icon Ícono vectorial temático alineado arriba.
 * @param modifier Modificador Compose opcional.
 * @param iconTint Tinte cromático del ícono.
 * @param showDivider Dibuja una línea de división horizontal sutil al final.
 */
@Composable
fun DraftoSheetInfoRow(
    title: String,
    description: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    showDivider: Boolean = false
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = SheetOptionItemPaddingHorizontal,
                    vertical = SheetOptionItemPaddingVertical
                ),
            verticalAlignment = Alignment.Top
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(SettingsItemIconSize)
                        .padding(top = PaddingMicro),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(SheetOptionIconSize)
                    )
                }
                Spacer(modifier = Modifier.width(SheetOptionItemPaddingHorizontal))
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(PaddingExtraSmall))
                    Text(
                        text = description,
                        fontFamily = PoppinsFontFamily,
                        style = MaterialTheme.typography.bodySmall,
                        color = DraftoTheme.colors.textSecondary
                    )
                }
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SheetOptionItemPaddingHorizontal),
                thickness = 0.8.dp,
                color = DraftoTheme.colors.divider.copy(alpha = 0.08f)
            )
        }
    }
}
