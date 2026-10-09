package com.ixeken.drafto.ui.components

import android.app.Activity
import android.graphics.Rect
import android.os.Build
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ixeken.drafto.core.R
import com.ixeken.drafto.ui.theme.Charred
import com.ixeken.drafto.ui.theme.DraftoTransitions
import com.ixeken.drafto.ui.theme.DraftoShapeDynamicIsland
import com.ixeken.drafto.ui.theme.DraftoShapeDynamicIslandInput
import com.ixeken.drafto.ui.theme.DraftoShapePill
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.LocalDraftoReducedMotion
import com.ixeken.drafto.ui.theme.DynamicIslandButtonHeight
import com.ixeken.drafto.ui.theme.DynamicIslandButtonSpacing
import com.ixeken.drafto.ui.theme.DynamicIslandCounterPaddingHorizontal
import com.ixeken.drafto.ui.theme.DynamicIslandCounterPaddingVertical
import com.ixeken.drafto.ui.theme.DynamicIslandDotSize
import com.ixeken.drafto.ui.theme.DynamicIslandElevation
import com.ixeken.drafto.ui.theme.DynamicIslandInputHeight
import com.ixeken.drafto.ui.theme.DynamicIslandPaddingBottom
import com.ixeken.drafto.ui.theme.DynamicIslandPaddingHorizontal
import com.ixeken.drafto.ui.theme.DynamicIslandWidth
import com.ixeken.drafto.ui.theme.FrauncesFontFamily
import com.ixeken.drafto.ui.theme.PaddingExtraSmall
import com.ixeken.drafto.ui.theme.PaddingMedium
import com.ixeken.drafto.ui.theme.PaddingSmall
import com.ixeken.drafto.ui.theme.PoppinsFontFamily
import com.ixeken.drafto.ui.theme.PureBlack
import com.ixeken.drafto.ui.theme.PureWhite
import com.ixeken.drafto.ui.theme.findActivity

/**
 * Dimensiones y posicion del cutout en dp precalculadas.
 */
private data class CutoutInfo(
    val widthDp: Dp,
    val heightDp: Dp,
    val leftDp: Dp,
    val topDp: Dp
)

/**
 * Dynamic Island simplificada de Drafto.
 *
 * Solo muestra el editor de notas persistentes (QuickNoteEditor),
 * posicionado proporcionalmente al cutout real de la camara del dispositivo.
 *
 * @param visible controla la visibilidad del editor
 * @param quickNoteText texto actual del editor
 * @param onQuickNoteTextChange callback de cambio de texto
 * @param onSaveQuickNote callback al guardar la nota
 * @param onCancelQuickNote callback al cancelar
 */
@Composable
public fun DraftoDynamicIsland(
    visible: Boolean,
    quickNoteText: String = "",
    onQuickNoteTextChange: (String) -> Unit = {},
    onSaveQuickNote: () -> Unit = {},
    onCancelQuickNote: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val cutoutInsets = WindowInsets.displayCutout
    val statusBarsInsets = WindowInsets.statusBars

    var cutoutInfo by remember {
        mutableStateOf(
            CutoutInfo(
                widthDp = 24.dp,
                heightDp = 24.dp,
                leftDp = 0.dp,
                topDp = 8.dp
            )
        )
    }

    LaunchedEffect(view, density, cutoutInsets, statusBarsInsets) {
        val rootInsets = view.rootWindowInsets
        if (rootInsets != null) {
            cutoutInfo = detectCutoutInfo(view, density, rootInsets)
        }
    }

    val context = LocalContext.current
    val info = cutoutInfo

    // Sincronizar la visibilidad de la status bar del sistema (Wifi, señal, reloj) con la Dynamic Island
    DisposableEffect(visible) {
        val window = context.findActivity()?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (visible) {
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
            } else {
                insetsController.show(WindowInsetsCompat.Type.statusBars())
            }
        }
        onDispose {
            val window = context.findActivity()?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    // Manejar el boton Back de Android para cancelar el editor
    if (visible) {
        BackHandler {
            onCancelQuickNote()
        }
    }

    val isReducedMotion = LocalDraftoReducedMotion.current

    // Borde superior real de la camara: el circulo esta centrado en el bounding rect
    val cameraTopEdge = (info.topDp + (info.heightDp - info.widthDp) / 2).coerceAtLeast(4.dp)

    Box(
        modifier = modifier.offset(y = cameraTopEdge),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = DraftoTransitions.dynamicIslandEnter(isReducedMotion),
            exit = DraftoTransitions.dynamicIslandExit(isReducedMotion)
        ) {
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = DynamicIslandElevation,
                        shape = DraftoShapeDynamicIsland,
                        spotColor = PureBlack,
                        ambientColor = PureBlack
                    )
                    .clip(DraftoShapeDynamicIsland)
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        shape = DraftoShapeDynamicIsland
                    )
                    .background(PureBlack),
                contentAlignment = Alignment.Center
            ) {
                QuickNoteEditorContent(
                    cutoutHeightDp = info.heightDp,
                    inputText = quickNoteText,
                    onInputTextChange = onQuickNoteTextChange,
                    onSaveClick = onSaveQuickNote,
                    onCancelClick = onCancelQuickNote
                )
            }
        }
    }
}

/**
 * Editor emergente de notas rápidas Nothing OS 5 para la Dynamic Island.
 *
 * Decisiones de arquitectura y diseño:
 * - Aislamiento absoluto de hardware: Su superficie permanece en [PureBlack] con micro-borde translúcido
 *   independientemente de que el usuario tenga tema Claro, Oscuro o AMOLED, asegurando camuflaje físico
 *   total con el sensor y orificio de la cámara frontal (cutout).
 * - Ergonomía de ahorro vertical: Los botones se disponen en una fila horizontal simétrica Nothing OS en píldora
 *   ([DraftoShapePill]), reduciendo más de 50dp de altura respecto al diseño previo de botones apilados.
 * - Tipografía y jerarquía: Cabecera con título en [FrauncesFontFamily] Bold y badge de conteo en micro-píldora.
 *   El campo de texto consume [Charred] con texto blanco puro para garantizar legibilidad inmediata sin deslumbrar.
 */
@Composable
private fun QuickNoteEditorContent(
    cutoutHeightDp: Dp,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val isSaveEnabled = inputText.isNotBlank() && inputText.length <= 64

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Margen superior: altura del cutout + 4dp para que nada quede detrás de la cámara
    val topClearance = cutoutHeightDp + PaddingExtraSmall

    Column(
        modifier = modifier
            .width(DynamicIslandWidth)
            .padding(
                start = DynamicIslandPaddingHorizontal,
                end = DynamicIslandPaddingHorizontal,
                bottom = DynamicIslandPaddingBottom,
                top = topClearance
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cabecera Nothing OS integrada de la isla
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = PaddingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PaddingSmall)
            ) {
                // Indicador Live Note activo
                Box(
                    modifier = Modifier
                        .size(DynamicIslandDotSize)
                        .clip(CircleShape)
                        .background(DraftoTheme.colors.sunsetCoral)
                )
                Text(
                    text = stringResource(R.string.title_persistent_note),
                    color = PureWhite,
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Micro-píldora translúcida Nothing OS para el contador
            Surface(
                shape = DraftoShapePill,
                color = Color.White.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Text(
                    text = stringResource(R.string.character_counter_format, inputText.length, 64),
                    color = PureWhite.copy(alpha = 0.80f),
                    fontFamily = PoppinsFontFamily,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(
                        horizontal = DynamicIslandCounterPaddingHorizontal,
                        vertical = DynamicIslandCounterPaddingVertical
                    )
                )
            }
        }

        // Contenedor del campo de texto en carbón oscuro (blindado contra tema claro)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(DynamicIslandInputHeight)
                .clip(DraftoShapeDynamicIslandInput)
                .border(1.dp, Color.White.copy(alpha = 0.10f), DraftoShapeDynamicIslandInput)
                .background(Charred)
                .padding(PaddingMedium)
        ) {
            BasicTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = PureWhite,
                    fontFamily = PoppinsFontFamily
                ),
                cursorBrush = SolidColor(if (DraftoTheme.colors.accent == PureBlack) PureWhite else DraftoTheme.colors.accent),
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester),
                decorationBox = { innerTextField ->
                    if (inputText.isEmpty()) {
                        Text(
                            text = stringResource(R.string.placeholder_write_something),
                            color = PureWhite.copy(alpha = 0.38f),
                            fontFamily = PoppinsFontFamily,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    innerTextField()
                }
            )
        }

        // Fila horizontal simétrica de acciones canónicas (Cancel rojo, Save verde esmeralda con Fraunces Bold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = PaddingMedium),
            horizontalArrangement = Arrangement.spacedBy(DynamicIslandButtonSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel (Rojo Nothing OS)
            DraftoSheetCancelButton(
                onClick = onCancelClick,
                modifier = Modifier.weight(1f)
            )

            // Save (Verde esmeralda Nothing OS, sin color de acento)
            DraftoSheetPrimaryButton(
                text = stringResource(R.string.action_save),
                onClick = onSaveClick,
                enabled = isSaveEnabled,
                containerColor = DraftoTheme.colors.emerald,
                contentColor = PureWhite,
                modifier = Modifier.weight(1f)
            )
        }
    }
}



/**
 * Detecta las dimensiones del cutout del display.
 *
 * Lee las dimensiones reales enviadas por el sistema Android. Si no hay cutout
 * o el dispositivo usa pantalla plana sin reportar cutout, se aplica un tamano
 * por defecto estilizado (18dp x 18dp) correspondiente al punch-hole moderno.
 */
private fun detectCutoutInfo(
    view: View,
    density: androidx.compose.ui.unit.Density,
    windowInsets: android.view.WindowInsets
): CutoutInfo {
    val defaultFallback = CutoutInfo(
        widthDp = 18.dp,
        heightDp = 18.dp,
        leftDp = 0.dp,
        topDp = 8.dp
    )

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return defaultFallback

    val displayCutout = windowInsets.displayCutout ?: return defaultFallback
    val boundingRects: List<Rect> = displayCutout.boundingRects
    if (boundingRects.isEmpty()) return defaultFallback

    val rect = boundingRects[0]

    return with(density) {
        CutoutInfo(
            widthDp = rect.width().toDp(),
            heightDp = rect.height().toDp(),
            leftDp = rect.left.toDp(),
            topDp = rect.top.toDp()
        )
    }
}

