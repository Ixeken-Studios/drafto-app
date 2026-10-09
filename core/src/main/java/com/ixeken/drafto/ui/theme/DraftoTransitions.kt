package com.ixeken.drafto.ui.theme

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.TransformOrigin

/**
 * Sistema centralizado desacoplado de transiciones y físicas de movimiento para Drafto.
 *
 * Funciona como el punto único de evolución (Single Point of Evolution) para todas las
 * animaciones de entrada, salida y contenido de la aplicación.
 *
 * Ventajas arquitecturales:
 * - Elimina la duplicación de bloques de animación inline en componentes visuales.
 * - Soporte nativo para el modo de movimiento reducido (Reduced Motion) y dispositivos
 *   de gama baja: cuando [isReduced] es true, los desplazamientos espaciales y deformaciones
 *   se omiten en favor de fundidos instantáneos de 150ms.
 * - Cumple con la filosofía de Emil Kowalski: timing asimétrico, salidas amortiguadas
 *   críticamente sin rebote y simetría en puntos de anclaje.
 */
object DraftoTransitions {

    private const val REDUCED_DURATION_MS = 150

    /**
     * Transición de entrada para la notificación flotante [DraftoFloatingToast].
     *
     * Entra desde el borde inferior de la pantalla con elevación elástica y ligera escala
     * física (0.95f) para evitar que aparezca de la nada.
     */
    fun toastEnter(isReduced: Boolean = false): EnterTransition {
        if (isReduced) {
            return fadeIn(animationSpec = tween(durationMillis = REDUCED_DURATION_MS))
        }
        return slideInVertically(
            initialOffsetY = { height -> height },
            animationSpec = DraftoSprings.SnappyOffset
        ) + fadeIn(animationSpec = DraftoSprings.SnappyFloat) +
                scaleIn(initialScale = 0.95f, animationSpec = DraftoSprings.SnappyFloat)
    }

    /**
     * Transición de salida simétrica para [DraftoFloatingToast].
     *
     * Sale completamente hacia el borde inferior con amortiguamiento crítico (sin rebote).
     */
    fun toastExit(isReduced: Boolean = false): ExitTransition {
        if (isReduced) {
            return fadeOut(animationSpec = tween(durationMillis = REDUCED_DURATION_MS))
        }
        return slideOutVertically(
            targetOffsetY = { height -> height },
            animationSpec = DraftoSprings.SnappyOffset
        ) + fadeOut(animationSpec = DraftoSprings.SnappyFloat) +
                scaleOut(targetScale = 0.95f, animationSpec = DraftoSprings.SnappyFloat)
    }

    /**
     * Transición de entrada para la cápsula flotante de selección múltiple [DraftoSelectionFloatingDock].
     */
    fun selectionDockEnter(isReduced: Boolean = false): EnterTransition {
        if (isReduced) {
            return fadeIn(animationSpec = tween(durationMillis = REDUCED_DURATION_MS))
        }
        return slideInVertically(
            initialOffsetY = { height -> height * 2 },
            animationSpec = DraftoSprings.SnappyOffset
        ) + fadeIn(animationSpec = DraftoSprings.SnappyFloat)
    }

    /**
     * Transición de salida para [DraftoSelectionFloatingDock].
     *
     * Usa resorte Snappy sin rebote para evitar sobreoscilación en el límite inferior.
     */
    fun selectionDockExit(isReduced: Boolean = false): ExitTransition {
        if (isReduced) {
            return fadeOut(animationSpec = tween(durationMillis = REDUCED_DURATION_MS))
        }
        return slideOutVertically(
            targetOffsetY = { height -> height * 2 },
            animationSpec = DraftoSprings.SnappyOffset
        ) + fadeOut(animationSpec = DraftoSprings.SnappyFloat)
    }

    /**
     * Transición de entrada para el botón de acción flotante en cápsula [DraftoFloatingActionCapsule].
     */
    fun floatingActionEnter(isReduced: Boolean = false): EnterTransition {
        if (isReduced) {
            return fadeIn(animationSpec = tween(durationMillis = REDUCED_DURATION_MS))
        }
        return slideInVertically(
            initialOffsetY = { height -> height * 2 },
            animationSpec = DraftoSprings.SnappyOffset
        ) + fadeIn(animationSpec = DraftoSprings.SnappyFloat) +
                scaleIn(initialScale = 0.92f, animationSpec = DraftoSprings.SnappyFloat)
    }

    /**
     * Transición de salida para [DraftoFloatingActionCapsule].
     */
    fun floatingActionExit(isReduced: Boolean = false): ExitTransition {
        if (isReduced) {
            return fadeOut(animationSpec = tween(durationMillis = REDUCED_DURATION_MS))
        }
        return slideOutVertically(
            targetOffsetY = { height -> height * 2 },
            animationSpec = DraftoSprings.SnappyOffset
        ) + fadeOut(animationSpec = DraftoSprings.SnappyFloat)
    }

    /**
     * Transición de entrada para la barra de navegación flotante Nothing OS [DraftoNavBar].
     */
    fun navBarEnter(isReduced: Boolean = false): EnterTransition {
        if (isReduced) {
            return fadeIn(animationSpec = tween(durationMillis = REDUCED_DURATION_MS))
        }
        return slideInVertically(
            initialOffsetY = { height -> height * 2 },
            animationSpec = DraftoSprings.NavTransitionOffset
        ) + fadeIn(animationSpec = DraftoSprings.NavTransitionFloat)
    }

    /**
     * Transición de salida para la barra de navegación flotante Nothing OS [DraftoNavBar].
     */
    fun navBarExit(isReduced: Boolean = false): ExitTransition {
        if (isReduced) {
            return fadeOut(animationSpec = tween(durationMillis = REDUCED_DURATION_MS))
        }
        return slideOutVertically(
            targetOffsetY = { height -> height * 2 },
            animationSpec = DraftoSprings.NavTransitionOffset
        ) + fadeOut(animationSpec = DraftoSprings.NavTransitionFloat)
    }

    /**
     * Transición de entrada para la Dynamic Island [DraftoDynamicIsland].
     *
     * Nace geométricamente desde el centro del sensor de la cámara frontal (TopCenter).
     */
    fun dynamicIslandEnter(isReduced: Boolean = false): EnterTransition {
        if (isReduced) {
            return fadeIn(animationSpec = tween(durationMillis = REDUCED_DURATION_MS))
        }
        return scaleIn(
            initialScale = 0.90f,
            transformOrigin = TransformOrigin(0.5f, 0f),
            animationSpec = DraftoSprings.BouncyFloat
        ) + fadeIn(animationSpec = DraftoSprings.BouncyFloat)
    }

    /**
     * Transición de salida para la Dynamic Island [DraftoDynamicIsland].
     */
    fun dynamicIslandExit(isReduced: Boolean = false): ExitTransition {
        if (isReduced) {
            return fadeOut(animationSpec = tween(durationMillis = REDUCED_DURATION_MS))
        }
        return scaleOut(
            targetScale = 0.90f,
            transformOrigin = TransformOrigin(0.5f, 0f),
            animationSpec = DraftoSprings.SnappyFloat
        ) + fadeOut(animationSpec = DraftoSprings.SnappyFloat)
    }

    /**
     * Conmutación de contenido entre título y barra de búsqueda en [DraftoSearchableTopBar].
     */
    fun searchBarContentTransform(
        isSearchActive: Boolean,
        isReduced: Boolean = false
    ): ContentTransform {
        if (isReduced) {
            return fadeIn(animationSpec = tween(durationMillis = REDUCED_DURATION_MS)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = REDUCED_DURATION_MS))
        }
        return if (isSearchActive) {
            (slideInHorizontally(animationSpec = DraftoSprings.SnappyOffset) { width -> width / 3 } +
                    fadeIn(animationSpec = DraftoSprings.SnappyFloat)) togetherWith
                    (slideOutHorizontally(animationSpec = DraftoSprings.SnappyOffset) { width -> -width / 3 } +
                            fadeOut(animationSpec = DraftoSprings.SnappyFloat))
        } else {
            (slideInHorizontally(animationSpec = DraftoSprings.SnappyOffset) { width -> -width / 3 } +
                    fadeIn(animationSpec = DraftoSprings.SnappyFloat)) togetherWith
                    (slideOutHorizontally(animationSpec = DraftoSprings.SnappyOffset) { width -> width / 3 } +
                            fadeOut(animationSpec = DraftoSprings.SnappyFloat))
        }
    }

    /**
     * Expansión vertical fluida para acordeones en línea.
     */
    fun accordionExpand(isReduced: Boolean = false): EnterTransition {
        if (isReduced) {
            return fadeIn(animationSpec = tween(durationMillis = REDUCED_DURATION_MS))
        }
        return expandVertically(animationSpec = DraftoSprings.SearchExpandSpec) +
                fadeIn(animationSpec = DraftoSprings.SnappyFloat)
    }

    /**
     * Contracción vertical fluida para acordeones en línea.
     */
    fun accordionShrink(isReduced: Boolean = false): ExitTransition {
        if (isReduced) {
            return fadeOut(animationSpec = tween(durationMillis = REDUCED_DURATION_MS))
        }
        return shrinkVertically(animationSpec = DraftoSprings.SearchExpandSpec) +
                fadeOut(animationSpec = DraftoSprings.SnappyFloat)
    }

    /**
     * Transición de entrada hacia pantalla de detalle en [NavHost].
     */
    fun navDetailEnter(isReduced: Boolean = false): EnterTransition {
        if (isReduced) {
            return fadeIn(animationSpec = tween(durationMillis = REDUCED_DURATION_MS))
        }
        return slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = DraftoSprings.NavTransitionOffset
        ) + fadeIn(animationSpec = DraftoSprings.NavTransitionFloat)
    }

    /**
     * Transición de salida al navegar hacia pantalla de detalle en [NavHost].
     */
    fun navDetailExit(isReduced: Boolean = false): ExitTransition {
        if (isReduced) {
            return fadeOut(animationSpec = tween(durationMillis = REDUCED_DURATION_MS))
        }
        return slideOutHorizontally(
            targetOffsetX = { fullWidth -> -fullWidth / 3 },
            animationSpec = DraftoSprings.NavTransitionOffset
        ) + fadeOut(animationSpec = DraftoSprings.NavTransitionFloat)
    }

    /**
     * Transición de re-entrada al volver (pop) de una pantalla de detalle en [NavHost].
     */
    fun navDetailPopEnter(isReduced: Boolean = false): EnterTransition {
        if (isReduced) {
            return fadeIn(animationSpec = tween(durationMillis = REDUCED_DURATION_MS))
        }
        return slideInHorizontally(
            initialOffsetX = { fullWidth -> -fullWidth / 3 },
            animationSpec = DraftoSprings.NavTransitionOffset
        ) + fadeIn(animationSpec = DraftoSprings.NavTransitionFloat)
    }

    /**
     * Transición de salida al volver (pop) de una pantalla de detalle en [NavHost].
     */
    fun navDetailPopExit(isReduced: Boolean = false): ExitTransition {
        if (isReduced) {
            return fadeOut(animationSpec = tween(durationMillis = REDUCED_DURATION_MS))
        }
        return slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = DraftoSprings.NavTransitionOffset
        ) + fadeOut(animationSpec = DraftoSprings.NavTransitionFloat)
    }

    /**
     * Transición direccional de pestañas raíz en [NavHost] según el delta de índice horizontal.
     */
    fun navTabEnter(direction: Int, isReduced: Boolean = false): EnterTransition {
        if (isReduced) {
            return fadeIn(animationSpec = tween(durationMillis = REDUCED_DURATION_MS))
        }
        return slideInHorizontally(
            initialOffsetX = { fullWidth -> direction * fullWidth / 4 },
            animationSpec = DraftoSprings.SnappyOffset
        ) + fadeIn(animationSpec = DraftoSprings.SnappyFloat)
    }

    /**
     * Transición de salida direccional de pestañas raíz en [NavHost].
     */
    fun navTabExit(direction: Int, isReduced: Boolean = false): ExitTransition {
        if (isReduced) {
            return fadeOut(animationSpec = tween(durationMillis = REDUCED_DURATION_MS))
        }
        return slideOutHorizontally(
            targetOffsetX = { fullWidth -> direction * fullWidth / 4 },
            animationSpec = DraftoSprings.SnappyOffset
        ) + fadeOut(animationSpec = DraftoSprings.SnappyFloat)
    }

    /**
     * Especificación asimétrica para diálogo destructivo con pulsación sostenida (Hold-to-Confirm).
     *
     * Si el usuario está sosteniendo, la animación es deliberadamente lenta (3000ms lineales).
     * Si suelta antes de finalizar, la barra retrocede a cero instantáneamente (160ms reactivos).
     */
    fun holdToConfirmSpec(isHolding: Boolean): AnimationSpec<Float> {
        return if (isHolding) {
            tween(durationMillis = 3000, easing = LinearEasing)
        } else {
            tween(durationMillis = 160, easing = FastOutSlowInEasing)
        }
    }

    /**
     * Especificación para progreso lineal en exportación o respaldos.
     */
    fun progressSpec(isReduced: Boolean = false): AnimationSpec<Float> {
        return if (isReduced) {
            snap()
        } else {
            DraftoSprings.SnappyFloat
        }
    }

    /**
     * Factor de escala micro-táctil para respuesta de pulsación en tarjetas y botones.
     */
    fun cardPressScale(isPressed: Boolean, isReduced: Boolean = false): Float {
        return if (isPressed && !isReduced) 0.97f else 1.0f
    }
}
