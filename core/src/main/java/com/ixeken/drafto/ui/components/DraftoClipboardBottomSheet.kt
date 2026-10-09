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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import com.ixeken.drafto.ui.theme.contrastContentColor
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ixeken.drafto.core.R
import com.ixeken.drafto.domain.model.BookmarkCollection
import com.ixeken.drafto.domain.model.PlatformFilter
import com.ixeken.drafto.ui.theme.BookmarkFaviconContainerSize
import com.ixeken.drafto.ui.theme.BookmarkFaviconSize
import com.ixeken.drafto.ui.theme.BookmarkFormCapsuleHeight
import com.ixeken.drafto.ui.theme.BookmarkPreviewThumbnailSize
import com.ixeken.drafto.ui.theme.DraftoShapeCardSmall
import com.ixeken.drafto.ui.theme.DraftoShapePill
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.PaddingLarge
import com.ixeken.drafto.ui.theme.PaddingMedium
import com.ixeken.drafto.ui.theme.PaddingScreenHorizontal
import com.ixeken.drafto.ui.theme.PaddingSmall

/**
 * Modal Bottom Sheet para avisar al usuario sobre un enlace detectado automáticamente
 * en el portapapeles y permitir su guardado inmediato con o sin colección asignada.
 *
 * Características de diseño:
 * - Diseñado con [DraftoModalBottomSheet] y desenfoque Haze Ultra Thin.
 * - Muestra previsualización visual con favicon, dominio, título extraído y thumbnail si existe.
 * - Permite pre-asignar una colección temática mediante chips coloreados antes de confirmar.
 * - Botón de guardado prominente en tono cálido Mango ([DraftoTheme.colors.warmMango]) y acción de descarte ("Not now").
 *
 * @param url Dirección URL detectada en el portapapeles.
 * @param domain Host o nombre de dominio del sitio.
 * @param onDismiss Callback invocado al descartar o posponer el modal.
 * @param onSave Callback invocado al confirmar el guardado, recibiendo el ID de la colección opcional.
 * @param modifier Modificador Compose opcional.
 * @param title Título web extraído opcional.
 * @param description Resumen o descripción web opcional.
 * @param imageUrl URL de la imagen de portada opcional.
 * @param faviconUrl URL directa del favicon opcional.
 * @param platform Plataforma clasificada según [PlatformFilter].
 * @param collections Lista de colecciones de usuario disponibles.
 * @param selectedCollectionId Identificador de la colección preseleccionada, si aplica.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftoClipboardBottomSheet(
    url: String,
    domain: String,
    onDismiss: () -> Unit,
    onSave: (collectionId: String?) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    imageUrl: String? = null,
    faviconUrl: String? = null,
    platform: PlatformFilter = PlatformFilter.GENERIC,
    collections: List<BookmarkCollection> = emptyList(),
    selectedCollectionId: String? = null
) {
    var activeCollectionId by remember(selectedCollectionId) { mutableStateOf(selectedCollectionId) }

    DraftoModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        DraftoSheetHeader(
            title = stringResource(R.string.bookmark_clipboard_title),
            onDismiss = onDismiss
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = PaddingLarge)
        ) {
            // Subtítulo descriptivo
            Text(
                text = stringResource(R.string.bookmark_clipboard_desc),
                style = MaterialTheme.typography.bodySmall,
                color = DraftoTheme.colors.textSecondary,
                modifier = Modifier.padding(horizontal = PaddingScreenHorizontal)
            )

            Spacer(modifier = Modifier.height(PaddingSmall))

            // Tarjeta de previsualización del enlace
            DraftoSheetCard(
                modifier = Modifier.padding(horizontal = PaddingScreenHorizontal, vertical = PaddingSmall)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PaddingMedium)
                ) {
                    // Favicon o icono de plataforma con fallback seguro
                    Box(
                        modifier = Modifier
                            .size(BookmarkFaviconContainerSize)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        DraftoWebsiteFavicon(
                            faviconUrl = faviconUrl,
                            platform = platform,
                            modifier = Modifier
                                .size(BookmarkFaviconSize)
                                .clip(CircleShape),
                            tint = DraftoTheme.colors.textSecondary
                        )
                    }

                    // Título y dominio
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = domain,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = DraftoTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val displayTitle = title?.takeIf { it.isNotBlank() } ?: domain
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = url,
                            style = MaterialTheme.typography.bodySmall,
                            color = DraftoTheme.colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Thumbnail si está disponible
                    if (!imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(BookmarkPreviewThumbnailSize)
                                .clip(DraftoShapeCardSmall)
                        )
                    }
                }
            }

            // Selector opcional de Colección en píldoras con desplazamiento horizontal
            if (collections.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.bookmark_section_collection),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = DraftoTheme.colors.textSecondary,
                    modifier = Modifier.padding(horizontal = PaddingScreenHorizontal)
                )

                Spacer(modifier = Modifier.height(PaddingSmall))

                val isDarkTheme = DraftoTheme.colors.isDark
                val noneLabel = stringResource(R.string.bookmark_no_collection)
                val collectionOptions = remember(collections, noneLabel, isDarkTheme) {
                    val list = ArrayList<DraftoCapsuleOption<String?>>(collections.size + 1)
                    list.add(
                        DraftoCapsuleOption(
                            key = null,
                            label = noneLabel,
                            icon = Icons.Rounded.Close
                        )
                    )
                    for (col in collections) {
                        list.add(
                            DraftoCapsuleOption(
                                key = col.id,
                                label = col.name,
                                icon = getCollectionIcon(col.iconName),
                                badgeContainerColor = parseCollectionColor(col.colorHex, isDark = isDarkTheme)
                            )
                        )
                    }
                    list
                }

                DraftoCapsuleFlowSelector(
                    options = collectionOptions,
                    selectedKey = activeCollectionId,
                    onOptionSelected = { activeCollectionId = it },
                    dividersAfterKeys = if (collections.isNotEmpty()) setOf(null) else emptySet(),
                    height = BookmarkFormCapsuleHeight,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(PaddingLarge))

            // Botones de acción: Guardar (Píldora verde) y Descartar (Píldora tonal suave)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaddingScreenHorizontal),
                verticalArrangement = Arrangement.spacedBy(PaddingSmall)
            ) {
                DraftoSheetPrimaryButton(
                    text = stringResource(R.string.bookmark_clipboard_save),
                    onClick = {
                        onSave(activeCollectionId)
                        onDismiss()
                    },
                    containerColor = DraftoTheme.colors.emerald,
                    contentColor = Color.White
                )

                DraftoSheetCancelButton(
                    text = stringResource(R.string.bookmark_clipboard_dismiss),
                    onClick = onDismiss
                )
            }
        }
    }
}
