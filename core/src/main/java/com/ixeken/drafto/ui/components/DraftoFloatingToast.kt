package com.ixeken.drafto.ui.components

import androidx.compose.animation.AnimatedVisibility
import com.ixeken.drafto.ui.theme.DraftoTransitions
import com.ixeken.drafto.ui.theme.LocalDraftoReducedMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.ixeken.drafto.core.R
import com.ixeken.drafto.ui.theme.DraftoEmerald
import com.ixeken.drafto.ui.theme.DraftoGlassMaterial
import com.ixeken.drafto.ui.theme.DraftoSkyBlue
import com.ixeken.drafto.ui.theme.DraftoSunsetCoral
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.DraftoToastShape
import com.ixeken.drafto.ui.theme.LocalDraftoBlurEnabled
import com.ixeken.drafto.ui.theme.LocalHazeState
import com.ixeken.drafto.ui.theme.PoppinsFontFamily
import com.ixeken.drafto.ui.theme.ToastBorderWidth
import com.ixeken.drafto.ui.theme.ToastBottomOffsetWithNavBar
import com.ixeken.drafto.ui.theme.ToastElevation
import com.ixeken.drafto.ui.theme.ToastIconSize
import com.ixeken.drafto.ui.theme.ToastMaxWidth
import com.ixeken.drafto.ui.theme.ToastPaddingHorizontal
import com.ixeken.drafto.ui.theme.ToastPaddingVertical
import com.ixeken.drafto.ui.theme.ToastSpacing
import com.ixeken.drafto.ui.theme.toHazeStyle
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest

/**
 * Clasificación semántica de las notificaciones flotantes en Drafto.
 *
 * Se eligió un esquema semántico para que el usuario identifique instantáneamente
 * el resultado de una acción mediante color e icono antes de leer el texto.
 */
enum class DraftoToastType {
    Success,
    Warning,
    Action,
    Info
}

/**
 * Modelo inmutable para transportar la información de un toast.
 *
 * Marcado con @Immutable para garantizar Smart Skipping y cero recomposiciones innecesarias.
 */
@Immutable
data class DraftoToastData(
    val id: Long = System.currentTimeMillis(),
    val message: String,
    val type: DraftoToastType = DraftoToastType.Info,
    val durationMs: Long = 3000L
)

/**
 * Gestor global desacoplado para emitir notificaciones flotantes en toda la aplicación.
 *
 * Se diseñó como un singleton con [MutableSharedFlow] para permitir que tanto ViewModels,
 * utilidades del sistema como la interfaz de usuario puedan disparar avisos sin acoplarse
 * a contextos de actividad ni requerir hilos específicos.
 */
object DraftoToastManager {
    private val _messages = MutableSharedFlow<DraftoToastData>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messages: SharedFlow<DraftoToastData> = _messages.asSharedFlow()

    fun show(
        message: String,
        type: DraftoToastType = DraftoToastType.Info,
        durationMs: Long = 3000L
    ) {
        if (message.isBlank()) return
        _messages.tryEmit(
            DraftoToastData(
                message = message,
                type = type,
                durationMs = durationMs
            )
        )
    }

    fun showSuccess(message: String, durationMs: Long = 3000L) {
        show(message = message, type = DraftoToastType.Success, durationMs = durationMs)
    }

    fun showWarning(message: String, durationMs: Long = 3500L) {
        show(message = message, type = DraftoToastType.Warning, durationMs = durationMs)
    }

    fun showAction(message: String, durationMs: Long = 3000L) {
        show(message = message, type = DraftoToastType.Action, durationMs = durationMs)
    }

    fun showInfo(message: String, durationMs: Long = 3000L) {
        show(message = message, type = DraftoToastType.Info, durationMs = durationMs)
    }
}

/**
 * Componente visual de píldora flotante Nothing OS para notificaciones en pantalla.
 *
 * Se construyó con soporte para desenfoque esmerilado Haze UltraThin, bordes finos de alto contraste
 * y animación elástica Nothing OS para flotar armoniosamente por encima de la barra de navegación.
 */
@Composable
fun DraftoFloatingToast(
    data: DraftoToastData,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hazeState = LocalHazeState.current
    val isBlurEnabled = LocalDraftoBlurEnabled.current

    val surfaceColor = if (isBlurEnabled && hazeState != null) {
        DraftoTheme.colors.navBarSurfaceTranslucent
    } else {
        DraftoTheme.colors.navBarSurface
    }

    val dismissInteractionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .widthIn(max = ToastMaxWidth)
            .shadow(
                elevation = ToastElevation,
                shape = DraftoToastShape,
                spotColor = Color.Black.copy(alpha = 0.35f),
                ambientColor = Color.Black.copy(alpha = 0.25f)
            )
            .clip(DraftoToastShape)
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
            .border(
                width = ToastBorderWidth,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                shape = DraftoToastShape
            )
            .clickable(
                interactionSource = dismissInteractionSource,
                indication = null,
                onClick = onDismiss
            )
            .padding(
                horizontal = ToastPaddingHorizontal,
                vertical = ToastPaddingVertical
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ToastSpacing)
        ) {
            when (data.type) {
                DraftoToastType.Success -> {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = DraftoEmerald,
                        modifier = Modifier.size(ToastIconSize)
                    )
                }
                DraftoToastType.Warning -> {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = DraftoSunsetCoral,
                        modifier = Modifier.size(ToastIconSize)
                    )
                }
                DraftoToastType.Action -> {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        tint = DraftoSkyBlue,
                        modifier = Modifier.size(ToastIconSize)
                    )
                }
                DraftoToastType.Info -> {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_notification),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(ToastIconSize)
                    )
                }
            }

            Text(
                text = data.message,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Contenedor anfitrión para presentar y animar las notificaciones flotantes sobre la jerarquía visual.
 *
 * Maneja el ciclo de vida del toast, recolecta del gestor global y anima la entrada/salida
 * respetando el desplazamiento vertical sobre la barra de navegación.
 */
@Composable
fun DraftoToastHost(
    modifier: Modifier = Modifier,
    bottomOffset: Dp = ToastBottomOffsetWithNavBar
) {
    var currentToast by remember { mutableStateOf<DraftoToastData?>(null) }

    LaunchedEffect(Unit) {
        DraftoToastManager.messages.collectLatest { toastData ->
            currentToast = toastData
            delay(toastData.durationMs)
            if (currentToast?.id == toastData.id) {
                currentToast = null
            }
        }
    }

    val isReducedMotion = LocalDraftoReducedMotion.current

    AnimatedVisibility(
        visible = currentToast != null,
        enter = DraftoTransitions.toastEnter(isReducedMotion),
        exit = DraftoTransitions.toastExit(isReducedMotion),
        modifier = modifier.padding(bottom = bottomOffset)
    ) {
        currentToast?.let { toast ->
            DraftoFloatingToast(
                data = toast,
                onDismiss = { currentToast = null }
            )
        }
    }
}
