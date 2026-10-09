package com.ixeken.drafto.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ixeken.drafto.core.R
import com.ixeken.drafto.domain.model.BookmarkCollection
import com.ixeken.drafto.ui.theme.DraftoAquaCyan
import com.ixeken.drafto.ui.theme.DraftoElectricMint
import com.ixeken.drafto.ui.theme.DraftoShapeActionBadge
import com.ixeken.drafto.ui.theme.DraftoShapeCard
import com.ixeken.drafto.ui.theme.DraftoShapeCardSmall
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.DraftoTransitions
import com.ixeken.drafto.ui.theme.LocalDraftoReducedMotion
import com.ixeken.drafto.ui.theme.PaddingScreenHorizontal
import com.ixeken.drafto.ui.theme.contrastContentColor

/**
 * Cápsula interactiva con despliegue tipo acordeón para la gestión y asignación de colecciones
 * en la ficha de detalle de un marcador ([DraftoBookmarkDetailBottomSheet]).
 *
 * Ventaja arquitectural:
 * - Evita abrir un Modal Bottom Sheet secundario anidado sobre otro Bottom Sheet,
 *   lo cual causa conflictos táctiles y anomalías de renderizado en Android.
 * - En estado colapsado ocupa solo 48dp de altura vertical, expandiéndose suavemente
 *   in-line para permitir asignar/desasignar colecciones de inmediato.
 */
@Composable
fun DraftoDetailCollectionAccordionCapsule(
    allCollections: List<BookmarkCollection>,
    assignedCollectionIds: Set<String>,
    onToggleCollection: (collectionId: String, isAssigned: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onCreateCollectionClick: (() -> Unit)? = null,
    horizontalPadding: Dp = PaddingScreenHorizontal
) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "accordionChevronRotation"
    )

    val assignedCollections = remember(allCollections, assignedCollectionIds) {
        allCollections.filter { assignedCollectionIds.contains(it.id) }
    }
    val isDarkTheme = DraftoTheme.colors.isDark

    val cardBg = if (DraftoTheme.colors.isDark) DraftoTheme.colors.bottomSheetInnerCard else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    val cardBorder = DraftoTheme.colors.cardBorder

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 4.dp)
            .clip(DraftoShapeCard)
            .background(cardBg)
            .border(1.dp, cardBorder, DraftoShapeCard)
    ) {
        // Cabecera de la Cápsula (Siempre visible)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Icono de Carpeta con contenedor sólido y glifo temático
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(DraftoShapeActionBadge)
                        .background(DraftoTheme.colors.electricMint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = DraftoTheme.colors.badgeGlyph,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Resumen de asignación activa
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = stringResource(R.string.bookmark_collection_card_label),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                        color = DraftoTheme.colors.textMuted
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (assignedCollections.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(DraftoTheme.colors.textMuted)
                            )
                            Text(
                                text = stringResource(R.string.bookmark_no_collection),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            val first = assignedCollections.first()
                            val firstColor = parseCollectionColor(first.colorHex, isDark = isDarkTheme, fallback = DraftoTheme.colors.electricMint)
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(firstColor)
                            )
                            Text(
                                text = first.name,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (assignedCollections.size > 1) {
                                Text(
                                    text = stringResource(R.string.bookmark_additional_collections_count, assignedCollections.size - 1),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = DraftoTheme.colors.electricMint
                                )
                            }
                        }
                    }
                }
            }

            // Indicador de Despliegue y Chevron
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.bookmark_touch_to_change),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = DraftoTheme.colors.textMuted
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = DraftoTheme.colors.textMuted,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer { rotationZ = rotationAngle }
                )
            }
        }

        val isReducedMotion = LocalDraftoReducedMotion.current

        // Acordeón Desplegable In-line
        AnimatedVisibility(
            visible = isExpanded,
            enter = DraftoTransitions.accordionExpand(isReducedMotion),
            exit = DraftoTransitions.accordionShrink(isReducedMotion)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Divisor sutil entre cabecera y opciones
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(cardBorder.copy(alpha = 0.5f))
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Opción 1: Ninguna (desasigna todas)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(DraftoShapeCardSmall)
                            .background(if (assignedCollections.isEmpty()) MaterialTheme.colorScheme.surface.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable {
                                assignedCollections.forEach { col ->
                                    onToggleCollection(col.id, false)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(DraftoShapeActionBadge)
                                    .background(DraftoTheme.colors.cardSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = null,
                                    tint = DraftoTheme.colors.textMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Text(
                                text = stringResource(R.string.bookmark_collection_none_option),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (assignedCollections.isEmpty()) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (assignedCollections.isEmpty()) MaterialTheme.colorScheme.onSurface else DraftoTheme.colors.textSecondary
                            )
                        }
                        if (assignedCollections.isEmpty()) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Lista de Colecciones Disponibles
                    allCollections.forEach { collection ->
                        val isAssigned = assignedCollectionIds.contains(collection.id)
                        val colColor = parseCollectionColor(collection.colorHex, isDark = isDarkTheme, fallback = DraftoTheme.colors.warmMango)
                        val colContentColor = colColor.contrastContentColor()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(DraftoShapeCardSmall)
                                .background(if (isAssigned) colColor.copy(alpha = 0.14f) else Color.Transparent)
                                .clickable { onToggleCollection(collection.id, !isAssigned) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(DraftoShapeActionBadge)
                                        .background(colColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getCollectionIcon(collection.iconName),
                                        contentDescription = null,
                                        tint = colContentColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    text = collection.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isAssigned) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isAssigned) colColor else DraftoTheme.colors.textPrimary
                                )
                            }

                            if (isAssigned) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = colColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Botón Crear Nueva Colección
                    if (onCreateCollectionClick != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(DraftoShapeCardSmall)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.08f))
                                .clickable(onClick = onCreateCollectionClick)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(DraftoShapeActionBadge)
                                    .background(DraftoTheme.colors.warmMango),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = DraftoTheme.colors.badgeGlyph,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.bookmark_create_new_collection_btn),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Cápsula de selección de colección con despliegue en acordeón diseñada para pantallas de guardado
 * ([DraftoShareTargetBottomSheet] y [DraftoAddBookmarkBottomSheet]).
 *
 * Ofrece selección única y colapsado automático al seleccionar una opción.
 */
@Composable
fun DraftoSelectCollectionAccordionCapsule(
    allCollections: List<BookmarkCollection>,
    selectedCollectionId: String?,
    onSelectCollection: (collectionId: String?) -> Unit,
    modifier: Modifier = Modifier,
    onCreateCollectionClick: (() -> Unit)? = null,
    horizontalPadding: Dp = PaddingScreenHorizontal
) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "selectAccordionChevronRotation"
    )

    val selectedCollection = remember(allCollections, selectedCollectionId) {
        allCollections.firstOrNull { it.id == selectedCollectionId }
    }
    val isDarkTheme = DraftoTheme.colors.isDark

    val cardBg = if (DraftoTheme.colors.isDark) DraftoTheme.colors.bottomSheetInnerCard else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    val cardBorder = DraftoTheme.colors.cardBorder

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 4.dp)
            .clip(DraftoShapeCard)
            .background(cardBg)
            .border(1.dp, cardBorder, DraftoShapeCard)
    ) {
        // Cabecera Clickeable
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(DraftoShapeActionBadge)
                        .background(DraftoTheme.colors.aquaCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = DraftoTheme.colors.badgeGlyph,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = stringResource(R.string.bookmark_select_collection),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                        color = DraftoTheme.colors.textMuted
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (selectedCollection == null) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(DraftoTheme.colors.textMuted)
                            )
                            Text(
                                text = stringResource(R.string.bookmark_no_collection),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            val colColor = parseCollectionColor(selectedCollection.colorHex, isDark = isDarkTheme, fallback = DraftoTheme.colors.aquaCyan)
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(colColor)
                            )
                            Text(
                                text = selectedCollection.name,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.bookmark_touch_to_change),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = DraftoTheme.colors.textMuted
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = DraftoTheme.colors.textMuted,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer { rotationZ = rotationAngle }
                )
            }
        }

        val isReducedMotion = LocalDraftoReducedMotion.current

        // Acordeón Desplegable
        AnimatedVisibility(
            visible = isExpanded,
            enter = DraftoTransitions.accordionExpand(isReducedMotion),
            exit = DraftoTransitions.accordionShrink(isReducedMotion)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Divisor sutil entre cabecera y opciones
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(cardBorder.copy(alpha = 0.5f))
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Opción: Ninguna
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(DraftoShapeCardSmall)
                            .background(if (selectedCollectionId == null) MaterialTheme.colorScheme.surface.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable {
                                onSelectCollection(null)
                                isExpanded = false
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(DraftoShapeActionBadge)
                                    .background(DraftoTheme.colors.cardSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = null,
                                    tint = DraftoTheme.colors.textMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Text(
                                text = stringResource(R.string.bookmark_collection_none_option),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (selectedCollectionId == null) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (selectedCollectionId == null) MaterialTheme.colorScheme.onSurface else DraftoTheme.colors.textSecondary
                            )
                        }
                        if (selectedCollectionId == null) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Colecciones
                    allCollections.forEach { collection ->
                        val isSelected = collection.id == selectedCollectionId
                        val colColor = parseCollectionColor(collection.colorHex, isDark = isDarkTheme, fallback = DraftoTheme.colors.warmMango)
                        val colContentColor = colColor.contrastContentColor()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(DraftoShapeCardSmall)
                                .background(if (isSelected) colColor.copy(alpha = 0.14f) else Color.Transparent)
                                .clickable {
                                    onSelectCollection(collection.id)
                                    isExpanded = false
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(DraftoShapeActionBadge)
                                        .background(colColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getCollectionIcon(collection.iconName),
                                        contentDescription = null,
                                        tint = colContentColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    text = collection.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) colColor else DraftoTheme.colors.textPrimary
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = colColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Botón Crear Colección
                    if (onCreateCollectionClick != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(DraftoShapeCardSmall)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.08f))
                                .clickable {
                                    onCreateCollectionClick()
                                    isExpanded = false
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(DraftoShapeActionBadge)
                                    .background(DraftoTheme.colors.warmMango),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = DraftoTheme.colors.badgeGlyph,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.bookmark_create_new_collection_btn),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
