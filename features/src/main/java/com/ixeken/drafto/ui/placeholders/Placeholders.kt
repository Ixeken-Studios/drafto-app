package com.ixeken.drafto.ui.placeholders

import com.ixeken.drafto.ui.theme.LocalHazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ixeken.drafto.core.R
import com.ixeken.drafto.ui.components.DraftoEmptyState
import com.ixeken.drafto.ui.components.DraftoStickyHeader
import com.ixeken.drafto.ui.components.DraftoTabTopBar
import com.ixeken.drafto.ui.components.DraftoTopBarIconButton
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import com.ixeken.drafto.ui.theme.DraftoTheme

/**
 * TopBar encapsulada para la pestaña de Tasks.
 */
@Composable
fun TasksTopBar(
    isGridView: Boolean = false,
    onToggleGridView: () -> Unit = {},
    onToggleSearch: () -> Unit = {},
    modifier: Modifier = Modifier,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
        DraftoTopBarIconButton(
            imageVector = if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
            contentDescription = stringResource(if (isGridView) R.string.view_mode_list else R.string.view_mode_grid),
            onClick = onToggleGridView
        )
        DraftoTopBarIconButton(
            imageVector = Icons.Default.Search,
            contentDescription = stringResource(R.string.action_search),
            onClick = onToggleSearch
        )
    }
) {
    DraftoTabTopBar(
        title = stringResource(R.string.tab_tasks),
        modifier = modifier,
        actions = actions
    )
}

/**
 * TopBar encapsulada para la pestaña de Saved.
 */
@Composable
fun SavedTopBar(
    isGridView: Boolean = false,
    onToggleGridView: () -> Unit = {},
    onToggleSearch: () -> Unit = {},
    modifier: Modifier = Modifier,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
        DraftoTopBarIconButton(
            imageVector = if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
            contentDescription = stringResource(if (isGridView) R.string.view_mode_list else R.string.view_mode_grid),
            onClick = onToggleGridView
        )
        DraftoTopBarIconButton(
            imageVector = Icons.Default.Search,
            contentDescription = stringResource(R.string.action_search),
            onClick = onToggleSearch
        )
    }
) {
    DraftoTabTopBar(
        title = stringResource(R.string.tab_saved),
        modifier = modifier,
        actions = actions
    )
}

/**
 * Pantalla base para las secciones en construcción (Tasks y Saved).
 */
@Composable
private fun PlaceholderScreenBase(
    topBar: @Composable () -> Unit,
    icon: ImageVector,
    description: String,
    iconTint: Color? = null,
    modifier: Modifier = Modifier
) {
    val resolvedIconTint = iconTint ?: DraftoTheme.colors.textMuted
    val hazeState = LocalHazeState.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(MaterialTheme.colorScheme.background)
    ) {
        DraftoEmptyState(
            icon = icon,
            iconTint = resolvedIconTint,
            title = stringResource(R.string.placeholder_coming_soon),
            description = description,
            modifier = Modifier
                .then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier)
                .padding(top = 100.dp)
        )

        DraftoStickyHeader(
            isScrolled = false,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            topBar()
        }
    }
}

@Composable
fun TasksScreen(modifier: Modifier = Modifier) {
    PlaceholderScreenBase(
        topBar = { TasksTopBar() },
        icon = Icons.AutoMirrored.Rounded.List,
        description = stringResource(R.string.placeholder_tasks_description),
        iconTint = DraftoTheme.colors.emerald,
        modifier = modifier
    )
}

@Composable
fun SavedScreen(modifier: Modifier = Modifier) = com.ixeken.drafto.ui.bookmarks.BookmarksScreen(modifier = modifier)

@Composable
fun BookmarksTopBar(
    isGridView: Boolean = false,
    onToggleGridView: () -> Unit = {},
    onToggleSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) = SavedTopBar(isGridView, onToggleGridView, onToggleSearch, modifier)

@Composable
fun BookmarksScreen(modifier: Modifier = Modifier) = com.ixeken.drafto.ui.bookmarks.BookmarksScreen(modifier = modifier)
