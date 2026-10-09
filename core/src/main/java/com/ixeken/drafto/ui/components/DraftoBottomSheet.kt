package com.ixeken.drafto.ui.components

import android.app.Activity
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import android.view.ViewParent
import android.view.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.ixeken.drafto.core.R
import com.ixeken.drafto.ui.theme.DraftoGlassMaterial
import com.ixeken.drafto.ui.theme.DraftoShapeCard
import com.ixeken.drafto.ui.theme.DraftoShapePill
import com.ixeken.drafto.ui.theme.DraftoShapePillButton
import com.ixeken.drafto.ui.theme.DraftoShapeSheetCard
import com.ixeken.drafto.ui.theme.DraftoSunsetCoral
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.FrauncesFontFamily
import com.ixeken.drafto.ui.theme.LocalDraftoBlurEnabled
import com.ixeken.drafto.ui.theme.LocalHazeState
import com.ixeken.drafto.ui.theme.PaddingExtraSmall
import com.ixeken.drafto.ui.theme.PaddingScreenHorizontal
import com.ixeken.drafto.ui.theme.PoppinsFontFamily
import com.ixeken.drafto.ui.theme.SheetActionPillHeight
import com.ixeken.drafto.ui.theme.SheetCancelButtonHeight
import com.ixeken.drafto.ui.theme.SheetDragHandleHeight
import com.ixeken.drafto.ui.theme.SheetDragHandleVerticalPadding
import com.ixeken.drafto.ui.theme.SheetDragHandleWidth
import com.ixeken.drafto.ui.theme.SheetHeaderPaddingBottom
import com.ixeken.drafto.ui.theme.toHazeStyle
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur

import com.ixeken.drafto.ui.theme.findActivity

/**
 * Busca de forma segura la ventana (Window) asociada al View de la jerarquía Compose (incluyendo ventanas de diálogo/bottom sheet).
 */
internal fun findWindow(view: View): Window? {
    var parent: ViewParent? = view.parent
    while (parent != null) {
        if (parent is DialogWindowProvider) {
            return parent.window
        }
        parent = parent.parent
    }
    return view.context.findActivity()?.window
}

/**
 * Forma estándar para el contenedor superior de todas las hojas modales (Bottom Sheets) en Drafto.
 */
val DraftoShapeBottomSheet: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

/**
 * Contenedor base modular y unificado para todos los Modal Bottom Sheets de la aplicación.
 *
 * Características de diseño:
 * - Desenfoque espacial de fondo Pure Blur Ultra Thin utilizando Haze 2.0.
 * - Fondo translúcido reactivo que se adapta armoniosamente al tema activo (Dark, Light, AMOLED).
 * - Ausencia total de bordes duros de 1dp, respetando el sistema de diseño esmerilado de Drafto.
 * - Extensión completa de fondo y desenfoque por detrás del handle gestual de la barra de navegación del SO.
 * - Sincronización de contraste y transparencia de la barra de navegación del sistema nativo.
 *
 * @param onDismissRequest Callback invocado al descartar la hoja modal.
 * @param modifier Modificador de diseño Compose.
 * @param sheetState Estado de la hoja modal.
 * @param scrimColor Color del velo translúcido oscurecedor de fondo.
 * @param content Bloque composable que define el cuerpo de la hoja modal.
 */
@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftoModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    scrimColor: Color = Color.Black.copy(alpha = 0.50f),
    isBlurEnabled: Boolean = LocalDraftoBlurEnabled.current,
    isFullScreen: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val hazeState = LocalHazeState.current
    val surfaceColor = if (isBlurEnabled && hazeState != null) {
        DraftoTheme.colors.bottomSheetSurface.copy(alpha = 0.94f)
    } else {
        DraftoTheme.colors.bottomSheetSurface
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

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = scrimColor,
        dragHandle = null,
        contentWindowInsets = { WindowInsets.statusBars.only(WindowInsetsSides.Top) },
        modifier = if (isFullScreen) modifier.fillMaxHeight() else modifier
    ) {
        val sheetView = LocalView.current
        SideEffect {
            val window = findWindow(sheetView)
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isFullScreen) Modifier.fillMaxHeight() else Modifier)
                .clip(DraftoShapeBottomSheet)
                .then(
                    if (isBlurEnabled && hazeState != null) {
                        Modifier.hazeBlur(
                            input = HazeInput.Sources(hazeState),
                            style = DraftoGlassMaterial.Default.toHazeStyle()
                        )
                    } else {
                        Modifier
                    }
                )
                .background(surfaceColor)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(bottom = 16.dp),
            content = content
        )
    }
}

/**
 * Encabezado universal y canónico para los Modal Bottom Sheets de Drafto (Editorial Nothing OS).
 *
 * Características visuales extraídas del diseño canónico:
 * - Manija de arrastre (drag handle) centrada con esquinas en píldora y opacidad sutil.
 * - Título editorial centrado en tipografía [FrauncesFontFamily] negrita ([FontWeight.Bold]).
 * - Subtítulo descriptivo opcional en [PoppinsFontFamily] centrado.
 * - Sin botones circulares invasivos superiores, preservando una estética editorial limpia.
 *
 * @param title Título visible de la hoja modal.
 * @param modifier Modificador opcional.
 * @param subtitle Subtítulo descriptivo opcional.
 * @param onDismiss Callback opcional de descarte para compatibilidad con llamadas existentes.
 * @param actions Bloque opcional para acciones personalizadas.
 */
@Composable
fun DraftoSheetHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onDismiss: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Manija de arrastre superior Nothing OS
        Box(
            modifier = Modifier
                .padding(top = SheetDragHandleVerticalPadding, bottom = SheetHeaderPaddingBottom)
                .width(SheetDragHandleWidth)
                .height(SheetDragHandleHeight)
                .clip(DraftoShapePill)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f))
        )

        // Título canónico centrado en Fraunces Bold
        Text(
            text = title,
            fontFamily = FrauncesFontFamily,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaddingScreenHorizontal)
        )

        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(PaddingExtraSmall))
            Text(
                text = subtitle,
                fontFamily = PoppinsFontFamily,
                style = MaterialTheme.typography.bodyMedium,
                color = DraftoTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaddingScreenHorizontal)
            )
        }

        if (actions != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaddingScreenHorizontal, vertical = PaddingExtraSmall),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
    }
}

/**
 * Botón canónico "Cancel" en píldora roja (Editorial Nothing OS) para Bottom Sheets.
 */
@Composable
fun DraftoSheetCancelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.action_cancel),
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = DraftoShapePillButton,
        colors = ButtonDefaults.buttonColors(
            containerColor = DraftoTheme.colors.sheetCancelButtonBackground,
            contentColor = DraftoTheme.colors.onSheetCancelButton,
            disabledContainerColor = DraftoTheme.colors.sheetCancelButtonBackground.copy(alpha = 0.40f),
            disabledContentColor = DraftoTheme.colors.onSheetCancelButton.copy(alpha = 0.40f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(SheetCancelButtonHeight)
    ) {
        Text(
            text = text,
            fontFamily = FrauncesFontFamily,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            color = DraftoTheme.colors.onSheetCancelButton
        )
    }
}

/**
 * Botón de acción principal en píldora con color de acento o esmeralda para Bottom Sheets.
 */
@Composable
fun DraftoSheetPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = DraftoTheme.colors.accent,
    contentColor: Color = DraftoTheme.colors.onAccent
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = DraftoShapePillButton,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.40f),
            disabledContentColor = contentColor.copy(alpha = 0.40f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(SheetActionPillHeight)
    ) {
        Text(
            text = text,
            fontFamily = FrauncesFontFamily,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) contentColor else contentColor.copy(alpha = 0.50f)
        )
    }
}

/**
 * Contenedor de tarjeta agrupada (Editorial Nothing OS) para opciones o contenido de Bottom Sheets.
 */
@Composable
fun DraftoSheetCardGroup(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    shape: Shape = DraftoShapeSheetCard,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = backgroundColor,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

/**
 * Tarjeta o sección contenedora para el contenido interno de cualquier Bottom Sheet de Drafto.
 *
 * @param modifier Modificador de diseño.
 * @param backgroundColor Color de fondo translúcido adaptativo.
 * @param content Contenido interno de la tarjeta.
 */
@Composable
fun DraftoSheetCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = DraftoTheme.colors.bottomSheetInnerCard,
    borderColor: Color = DraftoTheme.colors.cardBorder,
    horizontalPadding: androidx.compose.ui.unit.Dp = 16.dp,
    verticalPadding: androidx.compose.ui.unit.Dp = 4.dp,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
            .clip(DraftoShapeCard)
            .background(backgroundColor)
            .border(1.dp, borderColor, DraftoShapeCard)
            .padding(contentPadding),
        content = content
    )
}

