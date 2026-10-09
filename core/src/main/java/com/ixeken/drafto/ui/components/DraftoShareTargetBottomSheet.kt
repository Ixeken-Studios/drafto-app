package com.ixeken.drafto.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ixeken.drafto.core.R
import com.ixeken.drafto.domain.model.BookmarkCollection
import com.ixeken.drafto.domain.model.SharedPayload
import com.ixeken.drafto.ui.theme.BookmarkCapsuleSpacing
import com.ixeken.drafto.ui.theme.BookmarkFormCapsuleHeight
import com.ixeken.drafto.ui.theme.BookmarkFormSectionSpacing
import com.ixeken.drafto.ui.theme.DraftoShapeBookmarkFormInput
import com.ixeken.drafto.ui.theme.DraftoShapeCardSmall
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.FrauncesFontFamily
import com.ixeken.drafto.ui.theme.PaddingExtraSmall
import com.ixeken.drafto.ui.theme.PaddingLarge
import com.ixeken.drafto.ui.theme.PaddingMedium
import com.ixeken.drafto.ui.theme.PaddingScreenHorizontal
import com.ixeken.drafto.ui.theme.PaddingSmall
import com.ixeken.drafto.ui.theme.PureWhite
import com.ixeken.drafto.util.LinkMetadataExtractor
import java.net.URI
import java.util.Locale

private enum class ShareTargetTab {
    NOTE, BOOKMARK
}

/**
 * Modal universal tipo Bottom Sheet para enrutar contenido compartido desde Android
 * hacia Notas o Marcadores de Drafto con soporte de colecciones y estricto apego a Nothing OS 5.
 *
 * Decisiones arquitectonicas:
 * - Emplea [DraftoCapsuleSelector] para la alternancia entre Notas y Marcadores sin fondos artificiales en iconos.
 * - Iconos planos y monocromaticos sin contenedores circulares de color detras.
 * - Paridad absoluta con [DraftoBookmarkFormBottomSheet] en inputs y tipografia editorial [FrauncesFontFamily].
 * - Soporte para guardar notas asociadas a una coleccion existente mediante [DraftoScrollableCapsuleSelector].
 * - Botones simetricos inferiores [DraftoSheetPrimaryButton] y [DraftoSheetCancelButton].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftoShareTargetBottomSheet(
    payload: SharedPayload,
    onSaveAsNote: (title: String, content: String, collectionId: String?) -> Unit,
    onSaveAsBookmark: (url: String, title: String?, collectionId: String?) -> Unit,
    onDismiss: () -> Unit,
    collections: List<BookmarkCollection> = emptyList(),
    modifier: Modifier = Modifier
) {
    val initialTab = remember(payload) {
        when {
            payload is SharedPayload.Text && payload.isUrl -> ShareTargetTab.BOOKMARK
            else -> ShareTargetTab.NOTE
        }
    }
    var selectedTab by remember { mutableStateOf(initialTab) }

    val notesLabel = stringResource(R.string.share_target_tab_notes)
    val bookmarksLabel = stringResource(R.string.share_target_tab_bookmarks)
    val capsuleOptions = remember(notesLabel, bookmarksLabel) {
        listOf(
            DraftoCapsuleOption(
                key = ShareTargetTab.NOTE,
                label = notesLabel,
                icon = Icons.Rounded.Description,
                contentDescription = notesLabel
            ),
            DraftoCapsuleOption(
                key = ShareTargetTab.BOOKMARK,
                label = bookmarksLabel,
                icon = Icons.Rounded.Bookmark,
                contentDescription = bookmarksLabel
            )
        )
    }

    DraftoModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaddingScreenHorizontal)
                .verticalScroll(rememberScrollState())
        ) {
            DraftoSheetHeader(
                title = stringResource(R.string.share_target_title)
            )

            Spacer(modifier = Modifier.height(PaddingSmall))

            // Selector segmentado de destinos mediante capsulas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = PaddingExtraSmall),
                contentAlignment = Alignment.Center
            ) {
                DraftoCapsuleSelector(
                    options = capsuleOptions,
                    selectedKey = selectedTab,
                    onOptionSelected = { selectedTab = it },
                    height = BookmarkFormCapsuleHeight,
                    spacing = BookmarkCapsuleSpacing
                )
            }

            Spacer(modifier = Modifier.height(PaddingSmall))

            // Tarjeta de previsualizacion limpia con icono plano monocromatico
            PayloadPreviewCard(payload = payload)

            Spacer(modifier = Modifier.height(BookmarkFormSectionSpacing))

            // Contenido dinamico segun la pestana activa
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "shareTargetTabContent",
                modifier = Modifier.fillMaxWidth()
            ) { tab ->
                when (tab) {
                    ShareTargetTab.NOTE -> {
                        NoteTargetContent(
                            payload = payload,
                            collections = collections,
                            onSaveNote = { title, content, collectionId ->
                                onSaveAsNote(title, content, collectionId)
                                onDismiss()
                            },
                            onCancel = onDismiss
                        )
                    }
                    ShareTargetTab.BOOKMARK -> {
                        BookmarkTargetContent(
                            payload = payload,
                            collections = collections,
                            onSaveBookmark = { url, title, collectionId ->
                                onSaveAsBookmark(url, title, collectionId)
                                onDismiss()
                            },
                            onCancel = onDismiss
                        )
                    }
                }
            }
        }
    }
}

/**
 * Sobrecarga de retrocompatibilidad para llamadas sin parametro [collectionId] en notas.
 */
@Composable
fun DraftoShareTargetBottomSheet(
    payload: SharedPayload,
    onSaveAsNote: (title: String, content: String) -> Unit,
    onSaveAsBookmark: (url: String, title: String?, collectionId: String?) -> Unit,
    onDismiss: () -> Unit,
    collections: List<BookmarkCollection> = emptyList(),
    modifier: Modifier = Modifier
) {
    DraftoShareTargetBottomSheet(
        payload = payload,
        onSaveAsNote = { title, content, _ -> onSaveAsNote(title, content) },
        onSaveAsBookmark = onSaveAsBookmark,
        onDismiss = onDismiss,
        collections = collections,
        modifier = modifier
    )
}

/**
 * Tarjeta de previsualizacion sin contenedores ni burbujas de color detras del icono (Nothing OS 5).
 */
@Composable
private fun PayloadPreviewCard(payload: SharedPayload) {
    val cardBg = DraftoTheme.colors.primaryContainer
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DraftoShapeBookmarkFormInput)
            .background(cardBg)
            .padding(horizontal = PaddingLarge, vertical = PaddingMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PaddingMedium)
    ) {
        when (payload) {
            is SharedPayload.Text -> {
                Icon(
                    imageVector = if (payload.isUrl) Icons.Rounded.Link else Icons.Rounded.Description,
                    contentDescription = null,
                    tint = DraftoTheme.colors.accent,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (payload.isUrl) {
                            try {
                                URI(payload.text).host?.removePrefix("www.") ?: payload.text
                            } catch (_: Exception) {
                                payload.text
                            }
                        } else {
                            stringResource(R.string.share_target_title)
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = payload.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = DraftoTheme.colors.textMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            is SharedPayload.Media -> {
                AsyncImage(
                    model = payload.uriString,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(DraftoShapeCardSmall)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = payload.mimeType.substringAfter("/").uppercase(Locale.ROOT),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!payload.caption.isNullOrBlank()) {
                        Text(
                            text = payload.caption,
                            style = MaterialTheme.typography.bodySmall,
                            color = DraftoTheme.colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            is SharedPayload.MultipleMedia -> {
                Icon(
                    imageVector = Icons.Rounded.Image,
                    contentDescription = null,
                    tint = DraftoTheme.colors.accent,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${payload.uriStrings.size} archivos adjuntos",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Formulario editorial para guardar el contenido recibido como nota en Drafto con soporte de coleccion.
 */
@Composable
private fun NoteTargetContent(
    payload: SharedPayload,
    collections: List<BookmarkCollection> = emptyList(),
    onSaveNote: (title: String, content: String, collectionId: String?) -> Unit,
    onCancel: () -> Unit
) {
    val initialTitle = remember(payload) {
        when (payload) {
            is SharedPayload.Text -> {
                if (payload.isUrl) {
                    try {
                        val host = URI(payload.text).host?.removePrefix("www.")
                        if (!host.isNullOrBlank()) host else "Enlace compartido"
                    } catch (_: Exception) {
                        "Enlace compartido"
                    }
                } else {
                    val firstLine = payload.text.lineSequence().firstOrNull()?.trim()
                    if (!firstLine.isNullOrBlank()) {
                        firstLine.take(40)
                    } else {
                        "Nota compartida"
                    }
                }
            }
            is SharedPayload.Media -> "Adjunto compartido"
            is SharedPayload.MultipleMedia -> "Archivos compartidos"
        }
    }

    var title by remember(initialTitle) { mutableStateOf(initialTitle) }

    val initialContent = remember(payload) {
        when (payload) {
            is SharedPayload.Text -> if (payload.isUrl) "" else payload.text
            is SharedPayload.Media -> {
                val caption = payload.caption.orEmpty()
                if (caption.isNotBlank()) caption else ""
            }
            is SharedPayload.MultipleMedia -> ""
        }
    }
    var content by remember(initialContent) { mutableStateOf(initialContent) }
    var selectedCollectionId by remember { mutableStateOf<String?>(null) }

    val inputBg = DraftoTheme.colors.primaryContainer

    val isDarkTheme = DraftoTheme.colors.isDark
    val noneLabel = stringResource(R.string.bookmark_none_label)
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

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BookmarkFormSectionSpacing)
    ) {
        // 1. Campo de Titulo
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.bookmark_title_label),
                fontFamily = FrauncesFontFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(PaddingSmall))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DraftoShapeBookmarkFormInput)
                    .background(inputBg)
                    .padding(horizontal = PaddingLarge, vertical = PaddingMedium),
                contentAlignment = Alignment.CenterStart
            ) {
                if (title.isEmpty()) {
                    Text(
                        text = stringResource(R.string.note_title_placeholder),
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = DraftoTheme.colors.textMuted
                    )
                }
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    cursorBrush = SolidColor(DraftoTheme.colors.accent),
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 2. Campo de Contenido o Anotaciones adicionales
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.bookmark_content_section_label),
                fontFamily = FrauncesFontFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(PaddingSmall))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DraftoShapeBookmarkFormInput)
                    .background(inputBg)
                    .padding(horizontal = PaddingLarge, vertical = PaddingMedium),
                contentAlignment = Alignment.TopStart
            ) {
                if (content.isEmpty()) {
                    Text(
                        text = if (payload is SharedPayload.Text && payload.isUrl) {
                            "Añade reflexiones o notas sobre el enlace..."
                        } else {
                            stringResource(R.string.note_content_placeholder)
                        },
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DraftoTheme.colors.textMuted
                    )
                }
                BasicTextField(
                    value = content,
                    onValueChange = { content = it },
                    minLines = 3,
                    maxLines = 6,
                    cursorBrush = SolidColor(DraftoTheme.colors.accent),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 3. Selector de Coleccion (Opcional)
        if (collections.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.bookmark_section_collection),
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(PaddingSmall))

                DraftoCapsuleFlowSelector(
                    options = collectionOptions,
                    selectedKey = selectedCollectionId,
                    onOptionSelected = { selectedCollectionId = it },
                    dividersAfterKeys = setOf(null),
                    height = BookmarkFormCapsuleHeight,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(PaddingSmall))

        // 4. Botones simetricos de accion inferior
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = PaddingLarge),
            verticalArrangement = Arrangement.spacedBy(PaddingSmall)
        ) {
            DraftoSheetPrimaryButton(
                text = stringResource(R.string.share_target_save_note),
                onClick = {
                    val finalContent = when (payload) {
                        is SharedPayload.Text -> {
                            if (payload.isUrl) {
                                if (content.isNotBlank()) "${content.trim()}\n\n${payload.text.trim()}" else payload.text.trim()
                            } else {
                                content.trim()
                            }
                        }
                        is SharedPayload.Media -> {
                            val prefix = "[Photo: ${payload.uriString}]"
                            if (content.isNotBlank()) "$prefix\n${content.trim()}" else prefix
                        }
                        is SharedPayload.MultipleMedia -> {
                            val mediaBlock = payload.uriStrings.joinToString("\n") { "[Photo: $it]" }
                            if (content.isNotBlank()) "$mediaBlock\n${content.trim()}" else mediaBlock
                        }
                    }
                    onSaveNote(title.trim().ifBlank { "Nota compartida" }, finalContent, selectedCollectionId)
                },
                enabled = true,
                containerColor = DraftoTheme.colors.emerald,
                contentColor = PureWhite
            )

            DraftoSheetCancelButton(
                onClick = onCancel
            )
        }
    }
}

/**
 * Formulario editorial para guardar el enlace compartido como marcador con paridad total
 * a [DraftoBookmarkFormBottomSheet].
 */
@Composable
private fun BookmarkTargetContent(
    payload: SharedPayload,
    collections: List<BookmarkCollection> = emptyList(),
    onSaveBookmark: (url: String, title: String?, collectionId: String?) -> Unit,
    onCancel: () -> Unit
) {
    val initialUrl = remember(payload) {
        when (payload) {
            is SharedPayload.Text -> payload.text.trim()
            else -> ""
        }
    }
    var urlText by remember(initialUrl) { mutableStateOf(initialUrl) }
    var titleText by remember { mutableStateOf("") }
    var selectedCollectionId by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val inputBg = DraftoTheme.colors.primaryContainer

    val isDarkTheme = DraftoTheme.colors.isDark
    val noneLabel = stringResource(R.string.bookmark_none_label)
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

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BookmarkFormSectionSpacing)
    ) {
        // 1. Campo de Enlace Web (Website link) con boton conmutador Pegar / Limpiar
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.bookmark_section_link),
                fontFamily = FrauncesFontFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(PaddingSmall))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DraftoShapeBookmarkFormInput)
                    .background(inputBg)
                    .padding(start = PaddingLarge, end = PaddingSmall, top = PaddingExtraSmall, bottom = PaddingExtraSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = PaddingSmall),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (urlText.isEmpty()) {
                        Text(
                            text = stringResource(R.string.bookmark_placeholder_link),
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DraftoTheme.colors.textMuted
                        )
                    }
                    BasicTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        singleLine = true,
                        cursorBrush = SolidColor(DraftoTheme.colors.accent),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (urlText.isNotBlank()) TextDecoration.Underline else TextDecoration.None,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Boton conmutador: Limpiar si hay texto, Pegar si esta vacio
                if (urlText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { urlText = "" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.action_cancel),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable {
                                try {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clipText = cm?.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                                    if (!clipText.isNullOrBlank() && LinkMetadataExtractor.isValidUrl(clipText)) {
                                        urlText = clipText
                                    }
                                } catch (_: Exception) {}
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentPaste,
                            contentDescription = stringResource(R.string.bookmark_url_paste),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Indicador de error si la URL no es valida
            val showUrlError = urlText.isNotBlank() && !LinkMetadataExtractor.isValidUrl(urlText)
            AnimatedVisibility(
                visible = showUrlError,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = stringResource(R.string.bookmark_error_invalid_url),
                    style = MaterialTheme.typography.labelSmall,
                    color = DraftoTheme.colors.sunsetCoral,
                    modifier = Modifier.padding(start = PaddingMedium, top = PaddingSmall)
                )
            }
        }

        // 2. Campo de Titulo personalizado (Opcional)
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.bookmark_title_label),
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.bookmark_optional),
                    style = MaterialTheme.typography.labelMedium,
                    color = DraftoTheme.colors.textMuted
                )
            }

            Spacer(modifier = Modifier.height(PaddingSmall))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DraftoShapeBookmarkFormInput)
                    .background(inputBg)
                    .padding(horizontal = PaddingLarge, vertical = PaddingMedium),
                contentAlignment = Alignment.CenterStart
            ) {
                if (titleText.isEmpty()) {
                    Text(
                        text = stringResource(R.string.bookmark_placeholder_title),
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = DraftoTheme.colors.textMuted
                    )
                }
                BasicTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    singleLine = true,
                    cursorBrush = SolidColor(DraftoTheme.colors.accent),
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 3. Selector de Coleccion (Opcional)
        if (collections.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.bookmark_section_collection),
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(PaddingSmall))

                DraftoCapsuleFlowSelector(
                    options = collectionOptions,
                    selectedKey = selectedCollectionId,
                    onOptionSelected = { selectedCollectionId = it },
                    dividersAfterKeys = setOf(null),
                    height = BookmarkFormCapsuleHeight,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(PaddingSmall))

        // 4. Botones simetricos inferiores
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = PaddingLarge),
            verticalArrangement = Arrangement.spacedBy(PaddingSmall)
        ) {
            val isSaveEnabled = LinkMetadataExtractor.isValidUrl(urlText)
            DraftoSheetPrimaryButton(
                text = stringResource(R.string.share_target_save_bookmark),
                onClick = {
                    val trimmedUrl = urlText.trim()
                    if (LinkMetadataExtractor.isValidUrl(trimmedUrl)) {
                        onSaveBookmark(
                            trimmedUrl,
                            titleText.trim().takeIf { it.isNotBlank() },
                            selectedCollectionId
                        )
                    }
                },
                enabled = isSaveEnabled,
                containerColor = DraftoTheme.colors.emerald,
                contentColor = PureWhite
            )

            DraftoSheetCancelButton(
                onClick = onCancel
            )
        }
    }
}
