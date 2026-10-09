package com.ixeken.drafto.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.ixeken.drafto.core.R
import com.ixeken.drafto.domain.model.BookmarkCollection
import com.ixeken.drafto.ui.theme.BookmarkFormSectionSpacing
import com.ixeken.drafto.ui.theme.DraftoShapeCardSmall
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.PaddingExtraSmall
import com.ixeken.drafto.ui.theme.PaddingLarge
import com.ixeken.drafto.ui.theme.PaddingMedium
import com.ixeken.drafto.ui.theme.PaddingScreenHorizontal
import com.ixeken.drafto.ui.theme.PaddingSmall

/**
 * Modal BottomSheet unificado para exportar e importar marcadores en Drafto.
 *
 * Características de diseño Nothing OS / Cohesión Editorial:
 * - Manija de arrastre (drag handle) superior centrada, suprimiendo el botón circular rojo de cierre (X).
 * - Cabecera centrada con tipografía editorial [FrauncesFontFamily] en [MaterialTheme.typography.titleLarge].
 * - Selector de ámbito estilizado con píldoras segmentadas en alto contraste e inversión táctil.
 * - Opciones con estética sobria inspirada en la pantalla de Ajustes (Settings): iconos sólidos de 24dp
 *   directos en `onSurface`, título semi-bold y descripción atenuada en un contenedor único agrupado
 *   ([DraftoTheme.colors.primaryContainer]) con divisores ultrafinos ([BorderWidthThin]), sin fondos
 *   circulares de colores estridentes ni insignias innecesarias.
 * - Botón inferior simétrico de escape/cancelación en píldora ([DraftoShapePill], 50dp) en rojo semántico.
 *
 * @param onDismissRequest Callback para cerrar el modal.
 * @param onExportHtml Callback invocado al elegir exportar en HTML, recibiendo el ID de la colección o null.
 * @param onExportJson Callback invocado al elegir exportar en JSON, recibiendo el ID de la colección o null.
 * @param onImportBookmarks Callback invocado al elegir importar marcadores desde un archivo.
 * @param totalBookmarksCount Cantidad total de marcadores guardados en la app.
 * @param collections Lista de colecciones temáticas registradas.
 * @param activeCollectionId Identificador de la colección actualmente seleccionada en pantalla, si aplica.
 * @param modifier Modificador Compose opcional.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftoBookmarkBackupBottomSheet(
    onDismissRequest: () -> Unit,
    onExportHtml: (collectionId: String?) -> Unit,
    onExportJson: (collectionId: String?) -> Unit,
    onImportBookmarks: () -> Unit,
    totalBookmarksCount: Int,
    collections: List<BookmarkCollection>,
    activeCollectionId: String? = null,
    modifier: Modifier = Modifier
) {
    var selectedCollectionId by remember(activeCollectionId) {
        mutableStateOf(activeCollectionId)
    }

    val activeCollection = remember(activeCollectionId, collections) {
        collections.find { it.id == activeCollectionId }
    }

    DraftoModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaddingScreenHorizontal)
        ) {
            DraftoSheetHeader(
                title = stringResource(R.string.title_bookmark_backup)
            )

            Spacer(modifier = Modifier.height(BookmarkFormSectionSpacing))

            // Selector de Ámbito de Exportación (si hay una colección activa seleccionada)
            if (activeCollection != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(DraftoShapeCardSmall)
                        .background(DraftoTheme.colors.primaryContainer)
                        .padding(PaddingExtraSmall),
                    horizontalArrangement = Arrangement.spacedBy(PaddingExtraSmall)
                ) {
                    val isCollectionSelected = selectedCollectionId != null
                    // Píldora: Colección actual
                    ScopePill(
                        text = stringResource(
                            R.string.export_bookmarks_scope_collection,
                            activeCollection.name,
                            activeCollection.bookmarkCount
                        ),
                        isSelected = isCollectionSelected,
                        onClick = { selectedCollectionId = activeCollection.id },
                        modifier = Modifier.weight(1f)
                    )

                    // Píldora: Toda la biblioteca
                    ScopePill(
                        text = stringResource(R.string.export_bookmarks_scope_all, totalBookmarksCount),
                        isSelected = !isCollectionSelected,
                        onClick = { selectedCollectionId = null },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(PaddingMedium))
            }

            // Contenedor Agrupado de Opciones: Estética Settings Nothing OS
            DraftoSheetCardGroup(
                backgroundColor = DraftoTheme.colors.primaryContainer
            ) {
                // Opción 1: Exportar HTML Universal (Netscape)
                DraftoSheetOptionRow(
                    title = stringResource(R.string.export_bookmarks_html),
                    subtitle = stringResource(R.string.export_bookmarks_html_desc),
                    icon = Icons.Rounded.Language,
                    showDivider = true,
                    onClick = {
                        onDismissRequest()
                        onExportHtml(selectedCollectionId)
                    }
                )

                // Opción 2: Exportar JSON Nativo de Drafto
                DraftoSheetOptionRow(
                    title = stringResource(R.string.export_bookmarks_json),
                    subtitle = stringResource(R.string.export_bookmarks_json_desc),
                    icon = Icons.Rounded.Description,
                    showDivider = true,
                    onClick = {
                        onDismissRequest()
                        onExportJson(selectedCollectionId)
                    }
                )

                // Opción 3: Importar Marcadores
                DraftoSheetOptionRow(
                    title = stringResource(R.string.import_bookmarks),
                    subtitle = stringResource(R.string.import_bookmarks_desc),
                    icon = Icons.Rounded.Bookmark,
                    showDivider = false,
                    onClick = {
                        onDismissRequest()
                        onImportBookmarks()
                    }
                )
            }

            Spacer(modifier = Modifier.height(PaddingLarge))

            // Botón inferior Cancel (Píldora simétrica Nothing OS)
            DraftoSheetCancelButton(
                onClick = onDismissRequest
            )

            Spacer(modifier = Modifier.height(PaddingMedium))
        }
    }
}

/**
 * Cápsula segmentada para alternar el alcance de exportación entre la colección activa y la biblioteca completa.
 *
 * Utiliza contraste táctil en alto contraste para indicar el estado activo sin recurrir a fondos estridentes.
 */
@Composable
private fun ScopePill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color.Transparent
    }
    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.surface
    } else {
        DraftoTheme.colors.textMuted
    }

    Box(
        modifier = modifier
            .clip(DraftoShapeCardSmall)
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = PaddingSmall, horizontal = PaddingMedium),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = textColor,
            maxLines = 1
        )
    }
}
