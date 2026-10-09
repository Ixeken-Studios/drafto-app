package com.ixeken.drafto.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ixeken.drafto.core.R
import com.ixeken.drafto.domain.model.Bookmark
import com.ixeken.drafto.domain.model.BookmarkCollection
import com.ixeken.drafto.domain.model.hasValidImageUrl
import com.ixeken.drafto.ui.theme.BookmarkActionTouchSize
import com.ixeken.drafto.ui.theme.BookmarkDetailActionButtonSize
import com.ixeken.drafto.ui.theme.BookmarkDetailActionIconSize
import com.ixeken.drafto.ui.theme.BookmarkDetailImageHeightLarge
import com.ixeken.drafto.ui.theme.BookmarkDetailPillToggleHeight
import com.ixeken.drafto.ui.theme.BookmarkDetailPillToggleIconSize
import com.ixeken.drafto.ui.theme.BookmarkDetailPillTogglePaddingHorizontal
import com.ixeken.drafto.ui.theme.BookmarkDetailToggleIconSize
import com.ixeken.drafto.ui.theme.BookmarkDragHandleHeight
import com.ixeken.drafto.ui.theme.BookmarkDragHandlePaddingBottom
import com.ixeken.drafto.ui.theme.BookmarkDragHandlePaddingTop
import com.ixeken.drafto.ui.theme.BookmarkDragHandleWidth
import com.ixeken.drafto.ui.theme.BookmarkFaviconContainerSize
import com.ixeken.drafto.ui.theme.BookmarkFaviconSize
import com.ixeken.drafto.ui.theme.BookmarkFormCapsuleHeight
import com.ixeken.drafto.ui.theme.BookmarkFormSectionSpacing
import com.ixeken.drafto.ui.theme.DraftoShapeBookmarkDetailImage
import com.ixeken.drafto.ui.theme.DraftoShapeBookmarkFormInput
import com.ixeken.drafto.ui.theme.DraftoShapePill
import com.ixeken.drafto.ui.theme.DraftoSunsetCoral
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.FrauncesFontFamily
import com.ixeken.drafto.ui.theme.LocalFetchWebMetadataEnabled
import com.ixeken.drafto.ui.theme.PaddingExtraLarge
import com.ixeken.drafto.ui.theme.PaddingLarge
import com.ixeken.drafto.ui.theme.PaddingMedium
import com.ixeken.drafto.ui.theme.PaddingScreenHorizontal
import com.ixeken.drafto.ui.theme.PaddingSmall
import com.ixeken.drafto.ui.theme.PureWhite
import com.ixeken.drafto.ui.theme.contrastContentColor


/**
 * Modal Bottom Sheet unificado para visualización de detalles, metadatos y acciones rápidas
 * de un marcador guardado, alineado con la estética Nothing OS / editorial de Drafto.
 *
 * Características:
 * - Manija de arrastre (drag handle) superior y cabecera centrada con favicon y dominio en [FrauncesFontFamily].
 * - Previsualización de imagen destacada OpenGraph con botón alargado centrado posicionado debajo
 *   para conmutar visibilidad, adaptado al tema activo Nothing OS 5 (oscuro en temas claros y claro en temas oscuros).
 * - Sección "Bookmark info" dentro de un contenedor en [DraftoTheme.colors.primaryContainer].
 * - Sección "Website link" con enlace subrayado, desplazamiento horizontal y botón de copiado rápido.
 * - Sección "Collection" en cápsula informativa de solo lectura.
 * - Fila inferior con 4 botones de acción circulares: Delete (con diálogo de confirmación), Edit, Share y Open browser.
 *
 * @param bookmark Entidad de dominio inmutable del marcador.
 * @param onDismiss Callback para cerrar la hoja modal.
 * @param onOpenInBrowser Callback para abrir el enlace en el navegador externo.
 * @param onCopyUrl Callback para copiar el enlace al portapapeles.
 * @param onShareUrl Callback para compartir la URL mediante el selector del sistema.
 * @param onDeleteBookmark Callback para eliminar definitivamente el marcador.
 * @param modifier Modificador Compose opcional.
 * @param allCollections Lista de colecciones temáticas registradas por el usuario.
 * @param assignedCollectionIds Colecciones a las que se encuentra vinculado el marcador.
 * @param onTogglePreviewVisibility Callback para alternar la visibilidad de la imagen previa del marcador.
 * @param onToggleCollection Callback opcional para asociar o desasociar colecciones.
 * @param onUpdateBookmark Callback opcional invocado al guardar cambios desde la edición.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftoBookmarkDetailBottomSheet(
    bookmark: Bookmark,
    onDismiss: () -> Unit,
    onOpenInBrowser: (String) -> Unit,
    onCopyUrl: (String) -> Unit,
    onShareUrl: (String) -> Unit,
    onDeleteBookmark: (String) -> Unit,
    modifier: Modifier = Modifier,
    allCollections: List<BookmarkCollection> = emptyList(),
    assignedCollectionIds: Set<String> = emptySet(),
    onTogglePreviewVisibility: ((isVisible: Boolean) -> Unit)? = null,
    onToggleCollection: ((collectionId: String, isAssigned: Boolean) -> Unit)? = null,
    onUpdateBookmark: ((id: String, title: String, description: String, url: String?, collectionId: String?) -> Unit)? = null
) {
    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var isImageError by remember(bookmark.imageUrl) { mutableStateOf(false) }
    var isPreviewVisible by remember(bookmark.id, bookmark.isPreviewVisible) {
        mutableStateOf(bookmark.isPreviewVisible)
    }

    val allowNetwork = LocalFetchWebMetadataEnabled.current
    val hasValidImage = allowNetwork && bookmark.hasValidImageUrl && !isImageError

    DraftoModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = PaddingLarge)
        ) {
            // 1. Manija de arrastre (drag handle) superior
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(
                        top = BookmarkDragHandlePaddingTop,
                        bottom = BookmarkDragHandlePaddingBottom
                    )
                    .width(BookmarkDragHandleWidth)
                    .height(BookmarkDragHandleHeight)
                    .clip(DraftoShapePill)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f))
            )

            // 2. Cabecera centrada: Favicon + Dominio en tipografía Fraunces
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaddingScreenHorizontal)
                    .padding(bottom = PaddingMedium),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(BookmarkFaviconContainerSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)),
                    contentAlignment = Alignment.Center
                ) {
                    DraftoWebsiteFavicon(
                        faviconUrl = bookmark.faviconUrl,
                        platform = bookmark.platform,
                        modifier = Modifier
                            .size(BookmarkFaviconSize)
                            .clip(CircleShape),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(PaddingSmall))

                Text(
                    text = bookmark.domain,
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 3. Previsualización de Imagen OpenGraph (Ficha de Detalle de altura estable)
            if (hasValidImage) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PaddingScreenHorizontal)
                        .padding(bottom = PaddingMedium)
                ) {
                    AsyncImage(
                        model = bookmark.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        onError = { isImageError = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(BookmarkDetailImageHeightLarge)
                            .clip(DraftoShapeBookmarkDetailImage)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    )
                }
            }

            // 4. SECCIÓN: Bookmark info
            Text(
                text = stringResource(R.string.bookmark_section_info),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = DraftoTheme.colors.textSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaddingScreenHorizontal)
                    .padding(bottom = PaddingSmall)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaddingScreenHorizontal)
                    .clip(DraftoShapeBookmarkFormInput)
                    .background(DraftoTheme.colors.primaryContainer)
                    .padding(PaddingLarge)
            ) {
                val displayTitle = bookmark.title?.takeIf { it.isNotBlank() } ?: bookmark.domain
                Text(
                    text = displayTitle,
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!bookmark.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(PaddingSmall))
                    Text(
                        text = bookmark.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = DraftoTheme.colors.textSecondary
                    )
                }

                // Opción A: ToggleButton en Píldora para controlar la visibilidad del preview en la tarjeta del feed
                if (hasValidImage) {
                    Spacer(modifier = Modifier.height(PaddingMedium))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(PaddingMedium))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = PaddingSmall)
                        ) {
                            Text(
                                text = stringResource(R.string.bookmark_preview_card_title),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.bookmark_preview_card_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = DraftoTheme.colors.textSecondary
                            )
                        }

                        // Píldora Interactiva Nothing OS 5 ToggleButton
                        Row(
                            modifier = Modifier
                                .height(BookmarkDetailPillToggleHeight)
                                .clip(DraftoShapePill)
                                .background(
                                    if (isPreviewVisible) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
                                )
                                .clickable {
                                    val nextVisible = !isPreviewVisible
                                    isPreviewVisible = nextVisible
                                    onTogglePreviewVisibility?.invoke(nextVisible)
                                }
                                .padding(horizontal = BookmarkDetailPillTogglePaddingHorizontal),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(PaddingSmall)
                        ) {
                            Icon(
                                imageVector = if (isPreviewVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                contentDescription = null,
                                tint = if (isPreviewVisible) MaterialTheme.colorScheme.surface else DraftoTheme.colors.textSecondary,
                                modifier = Modifier.size(BookmarkDetailPillToggleIconSize)
                            )
                            Text(
                                text = stringResource(
                                    if (isPreviewVisible) R.string.bookmark_preview_visible
                                    else R.string.bookmark_preview_hidden
                                ),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isPreviewVisible) MaterialTheme.colorScheme.surface else DraftoTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }

            // 5. SECCIÓN: Website link
            Spacer(modifier = Modifier.height(BookmarkFormSectionSpacing))

            Text(
                text = stringResource(R.string.bookmark_section_link),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = DraftoTheme.colors.textSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaddingScreenHorizontal)
                    .padding(bottom = PaddingSmall)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaddingScreenHorizontal)
                    .clip(DraftoShapeBookmarkFormInput)
                    .background(DraftoTheme.colors.primaryContainer)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = bookmark.url,
                    style = MaterialTheme.typography.bodySmall.copy(
                        textDecoration = TextDecoration.Underline
                    ),
                    color = DraftoTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState())
                        .padding(end = PaddingSmall)
                )

                Box(
                    modifier = Modifier
                        .size(BookmarkActionTouchSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        .clickable { onCopyUrl(bookmark.url) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = stringResource(R.string.action_copy),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(BookmarkDetailToggleIconSize)
                    )
                }
            }

            // 6. SECCIÓN: Collection (Informativa de solo lectura)
            Spacer(modifier = Modifier.height(BookmarkFormSectionSpacing))

            Text(
                text = stringResource(R.string.bookmark_section_collection),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = DraftoTheme.colors.textSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaddingScreenHorizontal)
                    .padding(bottom = PaddingSmall)
            )

            val assignedCollection = remember(assignedCollectionIds, allCollections) {
                allCollections.firstOrNull { it.id in assignedCollectionIds }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaddingScreenHorizontal),
                horizontalArrangement = Arrangement.Start
            ) {
                if (assignedCollection != null) {
                    val isDarkTheme = DraftoTheme.colors.isDark
                    val colColor = parseCollectionColor(assignedCollection.colorHex, isDark = isDarkTheme)
                    DraftoStaticCapsule(
                        label = assignedCollection.name,
                        icon = getCollectionIcon(assignedCollection.iconName),
                        backgroundColor = colColor,
                        contentColor = colColor.contrastContentColor(),
                        height = BookmarkFormCapsuleHeight
                    )
                } else {
                    DraftoStaticCapsule(
                        label = stringResource(R.string.bookmark_none_label),
                        icon = Icons.Rounded.Close,
                        backgroundColor = DraftoTheme.colors.primaryContainer,
                        contentColor = DraftoTheme.colors.textSecondary,
                        height = BookmarkFormCapsuleHeight
                    )
                }
            }

            // 7. Fila inferior de 4 botones circulares de acción
            Spacer(modifier = Modifier.height(PaddingExtraLarge))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaddingScreenHorizontal),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Acción 1: Eliminar marcador
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(BookmarkDetailActionButtonSize)
                            .clip(CircleShape)
                            .background(DraftoTheme.colors.primaryContainer)
                            .clickable { showDeleteConfirmation = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null,
                            tint = DraftoSunsetCoral,
                            modifier = Modifier.size(BookmarkDetailActionIconSize)
                        )
                    }
                    Spacer(modifier = Modifier.height(PaddingSmall))
                    Text(
                        text = stringResource(R.string.bookmark_action_delete_bookmark),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }

                // Acción 2: Editar marcador
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(BookmarkDetailActionButtonSize)
                            .clip(CircleShape)
                            .background(DraftoTheme.colors.primaryContainer)
                            .clickable { showEditSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(BookmarkDetailActionIconSize)
                        )
                    }
                    Spacer(modifier = Modifier.height(PaddingSmall))
                    Text(
                        text = stringResource(R.string.bookmark_action_edit_bookmark),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }

                // Acción 3: Compartir enlace
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(BookmarkDetailActionButtonSize)
                            .clip(CircleShape)
                            .background(DraftoTheme.colors.primaryContainer)
                            .clickable { onShareUrl(bookmark.url) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(BookmarkDetailActionIconSize)
                        )
                    }
                    Spacer(modifier = Modifier.height(PaddingSmall))
                    Text(
                        text = stringResource(R.string.bookmark_action_share_bookmark),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }

                // Acción 4: Abrir en navegador
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(BookmarkDetailActionButtonSize)
                            .clip(CircleShape)
                            .background(DraftoTheme.colors.primaryContainer)
                            .clickable {
                                onOpenInBrowser(bookmark.url)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInBrowser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(BookmarkDetailActionIconSize)
                        )
                    }
                    Spacer(modifier = Modifier.height(PaddingSmall))
                    Text(
                        text = stringResource(R.string.bookmark_action_open_browser),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Modal de confirmación de eliminación segura
    if (showDeleteConfirmation) {
        DraftoConfirmationDialog(
            title = stringResource(R.string.dialog_delete_bookmark_title),
            message = stringResource(R.string.dialog_delete_bookmark_message),
            confirmText = stringResource(R.string.action_delete),
            cancelText = stringResource(R.string.action_cancel),
            isDestructive = true,
            onConfirm = {
                showDeleteConfirmation = false
                onDeleteBookmark(bookmark.id)
                onDismiss()
            },
            onDismiss = { showDeleteConfirmation = false }
        )
    }

    // Modal secundario de edición completa de marcador
    if (showEditSheet) {
        DraftoBookmarkFormBottomSheet(
            isEditMode = true,
            initialBookmark = bookmark,
            collections = allCollections,
            initialCollectionId = assignedCollectionIds.firstOrNull(),
            onDismiss = { showEditSheet = false },
            onSave = { updatedUrl, updatedTitle, updatedDesc, updatedColId ->
                onUpdateBookmark?.invoke(
                    bookmark.id,
                    updatedTitle.orEmpty(),
                    updatedDesc.orEmpty(),
                    updatedUrl,
                    updatedColId
                )
                showEditSheet = false
            }
        )
    }
}
