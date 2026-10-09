package com.ixeken.drafto.ui.components

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.ixeken.drafto.core.R
import com.ixeken.drafto.ui.theme.BorderWidthThin
import com.ixeken.drafto.ui.theme.DraftoShapeCard
import com.ixeken.drafto.ui.theme.DraftoShapeCircle
import com.ixeken.drafto.ui.theme.DraftoShapePill
import com.ixeken.drafto.ui.theme.DraftoShapePillButton
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.FrauncesFontFamily
import com.ixeken.drafto.ui.theme.PaddingExtraSmall
import com.ixeken.drafto.ui.theme.PaddingLarge
import com.ixeken.drafto.ui.theme.PaddingMicro
import com.ixeken.drafto.ui.theme.PaddingScreenHorizontal
import com.ixeken.drafto.ui.theme.PaddingSmall
import com.ixeken.drafto.ui.theme.SheetCardGroupSpacing
import com.ixeken.drafto.ui.theme.WalkthroughCardMinHeight
import com.ixeken.drafto.ui.theme.WalkthroughDotActiveWidth
import com.ixeken.drafto.ui.theme.WalkthroughDotHeight
import com.ixeken.drafto.ui.theme.WalkthroughDotInactiveWidth
import com.ixeken.drafto.ui.theme.WalkthroughDotSpacing
import com.ixeken.drafto.ui.theme.WalkthroughFaviconContainerSize
import com.ixeken.drafto.ui.theme.WalkthroughIconContainerSize
import com.ixeken.drafto.ui.theme.WalkthroughIconSize
import com.ixeken.drafto.ui.theme.WalkthroughNavButtonHeight
import com.ixeken.drafto.ui.theme.WalkthroughQuoteBarHeight
import com.ixeken.drafto.ui.theme.WalkthroughQuoteBarWidth
import com.ixeken.drafto.ui.theme.WalkthroughTodoCheckIconSize
import com.ixeken.drafto.ui.theme.WalkthroughTodoCheckboxSize
import kotlinx.coroutines.launch

/**
 * Hoja modal canónica de recorrido inicial y bienvenida de Drafto (Nothing OS Editorial).
 *
 * Se diseñó utilizando un [HorizontalPager] con 5 páginas que desglosan minuciosamente
 * los pilares de la aplicación (Colecciones escalonadas, Notas Markdown, Tareas, Marcadores
 * web y Privacidad total 100% fuera de línea). Se prescinde de contadores numéricos en la
 * cabecera en favor de un indicador de píldoras animadas al pie y controles adaptativos de
 * navegación ('Saltar' / 'Atrás' y 'Siguiente' / '¡Comenzar!').
 *
 * @param onDismiss Callback ejecutado al completar o descartar el recorrido.
 * @param modifier Modificador de composición.
 * @param sheetState Estado persistente del BottomSheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftoWalkthroughBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pagerState = rememberPagerState(pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()

    DraftoModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaddingScreenHorizontal)
        ) {
            // Encabezado canónico con tipografía editorial Fraunces
            DraftoSheetHeader(
                title = stringResource(R.string.walkthrough_title),
                subtitle = stringResource(R.string.walkthrough_subtitle)
            )

            Spacer(modifier = Modifier.height(SheetCardGroupSpacing))

            // Tarjeta contenedora grupal Nothing OS que hospeda las páginas horizontales
            DraftoSheetCardGroup(
                backgroundColor = DraftoTheme.colors.primaryContainer
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = WalkthroughCardMinHeight)
                        .padding(PaddingLarge)
                ) { page ->
                    when (page) {
                        0 -> WalkthroughCollectionsPage()
                        1 -> WalkthroughNotesPage()
                        2 -> WalkthroughTodosPage()
                        3 -> WalkthroughBookmarksPage()
                        else -> WalkthroughPrivacyPage()
                    }
                }
            }

            Spacer(modifier = Modifier.height(SheetCardGroupSpacing))

            // Barra inferior: Indicador de páginas en píldoras y controles de acción
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = PaddingExtraSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicadores tipo píldora Nothing OS con ancho reactivo animado
                Row(
                    horizontalArrangement = Arrangement.spacedBy(WalkthroughDotSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(5) { index ->
                        val isSelected = pagerState.currentPage == index
                        val dotWidth by animateDpAsState(
                            targetValue = if (isSelected) WalkthroughDotActiveWidth else WalkthroughDotInactiveWidth,
                            label = "walkthroughDotWidthAnimation"
                        )
                        Box(
                            modifier = Modifier
                                .height(WalkthroughDotHeight)
                                .width(dotWidth)
                                .clip(DraftoShapePill)
                                .background(
                                    if (isSelected) DraftoTheme.colors.accent
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f)
                                )
                                .clickable {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                        )
                    }
                }

                // Botones de acción (Secundario y Primario)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PaddingSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón secundario: 'Saltar' en la primera página o 'Atrás' en las posteriores
                    val isFirstPage = pagerState.currentPage == 0
                    OutlinedButton(
                        onClick = {
                            if (isFirstPage) {
                                onDismiss()
                            } else {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        },
                        shape = DraftoShapePillButton,
                        modifier = Modifier.height(WalkthroughNavButtonHeight),
                        border = androidx.compose.foundation.BorderStroke(
                            BorderWidthThin,
                            DraftoTheme.colors.cardBorderHighlight
                        )
                    ) {
                        Text(
                            text = if (isFirstPage) {
                                stringResource(R.string.walkthrough_action_skip)
                            } else {
                                stringResource(R.string.walkthrough_action_back)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = DraftoTheme.colors.textSecondary
                        )
                    }

                    // Botón primario: 'Siguiente' en páginas intermedias o '¡Comenzar!' en la última
                    val isLastPage = pagerState.currentPage == 4
                    Button(
                        onClick = {
                            if (isLastPage) {
                                onDismiss()
                            } else {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        shape = DraftoShapePillButton,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DraftoTheme.colors.accent,
                            contentColor = DraftoTheme.colors.onAccent
                        ),
                        modifier = Modifier.height(WalkthroughNavButtonHeight)
                    ) {
                        Text(
                            text = if (isLastPage) {
                                stringResource(R.string.walkthrough_action_start)
                            } else {
                                stringResource(R.string.walkthrough_action_next)
                            },
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            color = DraftoTheme.colors.onAccent
                        )
                        Spacer(modifier = Modifier.width(PaddingExtraSmall))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = DraftoTheme.colors.onAccent,
                            modifier = Modifier.size(PaddingLarge)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(PaddingSmall))
        }
    }
}

/**
 * Página 1: Explicación de las carpetas escalonadas Nothing OS y colecciones.
 */
@Composable
private fun WalkthroughCollectionsPage() {
    WalkthroughPageTemplate(
        icon = Icons.Rounded.Folder,
        title = stringResource(R.string.walkthrough_step_collections_title),
        description = stringResource(R.string.walkthrough_step_collections_desc)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(DraftoShapeCard)
                .background(DraftoTheme.colors.cardSurface)
                .border(BorderWidthThin, DraftoTheme.colors.cardBorder, DraftoShapeCard)
                .padding(horizontal = PaddingLarge, vertical = PaddingSmall),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PaddingSmall)
            ) {
                Box(
                    modifier = Modifier
                        .size(PaddingSmall)
                        .clip(DraftoShapeCircle)
                        .background(DraftoTheme.colors.accent)
                )
                Text(
                    text = stringResource(R.string.walkthrough_step_collections_badge),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = stringResource(R.string.walkthrough_step_collections_count_demo),
                style = MaterialTheme.typography.bodySmall,
                color = DraftoTheme.colors.textMuted
            )
        }
    }
}

/**
 * Página 2: Explicación del editor de notas Markdown y tipografía editorial.
 */
@Composable
private fun WalkthroughNotesPage() {
    WalkthroughPageTemplate(
        icon = Icons.Rounded.Description,
        title = stringResource(R.string.walkthrough_step_notes_title),
        description = stringResource(R.string.walkthrough_step_notes_desc)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(DraftoShapeCard)
                .background(DraftoTheme.colors.cardSurface)
                .border(BorderWidthThin, DraftoTheme.colors.cardBorder, DraftoShapeCard)
                .padding(PaddingSmall)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PaddingExtraSmall)
            ) {
                Text(
                    text = "#",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = DraftoTheme.colors.accent
                )
                Text(
                    text = stringResource(R.string.walkthrough_step_notes_badge),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(PaddingMicro))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = PaddingSmall)
            ) {
                Box(
                    modifier = Modifier
                        .width(WalkthroughQuoteBarWidth)
                        .height(WalkthroughQuoteBarHeight)
                        .background(DraftoTheme.colors.accent)
                )
                Spacer(modifier = Modifier.width(PaddingSmall))
                Text(
                    text = stringResource(R.string.walkthrough_step_notes_quote_demo),
                    style = MaterialTheme.typography.bodySmall,
                    color = DraftoTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Página 3: Explicación de tareas, subtareas y seguimiento con casilla interactiva.
 */
@Composable
private fun WalkthroughTodosPage() {
    var isChecked by remember { mutableStateOf(true) }

    WalkthroughPageTemplate(
        icon = Icons.Rounded.CheckCircle,
        title = stringResource(R.string.walkthrough_step_todos_title),
        description = stringResource(R.string.walkthrough_step_todos_desc)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(DraftoShapeCard)
                .background(DraftoTheme.colors.cardSurface)
                .border(BorderWidthThin, DraftoTheme.colors.cardBorder, DraftoShapeCard)
                .clickable { isChecked = !isChecked }
                .padding(horizontal = PaddingLarge, vertical = PaddingSmall),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PaddingSmall)
            ) {
                Box(
                    modifier = Modifier
                        .size(WalkthroughTodoCheckboxSize)
                        .clip(DraftoShapeCard)
                        .background(if (isChecked) DraftoTheme.colors.accent else androidx.compose.ui.graphics.Color.Transparent)
                        .border(
                            BorderWidthThin,
                            if (isChecked) DraftoTheme.colors.accent else DraftoTheme.colors.cardBorderHighlight,
                            DraftoShapeCard
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isChecked) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = DraftoTheme.colors.onAccent,
                            modifier = Modifier.size(WalkthroughTodoCheckIconSize)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.walkthrough_step_todos_demo_item),
                    style = MaterialTheme.typography.labelMedium,
                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (isChecked) DraftoTheme.colors.textMuted else MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = if (isChecked) "100%" else "0%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = DraftoTheme.colors.accent
            )
        }
    }
}

/**
 * Página 4: Explicación de los marcadores web y previsualizaciones locales.
 */
@Composable
private fun WalkthroughBookmarksPage() {
    WalkthroughPageTemplate(
        icon = Icons.Rounded.Bookmark,
        title = stringResource(R.string.walkthrough_step_bookmarks_title),
        description = stringResource(R.string.walkthrough_step_bookmarks_desc)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(DraftoShapeCard)
                .background(DraftoTheme.colors.cardSurface)
                .border(BorderWidthThin, DraftoTheme.colors.cardBorder, DraftoShapeCard)
                .padding(horizontal = PaddingLarge, vertical = PaddingSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaddingSmall)
        ) {
            Box(
                modifier = Modifier
                    .size(WalkthroughFaviconContainerSize)
                    .clip(DraftoShapeCircle)
                    .background(DraftoTheme.colors.accent.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "W",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = DraftoTheme.colors.accent
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.walkthrough_step_bookmarks_demo_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.walkthrough_step_bookmarks_demo_domain),
                    style = MaterialTheme.typography.bodySmall,
                    color = DraftoTheme.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Página 5: Explicación de privacidad absoluta, almacenamiento local Room y ausencia de telemetría.
 */
@Composable
private fun WalkthroughPrivacyPage() {
    WalkthroughPageTemplate(
        icon = Icons.Rounded.Security,
        title = stringResource(R.string.walkthrough_step_privacy_title),
        description = stringResource(R.string.walkthrough_step_privacy_desc)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(DraftoShapeCard)
                .background(DraftoTheme.colors.cardSurface)
                .border(BorderWidthThin, DraftoTheme.colors.cardBorder, DraftoShapeCard)
                .padding(horizontal = PaddingLarge, vertical = PaddingSmall),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PaddingSmall)
            ) {
                Box(
                    modifier = Modifier
                        .size(PaddingSmall)
                        .clip(DraftoShapeCircle)
                        .background(DraftoTheme.colors.emerald)
                )
                Text(
                    text = stringResource(R.string.walkthrough_step_privacy_badge),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = stringResource(R.string.walkthrough_step_privacy_status),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = DraftoTheme.colors.emerald
            )
        }
    }
}

/**
 * Plantilla base cohesiva para cada una de las diapositivas del recorrido horizontal.
 */
@Composable
private fun WalkthroughPageTemplate(
    icon: ImageVector,
    title: String,
    description: String,
    demoContent: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PaddingSmall)
            ) {
                Box(
                    modifier = Modifier
                        .size(WalkthroughIconContainerSize)
                        .clip(DraftoShapeCard)
                        .background(DraftoTheme.colors.accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = DraftoTheme.colors.accent,
                        modifier = Modifier.size(WalkthroughIconSize)
                    )
                }
                Text(
                    text = title,
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(PaddingSmall))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = DraftoTheme.colors.textSecondary,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
            )
        }

        Spacer(modifier = Modifier.height(PaddingLarge))

        demoContent()
    }
}
