package com.ixeken.drafto.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ixeken.drafto.core.R
import com.ixeken.drafto.ui.theme.BookmarkFormSectionSpacing
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.PaddingMedium
import com.ixeken.drafto.ui.theme.PaddingScreenHorizontal

/**
 * Modal Bottom Sheet Nothing OS para seleccionar el tipo de elemento a crear dentro de una colección.
 *
 * Utilizado cuando una colección se encuentra vacía o el usuario desea agregar un ítem eligiendo
 * explícitamente entre Nota, Tarea (To-do) o Marcador (Bookmark).
 *
 * Sigue la estética cohesiva de Drafto con cabecera editorial [DraftoSheetHeader], contenedor
 * unificado [DraftoSheetCardGroup] con opciones [DraftoSheetOptionRow] y botón inferior [DraftoSheetCancelButton].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftoCollectionTypeChooserBottomSheet(
    onDismissRequest: () -> Unit,
    onSelectNote: () -> Unit,
    onSelectTodo: () -> Unit,
    onSelectBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    DraftoModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaddingScreenHorizontal)
                .verticalScroll(rememberScrollState())
        ) {
            DraftoSheetHeader(
                title = stringResource(R.string.collection_add_item_title)
            )

            Spacer(modifier = Modifier.height(BookmarkFormSectionSpacing))

            // Contenedor estilizado Nothing OS con las 3 opciones
            DraftoSheetCardGroup(
                backgroundColor = DraftoTheme.colors.primaryContainer
            ) {
                // Opción 1: Nota
                DraftoSheetOptionRow(
                    title = stringResource(R.string.action_new_note),
                    subtitle = stringResource(R.string.collection_add_note_desc),
                    icon = Icons.Rounded.Book,
                    iconTint = DraftoTheme.colors.sunsetCoral,
                    showDivider = true,
                    onClick = {
                        onDismissRequest()
                        onSelectNote()
                    }
                )

                // Opción 2: To-do
                DraftoSheetOptionRow(
                    title = stringResource(R.string.action_new_todo),
                    subtitle = stringResource(R.string.collection_add_todo_desc),
                    icon = Icons.Rounded.TaskAlt,
                    iconTint = DraftoTheme.colors.emerald,
                    showDivider = true,
                    onClick = {
                        onDismissRequest()
                        onSelectTodo()
                    }
                )

                // Opción 3: Marcador
                DraftoSheetOptionRow(
                    title = stringResource(R.string.action_new_saved),
                    subtitle = stringResource(R.string.collection_add_bookmark_desc),
                    icon = Icons.Rounded.Bookmark,
                    iconTint = DraftoTheme.colors.aquaCyan,
                    showDivider = false,
                    onClick = {
                        onDismissRequest()
                        onSelectBookmark()
                    }
                )
            }

            Spacer(modifier = Modifier.height(BookmarkFormSectionSpacing))

            DraftoSheetCancelButton(
                onClick = onDismissRequest
            )

            Spacer(modifier = Modifier.height(PaddingMedium))
        }
    }
}
