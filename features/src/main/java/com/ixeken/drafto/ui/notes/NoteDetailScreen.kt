package com.ixeken.drafto.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.ixeken.drafto.core.R
import com.ixeken.drafto.domain.model.Note
import com.ixeken.drafto.ui.components.DraftoMarkdownContent
import com.ixeken.drafto.ui.components.DraftoSecondaryTopBar
import com.ixeken.drafto.ui.theme.DraftoStickyScaffoldFallbackTopPadding
import com.ixeken.drafto.ui.theme.FrauncesFontFamily
import com.ixeken.drafto.ui.theme.LocalHazeState
import com.ixeken.drafto.ui.theme.PaddingLarge
import com.ixeken.drafto.ui.theme.PaddingMedium
import com.ixeken.drafto.ui.theme.PaddingScreenHorizontal
import dev.chrisbanes.haze.hazeSource

/**
 * Pantalla de sólo lectura para visualizar notas individuales con soporte de formateo Markdown.
 *
 * Utiliza [DraftoMarkdownContent] para renderizar bloques estructurados Nothing OS
 * y respeta el sistema de tokens centralizado de espaciados y tipografías.
 *
 * @param note Nota a desplegar en pantalla.
 * @param onBack Callback invocado para regresar a la lista de notas.
 * @param modifier Modificador visual para el contenedor raíz.
 */
@Composable
fun NoteDetailScreen(
    note: Note?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    val density = LocalDensity.current
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val safeTopPadding = remember(headerHeightPx, density) {
        if (headerHeightPx > 0) {
            with(density) { headerHeightPx.toDp() } + PaddingMedium
        } else {
            DraftoStickyScaffoldFallbackTopPadding
        }
    }

    val hazeState = LocalHazeState.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PaddingScreenHorizontal, vertical = PaddingLarge)
        ) {
            Spacer(modifier = Modifier.height(safeTopPadding))

            if (note != null) {
                Text(
                    text = note.title.ifBlank { stringResource(R.string.title_new_note) },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(PaddingLarge))

                DraftoMarkdownContent(
                    content = note.content,
                    titleToDeduplicate = note.title
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = stringResource(R.string.title_new_note),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // Header adhesivo superior con Spatial Pure Blur Ultra Thin
        DraftoSecondaryTopBar(
            title = note?.title?.ifBlank { stringResource(R.string.title_new_note) } ?: "",
            onBack = onBack,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onGloballyPositioned { coordinates ->
                    if (headerHeightPx != coordinates.size.height) {
                        headerHeightPx = coordinates.size.height
                    }
                }
        )
    }
}
