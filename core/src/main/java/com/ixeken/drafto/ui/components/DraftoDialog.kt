package com.ixeken.drafto.ui.components

import androidx.compose.animation.core.animateFloatAsState
import com.ixeken.drafto.ui.theme.DraftoTransitions
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.vector.ImageVector
import com.ixeken.drafto.core.R
import com.ixeken.drafto.ui.theme.DraftoGlassMaterial
import com.ixeken.drafto.ui.theme.DraftoShapeCard
import com.ixeken.drafto.ui.theme.DraftoShapePill
import com.ixeken.drafto.ui.theme.DraftoSunsetCoral
import com.ixeken.drafto.ui.theme.FrauncesFontFamily
import com.ixeken.drafto.ui.theme.PoppinsFontFamily
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.LocalDraftoBlurEnabled
import com.ixeken.drafto.ui.theme.LocalHazeState
import com.ixeken.drafto.ui.theme.toHazeStyle
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import kotlinx.coroutines.delay

import android.os.Build
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Forma estándar para los contenedores flotantes de diálogo en Drafto.
 */
val DraftoShapeDialog: Shape = RoundedCornerShape(28.dp)

/**
 * Contenedor base estandarizado para todos los cuadros de diálogo (Dialogs) de Drafto.
 *
 * Características de diseño:
 * - Desenfoque espacial de fondo Pure Blur Ultra Thin utilizando Haze 2.0.
 * - Fondo translúcido reactivo que se adapta armoniosamente al tema activo (Dark, Light, AMOLED).
 * - Sin bordes duros de 1dp, manteniendo la estética esmerilada pura.
 * - Forma redondeada de 28.dp.
 *
 * @param onDismissRequest Callback invocado al descartar el diálogo.
 * @param modifier Modificador opcional.
 * @param properties Propiedades de configuración del diálogo nativo.
 * @param content Contenido interno del diálogo.
 */
@Suppress("DEPRECATION")
@Composable
fun DraftoDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    isBlurEnabled: Boolean = LocalDraftoBlurEnabled.current,
    content: @Composable ColumnScope.() -> Unit
) {
    val hazeState = LocalHazeState.current
    val surfaceColor = if (isBlurEnabled && hazeState != null) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    } else {
        DraftoTheme.colors.cardSurface
    }
    val isDark = DraftoTheme.colors.isDark

    val view = LocalView.current
    SideEffect {
        val window = findWindow(view)
        if (window != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
                window.isStatusBarContrastEnforced = false
            }
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.isAppearanceLightNavigationBars = !isDark
            insetsController.isAppearanceLightStatusBars = !isDark
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        val dialogView = LocalView.current
        SideEffect {
            val window = findWindow(dialogView)
            if (window != null) {
                window.setBackgroundDrawableResource(android.R.color.transparent)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                    window.isStatusBarContrastEnforced = false
                }
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightNavigationBars = !isDark
                insetsController.isAppearanceLightStatusBars = !isDark
            }
        }

        Box(
            modifier = modifier
                .fillMaxWidth(0.90f)
                .widthIn(max = 400.dp)
                .padding(vertical = 16.dp)
                .clip(DraftoShapeDialog)
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
                .background(surfaceColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                content = content
            )
        }
    }
}

/**
 * Cuadro de diálogo de alerta estandarizado (DraftoAlertDialog).
 *
 * @param onDismissRequest Callback al descartar el diálogo.
 * @param title Encabezado del diálogo.
 * @param confirmButton Botón de confirmación o acción principal.
 * @param modifier Modificador de Compose.
 * @param dismissButton Botón opcional de cancelación.
 * @param icon Icono ilustrativo opcional en la parte superior.
 * @param text Cuerpo o descripción del diálogo.
 */
@Composable
fun DraftoAlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable (() -> Unit)?,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null
) {
    DraftoDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
        }

        if (title != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (text != null) 12.dp else 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                title()
            }
        }

        if (text != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                text()
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (dismissButton != null) {
                dismissButton()
                Spacer(modifier = Modifier.width(12.dp))
            }
            confirmButton()
        }
    }
}

/**
 * Cuadro de diálogo para confirmaciones críticas o destructivas (ej. vaciar chat, borrar elementos, purga de datos).
 *
 * Características de diseño y seguridad:
 * - Soporte para icono contextual ilustrativo opcional centrado sin contenedor, con tinte monocromático u advertencia roja.
 * - Modo de cuenta regresiva obligatoria con bloqueo del botón de confirmación ([countdownSeconds]).
 * - Modo de confirmación por presión sostenida con barra animada de progreso y respuesta háptica ([requireHoldToConfirm]).
 * - Modo de confirmación estándar con colores semánticos reactivos de alto contraste ([DraftoTheme.colors.accent] y [DraftoSunsetCoral]).
 *
 * @param title Título del diálogo.
 * @param message Mensaje descriptivo.
 * @param onConfirm Callback ejecutado al confirmar la acción.
 * @param onDismiss Callback al cancelar o cerrar.
 * @param modifier Modificador Compose opcional.
 * @param icon Icono opcional en la cabecera del diálogo (ej. [Icons.Rounded.Warning]).
 * @param countdownSeconds Segundos que deben transcurrir antes de habilitar el botón de confirmación (0 para desactivar).
 * @param warning Advertencia secundaria opcional.
 * @param confirmText Texto del botón de confirmación.
 * @param cancelText Texto del botón de cancelación.
 * @param isDestructive Si la acción es destructiva (usa colores de alerta y error).
 * @param requireHoldToConfirm Si requiere mantener presionado durante 3 segundos para confirmar.
 */
@Composable
fun DraftoConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    countdownSeconds: Int = 0,
    warning: String? = null,
    confirmText: String = stringResource(R.string.action_delete),
    cancelText: String? = stringResource(R.string.action_cancel),
    isDestructive: Boolean = true,
    requireHoldToConfirm: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    var secondsLeft by remember { mutableIntStateOf(3) }
    var isHolding by remember { mutableStateOf(false) }

    var countdownLeft by remember(countdownSeconds) { mutableIntStateOf(countdownSeconds) }
    LaunchedEffect(countdownSeconds) {
        if (countdownSeconds > 0) {
            while (countdownLeft > 0) {
                delay(1000L)
                countdownLeft--
            }
        }
    }
    val isCountdownFinished = countdownLeft <= 0

    val progress by animateFloatAsState(
        targetValue = if (isHolding) 1f else 0f,
        animationSpec = DraftoTransitions.holdToConfirmSpec(isHolding),
        label = "holdProgress"
    )

    LaunchedEffect(isHolding) {
        if (isHolding) {
            secondsLeft = 3
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

            delay(1000L)
            if (isHolding) {
                secondsLeft = 2
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }

            delay(1000L)
            if (isHolding) {
                secondsLeft = 1
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }

            delay(1000L)
            if (isHolding) {
                onConfirm()
            }
        } else {
            secondsLeft = 3
        }
    }

    DraftoDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDestructive) DraftoSunsetCoral else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Text(
            text = title,
            fontFamily = FrauncesFontFamily,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = message,
            fontFamily = PoppinsFontFamily,
            style = MaterialTheme.typography.bodyMedium,
            color = DraftoTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        if (!warning.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = warning,
                fontFamily = PoppinsFontFamily,
                style = MaterialTheme.typography.bodySmall,
                color = DraftoTheme.colors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        val cancelContainerColor = if (DraftoTheme.colors.isDark) {
            Color.White.copy(alpha = 0.08f)
        } else {
            Color.Black.copy(alpha = 0.05f)
        }
        val cancelBorder = BorderStroke(1.dp, DraftoTheme.colors.cardBorder)

        when {
            countdownSeconds > 0 -> {
                val buttonLabel = if (isCountdownFinished) {
                    confirmText
                } else {
                    stringResource(R.string.wipe_dialog_confirm_countdown, countdownLeft)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(DraftoShapePill)
                        .background(
                            when {
                                !isCountdownFinished -> if (isDestructive) DraftoSunsetCoral.copy(alpha = 0.20f) else DraftoTheme.colors.accent.copy(alpha = 0.20f)
                                isDestructive -> DraftoSunsetCoral
                                else -> DraftoTheme.colors.accent
                            }
                        )
                        .then(
                            if (isCountdownFinished) Modifier.clickable(onClick = onConfirm)
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = buttonLabel,
                        color = when {
                            !isCountdownFinished -> if (isDestructive) DraftoSunsetCoral.copy(alpha = 0.7f) else DraftoTheme.colors.accent.copy(alpha = 0.7f)
                            isDestructive -> Color.White
                            else -> DraftoTheme.colors.onAccent
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (cancelText != null) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(DraftoShapePill)
                            .border(cancelBorder, DraftoShapePill)
                            .background(cancelContainerColor)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cancelText,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
            requireHoldToConfirm -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(DraftoShapePill)
                        .background(
                            if (isDestructive) DraftoSunsetCoral.copy(alpha = 0.15f)
                            else DraftoTheme.colors.accent.copy(alpha = 0.15f)
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isHolding = true
                                    try {
                                        tryAwaitRelease()
                                    } finally {
                                        isHolding = false
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (isHolding) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .background(
                                    if (isDestructive) DraftoSunsetCoral
                                    else DraftoTheme.colors.accent
                                )
                        )
                    }

                    val buttonLabel = if (isHolding) stringResource(R.string.action_hold_to_delete_countdown, secondsLeft) else confirmText
                    Text(
                        text = buttonLabel,
                        color = when {
                            isHolding && isDestructive -> Color.White
                            isHolding -> DraftoTheme.colors.onAccent
                            isDestructive -> DraftoSunsetCoral
                            else -> DraftoTheme.colors.accent
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                if (cancelText != null) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(DraftoShapePill)
                            .border(cancelBorder, DraftoShapePill)
                            .background(cancelContainerColor)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cancelText,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
            else -> {
                if (cancelText != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botón Cancelar (píldora con borde y fondo sutil Nothing OS 5.0)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(DraftoShapePill)
                                .border(cancelBorder, DraftoShapePill)
                                .background(cancelContainerColor)
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cancelText,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        // Botón Confirmar / Acción
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(DraftoShapePill)
                                .background(
                                    if (isDestructive) DraftoSunsetCoral
                                    else DraftoTheme.colors.accent
                                )
                                .clickable(onClick = onConfirm),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = confirmText,
                                color = if (isDestructive) Color.White else DraftoTheme.colors.onAccent,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                } else {
                    // Botón único para avisos informativos
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(DraftoShapePill)
                            .background(
                                if (isDestructive) DraftoSunsetCoral
                                else DraftoTheme.colors.accent
                            )
                            .clickable(onClick = onConfirm),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = confirmText,
                            color = if (isDestructive) Color.White else DraftoTheme.colors.onAccent,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

