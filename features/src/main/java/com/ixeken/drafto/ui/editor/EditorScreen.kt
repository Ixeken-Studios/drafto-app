package com.ixeken.drafto.ui.editor

import android.net.Uri
import com.ixeken.drafto.ui.components.DraftoToastManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import com.ixeken.drafto.core.R
import com.ixeken.drafto.domain.model.DraftoCollection
import com.ixeken.drafto.domain.model.Note
import com.ixeken.drafto.ui.components.DraftoConfirmationDialog
import com.ixeken.drafto.ui.components.DraftoDropdownMenuItem
import com.ixeken.drafto.ui.components.DraftoMarkdownContent
import com.ixeken.drafto.ui.components.DraftoMarkdownFormatBar
import com.ixeken.drafto.ui.components.DraftoMenuQuickActionButton
import com.ixeken.drafto.ui.components.DraftoTopBarIconButton
import com.ixeken.drafto.ui.components.DraftoVerticalMenu
import com.ixeken.drafto.ui.components.getCollectionIcon
import com.ixeken.drafto.ui.components.parseCollectionColor
import com.ixeken.drafto.ui.theme.contrastContentColor
import com.ixeken.drafto.ui.theme.CollectionCapsuleDotSize
import com.ixeken.drafto.ui.theme.CollectionCapsulePaddingHorizontal
import com.ixeken.drafto.ui.theme.CollectionCapsulePaddingVertical
import com.ixeken.drafto.ui.theme.CollectionCapsuleSpacing
import com.ixeken.drafto.ui.theme.DraftoShapeCircle
import com.ixeken.drafto.ui.theme.DraftoShapePill
import com.ixeken.drafto.ui.theme.DraftoSprings
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.EditorMinContentHeight
import com.ixeken.drafto.ui.theme.EditorTopBarActionsSpacing
import com.ixeken.drafto.ui.theme.FrauncesFontFamily
import com.ixeken.drafto.ui.theme.HeightTopBar
import com.ixeken.drafto.ui.theme.LocalHazeState
import com.ixeken.drafto.ui.theme.NavBarContextualButtonSize
import com.ixeken.drafto.ui.theme.NavBarContextualIconSize
import com.ixeken.drafto.ui.theme.PaddingExtraSmall
import com.ixeken.drafto.ui.theme.PaddingLarge
import com.ixeken.drafto.ui.theme.PaddingListBottomContent
import com.ixeken.drafto.ui.theme.PaddingMedium
import com.ixeken.drafto.ui.theme.PaddingScreenBottomSpacer
import com.ixeken.drafto.ui.theme.PaddingScreenHorizontal
import com.ixeken.drafto.ui.theme.PaddingScreenTopBarBottom
import com.ixeken.drafto.ui.theme.PaddingScreenTopBarTop
import com.ixeken.drafto.ui.theme.PaddingSmall
import com.ixeken.drafto.ui.theme.PoppinsFontFamily
import com.ixeken.drafto.ui.theme.PureWhite
import com.ixeken.drafto.ui.theme.TodoSubtaskCheckIconSize
import com.ixeken.drafto.ui.theme.VerticalMenuItemIconSize
import com.ixeken.drafto.util.MarkdownNoteParser
import dev.chrisbanes.haze.hazeSource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val NoteDetailDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())

/**
 * Formatea el timestamp para la vista detallada de la nota según el estándar MMM dd, yyyy.
 */
fun formatNoteDetailDate(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val formatted = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(NoteDetailDateFormatter)
    return formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

/**
 * Pantalla principal del editor de notas Markdown con soporte de previsualización renderizada.
 *
 * Características arquitectónicas y de diseño:
 * - Cabecera superior fija con fondo solido monocromatico sin efecto blur.
 * - Conmutación fluida entre modo edición con campos sin bordes y modo lectura Markdown.
 * - Barra flotante de sintaxis Markdown [DraftoMarkdownFormatBar] anclada sobre el teclado.
 * - Auto-guardado transparente en navegación hacia atrás al detectar cambios en el contenido.
 *
 * @param note Nota existente para editar, o null si se crea una nota nueva.
 * @param onSaveNote Callback invocado para persistir la nota con título, contenido y estado de fijado.
 * @param onDeleteNote Callback opcional para eliminar la nota existente.
 * @param onExportMarkdown Callback opcional para exportar la nota individual a un archivo Markdown (.md).
 * @param onBack Callback para retornar a la pantalla anterior.
 * @param modifier Modificador visual para el contenedor raíz.
 */
@Composable
fun EditorScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val defaultUntitledNote = stringResource(R.string.untitled_note)

    val exportMarkdownLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null) {
            viewModel.exportMarkdown(
                uri = uri,
                defaultTitle = defaultUntitledNote,
                onSuccess = {
                    DraftoToastManager.showSuccess(context.getString(R.string.toast_note_exported))
                },
                onError = {
                    DraftoToastManager.showWarning(context.getString(R.string.toast_note_export_error))
                }
            )
        }
    }

    val handleBack = {
        viewModel.saveIfNeeded(defaultTitle = defaultUntitledNote, onSaved = onBack)
    }

    val handleSaveNote = {
        viewModel.saveIfNeeded(defaultTitle = defaultUntitledNote) {
            viewModel.togglePreviewMode()
        }
    }

    BackHandler {
        handleBack()
    }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    EditorContent(
        uiState = uiState,
        onTitleChange = viewModel::onTitleChange,
        onContentChange = viewModel::onContentChange,
        onTogglePin = viewModel::togglePin,
        onSelectCollection = viewModel::selectCollection,
        onTogglePreviewMode = viewModel::togglePreviewMode,
        onToggleOptionsMenu = viewModel::toggleOptionsMenu,
        onDismissOptionsMenu = viewModel::dismissOptionsMenu,
        onSaveNote = handleSaveNote,
        onFormatBold = { viewModel.applyMarkdownFormat("**") },
        onFormatItalic = { viewModel.applyMarkdownFormat("_") },
        onFormatStrikethrough = { viewModel.applyMarkdownFormat("~~") },
        onFormatInlineCode = { viewModel.applyMarkdownFormat("`") },
        onFormatCodeBlock = { viewModel.applyMarkdownFormat("```\n", "\n```") },
        onFormatBulletList = { viewModel.applyLineMarkdownFormat("- ") },
        onFormatQuote = { viewModel.applyLineMarkdownFormat("> ") },
        onExportMarkdown = {
            val currentTitle = uiState.title.text.trim().ifBlank { uiState.initialNote?.title ?: defaultUntitledNote }
            val sanitized = MarkdownNoteParser.sanitizeFileName(currentTitle)
            exportMarkdownLauncher.launch(sanitized)
        },
        onDeleteNote = {
            showDeleteConfirmDialog = true
        },
        onBack = handleBack,
        modifier = modifier
    )

    if (showDeleteConfirmDialog) {
        DraftoConfirmationDialog(
            title = stringResource(R.string.dialog_delete_note_title),
            message = stringResource(R.string.dialog_delete_note_message),
            icon = Icons.Rounded.Delete,
            confirmText = stringResource(R.string.action_delete),
            cancelText = stringResource(R.string.action_cancel),
            isDestructive = true,
            onConfirm = {
                showDeleteConfirmDialog = false
                viewModel.deleteNote(onDeleted = onBack)
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }
}

/**
 * Componente de presentación sin estado (Stateless Presentation) para el editor de notas Markdown.
 *
 * Sigue estrictamente UDF y las directrices de estabilidad Compose para 120 FPS sin recomposiciones accidentales.
 */
@Composable
fun EditorContent(
    uiState: EditorUiState,
    onTitleChange: (TextFieldValue) -> Unit,
    onContentChange: (TextFieldValue) -> Unit,
    onTogglePin: () -> Unit,
    onSelectCollection: (String?) -> Unit,
    onTogglePreviewMode: () -> Unit,
    onToggleOptionsMenu: () -> Unit,
    onDismissOptionsMenu: () -> Unit,
    onSaveNote: () -> Unit,
    onFormatBold: () -> Unit,
    onFormatItalic: () -> Unit,
    onFormatStrikethrough: () -> Unit,
    onFormatInlineCode: () -> Unit,
    onFormatCodeBlock: () -> Unit,
    onFormatBulletList: () -> Unit,
    onFormatQuote: () -> Unit,
    onExportMarkdown: () -> Unit,
    onDeleteNote: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hazeState = LocalHazeState.current
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isDarkTheme = DraftoTheme.colors.isDark
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (hazeState != null) {
                        Modifier.hazeSource(state = hazeState)
                    } else Modifier
                )
                .verticalScroll(scrollState)
                .windowInsetsPadding(
                    WindowInsets.statusBars
                        .union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
                .padding(
                    top = HeightTopBar + PaddingScreenTopBarTop + PaddingSmall,
                    start = PaddingScreenHorizontal,
                    end = PaddingScreenHorizontal,
                    bottom = PaddingListBottomContent
                )
        ) {
            if (!uiState.isPreviewMode) {
                // Modo Edición: Selector horizontal de colecciones primero
                if (uiState.availableCollections.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(PaddingSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isNoneSelected = uiState.selectedCollectionId == null
                        Box(
                            modifier = Modifier
                                .clip(DraftoShapePill)
                                .background(
                                    if (isNoneSelected) MaterialTheme.colorScheme.onSurface else DraftoTheme.colors.primaryContainer
                                )
                                .clickable { onSelectCollection(null) }
                                .padding(
                                    horizontal = CollectionCapsulePaddingHorizontal,
                                    vertical = CollectionCapsulePaddingVertical
                                )
                        ) {
                            Text(
                                text = stringResource(R.string.collection_none),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isNoneSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        uiState.availableCollections.forEach { col ->
                            val isSelected = uiState.selectedCollectionId == col.id
                            val colColor = remember(col.colorHex, isDarkTheme) { parseCollectionColor(col.colorHex, isDark = isDarkTheme) }
                            val colIcon = remember(col.iconName) { getCollectionIcon(col.iconName) }
                            val colContentColor = colColor.contrastContentColor()
                            Row(
                                modifier = Modifier
                                    .clip(DraftoShapePill)
                                    .background(
                                        if (isSelected) colColor else DraftoTheme.colors.primaryContainer
                                    )
                                    .clickable { onSelectCollection(col.id) }
                                    .padding(
                                        horizontal = CollectionCapsulePaddingHorizontal,
                                        vertical = CollectionCapsulePaddingVertical
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(CollectionCapsuleSpacing)
                            ) {
                                Icon(
                                    imageVector = colIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(TodoSubtaskCheckIconSize),
                                    tint = if (isSelected) colContentColor else colColor
                                )
                                Text(
                                    text = col.name,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) colContentColor else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(PaddingMedium))
                }

                // Modo Edición: Título sin bordes con Fraunces Bold
                BasicTextField(
                    value = uiState.title,
                    onValueChange = onTitleChange,
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    cursorBrush = SolidColor(DraftoTheme.colors.accent),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (uiState.title.text.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.note_title_placeholder),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = FrauncesFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = DraftoTheme.colors.textMuted
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(PaddingMedium))

                // Modo Edición: Contenido sin bordes con Poppins
                BasicTextField(
                    value = uiState.content,
                    onValueChange = onContentChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = PoppinsFontFamily,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    cursorBrush = SolidColor(DraftoTheme.colors.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = EditorMinContentHeight),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = EditorMinContentHeight)
                        ) {
                            if (uiState.content.text.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.note_content_placeholder),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = PoppinsFontFamily
                                    ),
                                    color = DraftoTheme.colors.textMuted
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            } else {
                // Modo Previsualización: Fila de colección a la izquierda y fecha a la derecha
                val currentCollection = remember(uiState.selectedCollectionId, uiState.availableCollections) {
                    uiState.availableCollections.find { it.id == uiState.selectedCollectionId }
                }
                val targetTimestamp = remember(uiState.initialNote) {
                    val initial = uiState.initialNote
                    if (initial != null) {
                        if (initial.updatedAt > 0L) initial.updatedAt else initial.createdAt
                    } else {
                        System.currentTimeMillis()
                    }
                }
                val formattedDate = remember(targetTimestamp) { formatNoteDetailDate(targetTimestamp) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentCollection != null) {
                        val colColor = remember(currentCollection.colorHex, isDarkTheme) { parseCollectionColor(currentCollection.colorHex, isDark = isDarkTheme) }
                        val colIcon = remember(currentCollection.iconName) { getCollectionIcon(currentCollection.iconName) }
                        val colContentColor = colColor.contrastContentColor()
                        Row(
                            modifier = Modifier
                                .clip(DraftoShapePill)
                                .background(colColor)
                                .padding(
                                    horizontal = CollectionCapsulePaddingHorizontal,
                                    vertical = CollectionCapsulePaddingVertical
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(CollectionCapsuleSpacing)
                        ) {
                            Icon(
                                imageVector = colIcon,
                                contentDescription = null,
                                modifier = Modifier.size(TodoSubtaskCheckIconSize),
                                tint = colContentColor
                            )
                            Text(
                                text = currentCollection.name,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = colContentColor
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(PaddingExtraSmall))
                    }

                    if (formattedDate.isNotBlank()) {
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = stringResource(R.string.note_last_modified),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Normal
                                ),
                                color = DraftoTheme.colors.textMuted
                            )
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(PaddingMedium))

                // Modo Previsualización: Título renderizado
                if (uiState.title.text.isNotBlank()) {
                    Text(
                        text = uiState.title.text,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(PaddingMedium))
                }

                if (uiState.title.text.isBlank() && uiState.content.text.isBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = PaddingScreenBottomSpacer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.note_preview_empty),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = PoppinsFontFamily),
                            color = DraftoTheme.colors.textMuted
                        )
                    }
                } else {
                    DraftoMarkdownContent(
                        content = uiState.content.text,
                        titleToDeduplicate = uiState.title.text
                    )
                }
            }
        }

        // Encabezado superior solido sin blur
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.statusBars
                            .union(WindowInsets.displayCutout)
                            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                    )
                    .padding(
                        top = PaddingScreenTopBarTop,
                        bottom = PaddingScreenTopBarBottom,
                        start = PaddingScreenHorizontal,
                        end = PaddingScreenHorizontal
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DraftoTopBarIconButton(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    onClick = onBack
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(EditorTopBarActionsSpacing)
                ) {
                    if (uiState.isPreviewMode) {
                        // Conmutador a modo Edición
                        DraftoTopBarIconButton(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.action_edit_mode),
                            onClick = onTogglePreviewMode
                        )

                        // Conmutador de nota fijada con chincheta tintada
                        DraftoTopBarIconButton(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = stringResource(
                                if (uiState.isPinned) R.string.action_unpin_note else R.string.action_pin_note
                            ),
                            tint = if (uiState.isPinned) DraftoTheme.colors.accent else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            onClick = onTogglePin
                        )

                        // Menú contextual para notas existentes o exportación Markdown
                        val canExport = uiState.initialNote != null || uiState.title.text.isNotBlank() || uiState.content.text.isNotBlank()
                        val canDelete = uiState.initialNote != null
                        if (canExport || canDelete) {
                            Box {
                                DraftoTopBarIconButton(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = stringResource(R.string.action_options_menu),
                                    onClick = onToggleOptionsMenu
                                )

                                DraftoVerticalMenu(
                                    expanded = uiState.showOptionsMenu,
                                    onDismissRequest = onDismissOptionsMenu,
                                    quickActions = if (canExport) {
                                        {
                                            DraftoMenuQuickActionButton(
                                                icon = Icons.Rounded.FileDownload,
                                                label = stringResource(R.string.action_export),
                                                contentDescription = stringResource(R.string.action_export_note_md),
                                                onClick = {
                                                    onDismissOptionsMenu()
                                                    onExportMarkdown()
                                                }
                                            )
                                        }
                                    } else null
                                ) {
                                    if (canDelete) {
                                        DraftoDropdownMenuItem(
                                            text = stringResource(R.string.action_delete),
                                            onClick = {
                                                onDismissOptionsMenu()
                                                onDeleteNote()
                                            },
                                            trailingIcon = {
                                                Icon(
                                                    imageVector = Icons.Rounded.Delete,
                                                    contentDescription = null,
                                                    tint = DraftoTheme.colors.sunsetCoral,
                                                    modifier = Modifier.size(VerticalMenuItemIconSize)
                                                )
                                            },
                                            isDestructive = true
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // En modo edición: Pin + Botón circular verde de guardar
                        DraftoTopBarIconButton(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = stringResource(
                                if (uiState.isPinned) R.string.action_unpin_note else R.string.action_pin_note
                            ),
                            tint = if (uiState.isPinned) DraftoTheme.colors.accent else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            onClick = onTogglePin
                        )

                        Box(
                            modifier = Modifier
                                .size(NavBarContextualButtonSize)
                                .clip(DraftoShapeCircle)
                                .background(DraftoTheme.colors.emerald)
                                .clickable(onClick = onSaveNote),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Save,
                                contentDescription = stringResource(R.string.action_save),
                                tint = PureWhite,
                                modifier = Modifier.size(NavBarContextualIconSize)
                            )
                        }
                    }
                }
            }
        }

        // Barra flotante de formato Markdown sobre el teclado en modo edición
        AnimatedVisibility(
            visible = !uiState.isPreviewMode,
            enter = fadeIn(DraftoSprings.SnappyFloat) + slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight },
                animationSpec = DraftoSprings.SnappyOffset
            ),
            exit = fadeOut(DraftoSprings.SnappyFloat) + slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight },
                animationSpec = DraftoSprings.SnappyOffset
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = PaddingScreenHorizontal, vertical = PaddingSmall)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                DraftoMarkdownFormatBar(
                    onFormatBold = onFormatBold,
                    onFormatItalic = onFormatItalic,
                    onFormatStrikethrough = onFormatStrikethrough,
                    onFormatInlineCode = onFormatInlineCode,
                    onFormatCodeBlock = onFormatCodeBlock,
                    onFormatBulletList = onFormatBulletList,
                    onFormatQuote = onFormatQuote,
                    hazeState = hazeState
                )
            }
        }
    }
}



