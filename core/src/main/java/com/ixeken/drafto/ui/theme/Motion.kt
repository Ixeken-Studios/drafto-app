package com.ixeken.drafto.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Proveedor local de composición para consultar si el modo de movimiento reducido
 * o el modo de ahorro para dispositivos de gama baja está activo.
 */
val LocalDraftoReducedMotion = staticCompositionLocalOf { false }

/**
 * Sistema centralizado de físicas y resortes de Material 3 Expressive para Drafto.
 * Todos los valores son variables editables para calibrar la respuesta táctil global.
 */
object DraftoSprings {
    /**
     * Resorte Bouncy (Rebote Táctil):
     * Usado para botones flotantes, FAB (+), apertura de menús y rotaciones donde el valor no tiene frontera rígida en cero.
     */
    val BouncyFloat: SpringSpec<Float> = spring(
        stiffness = Spring.StiffnessMediumLow,
        dampingRatio = Spring.DampingRatioLowBouncy
    )

    val BouncyDp: SpringSpec<Dp> = spring(
        stiffness = Spring.StiffnessMediumLow,
        dampingRatio = Spring.DampingRatioLowBouncy
    )

    val BouncyOffset: SpringSpec<IntOffset> = spring(
        stiffness = Spring.StiffnessMediumLow,
        dampingRatio = Spring.DampingRatioLowBouncy
    )

    /**
     * Resorte Morph (Transformación de Formas):
     * Usado para interpolación geométrica suave (ej. círculo a squircle, expansión de esquinas).
     */
    val MorphDp: SpringSpec<Dp> = spring(
        stiffness = Spring.StiffnessLow,
        dampingRatio = Spring.DampingRatioMediumBouncy
    )

    val MorphFloat: SpringSpec<Float> = spring(
        stiffness = Spring.StiffnessLow,
        dampingRatio = Spring.DampingRatioMediumBouncy
    )

    /**
     * Resorte Snappy (Respuesta Inmediata y Segura):
     * Usado para transiciones de pestañas horizontales, selectores de cápsula, dimensiones, paddings y colores.
     * Amortiguamiento crítico (DampingRatioNoBouncy = 1.0f) que converge rápidamente sin oscilar ni rebasar los límites (0 a 1 o Dp negativo).
     */
    val SnappyFloat: SpringSpec<Float> = spring(
        stiffness = Spring.StiffnessMedium,
        dampingRatio = Spring.DampingRatioNoBouncy
    )

    val SnappyDp: SpringSpec<Dp> = spring(
        stiffness = Spring.StiffnessMedium,
        dampingRatio = Spring.DampingRatioNoBouncy
    )

    val SnappyOffset: SpringSpec<IntOffset> = spring(
        stiffness = Spring.StiffnessMedium,
        dampingRatio = Spring.DampingRatioNoBouncy
    )

    val SnappySize: SpringSpec<IntSize> = spring(
        stiffness = Spring.StiffnessMedium,
        dampingRatio = Spring.DampingRatioNoBouncy
    )

    val SnappyColor: SpringSpec<Color> = spring(
        stiffness = Spring.StiffnessMedium,
        dampingRatio = Spring.DampingRatioNoBouncy
    )

    /**
     * BouncyColor seguro (DampingRatioNoBouncy para evitar overshoots fuera del rango RGB/Alpha [0, 1]).
     */
    val BouncyColor: SpringSpec<Color> = spring(
        stiffness = Spring.StiffnessMediumLow,
        dampingRatio = Spring.DampingRatioNoBouncy
    )

    /**
     * BouncySize seguro (DampingRatioNoBouncy para evitar dimensiones negativas en IntSize al encogerse).
     */
    val BouncySize: SpringSpec<IntSize> = spring(
        stiffness = Spring.StiffnessMediumLow,
        dampingRatio = Spring.DampingRatioNoBouncy
    )

    /**
     * Resorte de Navegación M3 Expressive (Transición Cinemática Sincronizada):
     * Sincroniza la entrada/salida de pantallas completas y la animación vertical de la DraftoNavBar.
     * Amortiguamiento crítico y rigidez MediumLow para un desplazamiento sedoso y natural.
     */
    val NavTransitionOffset: SpringSpec<IntOffset> = spring(
        stiffness = Spring.StiffnessMediumLow,
        dampingRatio = Spring.DampingRatioNoBouncy
    )

    val NavTransitionFloat: SpringSpec<Float> = spring(
        stiffness = Spring.StiffnessMediumLow,
        dampingRatio = Spring.DampingRatioNoBouncy
    )

    val NavTransitionSize: SpringSpec<IntSize> = spring(
        stiffness = Spring.StiffnessMediumLow,
        dampingRatio = Spring.DampingRatioNoBouncy
    )

    /**
     * Resorte suave para animaciones de expansión y contracción vertical.
     */
    val SearchExpandSpec: SpringSpec<IntSize> = spring(
        stiffness = Spring.StiffnessMediumLow,
        dampingRatio = Spring.DampingRatioNoBouncy
    )

    /**
     * Resorte para transiciones de contenido y alfadeo en Bottom Sheets y diálogos.
     */
    val SheetContentSpec: SpringSpec<Float> = spring(
        stiffness = Spring.StiffnessMediumLow,
        dampingRatio = Spring.DampingRatioNoBouncy
    )

    /**
     * Resorte para respuesta táctil en pulsación de tarjetas y botones en píldora.
     */
    val CardPressSpec: SpringSpec<Float> = spring(
        stiffness = Spring.StiffnessMedium,
        dampingRatio = Spring.DampingRatioLowBouncy
    )
}
