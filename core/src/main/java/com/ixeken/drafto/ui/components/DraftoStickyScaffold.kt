package com.ixeken.drafto.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.ixeken.drafto.ui.theme.DraftoStickyScaffoldFallbackTopPadding
import com.ixeken.drafto.ui.theme.LocalDraftoBlurEnabled
import com.ixeken.drafto.ui.theme.LocalHazeState
import com.ixeken.drafto.ui.theme.PaddingSmall
import dev.chrisbanes.haze.hazeSource

/**
 * Scaffold cohesivo que integra una cabecera adhesiva flotante con desenfoque progresivo [DraftoStickyHeader]
 * y mide dinámicamente su altura para entregar un [safeTopPadding] exacto al contenido sin recortes ni números mágicos.
 *
 * Resuelve y centraliza:
 * 1. Medición de la altura del encabezado adhesivo en tiempo de layout sin provocar ciclos de recomposición.
 * 2. Cálculo reactivo de [safeTopPadding] para listas con scroll (LazyColumn, LazyVerticalGrid, LazyVerticalStaggeredGrid).
 * 3. Inyección automática del contenido como fuente de desenfoque (`hazeSource`) para el [DraftoStickyHeader].
 * 4. Respeto estricto a las directrices de rendimiento a 120 FPS y Strong Skipping Mode.
 *
 * @param isScrolled Indica si el contenido inferior se ha desplazado para activar el desenfoque progresivo.
 * @param header Contenido composable que se renderiza dentro de la cabecera adhesiva superior.
 * @param modifier Modificador visual para el contenedor raíz.
 * @param isBlurEnabled Bandera opcional para activar o desactivar el desenfoque en la cabecera.
 * @param fallbackTopPadding Espaciado superior inicial antes de que la cabecera sea medida en el primer frame.
 * @param content Slot principal que recibe el valor [safeTopPadding: Dp] calculado dinámicamente con el padding superior de la cabecera.
 */
@Composable
fun DraftoStickyScaffold(
    isScrolled: Boolean,
    header: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    isBlurEnabled: Boolean = LocalDraftoBlurEnabled.current,
    fallbackTopPadding: Dp = DraftoStickyScaffoldFallbackTopPadding,
    content: @Composable BoxScope.(safeTopPadding: Dp) -> Unit
) {
    val hazeState = LocalHazeState.current
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val safeTopPadding = remember(headerHeightPx, density, fallbackTopPadding) {
        if (headerHeightPx > 0) {
            with(density) { headerHeightPx.toDp() } + PaddingSmall
        } else {
            fallbackTopPadding
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (hazeState != null) Modifier.hazeSource(state = hazeState) else Modifier
                )
        ) {
            content(safeTopPadding)
        }

        DraftoStickyHeader(
            isScrolled = isScrolled,
            isBlurEnabled = isBlurEnabled,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onGloballyPositioned { coordinates ->
                    if (headerHeightPx != coordinates.size.height) {
                        headerHeightPx = coordinates.size.height
                    }
                },
            content = header
        )
    }
}
