package com.ixeken.drafto.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatStrikethrough
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.ixeken.drafto.core.R
import com.ixeken.drafto.ui.theme.DraftoGlassMaterial
import com.ixeken.drafto.ui.theme.DraftoShapeCircle
import com.ixeken.drafto.ui.theme.DraftoShapePill
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.EditorFormatBarElevation
import com.ixeken.drafto.ui.theme.EditorFormatBarPaddingHorizontal
import com.ixeken.drafto.ui.theme.EditorFormatBarPaddingVertical
import com.ixeken.drafto.ui.theme.EditorFormatBarSpacing
import com.ixeken.drafto.ui.theme.LocalDraftoBlurEnabled
import com.ixeken.drafto.ui.theme.LocalHazeState
import com.ixeken.drafto.ui.theme.NavBarContextualButtonSize
import com.ixeken.drafto.ui.theme.NavBarContextualIconSize
import com.ixeken.drafto.ui.theme.draftoGlass
import com.ixeken.drafto.ui.theme.toHazeStyle
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.hazeBlur

/**
 * Barra rápida de herramientas de formato Markdown para la barra de redacción de chat.
 *
 * Características de diseño:
 * - Contenedor flotante en forma de cápsula [DraftoShapePill] con Pure Blur Ultra Thin.
 * - Elementos centrados con soporte de desplazamiento horizontal fluido para pantallas compactas.
 * - Físicas táctiles con respuesta háptica en cada acción.
 * - Iconografía Material 3 Rounded y tokens semánticos del sistema de diseño.
 */
@Composable
fun DraftoMarkdownFormatBar(
    onFormatBold: () -> Unit,
    onFormatItalic: () -> Unit,
    onFormatStrikethrough: () -> Unit,
    onFormatInlineCode: () -> Unit,
    onFormatCodeBlock: () -> Unit,
    onFormatBulletList: () -> Unit,
    onFormatQuote: () -> Unit,
    modifier: Modifier = Modifier,
    isBlurEnabled: Boolean = LocalDraftoBlurEnabled.current,
    hazeState: HazeState? = LocalHazeState.current
) {
    val navShadow = DraftoTheme.colors.navBarShadow

    Row(
        modifier = modifier
            .shadow(
                elevation = EditorFormatBarElevation,
                shape = DraftoShapePill,
                spotColor = navShadow,
                ambientColor = navShadow
            )
            .draftoGlass(
                shape = DraftoShapePill,
                isBlurEnabled = isBlurEnabled,
                hazeState = hazeState
            )
            .horizontalScroll(rememberScrollState())
            .padding(
                horizontal = EditorFormatBarPaddingHorizontal,
                vertical = EditorFormatBarPaddingVertical
            ),
        horizontalArrangement = Arrangement.spacedBy(EditorFormatBarSpacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Negrita (**)
        FormatBarButton(
            icon = Icons.Rounded.FormatBold,
            contentDescription = stringResource(R.string.format_bold),
            onClick = onFormatBold
        )

        // 2. Cursiva (_)
        FormatBarButton(
            icon = Icons.Rounded.FormatItalic,
            contentDescription = stringResource(R.string.format_italic),
            onClick = onFormatItalic
        )

        // 3. Tachado (~~)
        FormatBarButton(
            icon = Icons.Rounded.FormatStrikethrough,
            contentDescription = stringResource(R.string.format_strikethrough),
            onClick = onFormatStrikethrough
        )

        // 4. Código en línea (`)
        FormatBarButton(
            icon = Icons.Rounded.Code,
            contentDescription = stringResource(R.string.format_inline_code),
            onClick = onFormatInlineCode
        )

        // 5. Bloque de código (```)
        FormatBarButton(
            icon = Icons.Rounded.DataObject,
            contentDescription = stringResource(R.string.format_code_block),
            onClick = onFormatCodeBlock
        )

        // 6. Lista con viñetas (- )
        FormatBarButton(
            icon = Icons.AutoMirrored.Rounded.FormatListBulleted,
            contentDescription = stringResource(R.string.format_bullet_list),
            onClick = onFormatBulletList
        )

        // 7. Cita (> )
        FormatBarButton(
            icon = Icons.Rounded.FormatQuote,
            contentDescription = stringResource(R.string.format_quote),
            onClick = onFormatQuote
        )
    }
}

@Composable
private fun FormatBarButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .size(NavBarContextualButtonSize)
            .clip(DraftoShapeCircle)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(NavBarContextualIconSize)
        )
    }
}
