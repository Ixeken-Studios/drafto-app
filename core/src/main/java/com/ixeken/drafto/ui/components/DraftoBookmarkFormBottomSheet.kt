package com.ixeken.drafto.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.ixeken.drafto.core.R
import com.ixeken.drafto.domain.model.Bookmark
import com.ixeken.drafto.domain.model.BookmarkCollection
import com.ixeken.drafto.util.LinkMetadataExtractor
import com.ixeken.drafto.ui.theme.BookmarkFormButtonHeight
import com.ixeken.drafto.ui.theme.BookmarkFormCapsuleHeight
import com.ixeken.drafto.ui.theme.BookmarkFormSectionSpacing
import com.ixeken.drafto.ui.theme.DraftoShapeBookmarkFormInput
import com.ixeken.drafto.ui.theme.DraftoShapePill
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.FrauncesFontFamily
import com.ixeken.drafto.ui.theme.PaddingLarge
import com.ixeken.drafto.ui.theme.PaddingMedium
import com.ixeken.drafto.ui.theme.PaddingScreenHorizontal
import com.ixeken.drafto.ui.theme.PaddingSmall
import com.ixeken.drafto.ui.theme.PureWhite

/**
 * Modal Bottom Sheet unificado para la creación y edición de marcadores en Drafto.
 *
 * Sigue la estética minimalista y editorial de Nothing OS:
 * - Título centrado en [FrauncesFontFamily] ("New bookmark" / "Edit bookmark").
 * - Sección "Bookmark info" con campo de título y contenedor de descripción expandible con animación suave.
 * - Sección "Website link" con soporte de desplazamiento horizontal, enlace subrayado y botón conmutador (pegar / limpiar).
 * - Sección "Collection" con selector horizontal: píldora "None" anclada fija y colecciones en cuadrícula de hasta 3 filas.
 * - Fila inferior con botones simétricos en píldora: "Cancel" (rojo semántico) y "Save" (verde semántico).
 *
 * @param isEditMode Indica si la hoja modal opera en modo edición ("Edit bookmark") o creación ("New bookmark").
 * @param initialBookmark Instancia opcional del marcador a editar para precargar valores.
 * @param collections Lista de colecciones temáticas creadas por el usuario.
 * @param initialCollectionId Identificador de la colección preseleccionada al abrir la hoja.
 * @param onDismiss Callback invocado al descartar la hoja modal.
 * @param onSave Callback invocado al confirmar el guardado con url, título, descripción y colección.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftoBookmarkFormBottomSheet(
    isEditMode: Boolean = false,
    initialBookmark: Bookmark? = null,
    prefillUrl: String = "",
    prefillTitle: String? = null,
    prefillDescription: String? = null,
    collections: List<BookmarkCollection> = emptyList(),
    initialCollectionId: String? = null,
    onDismiss: () -> Unit,
    onSave: (url: String, title: String?, description: String?, collectionId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var urlText by remember(initialBookmark, prefillUrl) {
        mutableStateOf(initialBookmark?.url ?: prefillUrl)
    }
    var titleText by remember(initialBookmark, prefillTitle) {
        mutableStateOf(initialBookmark?.title ?: prefillTitle.orEmpty())
    }
    var descriptionText by remember(initialBookmark, prefillDescription) {
        mutableStateOf(initialBookmark?.description ?: prefillDescription.orEmpty())
    }
    var selectedCollectionId by remember(initialBookmark, initialCollectionId) {
        mutableStateOf(initialCollectionId)
    }
    var isDescriptionManuallyExpanded by remember { mutableStateOf(false) }

    // Sincronizar metadatos asíncronos cuando se completen en segundo plano
    LaunchedEffect(prefillUrl) {
        if (prefillUrl.isNotBlank() && urlText.isBlank()) {
            urlText = prefillUrl
        }
    }
    LaunchedEffect(prefillTitle) {
        if (!prefillTitle.isNullOrBlank() && titleText.isBlank()) {
            titleText = prefillTitle
        }
    }
    LaunchedEffect(prefillDescription) {
        if (!prefillDescription.isNullOrBlank() && descriptionText.isBlank()) {
            descriptionText = prefillDescription
        }
    }

    // Prellenar automáticamente desde el portapapeles en modo creación si la URL está vacía
    LaunchedEffect(isEditMode, prefillUrl) {
        if (!isEditMode && urlText.isBlank() && prefillUrl.isBlank()) {
            try {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clipText = cm?.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                if (!clipText.isNullOrBlank() && (clipText.startsWith("http://", ignoreCase = true) || clipText.startsWith("https://", ignoreCase = true))) {
                    urlText = clipText
                }
            } catch (_: Exception) {
                // Silencioso
            }
        }
    }

    val isDescriptionVisible = titleText.isNotBlank() || descriptionText.isNotBlank() || isDescriptionManuallyExpanded
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
                title = stringResource(if (isEditMode) R.string.title_edit_bookmark else R.string.title_new_bookmark)
            )

            Spacer(modifier = Modifier.height(PaddingMedium))

            // 1. SECCIÓN: Bookmark info
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.bookmark_section_info),
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.bookmark_optional),
                        style = MaterialTheme.typography.labelMedium,
                        color = DraftoTheme.colors.textMuted,
                        modifier = Modifier
                            .clip(DraftoShapePill)
                            .clickable { isDescriptionManuallyExpanded = !isDescriptionManuallyExpanded }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(PaddingSmall))

                // Campo Título
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(DraftoShapeBookmarkFormInput)
                        .background(inputBg)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
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

                // Campo Descripción (Expansible con animación fluida)
                AnimatedVisibility(
                    visible = isDescriptionVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(PaddingSmall))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(DraftoShapeBookmarkFormInput)
                                .background(inputBg)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            contentAlignment = Alignment.TopStart
                        ) {
                            if (descriptionText.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.bookmark_placeholder_description),
                                    fontFamily = FrauncesFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = DraftoTheme.colors.textMuted
                                )
                            }
                            BasicTextField(
                                value = descriptionText,
                                onValueChange = { descriptionText = it },
                                minLines = 3,
                                maxLines = 5,
                                cursorBrush = SolidColor(DraftoTheme.colors.accent),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = FrauncesFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(BookmarkFormSectionSpacing))

            // 2. SECCIÓN: Website link
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
                        .padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 10.dp),
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

                    // Botón conmutador: Limpiar (X) si hay texto, Pegar si está vacío
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

                // Indicador de error visual cuando el texto no es una URL válida
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

            Spacer(modifier = Modifier.height(BookmarkFormSectionSpacing))

            // 3. SECCIÓN: Collection
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
                    dividersAfterKeys = if (collections.isNotEmpty()) setOf(null) else emptySet(),
                    height = BookmarkFormCapsuleHeight,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(PaddingLarge))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = PaddingLarge),
                verticalArrangement = Arrangement.spacedBy(PaddingSmall)
            ) {
                // Save (Arriba)
                val isSaveEnabled = LinkMetadataExtractor.isValidUrl(urlText)
                DraftoSheetPrimaryButton(
                    text = stringResource(R.string.action_save),
                    onClick = {
                        val trimmedUrl = urlText.trim()
                        if (LinkMetadataExtractor.isValidUrl(trimmedUrl)) {
                            onSave(
                                trimmedUrl,
                                titleText.trim().takeIf { it.isNotBlank() },
                                descriptionText.trim().takeIf { it.isNotBlank() },
                                selectedCollectionId
                            )
                            onDismiss()
                        }
                    },
                    enabled = isSaveEnabled,
                    containerColor = DraftoTheme.colors.emerald,
                    contentColor = PureWhite
                )

                // Cancel (Abajo)
                DraftoSheetCancelButton(
                    onClick = onDismiss
                )
            }
        }
    }
}
