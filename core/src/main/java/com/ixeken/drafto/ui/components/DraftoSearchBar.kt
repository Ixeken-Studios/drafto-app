package com.ixeken.drafto.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ixeken.drafto.core.R
import com.ixeken.drafto.ui.theme.DraftoSearchBarShape
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.LocalDraftoBlurEnabled
import com.ixeken.drafto.ui.theme.LocalHazeState
import com.ixeken.drafto.ui.theme.PaddingSmall
import com.ixeken.drafto.ui.theme.SearchBarActionIconSize
import com.ixeken.drafto.ui.theme.SearchBarActionTouchSize
import com.ixeken.drafto.ui.theme.SearchBarHeight
import com.ixeken.drafto.ui.theme.SearchBarIconSize
import com.ixeken.drafto.ui.theme.SearchBarInnerHorizontalPadding
import com.ixeken.drafto.ui.theme.draftoGlass

/**
 * Barra de búsqueda unificada y cohesiva de Drafto Brain OS.
 *
 * Características:
 * - Forma de píldora completa ([DraftoSearchBarShape] = [DraftoShapePill]) unificada en toda la aplicación.
 * - Superficie de vidrio esmerilado sin bordes duros ([draftoGlass]), degradando elegantemente a fondo plano.
 * - Soporte para acciones finales parametrizadas ([trailingContent]), como los botones de salto entre coincidencias (↑ ↓) en el detalle de chat.
 * - Conmutación inteligente de icono frontal: lupa de búsqueda cuando está vacía o cruz de cancelación/limpieza cuando tiene texto y acciones finales.
 */
@Composable
fun DraftoSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    isBlurEnabled: Boolean = LocalDraftoBlurEnabled.current,
    placeholder: String = stringResource(R.string.action_search),
    accentColor: Color = DraftoTheme.colors.accent,
    leadingIcon: ImageVector = Icons.Rounded.Search,
    imeAction: ImeAction = ImeAction.Search,
    onSearchAction: () -> Unit = {},
    focusRequester: FocusRequester? = null,
    autoFocus: Boolean = true,
    hideCloseWhenEmpty: Boolean = false,
    onLeadingCloseClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val hazeState = LocalHazeState.current
    val effectiveFocusRequester = remember(focusRequester) { focusRequester ?: FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isLeadingClose = query.isNotBlank() && trailingContent != null

    if (autoFocus) {
        LaunchedEffect(effectiveFocusRequester) {
            try {
                effectiveFocusRequester.requestFocus()
                keyboardController?.show()
            } catch (_: Exception) {
                delay(120L)
                try {
                    effectiveFocusRequester.requestFocus()
                    keyboardController?.show()
                } catch (_: Exception) {}
            }
        }
    }

    val surfaceColor = if (isBlurEnabled && hazeState != null) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
    } else {
        DraftoTheme.colors.cardSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SearchBarHeight)
            .shadow(
                elevation = 4.dp,
                shape = DraftoSearchBarShape,
                spotColor = DraftoTheme.colors.navBarShadow,
                ambientColor = DraftoTheme.colors.navBarShadow
            )
            .draftoGlass(
                shape = DraftoSearchBarShape,
                hazeState = hazeState,
                isBlurEnabled = isBlurEnabled,
                surfaceColor = surfaceColor,
                fallbackColor = DraftoTheme.colors.cardSurface
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                effectiveFocusRequester.requestFocus()
            }
            .padding(horizontal = SearchBarInnerHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLeadingClose) {
            IconButton(
                onClick = {
                    if (onLeadingCloseClick != null) {
                        onLeadingCloseClick()
                    } else {
                        onQueryChange("")
                    }
                },
                modifier = Modifier.size(SearchBarActionTouchSize)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.action_cancel),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(SearchBarIconSize)
                )
            }
        } else {
            Box(
                modifier = Modifier.size(SearchBarActionTouchSize),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = stringResource(R.string.action_search),
                    tint = if (query.isNotBlank()) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    modifier = Modifier.size(SearchBarIconSize)
                )
            }
        }

        Spacer(modifier = Modifier.width(PaddingSmall))

        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            ),
            cursorBrush = SolidColor(accentColor),
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            keyboardActions = KeyboardActions(onSearch = { onSearchAction() }),
            modifier = Modifier
                .weight(1f)
                .focusRequester(effectiveFocusRequester),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f)
                    )
                }
                innerTextField()
            }
        )

        if (trailingContent != null && query.isNotBlank()) {
            trailingContent()
        } else if (!hideCloseWhenEmpty || query.isNotBlank()) {
            IconButton(
                onClick = {
                    if (query.isNotBlank()) {
                        onQueryChange("")
                    } else {
                        onClose()
                    }
                },
                modifier = Modifier.size(SearchBarActionTouchSize)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.action_close_search),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (query.isNotBlank()) 0.85f else 0.40f),
                    modifier = Modifier.size(SearchBarActionIconSize)
                )
            }
        }
    }
}
