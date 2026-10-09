package com.ixeken.drafto.ui.components

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ixeken.drafto.core.R
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.DraftoTransitions
import com.ixeken.drafto.ui.theme.LocalDraftoReducedMotion
import com.ixeken.drafto.ui.theme.PaddingScreenHorizontal
import com.ixeken.drafto.ui.theme.PaddingScreenTopBarBottom
import com.ixeken.drafto.ui.theme.PaddingScreenTopBarTop
import com.ixeken.drafto.ui.theme.PaddingSmall
import com.ixeken.drafto.ui.theme.SearchBarHeight
import com.ixeken.drafto.ui.theme.TabTitleTextStyle

/**
 * TopBar conmutable y buscador cohesivo Nothing OS 5 para todo el ecosistema de Drafto.
 *
 * Características de diseño:
 * - En reposo: Muestra el título [title] de la pantalla con tipografía Fraunces y las acciones [actions] proporcionadas.
 * - Al activar búsqueda ([isSearchActive] = true): La barra de búsqueda [DraftoSearchBar] toma el lugar exacto de la TopBar
 *   en el mismo renglón, liberando espacio vertical de pantalla y mostrando un botón circular exterior (X)
 *   con contenedor en color [MaterialTheme.colorScheme.error] e icono blanco para máxima distinción.
 * - Limpieza interior: La barra interior posee un botón (x) discreto que solo borra el texto sin cancelar la búsqueda.
 *
 * @param title Título textual de la pantalla en reposo.
 * @param isSearchActive Si el modo de búsqueda está activo.
 * @param searchQuery Consulta de texto actual.
 * @param onSearchQueryChange Callback al escribir en la barra de búsqueda.
 * @param onCloseSearch Callback invocado al presionar el botón circular (X) exterior para cerrar la búsqueda.
 * @param onOpenSearch Callback opcional para abrir la búsqueda si se usa el botón de lupa integrado.
 * @param modifier Modificador Compose opcional.
 * @param placeholder Texto de sugerencia dentro de la barra de búsqueda.
 * @param showDefaultSearchButton Si se debe incluir automáticamente el botón de búsqueda al final de [actions].
 * @param navigationIcon Icono opcional a la izquierda del título en reposo.
 * @param actions Acciones composables a la derecha del título en reposo.
 */
@Composable
fun DraftoSearchableTopBar(
    title: String,
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSearch: (() -> Unit)? = null,
    placeholder: String = stringResource(R.string.action_search),
    showDefaultSearchButton: Boolean = false,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Box(
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
        contentAlignment = Alignment.Center
    ) {
        val isReducedMotion = LocalDraftoReducedMotion.current
        AnimatedContent(
            targetState = isSearchActive,
            transitionSpec = {
                DraftoTransitions.searchBarContentTransform(
                    isSearchActive = targetState,
                    isReduced = isReducedMotion
                )
            },
            label = "SearchableTopBarTransition"
        ) { active ->
            if (active) {
                // Modo Búsqueda: Ocupa todo el renglón de la AppBar con botón exterior (X)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SearchBarHeight),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PaddingSmall)
                ) {
                    DraftoSearchBar(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        onClose = onCloseSearch,
                        placeholder = placeholder,
                        autoFocus = true,
                        hideCloseWhenEmpty = true,
                        modifier = Modifier.weight(1f)
                    )

                    // Botón circular exterior (X) Nothing OS con contenedor error e icono blanco
                    Box(
                        modifier = Modifier
                            .size(SearchBarHeight)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                            .clickable(onClick = onCloseSearch),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.action_cancel),
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                // Modo Reposo: TopBar estándar
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                            style = TabTitleTextStyle,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PaddingSmall)
                    ) {
                        actions()
                        if (showDefaultSearchButton && onOpenSearch != null) {
                            DraftoTopBarIconButton(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = stringResource(R.string.action_search),
                                onClick = onOpenSearch
                            )
                        }
                    }
                }
            }
        }
    }
}
