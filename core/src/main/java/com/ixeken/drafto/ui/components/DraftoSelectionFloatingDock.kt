package com.ixeken.drafto.ui.components

import androidx.compose.animation.AnimatedVisibility
import com.ixeken.drafto.ui.theme.DraftoTransitions
import com.ixeken.drafto.ui.theme.LocalDraftoReducedMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.ixeken.drafto.core.R
import com.ixeken.drafto.ui.theme.DraftoGlassMaterial
import com.ixeken.drafto.ui.theme.DraftoShapePill
import com.ixeken.drafto.ui.theme.DraftoSprings
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.LocalDraftoBlurEnabled
import com.ixeken.drafto.ui.theme.LocalHazeState
import com.ixeken.drafto.ui.theme.NavBarElevation
import com.ixeken.drafto.ui.theme.NavBarMaxWidth
import com.ixeken.drafto.ui.theme.PaddingExtraSmall
import com.ixeken.drafto.ui.theme.PaddingMedium
import com.ixeken.drafto.ui.theme.PaddingNavBarHorizontal
import com.ixeken.drafto.ui.theme.PaddingSmall
import com.ixeken.drafto.ui.theme.PoppinsFontFamily
import com.ixeken.drafto.ui.theme.SelectionDockButtonSize
import com.ixeken.drafto.ui.theme.SelectionDockHeight
import com.ixeken.drafto.ui.theme.SelectionDockIconSize
import com.ixeken.drafto.ui.theme.toHazeStyle
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.hazeBlur

/**
 * Modelo de datos inmutable que representa una acción ejecutable dentro de la cápsula flotante de selección.
 *
 * Al estar anotada con [Immutable], el compilador de Compose garantiza Smart Skipping
 * previniendo recomposiciones innecesarias cuando las propiedades no cambian.
 *
 * @property icon Glifo vectorial sólido de Material Symbols.
 * @property contentDescription Descripción de accesibilidad para lectores de pantalla.
 * @property tint Tinte de color específico para el icono o [Color.Unspecified] para usar el color primario de superficie.
 * @property onClick Acción a disparar al pulsar el botón.
 */
@Immutable
data class DraftoSelectionAction(
    val icon: ImageVector,
    val contentDescription: String,
    val tint: Color = Color.Unspecified,
    val onClick: () -> Unit
)

/**
 * Cápsula flotante desacoplada y unificada para acciones contextuales en modo de selección múltiple.
 *
 * Decisiones de diseño y arquitectura:
 * - Desacopla 100% la barra de navegación principal [DraftoNavBar], la cual permanece inmutable con sus pestañas.
 * - Flota de manera elástica por encima de la barra inferior con cristal translúcido ultra delgado Nothing OS.
 * - Integra un contador en píldora con color de acento y tipografía Poppins, idéntico al estándar de listas to-do.
 * - Renderiza botones de acción circulares oscuros derivados de [DraftoBookmarkDetailBottomSheet].
 * - Recibe una lista dinámica de [DraftoSelectionAction] haciéndola agnóstica para Marcadores, Notas y Tareas.
 *
 * @param visible Determina si la cápsula se encuentra visible en pantalla.
 * @param selectedCount Cantidad de elementos actualmente seleccionados.
 * @param actions Lista inmutable de acciones contextuales a desplegar.
 * @param onClearSelection Callback ejecutado al presionar el botón de cierre para cancelar la selección.
 * @param modifier Modificador Compose opcional.
 * @param hazeState Estado ambiental de Haze para aplicar desenfoque espacial ultra delgado.
 * @param isBlurEnabled Bandera de configuración para activar o desactivar el efecto de cristal translúcido.
 */
@Composable
fun DraftoSelectionFloatingDock(
    visible: Boolean,
    selectedCount: Int,
    actions: List<DraftoSelectionAction>,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = LocalHazeState.current,
    isBlurEnabled: Boolean = LocalDraftoBlurEnabled.current
) {
    val isReducedMotion = LocalDraftoReducedMotion.current

    AnimatedVisibility(
        visible = visible,
        enter = DraftoTransitions.selectionDockEnter(isReducedMotion),
        exit = DraftoTransitions.selectionDockExit(isReducedMotion),
        modifier = modifier
    ) {
        val dockShape = DraftoShapePill
        val surfaceColor = if (isBlurEnabled && hazeState != null) DraftoTheme.colors.navBarSurfaceTranslucent else DraftoTheme.colors.navBarSurface
        val navShadow = DraftoTheme.colors.navBarShadow

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaddingNavBarHorizontal),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = NavBarMaxWidth)
                    .fillMaxWidth()
                    .height(SelectionDockHeight)
                    .shadow(
                        elevation = NavBarElevation,
                        shape = dockShape,
                        spotColor = navShadow,
                        ambientColor = navShadow
                    )
                    .clip(dockShape)
                    .then(
                        if (isBlurEnabled && hazeState != null) {
                            Modifier.hazeBlur(
                                input = HazeInput.Sources(hazeState),
                                style = DraftoGlassMaterial.UltraThin.toHazeStyle()
                            )
                        } else {
                            Modifier
                        }
                    )
                    .background(surfaceColor),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = PaddingMedium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 1. Contador en Píldora (Estilo idéntico a To-do List)
                    Surface(
                        shape = DraftoShapePill,
                        color = DraftoTheme.colors.accent
                    ) {
                        Text(
                            text = selectedCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = DraftoTheme.colors.onAccent,
                            modifier = Modifier.padding(
                                horizontal = PaddingMedium,
                                vertical = PaddingExtraSmall
                            )
                        )
                    }

                    // 2. Fila de botones de acción contextuales + botón de cancelación
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PaddingSmall)
                    ) {
                        actions.forEach { action ->
                            SelectionActionButton(
                                icon = action.icon,
                                contentDescription = action.contentDescription,
                                tint = if (action.tint != Color.Unspecified) action.tint else MaterialTheme.colorScheme.onSurface,
                                onClick = action.onClick
                            )
                        }

                        // Botón de cancelación (X) con contenedor en color error e icono blanco
                        SelectionActionButton(
                            icon = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.action_cancel),
                            containerColor = MaterialTheme.colorScheme.error,
                            tint = MaterialTheme.colorScheme.onError,
                            onClick = onClearSelection
                        )
                    }
                }
            }
        }
    }
}

/**
 * Botón circular individual para la cápsula flotante de selección.
 *
 * Implementado con contenedor circular oscuro [DraftoTheme.colors.primaryContainer] y glifo interior,
 * siguiendo con precisión la apariencia visual de los botones de acción de [DraftoBookmarkDetailBottomSheet].
 * Marcado con [NonRestartableComposable] como nodo hoja puro para máxima estabilidad en Compose a 120 FPS.
 */
@Composable
@NonRestartableComposable
private fun SelectionActionButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = DraftoTheme.colors.primaryContainer
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(SelectionDockButtonSize)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(SelectionDockIconSize)
        )
    }
}
