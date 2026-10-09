package com.ixeken.drafto.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ixeken.drafto.core.R
import com.ixeken.drafto.domain.model.BookmarkCollection
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.FolderActionDotsSize
import com.ixeken.drafto.ui.theme.FolderCardCornerRadius
import com.ixeken.drafto.ui.theme.FolderCardHeight
import com.ixeken.drafto.ui.theme.FolderContentPaddingBottom
import com.ixeken.drafto.ui.theme.FolderContentPaddingHorizontal
import com.ixeken.drafto.ui.theme.FolderFlapCurveWidthRatio
import com.ixeken.drafto.ui.theme.FolderFlapShelfHeightRatio
import com.ixeken.drafto.ui.theme.FolderFlapTabHeightRatio
import com.ixeken.drafto.ui.theme.FolderFlapTabWidthRatio
import com.ixeken.drafto.ui.theme.FolderTopIconSize
import com.ixeken.drafto.ui.theme.FolderTopPadding
import com.ixeken.drafto.ui.theme.PaddingMedium
import com.ixeken.drafto.ui.theme.PaddingSmall
import com.ixeken.drafto.ui.theme.VerticalMenuItemIconSize
import com.ixeken.drafto.ui.theme.contrastContentColor

/**
 * Silueta geométrica escalonada para el bolsillo frontal de la carpeta temática (Nothing OS).
 *
 * Traza una pestaña superior izquierda a una altura determinada ([tabHeightRatio]) y realiza
 * una transición suave en curva Bézier hacia un estante horizontal más bajo a la derecha ([shelfHeightRatio]).
 * Diseñado como clase inmutable para garantizar estabilidad total del compilador Compose y Strong Skipping.
 */
@Immutable
class DraftoFolderFlapShape(
    private val tabHeightRatio: Float = FolderFlapTabHeightRatio,
    private val shelfHeightRatio: Float = FolderFlapShelfHeightRatio,
    private val tabWidthRatio: Float = FolderFlapTabWidthRatio,
    private val curveWidthRatio: Float = FolderFlapCurveWidthRatio
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val yTab = size.height * tabHeightRatio
        val yShelf = size.height * shelfHeightRatio
        val xTabEnd = size.width * tabWidthRatio
        val xShelfStart = xTabEnd + (size.width * curveWidthRatio)

        val path = Path().apply {
            moveTo(0f, yTab)
            lineTo(xTabEnd, yTab)
            cubicTo(
                x1 = xTabEnd + (size.width * curveWidthRatio * 0.45f),
                y1 = yTab,
                x2 = xTabEnd + (size.width * curveWidthRatio * 0.55f),
                y2 = yShelf,
                x3 = xShelfStart,
                y3 = yShelf
            )
            lineTo(size.width, yShelf)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

val DefaultDraftoFolderFlapShape = DraftoFolderFlapShape()

/**
 * Tarjeta visual de carpeta temática en Drafto con estética Nothing OS y solapa escalonada.
 *
 * Estructura visual:
 * 1. Base sólida: Fondo con el color de la colección 100% plano (sin degradados) con el ícono temático arriba.
 * 2. Indicador de selección: Ubicado en la esquina superior derecha durante el modo multiselección.
 * 3. Bolsillo frontal escalonado ([DraftoFolderFlapShape]): Superficie mate oscura en modo oscuro o clara en modo claro,
 *    con línea de contorno trazada mediante [drawWithCache] para 120 FPS sin recomposiciones.
 * 4. Metadatos al pie: Nombre de la colección, cantidad de elementos en tono secundario y botón de tres puntos (⋮).
 *
 * @param collection Colección temática a representar.
 * @param bookmarkCount Cantidad de marcadores contenidos.
 * @param onClick Callback invocado al presionar la tarjeta para entrar a la carpeta.
 * @param onEdit Callback invocado para editar nombre, color o icono de la colección.
 * @param onExport Callback invocado para exportar exclusivamente esta colección.
 * @param onDelete Callback invocado para eliminar la colección.
 * @param modifier Modificador Compose opcional.
 * @param previewSnippet Texto de previsualización del recurso más reciente guardado en la carpeta.
 * @param isSelected Indica si la carpeta está seleccionada en modo multiselección.
 * @param isSelectionMode Indica si la pantalla se encuentra en modo multiselección.
 * @param onLongClick Callback invocado al mantener pulsada la carpeta para iniciar selección.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraftoFolderCard(
    collection: BookmarkCollection,
    bookmarkCount: Int = collection.totalCount,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    previewSnippet: String? = null,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val isDarkTheme = DraftoTheme.colors.isDark
    val baseColor = remember(collection.colorHex, isDarkTheme) {
        parseCollectionColor(collection.colorHex, isDark = isDarkTheme)
    }
    val iconTint = remember(baseColor) {
        baseColor.contrastContentColor()
    }
    val flapColor = if (isDarkTheme) DraftoTheme.colors.cardSurface else MaterialTheme.colorScheme.surface
    val flapBorderColor = DraftoTheme.colors.cardBorder

    var isMenuExpanded by remember { mutableStateOf(false) }
    val displayCount = if (bookmarkCount > 0) bookmarkCount else collection.totalCount
    val cardShape = remember { RoundedCornerShape(FolderCardCornerRadius) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(FolderCardHeight)
            .shadow(
                elevation = 4.dp,
                shape = cardShape,
                ambientColor = Color.Black.copy(alpha = if (isDarkTheme) 0.35f else 0.08f),
                spotColor = Color.Black.copy(alpha = if (isDarkTheme) 0.35f else 0.08f)
            )
            .clip(cardShape)
            .background(baseColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) DraftoTheme.colors.accent else DraftoTheme.colors.cardBorder,
                shape = cardShape
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        // 1. Ícono superior de la colección
        Icon(
            imageVector = getCollectionIcon(collection.iconName),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = FolderContentPaddingHorizontal, top = FolderTopPadding)
                .size(FolderTopIconSize)
        )

        // 2. Indicador de selección múltiple en esquina superior derecha
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = FolderContentPaddingHorizontal, top = FolderTopPadding)
            ) {
                DraftoSelectionCheckmarkIndicator(isSelected = isSelected)
            }
        }

        // 3. Bolsillo frontal escalonado con contorno superior trazado en caché
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(DefaultDraftoFolderFlapShape)
                .background(flapColor)
                .drawWithCache {
                    val yTab = size.height * FolderFlapTabHeightRatio
                    val yShelf = size.height * FolderFlapShelfHeightRatio
                    val xTabEnd = size.width * FolderFlapTabWidthRatio
                    val xShelfStart = xTabEnd + (size.width * FolderFlapCurveWidthRatio)

                    val contourPath = Path().apply {
                        moveTo(0f, yTab)
                        lineTo(xTabEnd, yTab)
                        cubicTo(
                            x1 = xTabEnd + (size.width * FolderFlapCurveWidthRatio * 0.45f),
                            y1 = yTab,
                            x2 = xTabEnd + (size.width * FolderFlapCurveWidthRatio * 0.55f),
                            y2 = yShelf,
                            x3 = xShelfStart,
                            y3 = yShelf
                        )
                        lineTo(size.width, yShelf)
                    }
                    val strokeWidth = 1.dp.toPx()
                    onDrawWithContent {
                        drawContent()
                        drawPath(
                            path = contourPath,
                            color = flapBorderColor,
                            style = Stroke(width = strokeWidth)
                        )
                    }
                }
        )

        // 4. Metadatos al pie: Nombre + Marcadores y menú de 3 puntos
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = FolderContentPaddingHorizontal,
                    end = 8.dp,
                    bottom = FolderContentPaddingBottom
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 6.dp)
            ) {
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = DraftoTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = pluralStringResource(
                        R.plurals.collection_item_count_plural,
                        displayCount,
                        displayCount
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = DraftoTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!isSelectionMode) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(FolderActionDotsSize)
                            .clip(CircleShape)
                            .background(DraftoTheme.colors.onCardSurface.copy(alpha = 0.08f))
                            .clickable { isMenuExpanded = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.action_edit),
                            tint = DraftoTheme.colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DraftoVerticalMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        quickActions = {
                            DraftoMenuQuickActionButton(
                                icon = Icons.Rounded.Edit,
                                label = stringResource(R.string.action_edit),
                                onClick = {
                                    isMenuExpanded = false
                                    onEdit()
                                }
                            )
                            DraftoMenuQuickActionButton(
                                icon = Icons.Rounded.FileUpload,
                                label = stringResource(R.string.action_export),
                                onClick = {
                                    isMenuExpanded = false
                                    onExport()
                                }
                            )
                        }
                    ) {
                        DraftoDropdownMenuItem(
                            text = stringResource(R.string.action_delete),
                            isDestructive = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = null,
                                    tint = DraftoTheme.colors.sunsetCoral,
                                    modifier = Modifier.size(VerticalMenuItemIconSize)
                                )
                            },
                            onClick = {
                                isMenuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta interactiva para crear una nueva carpeta o colección en la cuadrícula de carpetas.
 * Sigue las mismas dimensiones (156.dp de altura) y curvatura base que [DraftoFolderCard].
 *
 * @param onClick Callback invocado al presionar la tarjeta para crear una colección.
 * @param modifier Modificador Compose opcional.
 */
@Composable
fun DraftoCreateFolderCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = remember { RoundedCornerShape(FolderCardCornerRadius) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(FolderCardHeight)
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = cardShape
            )
            .clickable(onClick = onClick)
            .padding(PaddingMedium),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.bookmark_menu_create_collection),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(PaddingSmall))
            Text(
                text = stringResource(R.string.bookmark_menu_create_collection),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
