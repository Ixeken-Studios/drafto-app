package com.ixeken.drafto.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.graphics.compositeOver
import com.ixeken.drafto.ui.theme.BorderWidthSelectedCard
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import android.text.format.DateFormat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ixeken.drafto.core.R
import com.ixeken.drafto.domain.model.Bookmark
import com.ixeken.drafto.domain.model.hasValidImageUrl
import com.ixeken.drafto.domain.model.PlatformFilter
import com.ixeken.drafto.ui.theme.BookmarkActionIconSize
import com.ixeken.drafto.ui.theme.BookmarkActionTouchSize
import com.ixeken.drafto.ui.theme.BookmarkCardElevation
import com.ixeken.drafto.ui.theme.BookmarkFaviconSize
import com.ixeken.drafto.ui.theme.BookmarkImageHeightGrid
import com.ixeken.drafto.ui.theme.BookmarkImageHeightList
import com.ixeken.drafto.ui.theme.BookmarkSelectionIndicatorSize
import com.ixeken.drafto.ui.theme.BookmarkThumbnailWidthList
import com.ixeken.drafto.ui.theme.DraftoShapeBookmarkCard
import com.ixeken.drafto.ui.theme.DraftoShapeCardSmall
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.FrauncesFontFamily
import java.util.Date
import com.ixeken.drafto.ui.theme.PaddingExtraSmall
import com.ixeken.drafto.ui.theme.PaddingLarge
import com.ixeken.drafto.ui.theme.PaddingMedium
import com.ixeken.drafto.ui.theme.PaddingSmall
import com.ixeken.drafto.ui.theme.LocalFetchWebMetadataEnabled

import androidx.compose.foundation.layout.fillMaxSize
import coil.compose.SubcomposeAsyncImage

/**
 * Resuelve el glifo vectorial redondeado fallback correspondiente a la plataforma detectada
 * para marcadores que carecen de favicon descargable.
 */
fun getPlatformIcon(platform: PlatformFilter): ImageVector = when (platform) {
    PlatformFilter.INSTAGRAM -> Icons.Rounded.CameraAlt
    PlatformFilter.YOUTUBE -> Icons.Rounded.PlayArrow
    PlatformFilter.GITHUB -> Icons.Rounded.Code
    PlatformFilter.TWITTER_X -> Icons.Rounded.Language
    PlatformFilter.ARTICLE -> Icons.AutoMirrored.Rounded.Article
    PlatformFilter.ALL, PlatformFilter.GENERIC -> Icons.Rounded.Language
}

/**
 * Renderiza el favicon de un sitio web con degradación elegante hacia el icono estándar de Material.
 *
 * Si la URL del favicon es nula, vacía o falla durante la carga de red (ej. 403 en x.com o 404),
 * muestra de forma instantánea el icono clásico de navegación web [Icons.Rounded.Language].
 */
@Composable
fun DraftoWebsiteFavicon(
    faviconUrl: String?,
    platform: PlatformFilter,
    modifier: Modifier = Modifier,
    tint: Color = DraftoTheme.colors.textSecondary,
    allowNetwork: Boolean = LocalFetchWebMetadataEnabled.current
) {
    val fallbackVector = if (platform == PlatformFilter.TWITTER_X || platform == PlatformFilter.GENERIC || platform == PlatformFilter.ALL) {
        Icons.Rounded.Language
    } else {
        getPlatformIcon(platform)
    }

    if (allowNetwork && !faviconUrl.isNullOrBlank()) {
        SubcomposeAsyncImage(
            model = faviconUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
            loading = {
                Icon(
                    imageVector = fallbackVector,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.fillMaxSize()
                )
            },
            error = {
                Icon(
                    imageVector = fallbackVector,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
    } else {
        Icon(
            imageVector = fallbackVector,
            contentDescription = null,
            tint = tint,
            modifier = modifier
        )
    }
}

/**
 * Tarjeta unificada y reactiva de marcador para visualización en Lista y Cuadrícula Bento.
 *
 * Características de diseño y rendimiento:
 * - Esquinas redondeadas según el token semántico [BookmarkCardCornerRadius] (18dp).
 * - Ausencia total de bordes duros de 1dp, empleando elevación sutil [BookmarkCardElevation] y superficies adaptativas.
 * - Cabecera con favicon del sitio o glifo fallback según [PlatformFilter], dominio web y accesos rápidos a fijado o info.
 * - Soporte nativo para apertura directa en navegador al pulsar, despliegue de detalles con botón (i),
 *   y activación de selección múltiple mediante pulsación prolongada.
 * - Modo selección táctil que destaca visualmente la tarjeta sin provocar recomposiciones en cascada (120 FPS).
 *
 * @param bookmark Entidad de dominio inmutable del marcador.
 * @param onClick Acción ejecutada al pulsar la tarjeta (apertura directa de la URL).
 * @param onInfoClick Acción ejecutada al presionar el botón de información (i).
 * @param onLongClick Acción ejecutada al mantener pulsada la tarjeta (activa modo de selección).
 * @param onTogglePin Acción para alternar el estado de fijado (pin).
 * @param modifier Modificador Compose opcional.
 * @param isGridView Determina si el diseño se adapta a Cuadrícula Bento (vertical) o Lista (horizontal).
 * @param isSelected Indica si la tarjeta está seleccionada en modo de selección múltiple.
 * @param isSelectionMode Indica si la pantalla se encuentra actualmente en modo de selección múltiple.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraftoBookmarkCard(
    bookmark: Bookmark,
    onClick: () -> Unit,
    onInfoClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onTogglePin: () -> Unit = {},
    modifier: Modifier = Modifier,
    isGridView: Boolean = false,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false
) {
    val cardShape = DraftoShapeBookmarkCard
    val cardBgColor = DraftoCardSelectionDefaults.surfaceColor(isSelected)

    val haptic = LocalHapticFeedback.current

    Surface(
        shape = cardShape,
        color = cardBgColor,
        border = DraftoCardSelectionDefaults.border(isSelected),
        shadowElevation = DraftoCardSelectionDefaults.shadowElevation(isSelected),
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        if (isGridView) {
            // Diseño vertical de tarjeta en Cuadrícula Bento (Nothing OS)
            BookmarkCardGridContent(
                bookmark = bookmark,
                isSelectionMode = isSelectionMode,
                isSelected = isSelected,
                onInfoClick = onInfoClick
            )
        } else {
            // Diseño horizontal de tarjeta en Lista (Nothing OS)
            BookmarkCardListContent(
                bookmark = bookmark,
                isSelectionMode = isSelectionMode,
                isSelected = isSelected,
                onInfoClick = onInfoClick
            )
        }
    }
}

/**
 * Contenido completo de tarjeta vertical en modo Bento Grid (rediseño Nothing OS / Editorial).
 *
 * Estructura vertical cohesiva:
 * 1. Superior: Favicon del sitio + enlace web subrayado (maxLines = 1 con elipsis).
 * 2. Imagen previa OpenGraph (si existe): Full-width con esquinas redondeadas de 12.dp y altura BookmarkImageHeightGrid (110.dp).
 * 3. Título en Fraunces Bold (hasta 4 renglones dinámicos para efecto bento escalonado natural).
 * 4. Descripción en Poppins (hasta 4 renglones dinámicos).
 * 5. Inferior: Fecha del sistema (izquierda) y pin pasivo con botón info (derecha).
 */
@Composable
private fun BookmarkCardGridContent(
    bookmark: Bookmark,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formattedDate = remember(bookmark.createdAt, context) {
        DateFormat.getDateFormat(context).format(Date(bookmark.createdAt))
    }
    var isImageError by remember(bookmark.imageUrl) { mutableStateOf(false) }
    val allowNetwork = LocalFetchWebMetadataEnabled.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(PaddingMedium)
    ) {
        // 1. Fila superior: Favicon y enlace / dominio subrayado
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaddingSmall)
        ) {
            DraftoWebsiteFavicon(
                faviconUrl = bookmark.faviconUrl,
                platform = bookmark.platform,
                modifier = Modifier
                    .size(BookmarkFaviconSize)
                    .clip(RoundedCornerShape(4.dp))
            )

            Text(
                text = bookmark.domain,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 2. Imagen previa si existe y está visible (debajo del enlace y antes del título)
        if (allowNetwork && bookmark.hasValidImageUrl && bookmark.isPreviewVisible && !isImageError) {
            Spacer(modifier = Modifier.height(PaddingSmall))
            AsyncImage(
                model = bookmark.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onError = { isImageError = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BookmarkImageHeightGrid)
                    .clip(DraftoShapeCardSmall)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            )
        }

        Spacer(modifier = Modifier.height(PaddingSmall))

        // 3. Título en Fraunces Bold (hasta 4 renglones dinámicos para escalonado Bento)
        val displayTitle = bookmark.title?.takeIf { it.isNotBlank() } ?: bookmark.domain
        Text(
            text = displayTitle,
            fontFamily = FrauncesFontFamily,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )

        // 4. Descripción en Poppins (hasta 4 renglones dinámicos) si existe
        if (!bookmark.description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(PaddingExtraSmall))
            Text(
                text = bookmark.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(PaddingMedium))

        // 5. Fila inferior: Fecha en la izquierda, Pin e Info en la derecha
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PaddingExtraSmall)
            ) {
                // Pin pasivo en color de acento
                if (bookmark.isPinned) {
                    Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = stringResource(R.string.bookmark_action_pin),
                        tint = DraftoTheme.colors.accent,
                        modifier = Modifier.size(BookmarkActionIconSize)
                    )
                }

                if (isSelectionMode) {
                    DraftoSelectionCheckmarkIndicator(isSelected = isSelected)
                } else {
                    Box(
                        modifier = Modifier
                            .size(BookmarkActionTouchSize)
                            .clip(CircleShape)
                            .clickable(onClick = onInfoClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = stringResource(R.string.bookmark_action_info),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(BookmarkActionIconSize)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Contenido completo de tarjeta horizontal en modo Lista (rediseño Nothing OS / Editorial).
 *
 * Estructura de 3 filas:
 * 1. Superior: Favicon del sitio + enlace web subrayado (maxLines = 1 con elipsis).
 * 2. Central: Título en Fraunces Bold (hasta 4 renglones) + descripción en Poppins (hasta 4 renglones),
 *    con miniatura 4:3 (96dp x 72dp) a la derecha si existe imagen válida OpenGraph.
 * 3. Inferior: Fecha formateada según el estándar del sistema (izquierda) y pin pasivo con botón info (derecha).
 */
@Composable
private fun BookmarkCardListContent(
    bookmark: Bookmark,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formattedDate = remember(bookmark.createdAt, context) {
        DateFormat.getDateFormat(context).format(Date(bookmark.createdAt))
    }
    var isImageError by remember(bookmark.imageUrl) { mutableStateOf(false) }
    val allowNetwork = LocalFetchWebMetadataEnabled.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(PaddingLarge)
    ) {
        // 1. Fila superior: Favicon y enlace / dominio subrayado
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaddingSmall)
        ) {
            DraftoWebsiteFavicon(
                faviconUrl = bookmark.faviconUrl,
                platform = bookmark.platform,
                modifier = Modifier
                    .size(BookmarkFaviconSize)
                    .clip(RoundedCornerShape(4.dp))
            )

            Text(
                text = bookmark.domain,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(PaddingSmall))

        // 2. Fila central: Título y descripción (hasta 4 renglones) + miniatura a la derecha
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                val displayTitle = bookmark.title?.takeIf { it.isNotBlank() } ?: bookmark.domain
                Text(
                    text = displayTitle,
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )

                if (!bookmark.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(PaddingExtraSmall))
                    Text(
                        text = bookmark.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (allowNetwork && bookmark.hasValidImageUrl && bookmark.isPreviewVisible && !isImageError) {
                Spacer(modifier = Modifier.width(PaddingMedium))
                AsyncImage(
                    model = bookmark.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onError = { isImageError = true },
                    modifier = Modifier
                        .size(width = BookmarkThumbnailWidthList, height = BookmarkImageHeightList)
                        .clip(DraftoShapeCardSmall)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                )
            }
        }

        Spacer(modifier = Modifier.height(PaddingMedium))

        // 3. Fila inferior: Fecha en la izquierda, Pin e Info en la derecha
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PaddingSmall)
            ) {
                // Pin pasivo en color de acento
                if (bookmark.isPinned) {
                    Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = stringResource(R.string.bookmark_action_pin),
                        tint = DraftoTheme.colors.accent,
                        modifier = Modifier.size(BookmarkActionIconSize)
                    )
                }

                if (isSelectionMode) {
                    DraftoSelectionCheckmarkIndicator(isSelected = isSelected)
                } else {
                    Box(
                        modifier = Modifier
                            .size(BookmarkActionTouchSize)
                            .clip(CircleShape)
                            .clickable(onClick = onInfoClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = stringResource(R.string.bookmark_action_info),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(BookmarkActionIconSize)
                        )
                    }
                }
            }
        }
    }
}
