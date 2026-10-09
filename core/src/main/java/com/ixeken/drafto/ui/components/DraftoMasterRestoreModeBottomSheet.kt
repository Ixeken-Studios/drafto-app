package com.ixeken.drafto.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMerge
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ixeken.drafto.core.R
import com.ixeken.drafto.domain.model.MasterRestoreMode
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.PaddingExtraSmall
import com.ixeken.drafto.ui.theme.PaddingLarge
import com.ixeken.drafto.ui.theme.PaddingScreenHorizontal
import com.ixeken.drafto.ui.theme.PaddingSmall

/**
 * Modal BottomSheet para seleccionar la estrategia de restauración de un respaldo maestro (.zip).
 *
 * Sigue la arquitectura canónica Nothing OS 5 con [DraftoSheetHeader], [DraftoSheetCardGroup],
 * [DraftoSheetOptionRow] y botón de cancelación en píldora.
 *
 * @param onDismissRequest Callback invocado para cerrar la hoja modal sin tomar acción.
 * @param onSelectMode Callback que notifica la estrategia de restauración seleccionada por el usuario.
 * @param modifier Modificador visual opcional para el contenedor del modal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftoMasterRestoreModeBottomSheet(
    onDismissRequest: () -> Unit,
    onSelectMode: (MasterRestoreMode) -> Unit,
    modifier: Modifier = Modifier
) {
    DraftoModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        DraftoSheetHeader(
            title = stringResource(R.string.restore_mode_sheet_title)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaddingScreenHorizontal, vertical = PaddingSmall)
        ) {
            Text(
                text = stringResource(R.string.restore_mode_sheet_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = DraftoTheme.colors.textSecondary,
                modifier = Modifier.padding(horizontal = PaddingSmall, vertical = PaddingExtraSmall)
            )

            Spacer(modifier = Modifier.height(PaddingSmall))

            DraftoSheetCardGroup(
                backgroundColor = DraftoTheme.colors.primaryContainer
            ) {
                // Opción 1: Fusionar con datos actuales
                DraftoSheetOptionRow(
                    title = stringResource(R.string.restore_mode_merge_title),
                    subtitle = stringResource(R.string.restore_mode_merge_desc),
                    icon = Icons.AutoMirrored.Rounded.CallMerge,
                    showDivider = true,
                    onClick = {
                        onDismissRequest()
                        onSelectMode(MasterRestoreMode.MERGE)
                    }
                )

                // Opción 2: Reemplazo limpio total
                DraftoSheetOptionRow(
                    title = stringResource(R.string.restore_mode_clean_title),
                    subtitle = stringResource(R.string.restore_mode_clean_desc),
                    icon = Icons.Rounded.DeleteSweep,
                    showDivider = false,
                    onClick = {
                        onDismissRequest()
                        onSelectMode(MasterRestoreMode.CLEAN_RESTORE)
                    }
                )
            }

            Spacer(modifier = Modifier.height(PaddingLarge))

            DraftoSheetCancelButton(
                onClick = onDismissRequest
            )

            Spacer(modifier = Modifier.height(PaddingExtraSmall))
        }
    }
}
