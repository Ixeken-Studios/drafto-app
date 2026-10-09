package com.ixeken.drafto.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import com.ixeken.drafto.ui.theme.BorderWidthSelectedCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.ixeken.drafto.core.R
import com.ixeken.drafto.domain.model.Note
import com.ixeken.drafto.ui.theme.BookmarkActionIconSize
import com.ixeken.drafto.ui.theme.BookmarkActionTouchSize
import com.ixeken.drafto.ui.theme.BookmarkCardElevation
import com.ixeken.drafto.ui.theme.BookmarkSelectionIndicatorSize
import com.ixeken.drafto.ui.theme.DraftoShapeBookmarkCard
import com.ixeken.drafto.ui.theme.DraftoSprings
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.DraftoTransitions
import com.ixeken.drafto.ui.theme.LocalDraftoReducedMotion
import com.ixeken.drafto.ui.theme.FrauncesFontFamily
import com.ixeken.drafto.ui.theme.PaddingExtraSmall
import com.ixeken.drafto.ui.theme.PaddingLarge
import com.ixeken.drafto.ui.theme.PaddingMedium
import com.ixeken.drafto.ui.theme.PaddingSmall
import com.ixeken.drafto.ui.theme.VerticalMenuItemIconSize
import com.ixeken.drafto.ui.theme.PoppinsFontFamily
import com.ixeken.drafto.ui.utils.formatInlineMarkdown
import com.ixeken.drafto.ui.utils.formatMarkdownPreview
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formateador estático thread-safe para fechas en la tarjeta de notas.
 * Se instancia a nivel de archivo para evitar asignaciones continuas y sobrecarga de GC en recomposición.
 */
private val NoteCardDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())

/**
 * Convierte un sello de tiempo en milisegundos a una cadena con formato dd/MM/yyyy.
 */
fun formatNoteCardDate(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(NoteCardDateFormatter)
}

/**
 * Tarjeta de nota Nothing OS unificada para modos Lista y Bento Grid.
 *
 * Decisiones de diseño y arquitectura:
 * - Reutilización cohesiva en :core para garantizar un único punto de evolución visual.
 * - Estructura vertical consistente: etiqueta de colección, título Fraunces Bold, previsualización Markdown y pie de acciones.
 * - Soporte adaptativo para listas de ancho completo o celdas de cuadrícula sin duplicar lógica de componentes.
 * - Cero asignaciones pesadas durante composición gracias a la memorización de títulos y previews Markdown.
 * - Acceso a menú contextual flotante para exportar en Markdown (.md), alternar fijado y borrar.
 *
 * @param note Modelo inmutable de la nota.
 * @param onClick Acción ejecutada al pulsar sobre la tarjeta para abrir el editor.
 * @param onTogglePinned Callback para alternar el estado de fijado de la nota.
 * @param onDelete Callback para eliminar la nota.
 * @param onExport Callback para iniciar la exportación de la nota individual vía SAF.
 * @param collectionName Nombre de la colección asociada o null si no pertenece a ninguna.
 * @param modifier Modificador Compose opcional.
 * @param maxBodyLines Límite máximo de líneas visibles del contenido Markdown.
 * @param isSelected Indica si la nota se encuentra actualmente seleccionada.
 * @param isSelectionMode Indica si la interfaz se encuentra en modo multiselección.
 * @param onLongClick Callback ejecutado tras pulsación prolongada para activar multiselección.
 */
@Composable
fun DraftoNoteCard(
    note: Note,
    onClick: () -> Unit,
    onTogglePinned: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    collectionName: String? = null,
    modifier: Modifier = Modifier,
    maxBodyLines: Int = 6,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    var menuExpanded by remember { mutableStateOf(false) }
    val cardShape = DraftoShapeBookmarkCard
    val cardSurface = DraftoCardSelectionDefaults.surfaceColor(isSelected)
    val accent = DraftoTheme.colors.accent
    val untitledLabel = stringResource(R.string.untitled_note)

    val formattedTitle = remember(note.title, cardSurface, accent, untitledLabel) {
        if (note.title.isBlank()) {
            AnnotatedString(untitledLabel)
        } else {
            formatInlineMarkdown(
                text = note.title,
                codeBackground = cardSurface,
                codeColor = accent
            )
        }
    }

    val formattedContent = remember(note.content, cardSurface, accent) {
        formatMarkdownPreview(
            rawText = note.content,
            codeBackground = cardSurface,
            codeColor = accent
        )
    }

    val dateText = remember(note.updatedAt, note.createdAt) {
        val targetTimestamp = if (note.updatedAt > 0L) note.updatedAt else note.createdAt
        formatNoteCardDate(targetTimestamp)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isReducedMotion = LocalDraftoReducedMotion.current
    val pressScale by animateFloatAsState(
        targetValue = DraftoTransitions.cardPressScale(isPressed, isReducedMotion),
        animationSpec = DraftoSprings.CardPressSpec,
        label = "cardPressScale"
    )

    Surface(
        shape = cardShape,
        color = cardSurface,
        border = DraftoCardSelectionDefaults.border(isSelected),
        shadowElevation = DraftoCardSelectionDefaults.shadowElevation(isSelected),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(cardShape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingLarge)
        ) {
            // 1. Fila superior: Colección asociada
            if (!collectionName.isNullOrBlank()) {
                Text(
                    text = collectionName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    color = DraftoTheme.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(PaddingSmall))
            }

            // 2. Título principal en Fraunces Bold
            Text(
                text = formattedTitle,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // 3. Previsualización de contenido Markdown
            if (formattedContent.isNotBlank()) {
                Spacer(modifier = Modifier.height(PaddingSmall))
                Text(
                    text = formattedContent,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = PoppinsFontFamily
                    ),
                    color = DraftoTheme.colors.textSecondary,
                    maxLines = maxBodyLines,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(PaddingMedium))

            // 4. Pie de tarjeta: Fecha y controles contextuales
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PaddingExtraSmall)
                ) {
                    if (note.isPinned) {
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = stringResource(R.string.badge_pinned),
                            tint = DraftoTheme.colors.accent,
                            modifier = Modifier.size(BookmarkActionIconSize)
                        )
                    }

                    if (isSelectionMode) {
                        DraftoSelectionCheckmarkIndicator(isSelected = isSelected)
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(BookmarkActionTouchSize)
                                    .clip(CircleShape)
                                    .clickable { menuExpanded = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreHoriz,
                                    contentDescription = stringResource(R.string.action_options_menu),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(BookmarkActionIconSize)
                                )
                            }

                            DraftoVerticalMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                quickActions = {
                                    DraftoMenuQuickActionButton(
                                        icon = Icons.Rounded.PushPin,
                                        label = stringResource(
                                            if (note.isPinned) R.string.action_unpin else R.string.action_pin
                                        ),
                                        isActive = note.isPinned,
                                        onClick = {
                                            menuExpanded = false
                                            onTogglePinned()
                                        }
                                    )
                                    DraftoMenuQuickActionButton(
                                        icon = Icons.Rounded.FileUpload,
                                        label = stringResource(R.string.action_export),
                                        onClick = {
                                            menuExpanded = false
                                            onExport()
                                        }
                                    )
                                }
                            ) {
                                DraftoDropdownMenuItem(
                                    text = stringResource(R.string.action_delete),
                                    onClick = {
                                        menuExpanded = false
                                        onDelete()
                                    },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = null,
                                            tint = DraftoTheme.colors.sunsetCoral,
                                            modifier = Modifier.size(VerticalMenuItemIconSize)
                                        )
                                    },
                                    isDestructive = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
