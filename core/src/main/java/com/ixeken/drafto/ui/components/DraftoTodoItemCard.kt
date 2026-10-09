package com.ixeken.drafto.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.graphics.compositeOver
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.ixeken.drafto.core.R
import com.ixeken.drafto.domain.model.TodoItem
import com.ixeken.drafto.domain.model.TodoSubtask
import com.ixeken.drafto.ui.theme.BookmarkActionIconSize
import com.ixeken.drafto.ui.theme.BookmarkActionTouchSize
import com.ixeken.drafto.ui.theme.BookmarkCardElevation
import com.ixeken.drafto.ui.theme.BookmarkSelectionIndicatorSize
import com.ixeken.drafto.ui.theme.DraftoShapeBookmarkCard
import com.ixeken.drafto.ui.theme.DraftoShapePill
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.FrauncesFontFamily
import com.ixeken.drafto.ui.theme.PaddingExtraSmall
import com.ixeken.drafto.ui.theme.PaddingLarge
import com.ixeken.drafto.ui.theme.PaddingMedium
import com.ixeken.drafto.ui.theme.PaddingMicro
import com.ixeken.drafto.ui.theme.PaddingSmall
import com.ixeken.drafto.ui.theme.PoppinsFontFamily
import com.ixeken.drafto.ui.theme.TodoSubtaskBorderWidth
import com.ixeken.drafto.ui.theme.TodoSubtaskCheckIconSize
import com.ixeken.drafto.ui.theme.VerticalMenuItemIconSize
import com.ixeken.drafto.ui.theme.TodoSubtaskCheckboxSize
import com.ixeken.drafto.ui.utils.formatInlineMarkdown
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formateadores inmutables y thread-safe para fechas y horas de vencimiento.
 */
private val DueDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())
private val CardDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
private val CardDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy · h:mm a", Locale.getDefault())

/**
 * Convierte una marca de tiempo UTC en una etiqueta de fecha legible para el usuario.
 * Se fija el calculo en UTC para evitar desfasajes horarios de medianoche producidos por zonas
 * horarias locales respecto a los milisegundos emitidos por DatePicker.
 */
fun formatTodoDueDate(
    timestamp: Long,
    todayText: String = "Today",
    yesterdayText: String = "Yesterday"
): String {
    val dueDate = Instant.ofEpochMilli(timestamp).atZone(ZoneOffset.UTC).toLocalDate()
    val today = LocalDate.now(ZoneOffset.UTC)

    return when {
        dueDate.isEqual(today) -> todayText
        dueDate.isEqual(today.plusDays(1)) -> "Tomorrow"
        dueDate.isEqual(today.minusDays(1)) -> yesterdayText
        else -> dueDate.format(DueDateFormatter)
    }
}

/**
 * Da formato consistente a la fecha límite para desplegarla en el pie de la tarjeta rediseñada.
 * Si el sello de tiempo posee hora asignada se concatena con el glifo de punto medio.
 */
fun formatTodoDueDateCard(timestamp: Long): String {
    val utcDateTime = Instant.ofEpochMilli(timestamp).atZone(ZoneOffset.UTC)
    val hasTime = utcDateTime.hour != 0 || utcDateTime.minute != 0
    return if (hasTime) {
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(CardDateTimeFormatter)
    } else {
        utcDateTime.toLocalDate().format(CardDateFormatter)
    }
}

/**
 * Tarjeta de lista de tareas estructurada Nothing OS / Bento para la sección Notebook.
 *
 * Decisiones de diseño y arquitectura:
 * - Estética editorial Nothing OS alineada estrictamente con [DraftoBookmarkCard].
 * - Uso exclusivo de tokens semánticos de color y dimensiones sin valores mágicos.
 * - Cabecera con nombre directo de la colección a la izquierda y píldora con contador a la derecha.
 * - Título tipográfico destacado en [FrauncesFontFamily] negrita.
 * - Despliegue interactivo completo de subtareas sin truncamiento artificial.
 * - Checkboxes circulares con físicas de resorte: contorno nítido cuando pendiente y relleno de acento con glifo blanco al completarse.
 * - Bloque de fecha de vencimiento que cambia dinámicamente a color [DraftoTheme.colors.sunsetCoral] cuando la fecha ha expirado.
 * - Indicador de fijado pasivo visible únicamente cuando la tarea está anclada.
 * - Menú contextual flotante para acciones destructivas y de edición sin sobrecargar la tarjeta.
 *
 * @param todo Modelo inmutable de la lista de tareas.
 * @param onToggleCompleted Callback al alternar el estado general de completado.
 * @param onTogglePinned Callback para alternar el estado de fijado.
 * @param onDelete Callback para eliminar la lista de tareas.
 * @param onEdit Acción al presionar la opción de edición.
 * @param onClick Acción opcional al pulsar la tarjeta.
 * @param onToggleSubtask Callback para conmutar el estado de una subtarea individual.
 * @param collectionName Nombre opcional de la colección asociada a la que pertenece la tarea.
 * @param modifier Modificador Compose opcional.
 * @param isSelected Indica si la tarea se encuentra seleccionada en modo multiselección.
 * @param isSelectionMode Indica si la pantalla se encuentra en modo multiselección.
 * @param onLongClick Callback invocado al mantener pulsada la tarjeta para activar selección.
 */
@Composable
fun DraftoTodoItemCard(
    todo: TodoItem,
    onToggleCompleted: (Boolean) -> Unit,
    onTogglePinned: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {},
    onClick: (() -> Unit)? = null,
    onToggleSubtask: ((subtaskId: String, isDone: Boolean) -> Unit)? = null,
    collectionName: String? = null,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val cardShape = DraftoShapeBookmarkCard
    val cardBgColor = DraftoCardSelectionDefaults.surfaceColor(isSelected)

    Surface(
        shape = cardShape,
        color = cardBgColor,
        border = DraftoCardSelectionDefaults.border(isSelected),
        shadowElevation = DraftoCardSelectionDefaults.shadowElevation(isSelected),
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .combinedClickable(
                onClick = { onClick?.invoke() },
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
            // 1. Fila superior: Colección y contador de tareas completadas
            val completedCount = if (todo.hasSubtasks) todo.completedSubtasksCount else (if (todo.isCompleted) 1 else 0)
            val totalCount = if (todo.hasSubtasks) todo.totalSubtasksCount else 1

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (!collectionName.isNullOrBlank()) {
                    Text(
                        text = collectionName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Medium
                        ),
                        color = DraftoTheme.colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f, fill = false))
                }

                Surface(
                    shape = DraftoShapePill,
                    color = DraftoTheme.colors.accent
                ) {
                    Text(
                        text = stringResource(R.string.todo_subtasks_done_counter, completedCount, totalCount),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = DraftoTheme.colors.onAccent,
                        modifier = Modifier.padding(horizontal = PaddingMedium, vertical = PaddingExtraSmall)
                    )
                }
            }

            Spacer(modifier = Modifier.height(PaddingMedium))

            // 2. Título principal en Fraunces Bold
            val cardSurface = MaterialTheme.colorScheme.surface
            val accent = DraftoTheme.colors.accent
            val formattedTitle = remember(todo.title, cardSurface, accent) {
                formatInlineMarkdown(
                    text = todo.title,
                    codeBackground = cardSurface,
                    codeColor = accent
                )
            }

            Text(
                text = formattedTitle,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(PaddingMedium))

            // 3. Subtareas interactivas
            if (todo.hasSubtasks) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(PaddingSmall)
                ) {
                    todo.subtasks.forEach { subtask ->
                        TodoSubtaskRow(
                            subtask = subtask,
                            onToggle = {
                                if (isSelectionMode) {
                                    onClick?.invoke()
                                } else {
                                    onToggleSubtask?.invoke(subtask.id, !subtask.isDone)
                                }
                            }
                        )
                    }
                }
            } else {
                TodoAtomicFallbackRow(
                    todo = todo,
                    onToggle = {
                        if (isSelectionMode) {
                            onClick?.invoke()
                        } else {
                            onToggleCompleted(!todo.isCompleted)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(PaddingMedium))

            // 4. Pie de tarjeta: Due Date, Pin y More menu
            TodoCardFooter(
                todo = todo,
                onTogglePinned = onTogglePinned,
                onEdit = onEdit,
                onDelete = onDelete,
                isSelected = isSelected,
                isSelectionMode = isSelectionMode
            )
        }
    }
}

/**
 * Fila interactiva para cada subtarea individual.
 * Posee físicas de resorte para el checkbox circular y tachado de texto.
 */
@Composable
private fun TodoSubtaskRow(
    subtask: TodoSubtask,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subtaskCheckScale by animateFloatAsState(
        targetValue = if (subtask.isDone) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "subtaskScale"
    )
    val subtaskBgColor by animateColorAsState(
        targetValue = if (subtask.isDone) DraftoTheme.colors.accent else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "subtaskBg"
    )
    val subtaskBorderColor by animateColorAsState(
        targetValue = if (subtask.isDone) DraftoTheme.colors.accent else MaterialTheme.colorScheme.onSurface,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "subtaskBorder"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(DraftoShapePill)
            .clickable(onClick = onToggle)
            .padding(vertical = PaddingExtraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(TodoSubtaskCheckboxSize)
                .graphicsLayer {
                    scaleX = subtaskCheckScale
                    scaleY = subtaskCheckScale
                }
                .clip(CircleShape)
                .background(subtaskBgColor)
                .border(
                    width = TodoSubtaskBorderWidth,
                    color = subtaskBorderColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (subtask.isDone) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = DraftoTheme.colors.onAccent,
                    modifier = Modifier.size(TodoSubtaskCheckIconSize)
                )
            }
        }

        Spacer(modifier = Modifier.width(PaddingMedium))

        Text(
            text = subtask.text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                textDecoration = if (subtask.isDone) TextDecoration.LineThrough else TextDecoration.None
            ),
            color = if (subtask.isDone) DraftoTheme.colors.textMuted else MaterialTheme.colorScheme.onSurface,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Fila de fallback para tareas heredadas sin colección de subtareas.
 */
@Composable
private fun TodoAtomicFallbackRow(
    todo: TodoItem,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val checkScale by animateFloatAsState(
        targetValue = if (todo.isCompleted) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "atomicScale"
    )
    val checkBgColor by animateColorAsState(
        targetValue = if (todo.isCompleted) DraftoTheme.colors.accent else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "atomicBg"
    )
    val checkBorderColor by animateColorAsState(
        targetValue = if (todo.isCompleted) DraftoTheme.colors.accent else MaterialTheme.colorScheme.onSurface,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "atomicBorder"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(DraftoShapePill)
            .clickable(onClick = onToggle)
            .padding(vertical = PaddingExtraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(TodoSubtaskCheckboxSize)
                .graphicsLayer {
                    scaleX = checkScale
                    scaleY = checkScale
                }
                .clip(CircleShape)
                .background(checkBgColor)
                .border(
                    width = TodoSubtaskBorderWidth,
                    color = checkBorderColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (todo.isCompleted) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = DraftoTheme.colors.onAccent,
                    modifier = Modifier.size(TodoSubtaskCheckIconSize)
                )
            }
        }

        Spacer(modifier = Modifier.width(PaddingMedium))

        Text(
            text = todo.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
            ),
            color = if (todo.isCompleted) DraftoTheme.colors.textMuted else MaterialTheme.colorScheme.onSurface,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Pie de tarjeta unificado con bloque de fecha de vencimiento y controles contextuales.
 */
@Composable
private fun TodoCardFooter(
    todo: TodoItem,
    onTogglePinned: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Bloque de fecha de vencimiento: se oculta si no existe
        if (todo.dueDate != null) {
            val isOverdue = remember(todo.dueDate) {
                todo.dueDate < System.currentTimeMillis()
            }
            val dueDateLabelColor = if (isOverdue) {
                DraftoTheme.colors.sunsetCoral
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            }
            val dueDateColor = if (isOverdue) {
                DraftoTheme.colors.sunsetCoral
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            val formattedDate = remember(todo.dueDate) {
                formatTodoDueDateCard(todo.dueDate)
            }

            Column {
                Text(
                    text = stringResource(R.string.todo_due_date_label).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = dueDateLabelColor
                )
                Spacer(modifier = Modifier.height(PaddingMicro))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = dueDateColor
                )
            }
        } else {
            Spacer(modifier = Modifier.weight(1f, fill = false))
        }

        // Acciones: Pin pasivo (visible solo si fijado) y menú de más opciones
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaddingExtraSmall)
        ) {
            if (todo.isPinned) {
                Icon(
                    imageVector = Icons.Rounded.PushPin,
                    contentDescription = stringResource(R.string.action_unpin_note),
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
                                    if (todo.isPinned) R.string.action_unpin_note else R.string.action_pin_note
                                ),
                                isActive = todo.isPinned,
                                onClick = {
                                    menuExpanded = false
                                    onTogglePinned(!todo.isPinned)
                                }
                            )
                            DraftoMenuQuickActionButton(
                                icon = Icons.Rounded.Edit,
                                label = stringResource(R.string.action_edit),
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
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
