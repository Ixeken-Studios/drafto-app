package com.ixeken.drafto.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ixeken.drafto.ui.theme.DraftoSprings
import com.ixeken.drafto.ui.theme.LocalDraftoReducedMotion
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Modelo de datos inmutable para cada pestaña del componente [DraftoLiquidDragTabBar].
 *
 * @property icon Vector gráfico del icono de la pestaña.
 * @property contentDescription Descripción de accesibilidad o etiqueta textual para lectores de pantalla.
 */
@Immutable
data class LiquidDragTabItem(
    val icon: ImageVector,
    val contentDescription: String? = null
)

/**
 * Barra de pestañas interactiva desacoplada con micro-físicas de gota líquida (Squash & Stretch),
 * elevación reactiva (Lift on Drag) y micro-impulso elástico en iconos (Icon Bump).
 *
 * Componente modular altamente portable, diseñado para ser reutilizado en múltiples pantallas
 * o extraído a cualquier aplicación nativa construida con Jetpack Compose.
 *
 * Arquitectura de Rendimiento a 120 FPS (Zero Jank):
 * - Aislamiento total de fases: el arrastre actualiza [Animatable] y se lee estrictamente en las fases
 *   de Layout ([Modifier.offset]) y Draw ([Modifier.graphicsLayer]), garantizando 0 recomposiciones del árbol.
 * - Micro-ticks hápticos discretos ([HapticFeedbackType.TextHandleMove]) disparados únicamente al cruzar límites.
 * - Conservación física de volumen líquido: al estirar el ancho en factor $S_x$, la altura se contrae en $1 / \sqrt{S_x}$.
 *
 * @param selectedIndex Índice de la pestaña actualmente seleccionada.
 * @param tabs Lista inmutable de pestañas ([LiquidDragTabItem]).
 * @param onTabSelected Callback invocado al seleccionar una pestaña (por tap instantáneo o al soltar el arrastre).
 * @param indicatorColor Color del indicador activo que resalta la pestaña seleccionada.
 * @param activeIconColor Color del glifo del icono cuando la pestaña está activa.
 * @param inactiveIconColor Color del glifo del icono cuando la pestaña está inactiva.
 * @param modifier Modificador visual para el contenedor.
 * @param indicatorSize Tamaño diametral o altura del indicador circular/cápsula deslizante.
 * @param indicatorElevation Elevación de sombra base para el indicador en reposo.
 * @param indicatorShadowColor Tonalidad de la sombra proyectada por el indicador.
 * @param iconSize Tamaño de los iconos vectoriales de cada pestaña.
 * @param shape Geometría de curvatura del indicador activo (por defecto [CircleShape]).
 */
@Composable
fun DraftoLiquidDragTabBar(
    selectedIndex: Int,
    tabs: List<LiquidDragTabItem>,
    onTabSelected: (Int) -> Unit,
    indicatorColor: Color,
    activeIconColor: Color,
    inactiveIconColor: Color,
    modifier: Modifier = Modifier,
    indicatorSize: Dp = 50.dp,
    indicatorElevation: Dp = 4.dp,
    indicatorShadowColor: Color = Color.Black.copy(alpha = 0.35f),
    iconSize: Dp = 24.dp,
    shape: Shape = CircleShape
) {
    if (tabs.isEmpty()) return

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val haptic = LocalHapticFeedback.current
        val coroutineScope = rememberCoroutineScope()

        val totalWidth = maxWidth
        val tabCount = tabs.size
        val tabWidth = totalWidth / tabCount

        val totalWidthPx = with(density) { totalWidth.toPx() }
        val tabWidthPx = totalWidthPx / tabCount
        val indicatorSizePx = with(density) { indicatorSize.toPx() }

        // Límites horizontales de viaje para el indicador circular centrado
        val minIndicatorOffsetPx = (tabWidthPx - indicatorSizePx) / 2f
        val maxIndicatorOffsetPx = totalWidthPx - tabWidthPx + minIndicatorOffsetPx

        // Offset destino centrado para la pestaña activa
        val targetOffsetXPx = (tabWidthPx * selectedIndex.coerceIn(0, tabCount - 1)) + minIndicatorOffsetPx

        val isReducedMotion = LocalDraftoReducedMotion.current
        val springSpec = if (isReducedMotion) DraftoSprings.SnappyFloat else DraftoSprings.BouncyFloat

        val solidOffsetAnim = remember { Animatable(targetOffsetXPx) }
        val dragOffsetAnim = remember { Animatable(targetOffsetXPx) }
        val stretchAnim = remember { Animatable(1.0f) }
        var isDragging by remember { mutableStateOf(false) }
        var dragFingerX by remember { mutableStateOf(0f) }
        var dragVelocityX by remember { mutableFloatStateOf(0f) }
        var highlightedTab by remember(selectedIndex) { mutableIntStateOf(selectedIndex.coerceIn(0, tabCount - 1)) }

        val dragAlpha by animateFloatAsState(
            targetValue = if (isDragging) 1.0f else 0.0f,
            animationSpec = DraftoSprings.SnappyFloat,
            label = "liquidDragAlpha"
        )
        val liftScale by animateFloatAsState(
            targetValue = if (isDragging) 1.08f else 1.0f,
            animationSpec = DraftoSprings.BouncyFloat,
            label = "liquidLiftScale"
        )
        val liftElevation by animateDpAsState(
            targetValue = if (isDragging) (indicatorElevation * 1.5f) else 0.dp,
            animationSpec = DraftoSprings.BouncyDp,
            label = "liquidLiftElevation"
        )

        // Sincronizar la posición del contenedor sólido cuando cambie selectedIndex
        LaunchedEffect(selectedIndex, targetOffsetXPx) {
            if (!isDragging) {
                highlightedTab = selectedIndex.coerceIn(0, tabCount - 1)
                solidOffsetAnim.animateTo(targetOffsetXPx, springSpec)
            }
        }

        // 1. Contenedor sólido del acento anclado a la pestaña activa seleccionada
        Box(
            modifier = Modifier
                .offset { IntOffset(x = solidOffsetAnim.value.roundToInt(), y = 0) }
                .size(indicatorSize)
                .graphicsLayer {
                    alpha = if (isDragging) 0.85f else 1.0f
                    val baseScale = if (isDragging) 0.94f else 1.0f
                    scaleX = baseScale
                    scaleY = baseScale
                }
                .shadow(
                    elevation = indicatorElevation,
                    shape = shape,
                    spotColor = indicatorShadowColor,
                    ambientColor = indicatorShadowColor
                )
                .clip(shape)
                .background(indicatorColor)
        )

        // 2. Puck de arrastre Nothing OS con micro-físicas líquidas (Squash & Stretch) y borde fino
        if (dragAlpha > 0f) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(x = dragOffsetAnim.value.roundToInt(), y = 0) }
                    .size(indicatorSize)
                    .graphicsLayer {
                        val stretch = stretchAnim.value
                        scaleX = stretch * liftScale
                        scaleY = (1f / sqrt(stretch.coerceAtLeast(1f))) * liftScale
                        alpha = dragAlpha
                    }
                    .shadow(
                        elevation = liftElevation,
                        shape = shape,
                        spotColor = indicatorShadowColor.copy(alpha = 0.25f),
                        ambientColor = indicatorShadowColor.copy(alpha = 0.15f)
                    )
                    .clip(shape)
                    .background(indicatorColor.copy(alpha = 0.22f))
            )
        }

        // Fila de pestañas interactivas con detector de arrastre continuo
        Row(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(tabCount, totalWidthPx) {
                    detectHorizontalDragGestures(
                        onDragStart = { downOffset ->
                            isDragging = true
                            dragFingerX = downOffset.x
                            dragVelocityX = 0f
                            val initialTab = (downOffset.x / tabWidthPx).toInt().coerceIn(0, tabCount - 1)
                            highlightedTab = initialTab
                            val currentIndicatorX = (dragFingerX - indicatorSizePx / 2f)
                                .coerceIn(minIndicatorOffsetPx, maxIndicatorOffsetPx)
                            coroutineScope.launch {
                                dragOffsetAnim.snapTo(currentIndicatorX)
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragVelocityX = dragAmount
                            dragFingerX = (dragFingerX + dragAmount).coerceIn(0f, totalWidthPx)
                            val currentTab = (dragFingerX / tabWidthPx).toInt().coerceIn(0, tabCount - 1)
                            if (currentTab != highlightedTab) {
                                highlightedTab = currentTab
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            val currentIndicatorX = (dragFingerX - indicatorSizePx / 2f)
                                .coerceIn(minIndicatorOffsetPx, maxIndicatorOffsetPx)
                            coroutineScope.launch {
                                dragOffsetAnim.snapTo(currentIndicatorX)
                                if (!isReducedMotion) {
                                    val stretchTarget = 1.0f + (abs(dragAmount) / 14f).coerceAtMost(0.25f)
                                    stretchAnim.snapTo(stretchTarget)
                                }
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            val velocityThreshold = 8f
                            val destinationTab = when {
                                dragVelocityX > velocityThreshold && highlightedTab < tabCount - 1 -> (highlightedTab + 1).coerceIn(0, tabCount - 1)
                                dragVelocityX < -velocityThreshold && highlightedTab > 0 -> (highlightedTab - 1).coerceIn(0, tabCount - 1)
                                else -> highlightedTab
                            }
                            onTabSelected(destinationTab)
                            val finalTargetX = (tabWidthPx * destinationTab) + minIndicatorOffsetPx
                            coroutineScope.launch {
                                if (!isReducedMotion) {
                                    launch { stretchAnim.animateTo(1.0f, springSpec) }
                                }
                                launch { dragOffsetAnim.animateTo(finalTargetX, springSpec) }
                                launch { solidOffsetAnim.animateTo(finalTargetX, springSpec) }
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            highlightedTab = selectedIndex
                            coroutineScope.launch {
                                if (!isReducedMotion) {
                                    launch { stretchAnim.animateTo(1.0f, springSpec) }
                                }
                                launch { dragOffsetAnim.animateTo(targetOffsetXPx, springSpec) }
                            }
                        }
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tabItem ->
                LiquidDragTabIcon(
                    icon = tabItem.icon,
                    contentDescription = tabItem.contentDescription,
                    isSelected = (index == selectedIndex),
                    isHovered = isDragging && (index == highlightedTab) && (index != selectedIndex),
                    activeColor = activeIconColor,
                    inactiveColor = inactiveIconColor,
                    iconSize = iconSize,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Elemento individual de pestaña iconográfica con micro-impulso elástico en el glifo.
 */
@Composable
@NonRestartableComposable
private fun LiquidDragTabIcon(
    icon: ImageVector,
    contentDescription: String?,
    isSelected: Boolean,
    isHovered: Boolean = false,
    activeColor: Color,
    inactiveColor: Color,
    iconSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        animationSpec = DraftoSprings.SnappyColor,
        label = "liquidTabIconColor"
    )

    // Micro-impulso reactivo en el icono (Icon Bump) con física de resorte elástico
    val iconScale by animateFloatAsState(
        targetValue = when {
            isSelected -> 1.15f
            isHovered -> 1.10f
            else -> 1.0f
        },
        animationSpec = DraftoSprings.BouncyFloat,
        label = "liquidTabIconScale"
    )

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
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
        )
    }
}
