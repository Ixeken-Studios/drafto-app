package com.ixeken.drafto.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Input
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.ixeken.drafto.core.R
import com.ixeken.drafto.ui.theme.*
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.hazeBlur

/**
 * Modos de presentación para la barra de navegación dinámica.
 */
@Stable
sealed interface NavBarMode {
    /**
     * Modo estándar con las pestañas principales (Pins, Notebook, Bookmarks, Settings).
     */
    @Immutable
    data class MainTabs(
        val selectedTab: Int = 1
    ) : NavBarMode

    /**
     * Modo contextual para selección de elementos.
     */
    @Immutable
    data class Selection(
        val isPinned: Boolean = false
    ) : NavBarMode

    /**
     * Modo detalle para edición de nota.
     */
    @Immutable
    data object NoteDetail : NavBarMode
}

/**
 * Estados estructurales de presentación para el contenedor AnimatedContent de la barra.
 * Aísla el cambio de pestaña interna de las transiciones de diseño estructurales,
 * evitando recomposiciones duplicadas o artifacts de iluminación en los iconos.
 */
private enum class NavBarLayoutState {
    MAIN_TABS,
    EXPANDED,
    SELECTION,
    NOTE_DETAIL
}

/**
 * Componente principal de la barra de navegación dinámica DraftoNavBar 2.0.
 *
 * Características:
 * - Flotante con elevación y sombras suaves.
 * - Soporte para 6 pestañas con indicador redondeado activo (`#E5E7EB`).
 * - Iconografía estandarizada en formato Rounded y Filled.
 * - Botón lateral derecho (+) con morphing elástico y transición a (X) roja.
 * - Expansión elástica continua (`Spring`) hacia cuadrícula de 2 filas de acciones rápidas.
 * - Modo configurable de desenfoque (`isBlurEnabled`).
 */
@Composable
fun DraftoNavBar(
    mode: NavBarMode,
    onTabSelected: (Int) -> Unit = {},
    onNewTaskClick: () -> Unit = {},
    onNewNoteClick: () -> Unit = {},
    onNewSavedClick: () -> Unit = {},
    onNotificationNoteClick: () -> Unit = {},
    onImportClick: () -> Unit = {},
    onPinClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    isBlurEnabled: Boolean = LocalDraftoBlurEnabled.current,
    hazeState: HazeState? = LocalHazeState.current,
    glassMaterial: DraftoGlassMaterial = DraftoGlassMaterial.UltraThin,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val collapsedHeight = NavBarCollapsedHeight
    val targetHeight = if (isExpanded) NavBarExpandedHeight else collapsedHeight
    val targetCornerRadius = if (isExpanded) RadioNavBarExpanded else RadioNavBar

    val containerHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = DraftoSprings.BouncyDp,
        label = "navBarHeight"
    )

    val capsuleCornerRadius by animateDpAsState(
        targetValue = targetCornerRadius,
        animationSpec = DraftoSprings.BouncyDp,
        label = "capsuleCornerRadius"
    )
    val capsuleShape = RoundedCornerShape(capsuleCornerRadius)

    val isSelectionMode = mode is NavBarMode.Selection
    val isCloseActive = isExpanded || isSelectionMode

    val layoutState = when {
        isExpanded -> NavBarLayoutState.EXPANDED
        mode is NavBarMode.Selection -> NavBarLayoutState.SELECTION
        mode is NavBarMode.NoteDetail -> NavBarLayoutState.NOTE_DETAIL
        else -> NavBarLayoutState.MAIN_TABS
    }

    val currentSelectedTab = (mode as? NavBarMode.MainTabs)?.selectedTab ?: 0
    val isSelectionPinned = (mode as? NavBarMode.Selection)?.isPinned ?: false

    val rightButtonRotation by animateFloatAsState(
        targetValue = if (isCloseActive) 45f else 0f,
        animationSpec = DraftoSprings.BouncyFloat,
        label = "plusButtonRotation"
    )

    // Shape Morphing de M3 Expressive: Círculo (RadioNavBar) a Squircle (RadioNavBarCloseButton)
    val rightButtonCornerRadius by animateDpAsState(
        targetValue = if (isCloseActive) RadioNavBarCloseButton else RadioNavBar,
        animationSpec = DraftoSprings.MorphDp,
        label = "rightButtonCornerRadius"
    )
    val rightButtonMorphShape = RoundedCornerShape(rightButtonCornerRadius)
    val surfaceColor = if (isBlurEnabled && hazeState != null) DraftoTheme.colors.navBarSurfaceTranslucent else DraftoTheme.colors.navBarSurface
    val navShadow = DraftoTheme.colors.navBarShadow

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .padding(
                start = PaddingNavBarHorizontal,
                top = PaddingNavBarTop,
                end = PaddingNavBarHorizontal,
                bottom = PaddingNavBarBottom
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = NavBarMaxWidth)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(PaddingNavBarItemsSpacing)
        ) {
            // Cápsula Principal Dinámica
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(containerHeight)
                    .shadow(
                        elevation = NavBarElevation,
                        shape = capsuleShape,
                        spotColor = navShadow,
                        ambientColor = navShadow
                    )
                    .clip(capsuleShape)
                    .then(
                        if (isBlurEnabled && hazeState != null) {
                            Modifier.hazeBlur(
                                input = HazeInput.Sources(hazeState),
                                style = glassMaterial.toHazeStyle()
                            )
                        } else {
                            Modifier
                        }
                    )
                    .background(surfaceColor),
                contentAlignment = Alignment.Center
            ) {
                // Contenido nítido (íconos, pestañas y acciones)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = PaddingNavBarInnerHorizontal, vertical = PaddingNavBarInnerVertical),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = layoutState,
                        transitionSpec = {
                            (fadeIn(animationSpec = DraftoSprings.BouncyFloat) +
                                    scaleIn(initialScale = 0.92f, animationSpec = DraftoSprings.BouncyFloat))
                                .togetherWith(
                                    fadeOut(animationSpec = DraftoSprings.BouncyFloat) +
                                            scaleOut(targetScale = 0.92f, animationSpec = DraftoSprings.BouncyFloat)
                                )
                        },
                        label = "navBarContentTransition"
                    ) { state ->
                        when (state) {
                            NavBarLayoutState.EXPANDED -> {
                                QuickActionsGrid(
                                    onNewTaskClick = {
                                        isExpanded = false
                                        onNewTaskClick()
                                    },
                                    onNewNoteClick = {
                                        isExpanded = false
                                        onNewNoteClick()
                                    },
                                    onNewSavedClick = {
                                        isExpanded = false
                                        onNewSavedClick()
                                    },
                                    onNotificationNoteClick = {
                                        isExpanded = false
                                        onNotificationNoteClick()
                                    },
                                    onImportClick = {
                                        isExpanded = false
                                        onImportClick()
                                    }
                                )
                            }
                            NavBarLayoutState.MAIN_TABS -> {
                                MainTabsRow(
                                    selectedTab = currentSelectedTab,
                                    onTabSelected = onTabSelected
                                )
                            }
                            NavBarLayoutState.SELECTION -> {
                                SelectionBarRow(
                                    isPinned = isSelectionPinned,
                                    onPinClick = onPinClick,
                                    onShareClick = onShareClick,
                                    onDeleteClick = onDeleteClick
                                )
                            }
                            NavBarLayoutState.NOTE_DETAIL -> {
                                NoteDetailBarRow(
                                    onBackClick = onBackClick,
                                    onSaveClick = onSaveClick,
                                    onShareClick = onShareClick,
                                    onDeleteClick = onDeleteClick
                                )
                            }
                        }
                    }
                }
            }

            // Botón Flotante Derecho (+) / (X) con Shape Morphing elástico
            Box(
                modifier = Modifier
                    .size(collapsedHeight)
                    .shadow(
                        elevation = NavBarElevation,
                        shape = rightButtonMorphShape,
                        spotColor = navShadow,
                        ambientColor = navShadow
                    )
                    .clip(rightButtonMorphShape)
                    .then(
                        if (isBlurEnabled && hazeState != null) {
                            Modifier.hazeBlur(
                                input = HazeInput.Sources(hazeState),
                                style = glassMaterial.toHazeStyle()
                            )
                        } else {
                            Modifier
                        }
                    )
                    .background(surfaceColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (isSelectionMode) {
                            onBackClick()
                        } else {
                            isExpanded = !isExpanded
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Icono nítido (sin blur)
                val iconTint by animateColorAsState(
                    targetValue = if (isCloseActive) DraftoTheme.colors.navBarCloseRed else DraftoTheme.colors.navBarContent,
                    animationSpec = DraftoSprings.BouncyColor,
                    label = "plusIconColor"
                )

                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(if (isCloseActive) R.string.action_cancel else R.string.action_add),
                    tint = iconTint,
                    modifier = Modifier
                        .size(NavBarRightButtonIconSize)
                        .graphicsLayer { rotationZ = rightButtonRotation }
                )
            }
        }
    }
}

/**
 * Fila de las 4 pestañas principales en la barra colapsada con estética Nothing OS 5.
 *
 * Delega en el componente modular y desacoplado [DraftoLiquidDragTabBar] para la navegación
 * híbrida (tap instantáneo + arrastre continuo con Squash & Stretch líquido, Lift on Drag e Icon Bump).
 */
@Composable
private fun MainTabsRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = remember {
        listOf(
            LiquidDragTabItem(icon = Icons.Rounded.Folder, contentDescription = "Collections"),
            LiquidDragTabItem(icon = Icons.Rounded.Book, contentDescription = "Notebook"),
            LiquidDragTabItem(icon = Icons.Rounded.Bookmark, contentDescription = "Bookmarks"),
            LiquidDragTabItem(icon = Icons.Rounded.Settings, contentDescription = "Settings")
        )
    }

    DraftoLiquidDragTabBar(
        selectedIndex = selectedTab,
        tabs = tabs,
        onTabSelected = onTabSelected,
        indicatorSize = NavBarIndicatorSize,
        indicatorElevation = NavBarIndicatorElevation,
        indicatorColor = DraftoTheme.colors.navBarIndicator,
        indicatorShadowColor = DraftoTheme.colors.navBarShadow,
        activeIconColor = DraftoTheme.colors.onNavBarIndicator,
        inactiveIconColor = DraftoTheme.colors.navBarContent,
        iconSize = NavBarTabIconSize
    )
}

/**
 * Cuadrícula de 5 acciones rápidas con botones circulares Nothing OS en 2 filas dentro del panel expandido.
 *
 * Utiliza botones circulares monocromáticos en [DraftoTheme.colors.primaryContainer] e iconos
 * en [DraftoTheme.colors.onPrimaryContainer] con tipografía limpia en [MaterialTheme.typography.labelSmall]
 * debajo del círculo, eliminando colores saturados para un diseño minimalista de alto contraste.
 */
@Composable
private fun QuickActionsGrid(
    onNewTaskClick: () -> Unit,
    onNewNoteClick: () -> Unit,
    onNewSavedClick: () -> Unit,
    onNotificationNoteClick: () -> Unit,
    onImportClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaddingNavBarInnerHorizontal, vertical = PaddingNavBarInnerVertical),
        verticalArrangement = Arrangement.spacedBy(PaddingMedium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Fila 1: 3 botones circulares Nothing OS (To-do, Note, Bookmark)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            QuickActionButton(
                icon = Icons.Rounded.TaskAlt,
                label = stringResource(R.string.action_new_todo),
                onClick = onNewTaskClick,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                icon = Icons.AutoMirrored.Rounded.NoteAdd,
                label = stringResource(R.string.action_new_note),
                onClick = onNewNoteClick,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                icon = Icons.Rounded.BookmarkAdd,
                label = stringResource(R.string.action_new_saved),
                onClick = onNewSavedClick,
                modifier = Modifier.weight(1f)
            )
        }

        // Fila 2: 2 botones circulares Nothing OS centrados (Live note, Import)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {
            Spacer(modifier = Modifier.weight(0.5f))
            QuickActionButton(
                icon = Icons.Rounded.NotificationsActive,
                label = stringResource(R.string.action_notification_note),
                onClick = onNotificationNoteClick,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                icon = Icons.AutoMirrored.Rounded.Input,
                label = stringResource(R.string.action_import),
                onClick = onImportClick,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.weight(0.5f))
        }
    }
}

/**
 * Botón circular individual de acción rápida Nothing OS con etiqueta centrada debajo.
 */
@Composable
@NonRestartableComposable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(NavBarActionCircleSize)
                .shadow(
                    elevation = NavBarActionElevation,
                    shape = CircleShape,
                    spotColor = DraftoTheme.colors.navBarShadow,
                    ambientColor = DraftoTheme.colors.navBarShadow
                )
                .clip(CircleShape)
                .background(DraftoTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = DraftoTheme.colors.onBackground,
                modifier = Modifier.size(NavBarActionIconSize)
            )
        }

        Spacer(modifier = Modifier.height(PaddingExtraSmall))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            ),
            color = DraftoTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Botón individual para barra contextual con contenedor circular y glifo adaptativo.
 */
@Composable
@NonRestartableComposable
private fun ContextualActionButton(
    icon: ImageVector,
    label: String,
    badgeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(NavBarContextualButtonSize)
                .clip(CircleShape)
                .background(badgeColor.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = badgeColor,
                modifier = Modifier.size(NavBarContextualIconSize)
            )
        }
    }
}

/**
 * Barra contextual para modo selección de elementos.
 */
@Composable
private fun SelectionBarRow(
    isPinned: Boolean,
    onPinClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaddingSmall),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContextualActionButton(
            icon = Icons.Rounded.PushPin,
            label = if (isPinned) stringResource(R.string.action_unpin) else stringResource(R.string.action_pin),
            badgeColor = DraftoTheme.colors.sunsetCoral,
            onClick = onPinClick,
            modifier = Modifier.weight(1f)
        )
        ContextualActionButton(
            icon = Icons.Rounded.Share,
            label = stringResource(R.string.action_share),
            badgeColor = DraftoTheme.colors.aquaCyan,
            onClick = onShareClick,
            modifier = Modifier.weight(1f)
        )
        ContextualActionButton(
            icon = Icons.Rounded.Delete,
            label = stringResource(R.string.action_delete),
            badgeColor = DraftoTheme.colors.navBarCloseRed,
            onClick = onDeleteClick,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Barra contextual para modo detalle de notas.
 */
@Composable
private fun NoteDetailBarRow(
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaddingSmall),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContextualActionButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            label = stringResource(R.string.action_back),
            badgeColor = DraftoTheme.colors.cardBorderHighlight,
            onClick = onBackClick,
            modifier = Modifier.weight(1f)
        )
        ContextualActionButton(
            icon = Icons.Rounded.Save,
            label = stringResource(R.string.action_save),
            badgeColor = DraftoTheme.extraColors.save,
            onClick = onSaveClick,
            modifier = Modifier.weight(1f)
        )
        ContextualActionButton(
            icon = Icons.Rounded.Share,
            label = stringResource(R.string.action_share),
            badgeColor = DraftoTheme.colors.aquaCyan,
            onClick = onShareClick,
            modifier = Modifier.weight(1f)
        )
        ContextualActionButton(
            icon = Icons.Rounded.Delete,
            label = stringResource(R.string.action_delete),
            badgeColor = DraftoTheme.colors.navBarCloseRed,
            onClick = onDeleteClick,
            modifier = Modifier.weight(1f)
        )
    }
}
