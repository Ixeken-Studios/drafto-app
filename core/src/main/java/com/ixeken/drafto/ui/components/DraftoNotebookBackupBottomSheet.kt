package com.ixeken.drafto.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
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
 * Modal BottomSheet unificado para exportar e importar el cuaderno de notas y tareas en Drafto.
 *
 * Características de diseño Nothing OS y cohesión con el resto de la app:
 * - Manija de arrastre superior centrada y cabecera editorial con [DraftoSheetHeader].
 * - Contenedores agrupados estilo Settings en [DraftoTheme.colors.primaryContainer] usando [DraftoSheetCardGroup] y [DraftoSheetOptionRow].
 * - Separación limpia entre tarjetas mediante [SettingsGroupSpacing] sin títulos redundantes.
 * - Botón inferior simétrico de cancelación en píldora roja semántica [DraftoSheetCancelButton].
 *
 * @param onDismissRequest Callback invocado para cerrar la hoja modal.
 * @param onExportZip Callback invocado al seleccionar exportar el cuaderno en archivo .zip.
 * @param onImportZip Callback invocado al seleccionar importar y restaurar el cuaderno desde un archivo .zip.
 * @param onImportSingleNote Callback invocado al seleccionar importar una nota individual desde un archivo .md.
 * @param onImportSingleTodos Callback invocado al seleccionar importar tareas desde un archivo Markdown .md.
 * @param modifier Modificador Compose opcional para personalizar el contenedor externo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftoNotebookBackupBottomSheet(
    onDismissRequest: () -> Unit,
    onExportZip: () -> Unit,
    onImportZip: () -> Unit,
    onImportSingleNote: () -> Unit = {},
    onImportSingleTodos: () -> Unit = {},
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
        ) {
            DraftoSheetHeader(
                title = stringResource(R.string.title_backup_notebook)
            )

            Spacer(modifier = Modifier.height(BookmarkFormSectionSpacing))

            // 1. Tarjeta de exportación agrupada
            DraftoSheetCardGroup(
                backgroundColor = DraftoTheme.colors.primaryContainer
            ) {
                DraftoSheetOptionRow(
                    title = stringResource(R.string.export_notebook_zip_title),
                    subtitle = stringResource(R.string.export_notebook_zip_desc),
                    icon = Icons.Rounded.FileUpload,
                    showDivider = false,
                    onClick = {
                        onDismissRequest()
                        onExportZip()
                    }
                )
            }

            Spacer(modifier = Modifier.height(SettingsGroupSpacing))

            // 2. Tarjeta de importación agrupada
            DraftoSheetCardGroup(
                backgroundColor = DraftoTheme.colors.primaryContainer
            ) {
                // Opción 1: Importar libreta completa desde ZIP (todas las notas y tareas)
                DraftoSheetOptionRow(
                    title = stringResource(R.string.import_notebook_zip_title),
                    subtitle = stringResource(R.string.import_notebook_zip_desc),
                    icon = Icons.Rounded.FileDownload,
                    showDivider = true,
                    onClick = {
                        onDismissRequest()
                        onImportZip()
                    }
                )

                // Opción 2: Importar nota individual (.md)
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

                // Opción 3: Importar tareas (.md)
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

            // 3. Botón inferior Cancel en píldora simétrica Nothing OS
            DraftoSheetCancelButton(
                onClick = onDismissRequest
            )

            Spacer(modifier = Modifier.height(PaddingMedium))
        }
    }
}
