package com.ixeken.drafto.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon


import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ixeken.drafto.ui.theme.DraftoGlassMaterial
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.LocalDraftoBlurEnabled
import com.ixeken.drafto.ui.theme.LocalHazeState
import com.ixeken.drafto.ui.theme.PaddingScreenHorizontal
import com.ixeken.drafto.ui.theme.PaddingScreenTopBarBottom
import com.ixeken.drafto.ui.theme.PaddingScreenTopBarTop
import com.ixeken.drafto.ui.theme.PaddingSmall
import com.ixeken.drafto.ui.theme.TabTitleTextStyle
import com.ixeken.drafto.ui.theme.toProgressiveTopHazeStyle
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope

/**
 * Contenedor superior adhesivo con efecto Spatial Pure Blur progresivo (Haze Ultra Thin).
 * Fija la TopBar y componentes anexos (como buscadores o selectores) en la parte superior.
 * Cuando [isScrolled] es false, se mantiene plano (flat/transparente).
 * Cuando [isScrolled] es true (el usuario se desplaza o busca), activa el desenfoque progresivo en tiempo real,
 * disolviendo gradualmente el fondo y el desenfoque hacia el contenido scrolleable para eliminar cualquier corte duro.
 */
@Composable
fun DraftoStickyHeader(
    modifier: Modifier = Modifier,
    isScrolled: Boolean = true,
    isBlurEnabled: Boolean = LocalDraftoBlurEnabled.current,
    content: @Composable ColumnScope.() -> Unit
) {
    val hazeState = LocalHazeState.current
    val fallbackSurfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    val progressiveStyle = DraftoGlassMaterial.Default.toProgressiveTopHazeStyle()

    val fallbackBrush = remember(fallbackSurfaceColor) {
        Brush.verticalGradient(
            0.0f to fallbackSurfaceColor,
            0.78f to fallbackSurfaceColor,
            1.0f to Color.Transparent
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isScrolled) {
                    if (isBlurEnabled && hazeState != null) {
                        Modifier.hazeBlur(
                            input = HazeInput.Sources(hazeState),
                            style = progressiveStyle
                        )
                    } else if (isBlurEnabled) {
                        Modifier.background(fallbackBrush)
                    } else {
                        Modifier.background(DraftoTheme.colors.background)
                    }
                } else {
                    Modifier.background(Color.Transparent)
                }
            )
    ) {
        content()
        Spacer(modifier = Modifier.height(PaddingSmall))
    }
}

/**
 * Encabezado base modular para las pestañas de navegación de Drafto.
 *
 * Características:
 * - Título alineado a la izquierda (Start) por defecto con tamaño display.
 * - Insets automáticos seguros contra cámara (cutout) y status bar.
 * - Slot de acciones a la derecha (`actions`) para elementos específicos por pestaña.
 * - Soporte para ícono de navegación opcional (`navigationIcon`).
 */
@Composable
fun DraftoTabTopBar(
    title: String,
    modifier: Modifier = Modifier,
    titleColor: Color = MaterialTheme.colorScheme.onBackground,
    titleTextStyle: TextStyle = TabTitleTextStyle,
    titleMaxLines: Int = 1,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.statusBars
                    .union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
            .padding(
                top = PaddingScreenTopBarTop,
                bottom = PaddingScreenTopBarBottom,
                start = PaddingScreenHorizontal,
                end = PaddingScreenHorizontal
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (navigationIcon != null) {
                navigationIcon()
                Spacer(modifier = Modifier.width(PaddingSmall))
            }
            Text(
                text = title,
                style = titleTextStyle,
                color = titleColor,
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaddingSmall),
            content = actions
        )
    }
}

/**
 * Botón de acción limpio (sin contenedor ni fondo) para TopBars.
 */
@Composable
fun DraftoTopBarIconButton(
    imageVector: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onBackground
) {
    androidx.compose.material3.IconButton(
        onClick = onClick,
        modifier = modifier.size(40.dp)
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * TopBar superior adhesiva para pantallas secundarias (ChatDetail, NewChat, Editor, NoteDetail)
 * con efecto Spatial Pure Blur Ultra Thin de Haze.
 */
@Composable
fun DraftoSecondaryTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isScrolled: Boolean = true,
    isBlurEnabled: Boolean = LocalDraftoBlurEnabled.current,
    backIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    backDescription: String? = null,
    centerContent: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    DraftoStickyHeader(
        modifier = modifier,
        isScrolled = isScrolled,
        isBlurEnabled = isBlurEnabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.statusBars
                        .union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
                .padding(
                    top = PaddingScreenTopBarTop,
                    bottom = PaddingScreenTopBarBottom,
                    start = 16.dp,
                    end = 16.dp
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DraftoTopBarIconButton(
                imageVector = backIcon,
                contentDescription = backDescription,
                onClick = onBack
            )

            if (centerContent != null) {
                centerContent()
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PaddingSmall),
                content = actions
            )
        }
    }
}

