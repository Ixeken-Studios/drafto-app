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
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ixeken.drafto.core.R
import com.ixeken.drafto.ui.theme.BookmarkFormSectionSpacing
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.PaddingLarge
import com.ixeken.drafto.ui.theme.PaddingMedium
import com.ixeken.drafto.ui.theme.PaddingScreenHorizontal
import com.ixeken.drafto.ui.theme.SettingsGroupSpacing

/**
 * Modal BottomSheet unificado para seleccionar qué tipo de contenido importar a Drafto.
 *
 * Características de diseño Nothing OS / Cohesión Editorial:
 * - Manija de arrastre superior y cabecera centrada con [DraftoSheetHeader].
 * - Opciones estructuradas en dos grupos sobrios con estética Settings usando [DraftoSheetCardGroup] y [DraftoSheetOptionRow]:
 *   1. Grupo de respaldos y colecciones: Master Backup, Bookmarks y Notebook.
 *   2. Grupo de documentos individuales: Import note e Import to-dos.
 *   Ambos grupos separados por [SettingsGroupSpacing] sin títulos de sección redundantes.
 * - Botón inferior simétrico de escape [DraftoSheetCancelButton].
 *
 * @param onDismissRequest Callback para cerrar el modal.
 * @param onImportBookmarks Callback invocado al elegir importar marcadores en .html o .json.
 * @param onImportNotes Callback legado para compatibilidad con la importación previa de notas.
 * @param onImportNotebook Callback invocado al elegir importar libreta universal (.zip).
 * @param onImportSingleNote Callback invocado al elegir importar una nota individual (.md).
 * @param onImportSingleTodos Callback invocado al elegir importar tareas individuales (.md).
 * @param onImportMasterBackup Callback invocado al elegir importar un respaldo maestro completo (.zip).
 * @param modifier Modificador visual para el contenedor del modal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftoImportBottomSheet(
    onDismissRequest: () -> Unit,
    onImportBookmarks: () -> Unit = {},
    onImportNotes: () -> Unit = {},
    onImportNotebook: () -> Unit = onImportNotes,
    onImportSingleNote: () -> Unit = {},
    onImportSingleTodos: () -> Unit = {},
    onImportMasterBackup: () -> Unit = {},
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
                title = stringResource(R.string.title_import_content)
            )

            Spacer(modifier = Modifier.height(BookmarkFormSectionSpacing))

            // 1. Grupo 1: Respaldos y Archivos Completos (Master Backup, Bookmarks, Notebook)
            DraftoSheetCardGroup(
                backgroundColor = DraftoTheme.colors.primaryContainer
            ) {
                // Opción 1: Drafto Master Backup (.zip)
                DraftoSheetOptionRow(
                    title = stringResource(R.string.import_type_master_backup),
                    subtitle = stringResource(R.string.import_type_master_backup_desc),
                    icon = Icons.Rounded.FolderZip,
                    showDivider = true,
                    onClick = {
                        onDismissRequest()
                        onImportMasterBackup()
                    }
                )

                // Opción 2: Bookmarks (.html, .json)
                DraftoSheetOptionRow(
                    title = stringResource(R.string.import_bookmarks),
                    subtitle = stringResource(R.string.import_bookmarks_desc),
                    icon = Icons.Rounded.Bookmark,
                    showDivider = true,
                    onClick = {
                        onDismissRequest()
                        onImportBookmarks()
                    }
                )

                // Opción 3: Notebook (.zip)
                DraftoSheetOptionRow(
                    title = stringResource(R.string.import_type_notebook),
                    subtitle = stringResource(R.string.import_type_notebook_desc),
                    icon = Icons.Rounded.Book,
                    showDivider = false,
                    onClick = {
                        onDismissRequest()
                        onImportNotebook()
                    }
                )
            }

            // Separación limpia entre grupos estilo Settings
            Spacer(modifier = Modifier.height(SettingsGroupSpacing))

            // 2. Grupo 2: Documentos Individuales (Nota .md, Tareas .md)
            DraftoSheetCardGroup(
                backgroundColor = DraftoTheme.colors.primaryContainer
            ) {
                // Opción 4: Import note (.md)
                DraftoSheetOptionRow(
                    title = stringResource(R.string.import_single_note_title),
                    subtitle = stringResource(R.string.import_single_note_desc),
                    icon = Icons.Rounded.Description,
                    showDivider = true,
                    onClick = {
                        onDismissRequest()
                        onImportSingleNote()
                    }
                )

                // Opción 5: Import to-dos (.md)
                DraftoSheetOptionRow(
                    title = stringResource(R.string.import_single_todos_title),
                    subtitle = stringResource(R.string.import_single_todos_desc),
                    icon = Icons.Rounded.TaskAlt,
                    showDivider = false,
                    onClick = {
                        onDismissRequest()
                        onImportSingleTodos()
                    }
                )
            }

            Spacer(modifier = Modifier.height(PaddingLarge))

            // 3. Botón inferior Cancel (Píldora simétrica Nothing OS)
            DraftoSheetCancelButton(
                onClick = onDismissRequest
            )

            Spacer(modifier = Modifier.height(PaddingMedium))
        }
    }
}

