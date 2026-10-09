package com.ixeken.drafto.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ixeken.drafto.core.R
import com.ixeken.drafto.ui.theme.BookmarkFormSectionSpacing
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.PaddingScreenHorizontal
import com.ixeken.drafto.ui.theme.PaddingSmall
import com.ixeken.drafto.ui.theme.SheetCardGroupSpacing

/**
 * Modal Bottom Sheet canónico para seleccionar el idioma de la aplicación.
 *
 * Sigue la jerarquía visual de Nothing OS con encabezado centrado Fraunces,
 * opciones encapsuladas en un grupo de tarjetas con contraste dinámico y botón de cierre.
 *
 * @param selectedLanguageTag Código de idioma activo (ej. "en", "es-MX", "es").
 * @param onSelectLanguage Callback invocado al seleccionar una variante de idioma.
 * @param onDismiss Callback invocado para cerrar el modal.
 * @param sheetState Estado del BottomSheet modal de Material 3.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftoLanguageBottomSheet(
    selectedLanguageTag: String,
    onSelectLanguage: (String) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    DraftoModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaddingScreenHorizontal)
        ) {
            DraftoSheetHeader(
                title = stringResource(R.string.bs_language_title)
            )

            Spacer(modifier = Modifier.height(BookmarkFormSectionSpacing))

            DraftoSheetCardGroup(
                backgroundColor = DraftoTheme.colors.primaryContainer
            ) {
                // Opción: English
                val isEnglishSelected = selectedLanguageTag.startsWith("en", ignoreCase = true)
                DraftoSheetOptionRow(
                    title = stringResource(R.string.language_english),
                    subtitle = stringResource(R.string.language_english_subtitle),
                    icon = Icons.Rounded.Language,
                    showDivider = true,
                    onClick = {
                        onSelectLanguage("en")
                        onDismiss()
                    },
                    trailingContent = if (isEnglishSelected) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = DraftoTheme.colors.accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else null
                )

                // Opción: Español (México)
                val isSpanishSelected = selectedLanguageTag.startsWith("es", ignoreCase = true)
                DraftoSheetOptionRow(
                    title = stringResource(R.string.language_spanish),
                    subtitle = stringResource(R.string.language_spanish_subtitle),
                    icon = Icons.Rounded.Language,
                    showDivider = false,
                    onClick = {
                        onSelectLanguage("es-MX")
                        onDismiss()
                    },
                    trailingContent = if (isSpanishSelected) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = DraftoTheme.colors.accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else null
                )
            }

            Spacer(modifier = Modifier.height(SheetCardGroupSpacing))

            DraftoSheetCancelButton(
                onClick = onDismiss
            )

            Spacer(modifier = Modifier.height(PaddingSmall))
        }
    }
}
