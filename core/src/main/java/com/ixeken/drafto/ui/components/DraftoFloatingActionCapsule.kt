package com.ixeken.drafto.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import com.ixeken.drafto.ui.theme.DraftoTransitions
import com.ixeken.drafto.ui.theme.LocalDraftoReducedMotion
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.ixeken.drafto.ui.theme.DraftoShapePill
import com.ixeken.drafto.ui.theme.DraftoSprings
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.FloatingActionCapsuleHeight
import com.ixeken.drafto.ui.theme.FloatingActionCapsuleIconSize
import com.ixeken.drafto.ui.theme.FloatingActionCapsulePaddingHorizontal
import com.ixeken.drafto.ui.theme.FloatingActionCapsuleSpacing
import com.ixeken.drafto.ui.theme.NavBarElevation
import com.ixeken.drafto.ui.theme.PoppinsFontFamily

/**
 * Cápsula de acción flotante (Extended Floating Action Capsule) Nothing OS 5.
 *
 * Decisiones de diseño y arquitectura:
 * - Color de acento sólido reactivo [accentColor] con contraste dinámico [onAccentColor].
 * - Morfología continua de píldora ([DraftoShapePill]) con elevación suave Nothing OS.
 * - Soporte Quick-Return para ocultarse con deslizamiento hacia abajo al hacer scroll down
 *   y reaparecer elásticamente al hacer scroll up mediante la bandera [visible].
 * - Transición de texto suave con [AnimatedContent] al alternar entre diferentes acciones contextuales
 *   ("New note", "New to-do", "New bookmark") garantizando 120 FPS sin recomposiciones accidentales.
 *
 * @param text Etiqueta textual del botón según el contexto activo.
 * @param onClick Acción a ejecutar al pulsar la cápsula.
 * @param modifier Modificador Compose opcional.
 * @param visible Controla la visibilidad y animación quick-return del botón flotante.
 * @param icon Glifo vectorial sólido que precede al texto (por defecto [Icons.Rounded.Add]).
 * @param accentColor Color de fondo de la cápsula (por defecto [DraftoTheme.colors.accent]).
 * @param onAccentColor Color de contenido e icono con contraste dinámico (por defecto [DraftoTheme.colors.onAccent]).
 */
@Composable
fun DraftoFloatingActionCapsule(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    icon: ImageVector = Icons.Rounded.Add,
    accentColor: Color = DraftoTheme.colors.accent,
    onAccentColor: Color = DraftoTheme.colors.onAccent
) {
    val isReducedMotion = LocalDraftoReducedMotion.current

    AnimatedVisibility(
        visible = visible,
        enter = DraftoTransitions.floatingActionEnter(isReducedMotion),
        exit = DraftoTransitions.floatingActionExit(isReducedMotion),
        modifier = modifier
    ) {
        val navShadow = DraftoTheme.colors.navBarShadow

        Surface(
            onClick = onClick,
            shape = DraftoShapePill,
            color = accentColor,
            contentColor = onAccentColor,
            shadowElevation = NavBarElevation,
            modifier = Modifier
                .height(FloatingActionCapsuleHeight)
                .shadow(
                    elevation = NavBarElevation,
                    shape = DraftoShapePill,
                    spotColor = navShadow,
                    ambientColor = navShadow
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = FloatingActionCapsulePaddingHorizontal),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FloatingActionCapsuleSpacing)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = onAccentColor,
                    modifier = Modifier.size(FloatingActionCapsuleIconSize)
                )

                AnimatedContent(
                    targetState = text,
                    transitionSpec = {
                        if (isReducedMotion) {
                            fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                        } else {
                            fadeIn(DraftoSprings.SnappyFloat) togetherWith fadeOut(DraftoSprings.SnappyFloat)
                        }
                    },
                    label = "fabTextTransition"
                ) { targetText ->
                    Text(
                        text = targetText,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = onAccentColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
